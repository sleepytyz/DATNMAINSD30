package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.TaiKhoan;
import com.example.th06876_java202.Service.EmailService;
import com.example.th06876_java202.Service.OtpService;
import com.example.th06876_java202.Service.TaiKhoanService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/forgot")
@RequiredArgsConstructor
public class QuenMatKhauController {
    private final EmailService emailService;
    private final OtpService otpService;
    private final TaiKhoanService taiKhoanService;
    private final PasswordEncoder passwordEncoder;

    // Hiển thị form nhập email
    @GetMapping
    public String form(Model model) {
        model.addAttribute("step", "email");
        return "account/email";
    }

    // Gửi OTP qua email
    @PostMapping("/send-otp")
    public String sendOtp(@RequestParam String email,
                          HttpSession session,
                          Model model) {

        TaiKhoan acc = taiKhoanService.findByEmail(email);

        if (acc == null) {
            model.addAttribute("error", "Email không tồn tại trong hệ thống!");
            model.addAttribute("step", "email");
            return "account/email";
        }

        String otp = otpService.generateOtp();
        emailService.sendOtp(email, otp);

        session.setAttribute("resetEmail", email);
        session.setAttribute("otp", otp);
        session.setAttribute("otpTime", System.currentTimeMillis());

        model.addAttribute("step", "otp");
        return "account/email";
    }

    // Xác thực OTP
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String otp,
                            HttpSession session,
                            Model model) {

        String sessionOtp = (String) session.getAttribute("otp");
        Long otpTime = (Long) session.getAttribute("otpTime");

        if (sessionOtp == null || otpTime == null) {
            model.addAttribute("error", "OTP không tồn tại, vui lòng gửi lại!");
            model.addAttribute("step", "otp");
            return "account/email";
        }

        // Kiểm tra OTP có hết hạn chưa (5 phút)
        if (System.currentTimeMillis() - otpTime > 5 * 60 * 1000) {
            model.addAttribute("error", "OTP đã hết hạn, vui lòng gửi lại!");
            model.addAttribute("step", "otp");
            return "account/email";
        }

        if (!sessionOtp.equals(otp)) {
            model.addAttribute("error", "Mã OTP không đúng!");
            model.addAttribute("step", "otp");
            return "account/email";
        }

        model.addAttribute("step", "reset");
        return "account/email";
    }

    // Đặt lại mật khẩu mới
    @PostMapping("/reset")
    public String reset(@RequestParam String newPassword,
                        HttpSession session,
                        Model model) {

        String email = (String) session.getAttribute("resetEmail");

        if (email == null) {
            return "redirect:/forgot";
        }

        TaiKhoan acc = taiKhoanService.findByEmail(email);

        if (acc == null) {
            return "redirect:/forgot";
        }

        // Mã hóa mật khẩu mới trước khi lưu
        acc.setMatKhau(passwordEncoder.encode(newPassword));
        taiKhoanService.save(acc);

        // Xóa session sau khi reset thành công
        session.invalidate();

        return "redirect:/login?resetSuccess";
    }
}