package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Storefront.HoTroChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MODULE HỖ TRỢ TRỰC TUYẾN (khu quản lý — ADMIN/STAFF):
 *  - Trang /hotro/index: danh sách hội thoại + khung chat 2 chiều thời gian thực
 *  - Nhân viên xem ĐẦY ĐỦ lịch sử (tin của khách + tin CHATBOT đã trả lời)
 *  - Trả lời khách ngay trong trang; tin đến/đi đều realtime, không cần reload.
 */
@Controller
@RequiredArgsConstructor
public class HoTroQuanLyController {

    private final HoTroChatService hoTroChatService;

    /** Trang module Hỗ trợ trực tuyến. */
    @GetMapping("/hotro/index")
    public String trang() {
        return "hotro/index";
    }

    /** Danh sách hội thoại (tin cuối + số tin khách chưa đọc). */
    @GetMapping("/api/quan-ly/ho-tro/phien")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> danhSachPhien() {
        return ResponseEntity.ok(hoTroChatService.danhSachPhien());
    }

    /** Mở 1 hội thoại: trả toàn bộ lịch sử (khách + bot + nhân viên) và đánh dấu đã đọc. */
    @GetMapping("/api/quan-ly/ho-tro/phien/{maPhien}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> lichSuPhien(@PathVariable String maPhien) {
        hoTroChatService.danhDauDaXem(maPhien);
        Map<String, Object> kq = new HashMap<>();
        kq.put("maPhien", maPhien);
        kq.put("tinNhans", hoTroChatService.lichSu(maPhien));
        return ResponseEntity.ok(kq);
    }

    /** Nhân viên gửi trả lời cho 1 phiên. */
    @PostMapping("/api/quan-ly/ho-tro/tra-loi")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> traLoi(@RequestBody Map<String, String> body,
                                                      Authentication auth) {
        String maPhien = body != null ? body.get("maPhien") : null;
        String noiDung = body != null ? body.get("noiDung") : null;
        if (maPhien == null || maPhien.isBlank()) return ResponseEntity.badRequest().build();
        String tenNV = "Nhân viên " + (auth != null ? auth.getName() : "hỗ trợ");
        Map<String, Object> tin = hoTroChatService.nhanVienTraLoi(maPhien, noiDung, tenNV);
        Map<String, Object> kq = new HashMap<>();
        kq.put("tin", tin);
        return ResponseEntity.ok(kq);
    }

    /** Tổng số tin khách chưa đọc (hiển thị badge). */
    @GetMapping("/api/quan-ly/ho-tro/chua-doc")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> chuaDoc() {
        Map<String, Object> kq = new HashMap<>();
        kq.put("tong", hoTroChatService.tongChuaDoc());
        return ResponseEntity.ok(kq);
    }
}