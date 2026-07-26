package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.NhanVien;
import com.example.th06876_java202.Repository.NhanVienRepository;
import com.example.th06876_java202.Service.NhanVienService;
import com.example.th06876_java202.Service.TaiKhoanService;
import com.example.th06876_java202.Storefront.DoiMatKhauDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Khu vực "Thông tin cá nhân" dành cho nhân viên/quản trị viên đang đăng nhập
 * (ROLE_ADMIN, ROLE_STAFF - đã cấu hình ở SecurityConfig).
 * Khác với /nhan-vien (quản lý toàn bộ danh sách nhân viên, chỉ dành cho admin thao tác trên NGƯỜI KHÁC),
 * controller này chỉ cho phép người dùng chỉnh sửa hồ sơ và mật khẩu của CHÍNH MÌNH.
 */
@Controller
@RequestMapping("/nv-ca-nhan")
@RequiredArgsConstructor
public class NhanVienCaNhanController {

    private final NhanVienService nhanVienService;
    private final NhanVienRepository nhanVienRepository;
    private final TaiKhoanService taiKhoanService;

    private NhanVien nhanVienHienTai(Authentication authentication) {
        return nhanVienService.findByUsername(authentication.getName());
    }

    // ====== Hồ sơ cá nhân ======

    @GetMapping
    public String hoSo(Model model, Authentication authentication) {
        NhanVien nv = nhanVienHienTai(authentication);
        model.addAttribute("nhanVien", nv);
        model.addAttribute("activeMenu", "nvcanhan");
        // Tài khoản đã đăng nhập hợp lệ nhưng chưa được gán hồ sơ nhân viên (dữ liệu thiếu liên kết
        // NhanVien.MaTaiKhoan). Trước đây code redirect thẳng về /login khiến người dùng tưởng bị
        // đăng xuất/hết phiên. Giờ hiển thị thông báo rõ ràng ngay trên trang, KHÔNG đá về login.
        if (nv == null) {
            model.addAttribute("loi", "Tài khoản của bạn chưa được liên kết với hồ sơ nhân viên nào. " +
                    "Vui lòng liên hệ quản trị viên để được gán hồ sơ nhân viên cho tài khoản này.");
        }
        return "nhanvien/ho-so";
    }

    @PostMapping("/cap-nhat")
    public String capNhatHoSo(@Valid @ModelAttribute("nhanVien") NhanVien form,
                              BindingResult result,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        NhanVien hienTai = nhanVienHienTai(authentication);
        if (hienTai == null) {
            model.addAttribute("nhanVien", null);
            model.addAttribute("activeMenu", "nvcanhan");
            model.addAttribute("loi", "Tài khoản của bạn chưa được liên kết với hồ sơ nhân viên nào. " +
                    "Vui lòng liên hệ quản trị viên để được gán hồ sơ nhân viên cho tài khoản này.");
            return "nhanvien/ho-so";
        }

        // Không cho tự đổi các trường nhạy cảm/nghiệp vụ qua form này
        form.setMaNhanVien(hienTai.getMaNhanVien());
        form.setTaiKhoan(hienTai.getTaiKhoan());
        form.setChucVu(hienTai.getChucVu());
        form.setLuongCoBan(hienTai.getLuongCoBan());
        form.setTrangThai(hienTai.getTrangThai());
        form.setNgayVaoLam(hienTai.getNgayVaoLam());
        form.setGhiChu(hienTai.getGhiChu());

        if (form.getSoDienThoai() != null && !form.getSoDienThoai().equals(hienTai.getSoDienThoai())
                && nhanVienRepository.existsBySoDienThoaiAndNotMaNhanVien(form.getSoDienThoai(), hienTai.getMaNhanVien())) {
            result.rejectValue("soDienThoai", "error.nv", "Số điện thoại này đã được sử dụng!");
        }
        if (form.getEmail() != null && !form.getEmail().equals(hienTai.getEmail())
                && nhanVienRepository.existsByEmailAndNotMaNhanVien(form.getEmail(), hienTai.getMaNhanVien())) {
            result.rejectValue("email", "error.nv", "Email này đã được sử dụng!");
        }

        if (result.hasErrors()) {
            model.addAttribute("nhanVien", form);
            model.addAttribute("activeMenu", "nvcanhan");
            return "nhanvien/ho-so";
        }

        nhanVienService.save(form);
        redirectAttributes.addFlashAttribute("thongBao", "Cập nhật thông tin cá nhân thành công.");
        return "redirect:/nv-ca-nhan";
    }

    // ====== Đổi mật khẩu ======

    @GetMapping("/doi-mat-khau")
    public String trangDoiMatKhau(Model model) {
        model.addAttribute("doiMatKhau", new DoiMatKhauDTO());
        model.addAttribute("activeMenu", "nvcanhan");
        return "nhanvien/doi-mat-khau";
    }

    @PostMapping("/doi-mat-khau")
    public String xuLyDoiMatKhau(@Valid @ModelAttribute("doiMatKhau") DoiMatKhauDTO dto,
                                 BindingResult result,
                                 Authentication authentication,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!result.hasErrors() && !dto.getMatKhauMoi().equals(dto.getXnMatKhauMoi())) {
            result.rejectValue("xnMatKhauMoi", "error.doiMatKhau", "Mật khẩu xác nhận không khớp.");
        }
        if (result.hasErrors()) {
            model.addAttribute("activeMenu", "nvcanhan");
            return "nhanvien/doi-mat-khau";
        }

        NhanVien nv = nhanVienHienTai(authentication);
        if (nv == null || nv.getTaiKhoan() == null) {
            model.addAttribute("activeMenu", "nvcanhan");
            model.addAttribute("loiMatKhauCu", "Tài khoản của bạn chưa được liên kết với hồ sơ nhân viên nào. " +
                    "Vui lòng liên hệ quản trị viên để được gán hồ sơ nhân viên cho tài khoản này.");
            return "nhanvien/doi-mat-khau";
        }

        String loi = taiKhoanService.doiMatKhau(nv.getTaiKhoan().getMaTaiKhoan(), dto.getMatKhauCu(), dto.getMatKhauMoi());
        if (loi != null) {
            model.addAttribute("loiMatKhauCu", loi);
            model.addAttribute("activeMenu", "nvcanhan");
            return "nhanvien/doi-mat-khau";
        }

        redirectAttributes.addFlashAttribute("thongBao", "Đổi mật khẩu thành công.");
        return "redirect:/nv-ca-nhan";
    }
}