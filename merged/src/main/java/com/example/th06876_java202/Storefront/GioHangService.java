package com.example.th06876_java202.Storefront;

import com.example.th06876_java202.Entity.GiamGia;
import com.example.th06876_java202.Entity.SanPhamChiTiet;
import com.example.th06876_java202.Service.GiamGiaService;
import com.example.th06876_java202.Service.SanPhamChiTietService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nghiệp vụ giỏ hàng: thêm / cập nhật / xoá (luôn kiểm tra tồn kho thực tế),
 * dựng GioHangView đầy đủ (khuyến mãi theo sản phẩm, voucher, phí ship, tổng thanh toán).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GioHangService {

    private final GiaoHangNhanhService giaoHangNhanhService;

    /** Số còn bán được = TỒN KHO THẬT. */
    private static int khaDung(SanPhamChiTiet s) {
        return Math.max(0, s.getSoLuongTon() != null ? s.getSoLuongTon() : 0);
    }

    public static final BigDecimal NGUONG_FREESHIP = BigDecimal.valueOf(500_000);
    public static final BigDecimal PHI_SHIP_MAC_DINH = BigDecimal.valueOf(30_000);

    private final SanPhamChiTietService sanPhamChiTietService;
    private final KhuyenMaiService khuyenMaiService;
    private final GiamGiaService giamGiaService;

    public String themVaoGio(GioHang gioHang, String maSPCT, int soLuong) {
        if (soLuong <= 0) return "Số lượng không hợp lệ.";
        SanPhamChiTiet spct = sanPhamChiTietService.findbyId(maSPCT).orElse(null);
        if (spct == null) return "Sản phẩm không tồn tại.";
        String tt = spct.getTrangThai();
        if ("Ngừng bán".equals(tt) || "Ngừng kinh doanh".equals(tt)) return "Sản phẩm này đã ngừng kinh doanh.";
        if (khaDung(spct) <= 0) return "Sản phẩm đã hết hàng.";

        int daCo = gioHang.getDanhSach().containsKey(maSPCT)
                ? gioHang.getDanhSach().get(maSPCT).getSoLuong() : 0;
        int soLuongMong = soLuong;   // không còn trần cứng — chỉ giới hạn bởi tồn kho
        if (daCo + soLuongMong > khaDung(spct)) {
            int conLai = khaDung(spct) - daCo;
            if (conLai <= 0) return "Bạn đã thêm tối đa số lượng còn lại của sản phẩm này vào giỏ.";
            gioHang.themSanPham(maSPCT, conLai);
            return "Chỉ còn " + conLai + " sản phẩm, đã thêm tối đa có thể vào giỏ hàng.";
        }
        gioHang.themSanPham(maSPCT, soLuongMong);
        return null;
    }

    public String capNhatSoLuong(GioHang gioHang, String maSPCT, int soLuong) {
        if (soLuong <= 0) {
            gioHang.xoaSanPham(maSPCT);
            return null;
        }
        SanPhamChiTiet spct = sanPhamChiTietService.findbyId(maSPCT).orElse(null);
        if (spct == null) {
            gioHang.xoaSanPham(maSPCT);
            return "Sản phẩm không còn tồn tại, đã xoá khỏi giỏ hàng.";
        }
        int soLuongTon = khaDung(spct);
        if (soLuong > soLuongTon) {
            gioHang.capNhatSoLuong(maSPCT, soLuongTon);
            return soLuongTon == 0 ? "Sản phẩm đã hết hàng." : "Chỉ còn " + soLuongTon + " sản phẩm trong kho.";
        }
        gioHang.capNhatSoLuong(maSPCT, soLuong);
        return null;
    }

    /** Dựng toàn cảnh giỏ hàng (KM, voucher, ship) từ dữ liệu MỚI NHẤT trong CSDL. */
    public GioHangView xemGioHang(GioHang gioHang, String maKhachHangDangNhap) {
        return xemGioHang(gioHang, maKhachHangDangNhap, null, null);
    }

    /**
     * Dựng giỏ hàng kèm CƯỚC VẬN CHUYỂN THẬT: nếu biết quận/huyện + phường/xã người
     * nhận (mã GHN) và API đã cấu hình -> gọi Giao Hàng Nhanh lấy cước theo tuyến;
     * ngược lại (chưa chọn địa chỉ / chưa cấu hình / API lỗi) -> dùng biểu phí cố định.
     */
    public GioHangView xemGioHang(GioHang gioHang, String maKhachHangDangNhap,
                                  Integer maQuanHuyenGHN, String maPhuongXaGHN) {
        GioHangView view = new GioHangView();

        for (Map.Entry<String, GioHangItem> e : new LinkedHashMap<>(gioHang.getDanhSach()).entrySet()) {
            String maSPCT = e.getKey();
            int soLuongTrongGio = e.getValue().getSoLuong();

            SanPhamChiTiet spct = sanPhamChiTietService.findbyId(maSPCT).orElse(null);
            CartLineVM line = new CartLineVM();
            line.setMaSanPhamChiTiet(maSPCT);
            line.setSoLuong(soLuongTrongGio);

            if (spct == null) {
                line.setConHopLe(false);
                line.setTenSanPham("(Sản phẩm không còn tồn tại)");
                line.setAnh(SanPhamHienThiService.ANH_MAC_DINH);
                line.setThanhTien(BigDecimal.ZERO);
                view.getCanhBao().add("Một sản phẩm trong giỏ không còn tồn tại và sẽ bị loại bỏ khi đặt hàng.");
                view.getDongHang().add(line);
                continue;
            }

            int soLuongTon = khaDung(spct);
            int soLuongTinh = Math.min(soLuongTrongGio, soLuongTon);
            if (soLuongTinh != soLuongTrongGio) {
                gioHang.capNhatSoLuong(maSPCT, soLuongTinh);
                String ten = spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : maSPCT;
                view.getCanhBao().add("Sản phẩm \"" + ten + "\" chỉ còn " + soLuongTon
                        + ", đã tự điều chỉnh số lượng trong giỏ.");
            }

            int phanTram = khuyenMaiService.phanTramGiamChoBienThe(spct.getSanPham(), maSPCT);
            BigDecimal donGiaGoc = spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO;
            BigDecimal donGia = khuyenMaiService.giaSauGiam(donGiaGoc, phanTram);

            line.setMaSanPham(spct.getSanPham() != null ? spct.getSanPham().getMaSanPham() : null);
            line.setTenSanPham(spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : "");
            line.setTenMauSac(spct.getMauSac() != null ? spct.getMauSac().getTenMauSac() : "");
            line.setTenKichThuoc(spct.getKichThuoc() != null ? spct.getKichThuoc().getTenKichThuoc() : "");
            String anh = spct.getAnhDaiDien();
            line.setAnh((anh != null && !anh.isBlank()) ? "/images/" + anh : SanPhamHienThiService.ANH_MAC_DINH);
            line.setDonGia(donGia);
            line.setDonGiaGoc(donGiaGoc);
            line.setPhanTramGiam(phanTram);
            line.setSoLuong(soLuongTinh);
            line.setSoLuongTon(soLuongTon);
            line.setConHopLe(soLuongTinh > 0);
            line.setThanhTien(donGia.multiply(BigDecimal.valueOf(soLuongTinh)));

            if (soLuongTinh > 0 && phanTram > 0) {
                view.setTietKiemKhuyenMai(view.getTietKiemKhuyenMai()
                        .add(donGiaGoc.subtract(donGia).multiply(BigDecimal.valueOf(soLuongTinh))));
            }
            if (soLuongTinh <= 0) {
                String ten = spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : maSPCT;
                view.getCanhBao().add("Sản phẩm \"" + ten + "\" đã hết hàng và sẽ không được tính khi đặt hàng.");
            }

            view.getDongHang().add(line);
        }

        BigDecimal tongTienHang = view.getDongHang().stream()
                .filter(CartLineVM::isConHopLe)
                .map(CartLineVM::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        view.setTongTienHang(tongTienHang);
        view.setTongSoLuong(view.getDongHang().stream()
                .filter(CartLineVM::isConHopLe).mapToInt(CartLineVM::getSoLuong).sum());

        // Voucher — kiểm tra lại tính hợp lệ mỗi lần xem
        if (gioHang.getMaGiamGiaApDung() != null) {
            GiamGia gg = giamGiaService.getGiamGiaById(gioHang.getMaGiamGiaApDung()).orElse(null);
            String loi = giamGiaService.kiemTraVoucherHopLe(gg, maKhachHangDangNhap, tongTienHang);
            if (loi != null) {
                gioHang.setMaGiamGiaApDung(null);
                view.getCanhBao().add("Mã giảm giá đã áp dụng không còn hợp lệ: " + loi);
            } else {
                view.setVoucherApDung(gg);
                view.setSoTienGiamVoucher(giamGiaService.tinhSoTienGiam(gg, tongTienHang));
            }
        }

        BigDecimal tienSauVoucher = tongTienHang.subtract(view.getSoTienGiamVoucher());
        boolean coHang = view.getDongHang().stream().anyMatch(CartLineVM::isConHopLe);
        if (coHang && tienSauVoucher.compareTo(NGUONG_FREESHIP) < 0) {
            Integer phiGHN = giaoHangNhanhService.tinhPhi(
                    maQuanHuyenGHN, maPhuongXaGHN,
                    view.getTongSoLuong(),
                    tienSauVoucher.longValue());
            if (phiGHN != null) {
                view.setTienShip(BigDecimal.valueOf(phiGHN));
                view.setPhiShipGoc(BigDecimal.valueOf(phiGHN));
                view.setNguonPhiShip("GHN");
            } else {
                view.setTienShip(PHI_SHIP_MAC_DINH);
                view.setNguonPhiShip("CO_DINH");
            }
            view.setConThieuDeFreeship(NGUONG_FREESHIP.subtract(tienSauVoucher));
        } else {
            view.setTienShip(BigDecimal.ZERO);
            view.setNguonPhiShip("FREESHIP");
            view.setConThieuDeFreeship(BigDecimal.ZERO);
            if (coHang) {
                Integer phiGoc = giaoHangNhanhService.tinhPhi(
                        maQuanHuyenGHN, maPhuongXaGHN,
                        view.getTongSoLuong(),
                        tienSauVoucher.longValue());
                if (phiGoc != null) view.setPhiShipGoc(BigDecimal.valueOf(phiGoc));
            }
        }

        BigDecimal tongThanhToan = tienSauVoucher.add(view.getTienShip());
        if (tongThanhToan.compareTo(BigDecimal.ZERO) < 0) tongThanhToan = BigDecimal.ZERO;
        view.setTongThanhToan(tongThanhToan);

        return view;
    }

    public String apDungVoucher(GioHang gioHang, String maKhachHangDangNhap, String maVoucher) {
        System.out.println("=== SERVICE: apDungVoucher ===");
        System.out.println("1️⃣ tenVoucher: '" + maVoucher + "'");
        System.out.println("2️⃣ maKhachHang: '" + maKhachHangDangNhap + "'");
        System.out.println("3️⃣ gioHang: " + (gioHang != null ? "không null" : "NULL!"));

        if (maVoucher == null || maVoucher.isBlank()) {
            System.out.println("❌ Lỗi: voucher rỗng");
            return "Vui lòng nhập mã giảm giá.";
        }

        String trimmed = maVoucher.trim();
        System.out.println("4️⃣ Đang tìm voucher: '" + trimmed + "'");

        GiamGia gg = giamGiaService.findByMa(trimmed).orElse(null);

        if (gg == null) {
            System.out.println("   ⚠️ Không thấy theo mã, thử theo TÊN chương trình...");
            gg = giamGiaService.findByTen(trimmed).orElse(null);
        }
        if (gg == null) {
            System.out.println("   ⚠️ Vẫn chưa thấy, thử khớp GẦN ĐÚNG...");
            gg = giamGiaService.timGanDung(trimmed).orElse(null);
        }

        if (gg == null) {
            System.out.println("❌ Không tìm thấy voucher '" + trimmed + "'");
            return "Mã giảm giá không tồn tại.";
        }

        System.out.println("5️⃣ Tìm thấy voucher:");
        System.out.println("   - Mã: " + gg.getMaGiamGia());
        System.out.println("   - Tên: " + gg.getTenGiamGia());
        System.out.println("   - Loại áp dụng: " + gg.getLoaiApDung());
        System.out.println("   - Số lượng: " + gg.getSoLuong());
        System.out.println("   - Trạng thái: " + gg.getTrangThai());
        System.out.println("   - Đơn tối thiểu: " + gg.getDonToiThieu());

        GioHangView tam = xemGioHang(gioHang, maKhachHangDangNhap);
        System.out.println("6️⃣ Tổng tiền hàng: " + tam.getTongTienHang());

        String loi = giamGiaService.kiemTraVoucherHopLe(gg, maKhachHangDangNhap, tam.getTongTienHang());
        System.out.println("7️⃣ Kết quả kiểm tra: " + (loi == null ? "HỢP LỆ" : "LỖI: " + loi));

        if (loi != null) return loi;

        gioHang.setMaGiamGiaApDung(gg.getMaGiamGia());
        System.out.println("✅ Áp dụng thành công! Mã voucher: " + gg.getMaGiamGia());
        return null;
    }

    public void boVoucher(GioHang gioHang) {
        gioHang.setMaGiamGiaApDung(null);
    }
}
