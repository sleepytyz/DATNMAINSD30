package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.GiamGia;
import com.example.th06876_java202.Entity.VoucherEmailDTO;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.thymeleaf.context.Context;
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    @Autowired private org.thymeleaf.TemplateEngine templateEngine;

    @Async
    public void sendOtp(String to, String otp) {

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("OTP đặt lại mật khẩu");
        msg.setText("Mã OTP của bạn là: " + otp + "\nCó hiệu lực trong 5 phút");

        mailSender.send(msg);
    }

    @Async
    public void sendAccountDetails(String to, String hoTen, String username, String password) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariable("hoTen", hoTen);
            context.setVariable("username", username);
            context.setVariable("password", password);

            String htmlContent = templateEngine.process("email/email-template", context);

            helper.setTo(to);
            helper.setSubject("Thông tin tài khoản hệ thống");
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async
    public void sendVoucherEmail(String toEmail, VoucherEmailDTO dto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("voucher", dto);

            String html = templateEngine.process("email/email-voucher", context);

            helper.setTo(toEmail);
            helper.setSubject("Voucher giảm giá mới");
            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Email lỗi: " + e.getMessage());
        }
    }

    @Async
    public void sendVoucherStopEmail(String toEmail, VoucherEmailDTO dto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("voucher", dto);

            String html = templateEngine.process("email/email-voucher-stop", context);

            helper.setTo(toEmail);
            helper.setSubject("❌ Thông báo ngừng chương trình giảm giá");
            helper.setText(html, true);

            mailSender.send(message);
            System.out.println("Stop voucher email sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("Email stop voucher lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void sendSimpleEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            System.out.println("Simple email sent to: " + to);
        } catch (Exception e) {
            System.err.println("Failed to send simple email to: " + to);
            e.printStackTrace();
        }
    }

    @Async
    public void sendVoucherActivationEmail(String toEmail, VoucherEmailDTO dto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("voucher", dto);

            String html = templateEngine.process("email/email-voucher-activated", context);

            helper.setTo(toEmail);
            helper.setSubject("🎉 Voucher giảm giá đã được kích hoạt!");
            helper.setText(html, true);

            mailSender.send(message);
            System.out.println("Activation email sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("Email activation lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * EMAIL XIN LỖI KHI ĐƠN BỊ HUỶ VÌ HẾT HÀNG.
     * Xảy ra khi hai khách cùng mua lượng tồn cuối: người thanh toán xong TRƯỚC được
     * trừ kho và giữ đơn; người sau bị huỷ đơn và nhận email này.
     * Chạy nền (@Async) để khách không phải chờ gửi mail xong mới thấy trang kết quả;
     * mọi lỗi gửi mail đều được nuốt để không làm hỏng luồng thanh toán.
     */
    @Async
    public void guiEmailXinLoiHetHang(String to, String hoTen, String maHoaDon,
                                      String tenSanPham, String phuongThucThanhToan) {
        if (to == null || to.isBlank()) {
            System.out.println("[EMAIL] Bỏ qua thư xin lỗi: khách không có email (đơn " + maHoaDon + ").");
            return;
        }
        try {
            boolean daTraTruoc = phuongThucThanhToan != null
                    && (phuongThucThanhToan.toLowerCase().contains("vnpay")
                        || phuongThucThanhToan.toLowerCase().contains("chuyển khoản"));
            String ten = (hoTen == null || hoTen.isBlank()) ? "Quý khách" : hoTen;
            String sp = (tenSanPham == null || tenSanPham.isBlank()) ? "sản phẩm bạn đặt" : tenSanPham;

            // Ghép chuỗi trực tiếp — KHÔNG dùng String.formatted() để tránh lỗi khi nội
            // dung có ký tự '%' (vd "hoàn tiền 100%") khiến toàn bộ email không gửi được.
            String khoiHoanTien = daTraTruoc
                    ? "<p style=\"background:#fff7ed;border-left:4px solid #f59e0b;padding:11px 14px;"
                      + "border-radius:6px;margin:14px 0\"><b>Về khoản tiền đã thanh toán:</b> chúng tôi sẽ "
                      + "hoàn tiền 100% về phương thức bạn đã dùng trong 3–5 ngày làm việc.</p>"
                    : "";

            String html =
                "<div style=\"font-family:Segoe UI,Arial,sans-serif;max-width:600px;margin:0 auto;"
                + "border:1px solid #eee;border-radius:12px;overflow:hidden\">"
                + "<div style=\"background:#c92327;color:#fff;padding:18px 22px\">"
                + "<h2 style=\"margin:0;font-size:19px\">FS Shoes — Thông báo về đơn hàng</h2></div>"
                + "<div style=\"padding:22px;color:#222;line-height:1.65;font-size:14.5px\">"
                + "<p>Xin chào <b>" + ten + "</b>,</p>"
                + "<p>FS Shoes thành thật xin lỗi vì đơn hàng <b>#" + maHoaDon + "</b> của bạn "
                + "<b>không thể hoàn tất</b>.</p>"
                + "<p><b>Lý do:</b> sản phẩm <b>" + sp + "</b> vừa được khách hàng khác mua hết đúng "
                + "thời điểm bạn thanh toán nên kho không còn đủ hàng. Đơn của bạn đã được huỷ tự động.</p>"
                + khoiHoanTien
                + "<p>Chúng tôi rất tiếc vì sự bất tiện này. Bạn có thể xem các mẫu tương tự còn hàng "
                + "trên website, hoặc liên hệ để được hỗ trợ đặt lại ngay khi hàng về.</p>"
                + "<p style=\"margin-top:22px\">Trân trọng,<br><b>Đội ngũ FS Shoes</b></p></div>"
                + "<div style=\"background:#fafafa;padding:13px 22px;color:#888;font-size:12px\">"
                + "Email tự động — vui lòng không trả lời thư này.</div></div>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("FS Shoes xin lỗi — đơn hàng #" + maHoaDon + " không thể hoàn tất");
            helper.setText(html, true);
            mailSender.send(message);
            System.out.println("[EMAIL] Đã gửi thư xin lỗi hết hàng tới " + to + " (đơn " + maHoaDon + ").");
        } catch (Exception ex) {
            System.err.println("[EMAIL] LỖI gửi thư xin lỗi cho " + to + " (đơn " + maHoaDon + "): " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}