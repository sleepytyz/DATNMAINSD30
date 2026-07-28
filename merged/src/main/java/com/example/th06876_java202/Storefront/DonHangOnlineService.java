package com.example.th06876_java202.Storefront;

import com.example.th06876_java202.Entity.*;
import com.example.th06876_java202.Repository.HoaDonRepo;
import com.example.th06876_java202.Repository.SanPhamChiTietRepository;
import com.example.th06876_java202.Service.GiamGiaService;
import com.example.th06876_java202.Service.HoaDonChiTietService;
import com.example.th06876_java202.Service.SanPhamChiTietService;
import com.example.th06876_java202.realtime.ThongBaoRealtimeService;
import com.example.th06876_java202.realtime.TrangThaiModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Xử lý đặt hàng online — TRÁI TIM của kết nối Website bán hàng ⇄ Quản lý bán hàng:
 *  1. Kiểm tra lại tồn kho & giá tại đúng thời điểm đặt (khoá dòng biến thể bằng
 *     PESSIMISTIC_WRITE để 2 khách đặt cùng lúc không thể bán vượt tồn).
 *  2. Tạo HoaDon (LoaiBan = "Online"; COD -> "Chờ xác nhận", Chuyển khoản -> "Chờ thanh toán").
 *     KHÔNG trừ tồn kho ở bước này — chỉ trừ khi Quản lý bấm "Xác nhận" (xacNhanDonTruTonKho).
 *  3. Phát thông báo THỜI GIAN THỰC (sau khi commit) để màn Quản lý đơn hàng nhận
 *     "hoá đơn chờ xác nhận" ngay lập tức kèm số lượng, tổng tiền; đồng thời cảnh báo
 *     tồn kho và cập nhật bảng trạng thái module.
 */
@Service
@RequiredArgsConstructor
public class DonHangOnlineService {

    private final HoaDonRepo hoaDonRepo;
    private final HoaDonChiTietService hoaDonChiTietService;
    private final SanPhamChiTietService sanPhamChiTietService;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final KhuyenMaiService khuyenMaiService;
    private final GiamGiaService giamGiaService;
    private final GiaoHangNhanhService giaoHangNhanhService;
    private final ThongBaoRealtimeService thongBaoRealtimeService;
    private final TrangThaiModuleService trangThaiModuleService;
    private final com.example.th06876_java202.Service.EmailService emailService;

    /** Số còn bán được = TỒN KHO THẬT (không còn cơ chế giữ chỗ). */
    private static int khaDung(SanPhamChiTiet s) {
        return Math.max(0, s.getSoLuongTon() != null ? s.getSoLuongTon() : 0);
    }

    @Transactional
    public HoaDon datHang(KhachHang khachHang, GioHang gioHang, String diaChiGiaoHang,
                          String phuongThucThanhToan, String ghiChu) {
        return datHang(khachHang, gioHang, diaChiGiaoHang, phuongThucThanhToan, ghiChu, null, null);
    }

    /**
     * Đặt hàng kèm mã địa giới Giao Hàng Nhanh của người nhận để chốt CƯỚC THẬT
     * vào hoá đơn (khớp đúng số tiền khách nhìn thấy ở trang thanh toán).
     */
    public HoaDon datHang(KhachHang khachHang, GioHang gioHang, String diaChiGiaoHang,
                          String phuongThucThanhToan, String ghiChu,
                          Integer maQuanHuyenGHN, String maPhuongXaGHN) {
        return datHang(khachHang, gioHang, diaChiGiaoHang, phuongThucThanhToan, ghiChu,
                maQuanHuyenGHN, maPhuongXaGHN, null, null);
    }

    /**
     * Đặt hàng — bản đầy đủ: kèm mã địa giới GHN (chốt cước thật) và TÊN + SĐT NGƯỜI NHẬN
     * (lưu vào hoá đơn để hiển thị đúng người nhận, có thể khác chủ tài khoản).
     */
    @Transactional
    public HoaDon datHang(KhachHang khachHang, GioHang gioHang, String diaChiGiaoHang,
                          String phuongThucThanhToan, String ghiChu,
                          Integer maQuanHuyenGHN, String maPhuongXaGHN,
                          String tenNguoiNhan, String sdtNguoiNhan) {

        if (gioHang == null || gioHang.isEmpty()) {
            throw new DatHangException("Giỏ hàng của bạn đang trống.");
        }
        if (diaChiGiaoHang == null || diaChiGiaoHang.isBlank()) {
            throw new DatHangException("Vui lòng chọn hoặc nhập địa chỉ giao hàng.");
        }

        List<HoaDonChiTiet> dsChiTiet = new ArrayList<>();
        List<SanPhamChiTiet> dsCanLuuLaiTon = new ArrayList<>();
        BigDecimal tongTienHang = BigDecimal.ZERO;

        // Bản sao để tránh ConcurrentModification khi đọc qua session bean
        Map<String, GioHangItem> banSao = new LinkedHashMap<>(gioHang.getDanhSach());

        for (Map.Entry<String, GioHangItem> entry : banSao.entrySet()) {
            String maSPCT = entry.getKey();
            int soLuong = entry.getValue().getSoLuong();

            // KHOÁ dòng biến thể tới khi giao dịch kết thúc — chống 2 đơn cùng lúc bán vượt tồn
            SanPhamChiTiet spct = sanPhamChiTietRepository.khoaBienTheDeDatHang(maSPCT)
                    .orElseThrow(() -> new DatHangException(
                            "Một sản phẩm trong giỏ hàng không còn tồn tại, vui lòng kiểm tra lại giỏ hàng."));

            String tenSp = (spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : "Sản phẩm");
            String ttBienThe = spct.getTrangThai();
            if ("Ngừng bán".equals(ttBienThe) || "Ngừng kinh doanh".equals(ttBienThe)) {
                throw new DatHangException("Sản phẩm \"" + tenSp + "\" đã ngừng kinh doanh. Vui lòng xoá khỏi giỏ hàng.");
            }

            int soKhaDung = khaDung(spct);
            if (soKhaDung < soLuong) {
                // Kiểm tra SỚM để báo cho khách ngay khi hàng đã hết tại thời điểm đặt.
                // Lưu ý: đây CHƯA phải chốt chặn cuối — tồn kho chỉ bị trừ khi thanh toán
                // thành công / quầy xác nhận, nên người hoàn tất TRƯỚC mới là người giữ được hàng.
                throw new DatHangException("Rất tiếc, sản phẩm \"" + tenSp + "\" hiện chỉ còn "
                        + soKhaDung + " sản phẩm. Vui lòng cập nhật lại giỏ hàng.");
            }

            int phanTramGiam = khuyenMaiService.phanTramGiamChoBienThe(spct.getSanPham(), maSPCT);
            BigDecimal donGiaGoc = spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO;
            BigDecimal donGiaSauGiam = khuyenMaiService.giaSauGiam(donGiaGoc, phanTramGiam);
            BigDecimal tienGiamDong = donGiaGoc.subtract(donGiaSauGiam).multiply(BigDecimal.valueOf(soLuong));
            BigDecimal thanhTienDong = donGiaSauGiam.multiply(BigDecimal.valueOf(soLuong));

            HoaDonChiTiet ct = new HoaDonChiTiet();
            ct.setSanPhamChiTiet(spct);
            ct.setSoLuong(soLuong);
            ct.setDonGia(donGiaGoc);
            ct.setTienGiam(tienGiamDong);
            ct.setThanhTien(thanhTienDong);
            dsChiTiet.add(ct);

            tongTienHang = tongTienHang.add(thanhTienDong);

            // KHÔNG giữ chỗ, KHÔNG trừ tồn ở bước đặt: hàng vẫn mở cho mọi khách.
            // Ai THANH TOÁN XONG TRƯỚC (hoặc được quầy xác nhận trước) thì trừ kho và
            // giữ được hàng; người sau nếu kho đã hết sẽ bị huỷ đơn + nhận email xin lỗi.
            dsCanLuuLaiTon.add(spct);
        }

        // Áp dụng voucher (nếu có) — kiểm tra lại lần cuối cho chắc chắn
        GiamGia voucher = null;
        BigDecimal soTienGiamVoucher = BigDecimal.ZERO;
        String maKhachHang = khachHang != null ? khachHang.getMaKH() : null;
        if (gioHang.getMaGiamGiaApDung() != null) {
            GiamGia gg = giamGiaService.getGiamGiaById(gioHang.getMaGiamGiaApDung()).orElse(null);
            String loi = giamGiaService.kiemTraVoucherHopLe(gg, maKhachHang, tongTienHang);
            if (loi == null) {
                voucher = gg;
                soTienGiamVoucher = giamGiaService.tinhSoTienGiam(gg, tongTienHang);
            }
        }

        BigDecimal tienSauVoucher = tongTienHang.subtract(soTienGiamVoucher);
        // CƯỚC VẬN CHUYỂN: ưu tiên cước THẬT từ Giao Hàng Nhanh theo tuyến người nhận;
        // chưa cấu hình API / chưa chọn địa chỉ chuẩn / API lỗi -> biểu phí cố định.
        BigDecimal tienShip = BigDecimal.ZERO;
        if (tienSauVoucher.compareTo(GioHangService.NGUONG_FREESHIP) < 0) {
            int tongSoLuongDat = banSao.values().stream()
                    .mapToInt(it -> it.getSoLuong() != null ? it.getSoLuong() : 0).sum();
            Integer phiGHN = giaoHangNhanhService.tinhPhi(maQuanHuyenGHN, maPhuongXaGHN,
                    tongSoLuongDat, tienSauVoucher.longValue());
            tienShip = phiGHN != null ? BigDecimal.valueOf(phiGHN) : GioHangService.PHI_SHIP_MAC_DINH;
        }
        BigDecimal tongThanhToan = tienSauVoucher.add(tienShip);
        if (tongThanhToan.compareTo(BigDecimal.ZERO) < 0) tongThanhToan = BigDecimal.ZERO;

        HoaDon hoaDon = new HoaDon();
        hoaDon.setMaHoaDon(taoMaHoaDon());
        hoaDon.setMaKhachHang(khachHang);
        hoaDon.setMaNhanVien(null); // khách tự đặt online, chưa có nhân viên xử lý
        hoaDon.setMaGiamGia(voucher);
        hoaDon.setTongTien(tongThanhToan);
        hoaDon.setTienShip(tienShip);
        hoaDon.setPhuongThucThanhToan(phuongThucThanhToan);
        // NGHIỆP VỤ THANH TOÁN:
        //  - COD: đơn vào thẳng hàng đợi "Chờ xác nhận" của quầy quản lý.
        //  - Chuyển khoản: đơn ở "Chờ thanh toán" — CHƯA coi là chốt; chỉ khi khách bấm
        //    "Tôi đã chuyển khoản" (hoặc quầy đối soát) đơn mới chuyển "Đã xác nhận" + trừ kho.
        String ptThuong = phuongThucThanhToan != null ? phuongThucThanhToan.toLowerCase() : "";
        // Đơn TRẢ TRƯỚC (VNPay hoặc chuyển khoản VietQR) khởi tạo ở "Chờ thanh toán":
        //  - VNPay: cổng báo thành công -> tự "Đã xác nhận" + trừ kho.
        //  - VietQR: khách bấm "Tôi đã chuyển khoản" -> "Chờ xác nhận" (CHƯA trừ kho)
        //    để quầy ĐỐI SOÁT sao kê; quầy bấm Xác nhận -> "Đã xác nhận" + trừ kho.
        boolean thanhToanOnline = ptThuong.contains("chuyển khoản") || ptThuong.contains("vnpay");
        hoaDon.setTrangThai(thanhToanOnline ? "Chờ thanh toán" : "Chờ xác nhận");
        hoaDon.setGhiChu(ghiChu);
        hoaDon.setNgayTao(LocalDateTime.now());
        hoaDon.setLoaiBan("Online");
        hoaDon.setDiaChiGiaoHang(diaChiGiaoHang);
        // Tên + SĐT người nhận (nếu trống thì lấy theo chủ tài khoản) — để hoá đơn hiển thị đúng
        hoaDon.setTenNguoiNhan(tenNguoiNhan != null && !tenNguoiNhan.isBlank()
                ? tenNguoiNhan.trim()
                : (khachHang != null ? khachHang.getHoTen() : null));
        hoaDon.setSdtNguoiNhan(sdtNguoiNhan != null && !sdtNguoiNhan.isBlank()
                ? sdtNguoiNhan.trim()
                : (khachHang != null ? khachHang.getSdt() : null));

        HoaDon hoaDonDaLuu = hoaDonRepo.save(hoaDon);

        for (HoaDonChiTiet ct : dsChiTiet) {
            ct.setMaHoaDon(hoaDonDaLuu);
        }
        hoaDonChiTietService.luuTatCa(dsChiTiet);

        // (Không lưu lại tồn kho ở đây — tồn chỉ đổi khi trừ kho lúc xác nhận/thanh toán)

        if (voucher != null) {
            giamGiaService.giamSoLuongVoucher(voucher.getMaGiamGia());
            giamGiaService.danhDauDaSuDungChoKhachHang(maKhachHang, voucher.getMaGiamGia());
        }

        gioHang.xoaTatCa();

        // ===== THỜI GIAN THỰC: đẩy "hoá đơn chờ xác nhận" + cảnh báo tồn kho sang Quản lý =====
        // (Các thông điệp chỉ được gửi SAU KHI transaction này commit thành công)
        thongBaoRealtimeService.donHangOnlineMoi(hoaDonDaLuu, dsChiTiet);
        for (SanPhamChiTiet spct : dsCanLuuLaiTon) {
            thongBaoRealtimeService.kiemTraVaCanhBaoTonKho(spct);
        }
        trangThaiModuleService.phatNgay();

        // ===== EMAIL xác nhận đặt hàng (gửi SAU KHI đơn đã lưu chắc chắn) =====
        guiThuXacNhanDatHang(hoaDonDaLuu);

        return hoaDonDaLuu;
    }

    /**
     * ĐẶT LỊCH gửi THƯ XIN LỖI khi đơn bị huỷ vì hết hàng (khách khác thanh toán trước).
     *
     * Hai điều quan trọng:
     *  1) Thư chỉ gửi SAU KHI giao dịch huỷ đơn commit — mọi trục trặc email không thể
     *     làm hỏng việc huỷ đơn (tránh lỗi "Transaction silently rolled back").
     *  2) Mọi dữ liệu được đọc ra THÀNH CHUỖI NGAY BÂY GIỜ, khi entity còn gắn phiên
     *     Hibernate. Nếu để tới sau commit mới đọc quan hệ (khách hàng, chi tiết đơn)
     *     thì phiên đã đóng, dữ liệu LAZY không nạp được và thư lặng lẽ không được gửi.
     */
    private void guiThuXinLoi(HoaDon hoaDon) {
        try {
            if (hoaDon == null || hoaDon.getMaKhachHang() == null) {
                System.out.println("[EMAIL] Đơn huỷ không có thông tin khách — bỏ qua thư xin lỗi.");
                return;
            }
            final String maHD = hoaDon.getMaHoaDon();
            final String email = hoaDon.getMaKhachHang().getEmail();
            if (email == null || email.isBlank()) {
                System.out.println("[EMAIL] Đơn " + maHD + " bị huỷ nhưng khách không có email.");
                return;
            }
            final String hoTen = hoaDon.getMaKhachHang().getHoTen();
            final String phuongThuc = hoaDon.getPhuongThucThanhToan();

            String ten = null;
            try {
                java.util.List<HoaDonChiTiet> ds = hoaDonChiTietService.findByHoaDOn(hoaDon);
                if (ds != null && !ds.isEmpty()) {
                    SanPhamChiTiet spct = ds.get(0).getSanPhamChiTiet();
                    if (spct != null && spct.getSanPham() != null) {
                        ten = spct.getSanPham().getTenSanPham();
                        if (ds.size() > 1) ten += " (và " + (ds.size() - 1) + " sản phẩm khác)";
                    }
                }
            } catch (Exception bqua) {
                System.err.println("[EMAIL] Không đọc được tên sản phẩm: " + bqua.getMessage());
            }
            final String tenSp = ten;

            Runnable gui = () -> emailService.guiEmailXinLoiHetHang(email, hoTen, maHD, tenSp, phuongThuc);

            if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                gui.run();
                            }
                        });
            } else {
                gui.run();
            }
        } catch (Exception ex) {
            System.err.println("[EMAIL] Bỏ qua lỗi chuẩn bị thư xin lỗi: " + ex.getMessage());
        }
    }

    /**
     * Đặt lịch gửi THƯ XÁC NHẬN ĐẶT HÀNG — chỉ gửi sau khi giao dịch tạo đơn commit.
     * Mọi thông tin được lấy ra thành chuỗi NGAY BÂY GIỜ (khi entity còn gắn với phiên),
     * nên lúc gửi thư không cần truy vấn lại CSDL — vừa nhanh vừa không thể gây lỗi.
     */
    private void guiThuXacNhanDatHang(HoaDon hoaDon) {
        try {
            if (hoaDon == null || hoaDon.getMaKhachHang() == null) return;
            final String email = hoaDon.getMaKhachHang().getEmail();
            if (email == null || email.isBlank()) return;

            final String hoTen = hoaDon.getMaKhachHang().getHoTen();
            final String maHD = hoaDon.getMaHoaDon();
            final java.math.BigDecimal tongTien = hoaDon.getTongTien();
            final String phuongThuc = hoaDon.getPhuongThucThanhToan();
            final String trangThai = hoaDon.getTrangThai();
            final String diaChi = hoaDon.getDiaChiGiaoHang();

            Runnable gui = () -> emailService.guiEmailDatHangThanhCong(
                    email, hoTen, maHD, tongTien, phuongThuc, trangThai, diaChi);

            if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                gui.run();
                            }
                        });
            } else {
                gui.run();
            }
        } catch (Exception ex) {
            System.err.println("[EMAIL] Bỏ qua lỗi chuẩn bị thư xác nhận đặt hàng: " + ex.getMessage());
        }
    }

    /**
     * Sinh mã hoá đơn duy nhất cho đơn online, dạng "HDyyMMddHHmmss" + hậu tố nếu trùng,
     * vừa khít cột MaHoaDon VARCHAR(20).
     */
    private String taoMaHoaDon() {
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss");
        String base = "HD" + LocalDateTime.now().format(fmt);
        String ma = base;
        int suffix = 0;
        while (hoaDonRepo.existsById(ma)) {
            suffix++;
            ma = base + suffix;
            if (ma.length() > 20) {
                ma = "HD" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
                if (ma.length() > 20) ma = ma.substring(0, 20);
            }
        }
        return ma;
    }

    /**
     * QUẢN LÝ XÁC NHẬN ĐƠN — chính là lúc TRỪ TỒN KHO.
     * Chờ xác nhận -> Đã xác nhận. Khoá từng biến thể (PESSIMISTIC_WRITE) rồi kiểm tra
     * & trừ tồn kho. Nếu bất kỳ sản phẩm nào không đủ hàng, toàn bộ giao dịch rollback
     * và đơn KHÔNG được xác nhận (ném DatHangException để tầng gọi báo lỗi).
     *
     * @return HoaDon sau khi đã chuyển sang "Đã xác nhận".
     */
    @Transactional
    public HoaDon xacNhanDonTruTonKho(HoaDon hoaDon) {
        if (hoaDon == null) {
            throw new DatHangException("Không tìm thấy đơn hàng.");
        }
        // TẦNG KHOÁ 1 — KHOÁ DÒNG ĐƠN: hai request cùng xử lý MỘT đơn (Return + IPN
        // của VNPay, hay 2 nhân viên cùng bấm Xác nhận) phải xếp hàng; request vào sau
        // đọc trạng thái đã đổi -> bị guard chặn -> KHÔNG BAO GIỜ trừ kho 2 lần.
        hoaDon = hoaDonRepo.khoaDonDeXuLy(hoaDon.getMaHoaDon())
                .orElseThrow(() -> new DatHangException("Không tìm thấy đơn hàng."));
        String ttHienTai = hoaDon.getTrangThai();
        if (!"Chờ xác nhận".equals(ttHienTai) && !"Chờ thanh toán".equals(ttHienTai)) {
            throw new DatHangException("Đơn ở trạng thái \"" + ttHienTai + "\" không thể xác nhận.");
        }

        List<HoaDonChiTiet> dsChiTiet = hoaDonChiTietService.findByHoaDOn(hoaDon);
        List<SanPhamChiTiet> dsDaTru = new ArrayList<>();

        for (HoaDonChiTiet ct : dsChiTiet) {
            SanPhamChiTiet ctSpct = ct.getSanPhamChiTiet();
            if (ctSpct == null || ctSpct.getMaSanPhamChiTiet() == null) continue;
            int soLuong = ct.getSoLuong() != null ? ct.getSoLuong() : 0;

            // TẦNG KHOÁ 2 — KHOÁ BIẾN THỂ: hai ĐƠN KHÁC NHAU giành cùng lượng tồn
            // phải xếp hàng; đơn vào sau đọc tồn đã trừ -> thiếu là rollback toàn bộ.
            SanPhamChiTiet spct = sanPhamChiTietRepository
                    .khoaBienTheDeDatHang(ctSpct.getMaSanPhamChiTiet())
                    .orElseThrow(() -> new DatHangException(
                            "Một sản phẩm trong đơn không còn tồn tại, không thể xác nhận đơn."));

            String tenSp = (spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : "Sản phẩm");
            int ton = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
            if (ton < soLuong) {
                // rollback toàn bộ: đơn giữ nguyên trạng thái trước đó
                throw new DatHangException("Không đủ tồn kho để xác nhận: sản phẩm \"" + tenSp
                        + "\" chỉ còn " + ton + " (đơn cần " + soLuong + "). Vui lòng nhập thêm hàng.");
            }

            spct.setSoLuongTon(ton - soLuong);
            sanPhamChiTietService.capNhatTrangThaii(spct);
            dsDaTru.add(spct);
        }

        for (SanPhamChiTiet spct : dsDaTru) {
            sanPhamChiTietService.them(spct);
        }

        String trangThaiCu = hoaDon.getTrangThai();
        hoaDon.setTrangThai("Đã xác nhận");
        hoaDonRepo.save(hoaDon);

        // Realtime: KHÔNG đẩy thông báo lên bảng của quản lý (trang bán hàng đã tự hiện
        // thông báo khi nhân viên bấm Xác nhận) — chỉ cập nhật trang khách + gửi email khách.
        thongBaoRealtimeService.trangThaiDonThayDoiKhongBaoQuanLy(hoaDon, trangThaiCu, "Quản lý bán hàng");
        for (SanPhamChiTiet spct : dsDaTru) {
            thongBaoRealtimeService.kiemTraVaCanhBaoTonKho(spct);
        }
        trangThaiModuleService.phatNgay();

        return hoaDon;
    }

    /**
     * KHÁCH BÁO ĐÃ CHUYỂN KHOẢN (đơn VietQR): Chờ thanh toán -> Chờ xác nhận.
     * CHƯA trừ kho, CHƯA coi là đã thanh toán chắc chắn — chỉ ghi thời điểm khách báo
     * và đưa đơn vào hàng đợi để QUẦY ĐỐI SOÁT sao kê; quầy bấm Xác nhận mới trừ kho.
     */
    @Transactional
    public HoaDon khachXacNhanChuyenKhoan(HoaDon hoaDon, KhachHang khach) {
        if (hoaDon == null || khach == null || hoaDon.getMaKhachHang() == null
                || !khach.getMaKH().equals(hoaDon.getMaKhachHang().getMaKH())) {
            throw new DatHangException("Đơn hàng không hợp lệ.");
        }
        String pt = hoaDon.getPhuongThucThanhToan();
        if (pt == null || !pt.toLowerCase().contains("chuyển khoản")) {
            throw new DatHangException("Đơn này không thanh toán bằng chuyển khoản.");
        }
        if (!"Chờ thanh toán".equals(hoaDon.getTrangThai())) {
            throw new DatHangException("Đơn đã được xử lý thanh toán trước đó.");
        }
        // Chỉ BÁO đã chuyển khoản: KHÔNG trừ kho, KHÔNG tự xác nhận — đơn chuyển
        // "Chờ xác nhận" để quầy đối soát sao kê rồi mới duyệt (lúc đó mới trừ kho).
        ghiNhanChuyenKhoanChoXuLy(hoaDon);
        return hoaDon;
    }

    /**
     * CỔNG THANH TOÁN (VNPay...) BÁO THÀNH CÔNG — chữ ký đã được kiểm ở tầng gọi.
     * Chờ thanh toán -> Đã xác nhận + TRỪ TỒN KHO. Thiếu hàng -> ném DatHangException
     * để tầng gọi chuyển đơn sang hàng đợi quầy xử lý (ghiNhanChuyenKhoanChoXuLy).
     */
    @Transactional
    public HoaDon thanhToanOnlineThanhCong(HoaDon hoaDon) {
        if (hoaDon == null) {
            throw new DatHangException("Không tìm thấy đơn hàng.");
        }
        // KHOÁ DÒNG ĐƠN + đọc trạng thái MỚI NHẤT ngay trong transaction
        HoaDon donKhoa = hoaDonRepo.khoaDonDeXuLy(hoaDon.getMaHoaDon())
                .orElseThrow(() -> new DatHangException("Không tìm thấy đơn hàng."));
        if (!"Chờ thanh toán".equals(donKhoa.getTrangThai())) {
            throw new DatHangException("Đơn không ở trạng thái chờ thanh toán.");
        }
        donKhoa.setNgayThanhToan(java.time.LocalDateTime.now());
        return xacNhanDonTruTonKho(donKhoa);
    }

    /**
     * GHI NHẬN KHÁCH BÁO ĐÃ CHUYỂN KHOẢN: lưu thời điểm báo và chuyển đơn sang
     * "Chờ xác nhận" (CHƯA trừ kho) để quầy đối soát sao kê rồi mới duyệt.
     * Idempotent: khoá dòng đơn — bấm trùng/chạy song song chỉ ghi nhận MỘT lần.
     */
    @Transactional
    public void ghiNhanChuyenKhoanChoXuLy(HoaDon hoaDon) {
        if (hoaDon == null) return;
        hoaDon = hoaDonRepo.khoaDonDeXuLy(hoaDon.getMaHoaDon()).orElse(null);
        if (hoaDon == null || !"Chờ thanh toán".equals(hoaDon.getTrangThai())) return;
        hoaDon.setNgayThanhToan(java.time.LocalDateTime.now());
        hoaDon.setTrangThai("Chờ xác nhận");
        hoaDonRepo.save(hoaDon);
        thongBaoRealtimeService.trangThaiDonThayDoi(hoaDon, "Chờ thanh toán", "Khách hàng (đã chuyển khoản)");
        trangThaiModuleService.phatNgay();
    }

    /**
     * THANH TOÁN CỔNG THÀNH CÔNG NHƯNG HẾT HÀNG (2 khách cùng thanh toán lượng tồn cuối):
     * người thanh toán SAU không thể được xác nhận — đơn bị HUỶ ngay, khách nhận thông
     * báo lỗi rõ ràng (môi trường thật sẽ hoàn tiền giao dịch). Voucher được hoàn lượt;
     * tồn kho KHÔNG hoàn vì đơn này chưa từng trừ.
     */
    @Transactional
    public void huyDonHetHangSauThanhToan(HoaDon hoaDon) {
        if (hoaDon == null) return;
        // Khoá dòng đơn + đọc trạng thái mới nhất: nếu một request song song VỪA xác
        // nhận thành công đơn này thì tuyệt đối không huỷ đè.
        hoaDon = hoaDonRepo.khoaDonDeXuLy(hoaDon.getMaHoaDon()).orElse(null);
        if (hoaDon == null || !"Chờ thanh toán".equals(hoaDon.getTrangThai())) return;
        hoaDon.setNgayThanhToan(java.time.LocalDateTime.now());
        hoanTonKhoVaVoucher(hoaDon);   // trạng thái đang "Chờ thanh toán" -> chỉ hoàn voucher
        hoaDon.setTrangThai("Đã huỷ");
        hoaDonRepo.save(hoaDon);
        thongBaoRealtimeService.trangThaiDonThayDoi(hoaDon, "Chờ thanh toán",
                "Hệ thống (hết hàng khi thanh toán)");
        trangThaiModuleService.phatNgay();
        guiThuXinLoi(hoaDon);          // gửi email xin lỗi cho khách bị mất hàng
    }

    /**
     * Khách tự huỷ đơn khi đơn còn ở trạng thái "Chờ xác nhận" (chưa được nhân viên xử lý).
     * Hoàn lại voucher (và tồn kho nếu đơn đã được xác nhận), đồng thời báo realtime cho Quản lý.
     */
    @Transactional
    public void khachHuyDon(HoaDon hoaDon) {
        String ttKhach = hoaDon != null ? hoaDon.getTrangThai() : null;
        boolean khachDuocHuy = "Chờ xác nhận".equals(ttKhach) || "Chờ thanh toán".equals(ttKhach);
        if (hoaDon == null || !khachDuocHuy) {
            throw new DatHangException("Đơn hàng này không thể huỷ ở trạng thái hiện tại. Vui lòng liên hệ FS Shoes để được hỗ trợ.");
        }
        String trangThaiCu = hoaDon.getTrangThai();

        hoanTonKhoVaVoucher(hoaDon);

        hoaDon.setTrangThai("Đã huỷ");
        hoaDonRepo.save(hoaDon);

        thongBaoRealtimeService.trangThaiDonThayDoi(hoaDon, trangThaiCu, "Khách hàng");
        trangThaiModuleService.phatNgay();
    }

    /**
     * Nhân viên/Quản lý huỷ đơn online khi đơn CHƯA giao xong
     * (Chờ xác nhận / Đã xác nhận / Đang giao). Hoàn lại tồn kho và voucher đã trừ.
     * Không cho huỷ đơn đã ở trạng thái kết thúc (Đã giao / Đã huỷ / Đã trả hàng).
     */
    @Transactional
    public void huyDonAdmin(HoaDon hoaDon) {
        if (hoaDon == null) {
            throw new DatHangException("Không tìm thấy đơn hàng.");
        }
        String tt = hoaDon.getTrangThai();
        boolean coTheHuy = "Chờ thanh toán".equals(tt) || "Chờ xác nhận".equals(tt)
                || "Đã xác nhận".equals(tt) || "Đang giao".equals(tt);
        if (!coTheHuy) {
            throw new DatHangException("Đơn ở trạng thái \"" + tt + "\" không thể huỷ.");
        }

        hoanTonKhoVaVoucher(hoaDon);

        hoaDon.setTrangThai("Đã huỷ");
        hoaDonRepo.save(hoaDon);

        thongBaoRealtimeService.trangThaiDonThayDoi(hoaDon, tt, "Quản lý bán hàng");
        trangThaiModuleService.phatNgay();
    }

    /**
     * Hoàn voucher của một đơn sắp huỷ, và hoàn TỒN KHO **chỉ khi** tồn đã thực sự bị trừ.
     * Tồn kho chỉ bị trừ sau khi đơn được Quản lý xác nhận, nên:
     *   - Đơn còn "Chờ xác nhận"  -> CHƯA trừ tồn -> KHÔNG hoàn (tránh cộng khống tồn kho).
     *   - Đơn "Đã xác nhận"/"Đang giao" -> ĐÃ trừ tồn -> hoàn lại.
     */
    private void hoanTonKhoVaVoucher(HoaDon hoaDon) {
        String ttTruocHuy = hoaDon.getTrangThai();
        boolean daTruTon = !"Chờ xác nhận".equals(ttTruocHuy) && !"Chờ thanh toán".equals(ttTruocHuy);
        List<HoaDonChiTiet> dsChiTiet = hoaDonChiTietService.findByHoaDOn(hoaDon);
        for (HoaDonChiTiet ct : dsChiTiet) {
            SanPhamChiTiet ctSpct = ct.getSanPhamChiTiet();
            if (ctSpct == null || ctSpct.getMaSanPhamChiTiet() == null) continue;
            int soLuong = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
            // Khoá biến thể để cộng/nhả không đè lên thao tác đặt hàng song song
            SanPhamChiTiet spct = sanPhamChiTietRepository
                    .khoaBienTheDeDatHang(ctSpct.getMaSanPhamChiTiet()).orElse(null);
            if (spct == null) continue;
            if (daTruTon) {
                int tonHienTai = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
                spct.setSoLuongTon(tonHienTai + soLuong);      // hoàn tồn thật
                sanPhamChiTietService.capNhatTrangThaii(spct);
                sanPhamChiTietService.them(spct);
            }
            // Đơn chưa trừ tồn (Chờ thanh toán / Chờ xác nhận): không phải hoàn gì cả
        }
        if (hoaDon.getMaGiamGia() != null) {
            String maKH = hoaDon.getMaKhachHang() != null ? hoaDon.getMaKhachHang().getMaKH() : null;
            giamGiaService.hoanLaiVoucher(hoaDon.getMaGiamGia().getMaGiamGia(), maKH);
        }
    }

    /**
     * GỬI THƯ XIN LỖI khi đơn bị huỷ vì hết hàng (khách khác thanh toán xong trước).
     * Chạy nền và nuốt mọi lỗi: không được để việc gửi mail làm hỏng luồng thanh toán.
     */
}