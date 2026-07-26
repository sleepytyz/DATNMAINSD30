package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.LienHe;
import com.example.th06876_java202.Repository.LienHeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MODULE LIÊN HỆ (khu quản lý — ADMIN/STAFF): xem tin khách gửi từ trang
 * Liên hệ, nhận thông báo realtime khi có tin mới, và TRẢ LỜI KHÁCH QUA GMAIL
 * bằng nút mở sẵn cửa sổ soạn thư (điền sẵn người nhận, tiêu đề và trích dẫn
 * tin nhắn của khách) — gửi xong bấm "Đã xử lý" để đánh dấu.
 */
@Controller
@RequestMapping("/lienhe")
@RequiredArgsConstructor
public class LienHeQuanLyController {

    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final LienHeRepository lienHeRepository;

    @GetMapping("/index")
    public String index(@RequestParam(required = false) String tt, Model model) {
        List<LienHe> tatCa = lienHeRepository.findAllByOrderByMaLienHeDesc();
        String ttLoc = tt != null && !tt.isBlank() ? tt : null;

        List<Map<String, Object>> rows = new ArrayList<>();
        for (LienHe l : tatCa) {
            if (ttLoc != null && !ttLoc.equals(l.getTrangThai())) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("id", l.getMaLienHe());
            m.put("hoTen", l.getHoTen());
            m.put("email", l.getEmail());
            m.put("noiDung", l.getNoiDung());
            m.put("thoiGian", l.getThoiGian() != null ? GIO.format(l.getThoiGian()) : "");
            m.put("daXuLy", "Đã xử lý".equals(l.getTrangThai()));
            m.put("gmailUrl", taoGmailUrl(l));
            m.put("mailtoUrl", taoMailtoUrl(l));
            rows.add(m);
        }

        model.addAttribute("rows", rows);
        model.addAttribute("tongTatCa", tatCa.size());
        model.addAttribute("tongChuaXuLy", lienHeRepository.countByTrangThai("Chưa xử lý"));
        model.addAttribute("fTt", ttLoc);
        return "lienhe/index";
    }

    /** Đánh dấu Đã xử lý / Chưa xử lý. */
    @GetMapping("/xu-ly")
    public String xuLy(@RequestParam Integer id,
                       @RequestParam boolean xong,
                       RedirectAttributes ra) {
        lienHeRepository.findById(id).ifPresent(l -> {
            l.setTrangThai(xong ? "Đã xử lý" : "Chưa xử lý");
            lienHeRepository.save(l);
        });
        ra.addFlashAttribute("thongBao", xong ? "Đã đánh dấu ĐÃ XỬ LÝ." : "Đã chuyển về CHƯA XỬ LÝ.");
        return "redirect:/lienhe/index";
    }

    /* ---------- dựng link trả lời ---------- */

    private String noiDungThu(LienHe l) {
        String khi = l.getThoiGian() != null ? GIO.format(l.getThoiGian()) : "";
        return "Chào " + l.getHoTen() + ",\r\n\r\n"
                + "Cảm ơn bạn đã liên hệ với FS Shoes. Về câu hỏi của bạn:\r\n\r\n"
                + "(Nhập nội dung trả lời tại đây)\r\n\r\n"
                + "----- Tin nhắn của bạn (" + khi + ") -----\r\n"
                + l.getNoiDung() + "\r\n\r\n"
                + "Trân trọng,\r\nFS Shoes — 13 Trịnh Văn Bô, Nam Từ Liêm, Hà Nội\r\n"
                + "Hotline/Zalo: 0344 552 008";
    }

    /** Mở Gmail soạn thư mới, điền sẵn người nhận + tiêu đề + nội dung. */
    private String taoGmailUrl(LienHe l) {
        String su = "FS Shoes phản hồi liên hệ của bạn";
        return "https://mail.google.com/mail/?view=cm&fs=1"
                + "&to=" + url(l.getEmail())
                + "&su=" + url(su)
                + "&body=" + url(noiDungThu(l));
    }

    /** Dự phòng: mở ứng dụng mail mặc định của máy. */
    private String taoMailtoUrl(LienHe l) {
        return "mailto:" + url(l.getEmail())
                + "?subject=" + url("FS Shoes phản hồi liên hệ của bạn")
                + "&body=" + url(noiDungThu(l));
    }

    private String url(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}