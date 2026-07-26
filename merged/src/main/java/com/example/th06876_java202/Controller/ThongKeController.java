package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.NhanVien;
import com.example.th06876_java202.Entity.NhanVienHieuSuatDTO;
import com.example.th06876_java202.Entity.ThongKeDoanhThuDTO;
import com.example.th06876_java202.Entity.ThongKeTheoThangDTO;
import com.example.th06876_java202.Entity.ThongKeTongQuanDTO;
import com.example.th06876_java202.Service.NhanVienService;
import com.example.th06876_java202.Service.ThongKeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/thong-ke")
@RequiredArgsConstructor
public class ThongKeController {

    private final ThongKeService thongKeService;
    private final NhanVienService nhanVienService;

    /**
     * Trang thống kê tổng quan cho ADMIN: doanh thu toàn cửa hàng + hiệu suất
     * (doanh số & chấm công) của từng nhân viên.
     */
    @GetMapping("/doanh-thu")
    public String viewThongKeDoanhThu(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDateTime endDate,
            // [MỚI] Bộ lọc nhanh: homnay | tuannay | thangnay | namnay
            @RequestParam(required = false) String loc,
            Model model) {

        // ============================================================
        // [MỚI] BỘ LỌC NHANH - ưu tiên hơn ngày chọn tay
        // ============================================================
        java.time.LocalDate homNay = java.time.LocalDate.now();
        if (loc != null && !loc.isBlank()) {
            switch (loc) {
                case "homnay":
                    startDate = homNay.atStartOfDay();
                    endDate = homNay.atStartOfDay();
                    break;
                case "tuannay":
                    // Tuần bắt đầu từ Thứ 2
                    startDate = homNay.with(java.time.DayOfWeek.MONDAY).atStartOfDay();
                    endDate = homNay.atStartOfDay();
                    break;
                case "thangnay":
                    startDate = homNay.withDayOfMonth(1).atStartOfDay();
                    endDate = homNay.atStartOfDay();
                    break;
                case "namnay":
                    startDate = homNay.withDayOfYear(1).atStartOfDay();
                    endDate = homNay.atStartOfDay();
                    break;
                default:
                    break; // giá trị lạ -> bỏ qua, dùng ngày chọn tay
            }
        }

        if (startDate == null) {
            endDate = LocalDateTime.now();
            startDate = endDate.minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        // Bao trọn hết ngày endDate (nếu không, đơn tạo lúc 15h hôm nay sẽ bị loại
        // khi endDate = hôm nay 00:00 do người dùng chỉ chọn ngày, không chọn giờ).
        LocalDateTime endDateInclusive = endDate.toLocalDate().atTime(23, 59, 59);

        List<ThongKeDoanhThuDTO> thongKeNgay =
                thongKeService.thongKeDoanhThuTheoNgay(startDate, endDateInclusive);

        List<ThongKeTheoThangDTO> thongKeThang =
                thongKeService.thongKeDoanhThuTheoThang(startDate, endDateInclusive);

        // [SỬA] Trước đây gọi thongKeTongQuan() không truyền ngày -> luôn ra số liệu
        // toàn bộ lịch sử, lệch với bảng/biểu đồ bên dưới đã lọc theo ngày.
        ThongKeTongQuanDTO tongQuan = thongKeService.thongKeTongQuan(startDate, endDateInclusive);

        // [MỚI] Hiệu suất từng nhân viên trong đúng khoảng ngày đang xem
        List<NhanVienHieuSuatDTO> hieuSuatNhanVien =
                thongKeService.thongKeHieuSuatNhanVien(startDate, endDateInclusive);

        // ============================================================
        // [MỚI] THỐNG KÊ NÂNG CAO
        // ============================================================
        // Doanh thu THỰC TẾ (đã thu tiền) vs DỰ KIẾN (đơn còn đang chạy)
        model.addAttribute("doanhThuThucTe",
                thongKeService.doanhThuThucTe(startDate, endDateInclusive));
        model.addAttribute("doanhThuDuKien",
                thongKeService.doanhThuDuKien(startDate, endDateInclusive));

        // Phân bổ trạng thái đơn hàng (vẽ biểu đồ tròn)
        model.addAttribute("phanBoTrangThai",
                thongKeService.phanBoTrangThaiDonHang(startDate, endDateInclusive));

        // Top sản phẩm bán chạy trong khoảng đang xem
        model.addAttribute("topSanPham",
                thongKeService.layTopSanPhamBanChay(startDate, endDateInclusive, 10));

        // Sản phẩm sắp hết hàng (tồn <= 10)
        model.addAttribute("sanPhamSapHet", thongKeService.sanPhamSapHetHang(10));

        model.addAttribute("thongKeNgay", thongKeNgay);
        model.addAttribute("thongKeThang", thongKeThang);
        model.addAttribute("tongQuan", tongQuan);
        model.addAttribute("hieuSuatNhanVien", hieuSuatNhanVien);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("locHienTai", loc);   // để tô sáng nút lọc đang chọn
        model.addAttribute("activeMenu", "thongke");

        return "thongke/index";
    }

    /**
     * Trang "Thống kê của tôi" dành cho NHÂN VIÊN (STAFF, ADMIN cũng xem được số
     * liệu của chính mình): doanh số cá nhân đã bán ra + giờ công/chấm công.
     */
    @GetMapping("/ca-nhan")
    public String viewThongKeCaNhan(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDateTime endDate,
            Authentication authentication,
            Model model) {

        if (startDate == null) {
            endDate = LocalDateTime.now();
            startDate = endDate.minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        LocalDateTime endDateInclusive = endDate.toLocalDate().atTime(23, 59, 59);

        NhanVien nv = nhanVienService.findByUsername(authentication.getName());
        model.addAttribute("activeMenu", "thongkecanhan");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        if (nv == null) {
            model.addAttribute("loi", "Tài khoản của bạn chưa được liên kết với hồ sơ nhân viên nào. " +
                    "Vui lòng liên hệ quản trị viên để được gán hồ sơ nhân viên cho tài khoản này.");
            return "thongke/canhan";
        }

        List<ThongKeDoanhThuDTO> thongKeNgay =
                thongKeService.thongKeDoanhThuCaNhanTheoNgay(nv.getMaNhanVien(), startDate, endDateInclusive);
        ThongKeTongQuanDTO tongQuan =
                thongKeService.thongKeTongQuanCaNhan(nv.getMaNhanVien(), startDate, endDateInclusive);
        NhanVienHieuSuatDTO chamCong =
                thongKeService.thongKeChamCongCaNhan(nv.getMaNhanVien(), startDate.toLocalDate(), endDate.toLocalDate());

        model.addAttribute("nhanVien", nv);
        model.addAttribute("thongKeNgay", thongKeNgay);
        model.addAttribute("tongQuan", tongQuan);
        model.addAttribute("chamCong", chamCong);

        return "thongke/canhan";
    }
}
