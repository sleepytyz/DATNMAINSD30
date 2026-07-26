package com.example.th06876_java202.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tai-khoan")
public class TaiKhoanController {

//    private final AccountServiceImpl serviceac;
//    private final NhanVienRepository nhanVienRepo;
//
//    public AccountController(AccountServiceImpl serviceac, NhanVienRepository nhanVienRepo) {
//        this.serviceac = serviceac;
//        this.nhanVienRepo = nhanVienRepo;
//    }
//
//    @GetMapping
//    public String index(@RequestParam(required = false) String keyword, Model model) {
//        model.addAttribute("activeMenu", "taikhoan");
//        model.addAttribute("list", serviceac.search(keyword));
//        model.addAttribute("account", new TaiKhoan());
//        model.addAttribute("listNhanVien", nhanVienRepo.findAll());
//        return "account/index";
//    }
//
//    @PostMapping("/add")
//    public String add(@ModelAttribute("account") TaiKhoan accounttt, RedirectAttributes redirectAttributes) {
//        try {
//            accounttt.setTrangThai(true);
//            serviceac.save(accounttt);
//            redirectAttributes.addFlashAttribute("successMessage", "Thêm tài khoản mới thành công!");
//        } catch (DataIntegrityViolationException e) {
//            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Nhân viên được chọn đã có tài khoản hoặc không hợp lệ!");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("errorMessage", "Hệ thống gặp sự cố khi lưu dữ liệu!");
//        }
//        return "redirect:/tai-khoan";
//    }
//
//    @GetMapping("/lock/{id}")
//    public String lock(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
//        serviceac.lock(id);
//        redirectAttributes.addFlashAttribute("successMessage", "Đã khóa tài khoản thành công!");
//        return "redirect:/tai-khoan";
//    }
//
//    @GetMapping("/unlock/{id}")
//    public String unlock(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
//        serviceac.unlock(id);
//        redirectAttributes.addFlashAttribute("successMessage", "Đã mở khóa tài khoản thành công!");
//        return "redirect:/tai-khoan";
//    }
//
//    @GetMapping("/delete/{id}")
//    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
//        try {
//            serviceac.delete(id);
//            redirectAttributes.addFlashAttribute("successMessage", "Xóa tài khoản thành công!");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa tài khoản đã phát sinh dữ liệu hóa đơn/lịch sử!");
//        }
//        return "redirect:/tai-khoan";
//    }
}