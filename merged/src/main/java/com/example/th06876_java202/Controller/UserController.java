package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.*;
import com.example.th06876_java202.Service.*;
import com.example.th06876_java202.Storefront.DangKyKhachHangDTO;
import com.example.th06876_java202.Storefront.SanPhamCardVM;
import com.example.th06876_java202.Storefront.SanPhamHienThiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final TaiKhoanService taiKhoanService;
    private final HoaDonService hoaDonService;
    private final KhachHangService khachHangService;
    private final SanPhamService sanPhamService;
    private final SanPhamChiTietService sanPhamChiTietService;
    private final ThongKeService thongKeService;
    private final DanhMucSanPhamService danhMucSanPhamService;
    private final ThuongHieuService thuongHieuService;
    private final SanPhamHienThiService sanPhamHienThiService;
    private final GiamGiaService giamGiaService;

    @GetMapping("/")
    public String index(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isStaff = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));

            if (isAdmin) {
                LocalDateTime endDate = LocalDateTime.now();
                LocalDateTime startDate = endDate.minusDays(7);
                ThongKeTongQuanDTO tongQuan = thongKeService.thongKeTongQuan(startDate, endDate);
                model.addAttribute("tongQuan", tongQuan);

                List<ThongKeDoanhThuDTO> thongKeNgay = thongKeService.thongKeDoanhThuTheoNgay(startDate, endDate);
                model.addAttribute("thongKeNgay", thongKeNgay);
                LocalDateTime prevStart = startDate.minusDays(7);
                ThongKeTongQuanDTO tongQuanTruoc = thongKeService.thongKeTongQuan(prevStart, startDate);
                model.addAttribute("phanTramDoanhThu", tinhPhanTramThayDoi(tongQuan.getTongDoanhThu(), tongQuanTruoc.getTongDoanhThu()));
                model.addAttribute("phanTramDonHang", tinhPhanTramThayDoi(
                        java.math.BigDecimal.valueOf(tongQuan.getTongDonHang()),
                        java.math.BigDecimal.valueOf(tongQuanTruoc.getTongDonHang())));

                List<KhachHang> khachHangs = khachHangService.getAllKhachHang();
                model.addAttribute("tongKhachHang", khachHangs != null ? khachHangs.size() : 0);
                LocalDate ngay7TruocLD = LocalDate.now().minusDays(7);
                long khachMoi7Ngay = khachHangs != null ? khachHangs.stream()
                        .filter(k -> k.getNgayDangKy() != null && !k.getNgayDangKy().isBefore(ngay7TruocLD))
                        .count() : 0;
                model.addAttribute("khachMoi7Ngay", khachMoi7Ngay);

                List<SanPham> sanPhams = sanPhamService.getAll();
                model.addAttribute("tongSanPham", sanPhams != null ? sanPhams.size() : 0);

                List<SanPhamChiTiet> spct = sanPhamChiTietService.getalll();
                long sapHet = spct.stream()
                        .filter(s -> s.getSoLuongTon() != null && s.getSoLuongTon() <= 10 && s.getSoLuongTon() > 0)
                        .count();
                model.addAttribute("sanPhamSapHet", sapHet);
                List<HoaDon> hoaDonGanDay = hoaDonService.getAll().stream()
                        .sorted((a, b) -> {
                            if (a.getNgayTao() == null) return 1;
                            if (b.getNgayTao() == null) return -1;
                            return b.getNgayTao().compareTo(a.getNgayTao());
                        })
                        .limit(10)
                        .toList();
                model.addAttribute("hoaDonGanDay", hoaDonGanDay);

                long dangXuLy = hoaDonService.getAll().stream()
                        .filter(h -> "Đang xử lý".equals(h.getTrangThai()) || "Chờ xác nhận".equals(h.getTrangThai()))
                        .count();
                long daGiao = hoaDonService.getAll().stream()
                        .filter(h -> "Đã giao".equals(h.getTrangThai()))
                        .count();
                long daHuy = hoaDonService.getAll().stream()
                        .filter(h -> "Đã hủy".equals(h.getTrangThai()))
                        .count();
                model.addAttribute("donDangXuLy", dangXuLy);
                model.addAttribute("donDaGiao", daGiao);
                model.addAttribute("donDaHuy", daHuy);

                // [SỬA] "Đơn hàng mới hôm nay" đếm đúng đơn được tạo HÔM NAY.
                LocalDate homNay = LocalDate.now();
                long donMoi = hoaDonService.getAll().stream()
                        .filter(h -> h.getNgayTao() != null && h.getNgayTao().toLocalDate().equals(homNay))
                        .count();
                model.addAttribute("tongDonHangMoi", donMoi);

                List<SanPhamBanChayDTO> topSanPham = thongKeService.layTopSanPhamBanChay(
                        endDate.minusDays(30), endDate, 5);
                model.addAttribute("topSanPham", topSanPham);

                return "account/admin/home";
            } else if (isStaff) {
                LocalDateTime endDate = LocalDateTime.now();
                LocalDateTime startDate = endDate.minusDays(7);
                List<ThongKeDoanhThuDTO> thongKeNgay = thongKeService.thongKeDoanhThuTheoNgay(startDate, endDate);
                model.addAttribute("thongKeNgay", thongKeNgay);
                List<HoaDon> hoaDonGanDay = hoaDonService.getAll().stream()
                        .sorted((a, b) -> {
                            if (a.getNgayTao() == null) return 1;
                            if (b.getNgayTao() == null) return -1;
                            return b.getNgayTao().compareTo(a.getNgayTao());
                        })
                        .limit(10)
                        .toList();
                model.addAttribute("hoaDonGanDay", hoaDonGanDay);

                LocalDate today = LocalDate.now();
                LocalDate yesterday = today.minusDays(1);

                // [SỬA] NgayTao là LocalDateTime -> phải quy về LocalDate trước khi so sánh.
                List<HoaDon> tatCaHoaDon = hoaDonService.getAll();
                List<HoaDon> hoaDonHomNay = tatCaHoaDon.stream()
                        .filter(h -> h.getNgayTao() != null && h.getNgayTao().toLocalDate().equals(today))
                        .toList();
                List<HoaDon> hoaDonHomQua = tatCaHoaDon.stream()
                        .filter(h -> h.getNgayTao() != null && h.getNgayTao().toLocalDate().equals(yesterday))
                        .toList();
                model.addAttribute("donHomNay", hoaDonHomNay.size());
                // [THÊM] so sánh thật với hôm qua.
                model.addAttribute("soSanhDonHomQua", hoaDonHomNay.size() - hoaDonHomQua.size());

                // [SỬA] Tính cả đơn "Đã thanh toán" (tại quầy) lẫn "Đã giao" (online).
                double doanhThuHomNay = hoaDonHomNay.stream()
                        .filter(h -> "Đã thanh toán".equals(h.getTrangThai()) || "Đã giao".equals(h.getTrangThai()))
                        .mapToDouble(h -> h.getTongTien() != null ? h.getTongTien().doubleValue() : 0)
                        .sum();
                double doanhThuHomQua = hoaDonHomQua.stream()
                        .filter(h -> "Đã thanh toán".equals(h.getTrangThai()) || "Đã giao".equals(h.getTrangThai()))
                        .mapToDouble(h -> h.getTongTien() != null ? h.getTongTien().doubleValue() : 0)
                        .sum();
                model.addAttribute("doanhThuHomNay", doanhThuHomNay);
                // [THÊM] % thay đổi thật so với hôm qua.
                model.addAttribute("phanTramDoanhThuHomQua",
                        tinhPhanTramThayDoi(java.math.BigDecimal.valueOf(doanhThuHomNay), java.math.BigDecimal.valueOf(doanhThuHomQua)));

                long dangXuLy = tatCaHoaDon.stream()
                        .filter(h -> "Đang xử lý".equals(h.getTrangThai()) || "Chờ xác nhận".equals(h.getTrangThai()))
                        .count();
                model.addAttribute("donDangXuLy", dangXuLy);

                // [THÊM] Đơn đang giao thật.
                long dangGiao = tatCaHoaDon.stream()
                        .filter(h -> "Đang giao".equals(h.getTrangThai()))
                        .count();
                model.addAttribute("donDangGiao", dangGiao);

                List<KhachHang> khachHangs = khachHangService.getAllKhachHang();
                long khachMoi = khachHangs.stream()
                        .filter(k -> k.getNgayDangKy() != null && k.getNgayDangKy().equals(today))
                        .count();
                long khachHomQua = khachHangs.stream()
                        .filter(k -> k.getNgayDangKy() != null && k.getNgayDangKy().equals(yesterday))
                        .count();
                model.addAttribute("khachMoiHomNay", khachMoi);
                // [THÊM] so sánh thật.
                model.addAttribute("soSanhKhachHomQua", khachMoi - khachHomQua);

                long donMoi = tatCaHoaDon.stream()
                        .filter(h -> "Chờ xác nhận".equals(h.getTrangThai()))
                        .count();
                model.addAttribute("donHangMoi", donMoi);

                return "account/staff/home";
            }
        }

        // === KHÁCH / KHÁCH HÀNG: TRANG CHỦ website bán hàng FS Shoes ===
        List<SanPham> dangBan = sanPhamService.getAll().stream()
                .filter(sp -> Boolean.TRUE.equals(sp.getTrangThai()))
                .toList();
        List<SanPhamCardVM> cards = sanPhamHienThiService.taoDanhSachCard(dangBan);

        // Mới nhất (mã lớn hơn = tạo sau)
        List<SanPhamCardVM> sanPhamMoi = cards.stream()
                .sorted(Comparator.comparing(SanPhamCardVM::getMaSanPham,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8).toList();

        // Bán chạy (theo số lượng đã bán thật trong CSDL)
        List<SanPhamCardVM> sanPhamBanChay = cards.stream()
                .sorted(Comparator.comparingLong(SanPhamCardVM::getDaBan).reversed())
                .limit(8).toList();

        // Đang khuyến mãi (giảm sâu lên trước)
        List<SanPhamCardVM> sanPhamKhuyenMai = cards.stream()
                .filter(c -> c.getPhanTramGiam() != null && c.getPhanTramGiam() > 0)
                .sorted(Comparator.comparingInt(
                        (SanPhamCardVM c) -> c.getPhanTramGiam() != null ? c.getPhanTramGiam() : 0).reversed())
                .limit(8).toList();

        // Đánh giá tốt nhất
        List<SanPhamCardVM> sanPhamDanhGiaCao = cards.stream()
                .filter(c -> c.getSoLuotDanhGia() > 0)
                .sorted(Comparator.comparingDouble(SanPhamCardVM::getDiemTrungBinh).reversed()
                        .thenComparing(Comparator.comparingLong(SanPhamCardVM::getSoLuotDanhGia).reversed()))
                .limit(4).toList();

        model.addAttribute("sanPhamMoi", sanPhamMoi);
        model.addAttribute("sanPhamBanChay", sanPhamBanChay);
        model.addAttribute("sanPhamKhuyenMai", sanPhamKhuyenMai);
        model.addAttribute("sanPhamDanhGiaCao", sanPhamDanhGiaCao);
        model.addAttribute("tongSoSanPham", cards.size());
        model.addAttribute("danhMucs", danhMucSanPhamService.getAll().stream()
                .filter(DanhMucSanPham::isTrangThai).toList());
        model.addAttribute("thuongHieus", thuongHieuService.findAll().stream()
                .filter(ThuongHieu::isTrangThai).toList());

        // Voucher công khai đang chạy (mời khách săn mã)
        try {
            List<GiamGia> voucherCongKhai = giamGiaService.getVoucherKhaDungChoKhachHang(null);
            model.addAttribute("voucherCongKhai",
                    voucherCongKhai.stream().limit(4).toList());
        } catch (Exception e) {
            model.addAttribute("voucherCongKhai", List.of());
        }

        return "trangchu/index";
    }

    @GetMapping("/login")
    public String login() {
        return "account/user/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("dangKy", new DangKyKhachHangDTO());
        return "account/user/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("dangKy") DangKyKhachHangDTO dangKy,
                           BindingResult result,
                           Model model) {

        if (!result.hasErrors() && !dangKy.getMatKhau().equals(dangKy.getXnMatKhau())) {
            result.rejectValue("xnMatKhau", "error.dangKy", "Mật khẩu xác nhận không khớp.");
        }
        if (!result.hasFieldErrors("tenDangNhap") && taiKhoanService.isTenDangNhapExist(dangKy.getTenDangNhap())) {
            result.rejectValue("tenDangNhap", "error.dangKy", "Tên đăng nhập đã tồn tại.");
        }

        if (result.hasErrors()) {
            return "account/user/register";
        }

        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setTenDangNhap(dangKy.getTenDangNhap().trim());
        taiKhoan.setMatKhau(dangKy.getMatKhau());

        // Đăng ký chỉ thu thập tên đăng nhập/mật khẩu. Các thông tin hồ sơ (họ tên, SĐT, email,
        // giới tính...) để trống (NULL), khách hàng sẽ tự bổ sung sau trong mục "Tài khoản của tôi".
        String maKH = khachHangService.generateMaKH();

        KhachHang khachHang = new KhachHang();
        khachHang.setMaKH(maKH);

        taiKhoanService.registerCustomer(taiKhoan, khachHang);

        return "redirect:/login?registered";
    }

    @GetMapping("/accessDenied")
    public String deny() {
        return "account/user/deny";
    }

    /**
     * [THÊM - MERGE thống kê] Tính % thay đổi thật giữa 2 mốc, dùng để thay cho các số trend
     * (+12.5%, +8%, ...) trước đây được viết chết cứng trong giao diện.
     * Trả về null khi không có mốc trước để so sánh (coi là "mới", không có xu hướng).
     */
    private Double tinhPhanTramThayDoi(java.math.BigDecimal hienTai, java.math.BigDecimal truoc) {
        if (hienTai == null) hienTai = java.math.BigDecimal.ZERO;
        if (truoc == null) truoc = java.math.BigDecimal.ZERO;
        if (truoc.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return hienTai.compareTo(java.math.BigDecimal.ZERO) == 0 ? 0.0 : null;
        }
        return hienTai.subtract(truoc)
                .divide(truoc, 4, java.math.RoundingMode.HALF_UP)
                .multiply(java.math.BigDecimal.valueOf(100))
                .doubleValue();
    }
}
