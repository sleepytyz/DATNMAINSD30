package com.example.th06876_java202.Storefront;

import com.example.th06876_java202.Entity.SanPham;
import com.example.th06876_java202.Entity.SanPhamChiTiet;
import com.example.th06876_java202.Repository.DanhGiaRepository;
import com.example.th06876_java202.Repository.HoaDonChiTietRepository;
import com.example.th06876_java202.Service.SanPhamChiTietService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Dựng dữ liệu hiển thị cho website bán hàng (card sản phẩm, biến thể, lọc/sắp xếp,
 * thống kê đánh giá, gợi ý tìm kiếm) từ dữ liệu THẬT trong CSDL: giá, tồn kho,
 * khuyến mãi, số đã bán, điểm đánh giá.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SanPhamHienThiService {

    /** Số còn bán được = TỒN KHO THẬT. */
    private static int khaDung(SanPhamChiTiet b) {
        return Math.max(0, b.getSoLuongTon() != null ? b.getSoLuongTon() : 0);
    }

    /** Mốc "bán chạy" mặc định — cấu hình fsshoes.ban-chay-toi-thieu (mặc định 5). */
    @org.springframework.beans.factory.annotation.Value("${fsshoes.ban-chay-toi-thieu:5}")
    private long banChayToiThieu;

    public long getBanChayToiThieu() {
        return banChayToiThieu;
    }

    public static final String ANH_MAC_DINH = "/storefront/img/no-image.svg";
    public static final int SO_NGAY_COI_LA_MOI = 30;

    private final SanPhamChiTietService sanPhamChiTietService;
    private final KhuyenMaiService khuyenMaiService;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final DanhGiaRepository danhGiaRepository;

    // =====================================================================
    // Thống kê gộp (1 truy vấn cho cả danh sách — tránh N+1)
    // =====================================================================

    /** Map maSanPham -> tổng số lượng đã bán. */
    public Map<String, Long> bangDaBan() {
        Map<String, Long> map = new HashMap<>();
        try {
            for (Object[] row : hoaDonChiTietRepository.thongKeSoLuongDaBanTheoSanPham()) {
                if (row[0] == null) continue;
                long sl = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                map.put(String.valueOf(row[0]), sl);
            }
        } catch (Exception ignored) { }
        return map;
    }

    /** Map maSanPham -> [điểm trung bình, số lượt]. */
    public Map<String, double[]> bangDanhGia() {
        Map<String, double[]> map = new HashMap<>();
        try {
            for (Object[] row : danhGiaRepository.thongKeDanhGiaTheoSanPham()) {
                if (row[0] == null) continue;
                double diem = row[1] != null ? ((Number) row[1]).doubleValue() : 0d;
                long luot = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                map.put(String.valueOf(row[0]), new double[]{Math.round(diem * 10.0) / 10.0, luot});
            }
        } catch (Exception ignored) { }
        return map;
    }

    // =====================================================================
    // Card sản phẩm
    // =====================================================================

    public List<SanPhamCardVM> taoDanhSachCard(List<SanPham> sanPhams) {
        List<SanPhamCardVM> list = new ArrayList<>();
        if (sanPhams == null || sanPhams.isEmpty()) return list;
        Map<String, Long> daBan = bangDaBan();
        Map<String, double[]> danhGia = bangDanhGia();
        for (SanPham sp : sanPhams) {
            list.add(taoCard(sp, daBan, danhGia));
        }
        return list;
    }

    public SanPhamCardVM taoCard(SanPham sp) {
        return taoCard(sp, bangDaBan(), bangDanhGia());
    }

    private SanPhamCardVM taoCard(SanPham sp, Map<String, Long> daBan, Map<String, double[]> danhGia) {
        SanPhamCardVM vm = new SanPhamCardVM();
        vm.setMaSanPham(sp.getMaSanPham());
        vm.setTenSanPham(sp.getTenSanPham());
        vm.setTenThuongHieu(sp.getThuongHieu() != null ? sp.getThuongHieu().getTenThuongHieu() : "");
        vm.setTenDanhMuc(sp.getDanhMucSanPham() != null ? sp.getDanhMucSanPham().getTenDanhMuc() : "");
        vm.setDaBan(daBan.getOrDefault(sp.getMaSanPham(), 0L));
        vm.setBanChay(vm.getDaBan() >= banChayToiThieu);

        double[] dg = danhGia.get(sp.getMaSanPham());
        if (dg != null) {
            vm.setDiemTrungBinh(dg[0]);
            vm.setSoLuotDanhGia((long) dg[1]);
        }

        if (sp.getNgayTao() != null) {
            vm.setMoiVe(sp.getNgayTao().isAfter(LocalDateTime.now().minusDays(SO_NGAY_COI_LA_MOI)));
        }

        List<SanPhamChiTiet> bienThe = locBienTheDangBan(sanPhamChiTietService.getallsp(sp.getMaSanPham()));
        if (bienThe.isEmpty()) {
            vm.setAnh(ANH_MAC_DINH);
            vm.setGiaGoc(BigDecimal.ZERO);
            vm.setGiaSauGiam(BigDecimal.ZERO);
            vm.setPhanTramGiam(0);
            vm.setConHang(false);
            return vm;
        }

        // Màu & size đang có (giữ thứ tự, không trùng)
        LinkedHashSet<String> mau = new LinkedHashSet<>();
        LinkedHashSet<String> size = new LinkedHashSet<>();
        int tongTon = 0;
        for (SanPhamChiTiet b : bienThe) {
            if (b.getMauSac() != null && b.getMauSac().getTenMauSac() != null) mau.add(b.getMauSac().getTenMauSac());
            if (b.getKichThuoc() != null && b.getKichThuoc().getTenKichThuoc() != null) size.add(b.getKichThuoc().getTenKichThuoc());
            tongTon += khaDung(b);
        }
        vm.setTenMauSacs(new ArrayList<>(mau));
        vm.setTenKichThuocs(new ArrayList<>(size));
        vm.setTongTon(tongTon);

        // ===== TÍNH GIÁ HIỂN THỊ THEO TỪNG BIẾN THỂ (giá card = giá mua được THẬT) =====
        // Nguyên tắc: % giảm của biến thể nào chỉ áp cho biến thể đó (hoặc dòng gán cả
        // sản phẩm). TUYỆT ĐỐI không lấy % của biến thể khác gắn lên giá biến thể này.
        List<SanPhamChiTiet> conHang = bienThe.stream()
                .filter(b -> khaDung(b) > 0)
                .toList();
        List<SanPhamChiTiet> nguon = conHang.isEmpty() ? bienThe : conHang;

        SanPhamChiTiet chon = null;          // biến thể được trưng lên card
        int chonPhanTram = 0;
        BigDecimal chonGiaCuoi = null;

        SanPhamChiTiet chonKM = null;        // ứng viên tốt nhất TRONG NHÓM đang giảm giá
        int chonKMPhanTram = 0;
        BigDecimal chonKMGiaCuoi = null;

        for (SanPhamChiTiet b : nguon) {
            if (b.getGiaBan() == null) continue;
            int pt = khuyenMaiService.phanTramGiamChoBienThe(sp, b.getMaSanPhamChiTiet());
            BigDecimal giaCuoi = khuyenMaiService.giaSauGiam(b.getGiaBan(), pt);

            if (chonGiaCuoi == null || giaCuoi.compareTo(chonGiaCuoi) < 0) {
                chon = b; chonPhanTram = pt; chonGiaCuoi = giaCuoi;
            }
            if (pt > 0 && (chonKMGiaCuoi == null || giaCuoi.compareTo(chonKMGiaCuoi) < 0)) {
                chonKM = b; chonKMPhanTram = pt; chonKMGiaCuoi = giaCuoi;
            }
        }
        // Nếu sản phẩm CÓ biến thể đang giảm -> ưu tiên trưng biến thể đó (đúng tinh thần khu sale)
        if (chonKM != null) {
            chon = chonKM; chonPhanTram = chonKMPhanTram; chonGiaCuoi = chonKMGiaCuoi;
        }
        if (chon == null) {                   // mọi biến thể đều thiếu giá — phòng hờ
            chon = nguon.get(0);
            chonPhanTram = 0;
            chonGiaCuoi = BigDecimal.ZERO;
        }

        // Ảnh card = ẢNH CỦA ĐÚNG BIẾN THỂ ĐANG TRƯNG ("chon"): bấm vào card, trang chi
        // tiết tự chọn biến thể này nên ảnh bên ngoài không được lệch với bên trong.
        // Biến thể ĐANG GIẢM GIÁ được ưu tiên chọn ở trên -> card tự hiển thị ảnh sale.
        String anh = (chon.getAnhDaiDien() != null && !chon.getAnhDaiDien().isBlank())
                ? chon.getAnhDaiDien()
                : bienThe.stream()
                .map(SanPhamChiTiet::getAnhDaiDien)
                .filter(a -> a != null && !a.isBlank())
                .findFirst()
                .orElse(null);

        vm.setAnh(duongDanAnh(anh));
        vm.setGiaGoc(chon.getGiaBan() != null ? chon.getGiaBan() : BigDecimal.ZERO);
        vm.setPhanTramGiam(chonPhanTram);
        vm.setGiaSauGiam(chonGiaCuoi);
        vm.setMaBienTheHienThi(chon.getMaSanPhamChiTiet());
        vm.setConHang(!conHang.isEmpty());
        return vm;
    }

    // =====================================================================
    // Lọc / sắp xếp / phân trang trong bộ nhớ (áp lên danh sách card đã dựng)
    // =====================================================================

    /**
     * @param cards      danh sách card đã dựng (đã lọc danh mục/thương hiệu/kiểu giày/từ khoá ở tầng repo)
     * @param giaTu      lọc giá từ (theo giá SAU khuyến mãi), null bỏ qua
     * @param giaDen     lọc giá đến, null bỏ qua
     * @param mauSac     lọc theo TÊN màu, null/rỗng bỏ qua
     * @param kichThuoc  lọc theo TÊN size, null/rỗng bỏ qua
     * @param chiKhuyenMai chỉ lấy sản phẩm đang có khuyến mãi
     * @param chiConHang chỉ lấy sản phẩm còn hàng
     * @param sapXep     moi-nhat | gia-tang | gia-giam | ban-chay | danh-gia | giam-sau
     */
    public KetQuaTrangVM locSapXepPhanTrang(List<SanPhamCardVM> cards,
                                            BigDecimal giaTu, BigDecimal giaDen,
                                            String mauSac, String kichThuoc,
                                            boolean chiKhuyenMai, boolean chiConHang,
                                            String sapXep, int trang, int kichThuocTrang) {
        List<SanPhamCardVM> ds = new ArrayList<>(cards != null ? cards : List.of());

        if (giaTu != null) ds.removeIf(c -> c.getGiaSauGiam() == null || c.getGiaSauGiam().compareTo(giaTu) < 0);
        if (giaDen != null) ds.removeIf(c -> c.getGiaSauGiam() == null || c.getGiaSauGiam().compareTo(giaDen) > 0);
        if (mauSac != null && !mauSac.isBlank()) {
            String m = mauSac.trim();
            ds.removeIf(c -> c.getTenMauSacs() == null || c.getTenMauSacs().stream().noneMatch(t -> t.equalsIgnoreCase(m)));
        }
        if (kichThuoc != null && !kichThuoc.isBlank()) {
            String k = kichThuoc.trim();
            ds.removeIf(c -> c.getTenKichThuocs() == null || c.getTenKichThuocs().stream().noneMatch(t -> t.equalsIgnoreCase(k)));
        }
        if (chiKhuyenMai) ds.removeIf(c -> c.getPhanTramGiam() == null || c.getPhanTramGiam() <= 0);
        if (chiConHang) ds.removeIf(c -> !c.isConHang());

        Comparator<SanPhamCardVM> cmp;
        String sx = sapXep != null ? sapXep : "moi-nhat";
        switch (sx) {
            case "gia-tang" -> cmp = Comparator.comparing(SanPhamCardVM::getGiaSauGiam,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "gia-giam" -> cmp = Comparator.comparing(SanPhamCardVM::getGiaSauGiam,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            case "ban-chay" -> cmp = Comparator.comparingLong(SanPhamCardVM::getDaBan).reversed();
            case "danh-gia" -> cmp = Comparator.comparingDouble(SanPhamCardVM::getDiemTrungBinh).reversed()
                    .thenComparing(Comparator.comparingLong(SanPhamCardVM::getSoLuotDanhGia).reversed());
            case "giam-sau" -> cmp = Comparator.comparingInt(
                    (SanPhamCardVM c) -> c.getPhanTramGiam() != null ? c.getPhanTramGiam() : 0).reversed();
            default -> cmp = Comparator.comparing(SanPhamCardVM::getMaSanPham,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed(); // moi-nhat: mã lớn = mới hơn
        }
        // Ưu tiên còn hàng lên trước trong mọi kiểu sắp xếp
        ds.sort(Comparator.comparing(SanPhamCardVM::isConHang).reversed().thenComparing(cmp));

        int size = Math.max(kichThuocTrang, 1);
        long tong = ds.size();
        int tongTrang = (int) Math.ceil((double) tong / size);
        int trangHienTai = Math.min(Math.max(trang, 0), Math.max(tongTrang - 1, 0));
        int tu = trangHienTai * size;
        int den = (int) Math.min(tu + size, tong);

        KetQuaTrangVM kq = new KetQuaTrangVM();
        kq.setNoiDung(tu < den ? ds.subList(tu, den) : List.of());
        kq.setTrangHienTai(trangHienTai);
        kq.setTongTrang(Math.max(tongTrang, 1));
        kq.setTongPhanTu(tong);
        kq.setKichThuoc(size);
        return kq;
    }

    // =====================================================================
    // Biến thể (trang chi tiết)
    // =====================================================================

    /** Danh sách biến thể đang bán của 1 sản phẩm, kèm giá khuyến mãi & bộ ảnh. */
    public List<BienTheVM> taoDanhSachBienThe(String maSanPham) {
        List<SanPhamChiTiet> bienThe = locBienTheDangBan(sanPhamChiTietService.getallsp(maSanPham));
        List<BienTheVM> list = new ArrayList<>();
        for (SanPhamChiTiet b : bienThe) {
            int phanTram = khuyenMaiService.phanTramGiamChoBienThe(b.getSanPham(), b.getMaSanPhamChiTiet());
            BigDecimal giaGoc = b.getGiaBan() != null ? b.getGiaBan() : BigDecimal.ZERO;

            List<String> anhBoSuuTap = new ArrayList<>();
            for (String a : b.getDanhSachAnhList()) {
                if (a != null && !a.isBlank()) anhBoSuuTap.add(duongDanAnh(a));
            }
            String anhDaiDien = duongDanAnh(b.getAnhDaiDien());
            if (anhBoSuuTap.isEmpty()) anhBoSuuTap.add(anhDaiDien);

            BienTheVM vm = new BienTheVM();
            vm.setMaSanPhamChiTiet(b.getMaSanPhamChiTiet());
            vm.setMaMauSac(b.getMauSac() != null ? b.getMauSac().getMaMauSac() : null);
            vm.setTenMauSac(b.getMauSac() != null ? b.getMauSac().getTenMauSac() : "");
            vm.setMaKichThuoc(b.getKichThuoc() != null ? b.getKichThuoc().getMaKichThuoc() : null);
            vm.setTenKichThuoc(b.getKichThuoc() != null ? b.getKichThuoc().getTenKichThuoc() : "");
            vm.setGiaGoc(giaGoc);
            vm.setGiaSauGiam(khuyenMaiService.giaSauGiam(giaGoc, phanTram));
            vm.setPhanTramGiam(phanTram);
            vm.setSoLuongTon(khaDung(b));
            vm.setAnh(anhDaiDien);
            vm.setDanhSachAnh(anhBoSuuTap);
            list.add(vm);
        }
        return list;
    }

    // =====================================================================
    // Thống kê đánh giá 1 sản phẩm (phân bố sao)
    // =====================================================================

    public DanhGiaThongKeVM thongKeDanhGiaSanPham(String maSanPham) {
        DanhGiaThongKeVM vm = new DanhGiaThongKeVM();
        try {
            long tong = 0;
            long tongDiem = 0;
            for (Object[] row : danhGiaRepository.phanBoSoSao(maSanPham)) {
                int sao = row[0] != null ? ((Number) row[0]).intValue() : 0;
                long luot = row[1] != null ? ((Number) row[1]).longValue() : 0;
                if (sao >= 1 && sao <= 5) {
                    vm.getSoLuot()[sao - 1] = luot;
                    tong += luot;
                    tongDiem += (long) sao * luot;
                }
            }
            vm.setTongLuot(tong);
            vm.setDiemTrungBinh(tong > 0 ? Math.round((double) tongDiem / tong * 10.0) / 10.0 : 0);
            for (int i = 0; i < 5; i++) {
                vm.getPhanTram()[i] = tong > 0 ? (int) Math.round(vm.getSoLuot()[i] * 100.0 / tong) : 0;
            }
        } catch (Exception ignored) { }
        return vm;
    }

    // =====================================================================
    // Tiện ích
    // =====================================================================

    /** Ẩn biến thể "Ngừng bán"/"Ngừng kinh doanh"; các trạng thái tồn kho khác vẫn hiển thị. */
    private List<SanPhamChiTiet> locBienTheDangBan(List<SanPhamChiTiet> bienThe) {
        if (bienThe == null) return List.of();
        return bienThe.stream()
                .filter(b -> {
                    String tt = b.getTrangThai();
                    return tt == null || !("Ngừng bán".equals(tt) || "Ngừng kinh doanh".equals(tt));
                })
                .toList();
    }

    /** Chuẩn hoá đường dẫn ảnh sản phẩm (ảnh lưu trên đĩa được phục vụ qua /images/**). */
    public String duongDanAnh(String tenFile) {
        if (tenFile == null || tenFile.isBlank()) return ANH_MAC_DINH;
        String t = tenFile.trim();
        if (t.startsWith("http://") || t.startsWith("https://") || t.startsWith("/")) return t;
        return "/images/" + t;
    }
}