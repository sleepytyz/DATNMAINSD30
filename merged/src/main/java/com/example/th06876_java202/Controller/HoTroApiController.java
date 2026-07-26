package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.KhachHang;
import com.example.th06876_java202.Service.KhachHangService;
import com.example.th06876_java202.Storefront.HoTroChatService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * API chat cho WEBSITE BÁN HÀNG (khách + khách vãng lai đều dùng được).
 * Widget trợ lý gọi 2 endpoint này; tin nhắn mới được đẩy realtime qua
 * /topic/hotro/phien/{maPhien} nên không cần polling / reload.
 */
@RestController
@RequestMapping("/api/ho-tro")
@RequiredArgsConstructor
public class HoTroApiController {

    private final HoTroChatService hoTroChatService;
    private final KhachHangService khachHangService;

    private KhachHang khachHienTai(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) return null;
        try {
            return khachHangService.findByTenDangNhap(auth.getName());
        } catch (Exception e) {
            return null;
        }
    }

    /** Khởi tạo widget: trả về mã phiên + toàn bộ lịch sử hội thoại của phiên. */
    @GetMapping("/lich-su")
    public ResponseEntity<Map<String, Object>> lichSu(HttpSession session, Authentication auth) {
        KhachHang kh = khachHienTai(auth);
        String maPhien = hoTroChatService.maPhien(session, kh);
        Map<String, Object> kq = new HashMap<>();
        kq.put("maPhien", maPhien);
        kq.put("tenHienThi", hoTroChatService.tenHienThi(maPhien, kh));
        kq.put("tinNhans", hoTroChatService.lichSu(maPhien));
        return ResponseEntity.ok(kq);
    }

    /** Khách gửi 1 tin nhắn. Bot sẽ tự trả lời (trừ khi nhân viên đang hỗ trợ trực tiếp). */
    @PostMapping("/gui")
    public ResponseEntity<Map<String, Object>> gui(@RequestBody Map<String, String> body,
                                                   HttpSession session, Authentication auth) {
        KhachHang kh = khachHienTai(auth);
        String maPhien = hoTroChatService.maPhien(session, kh);
        String ten = hoTroChatService.tenHienThi(maPhien, kh);
        String noiDung = body != null ? body.get("noiDung") : null;

        Map<String, Object> kq = new HashMap<>();
        kq.put("maPhien", maPhien);
        kq.put("tinMoi", hoTroChatService.khachGui(maPhien, kh, ten, noiDung));
        return ResponseEntity.ok(kq);
    }
}