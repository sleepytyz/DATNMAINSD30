package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.ChatLieu;
import com.example.th06876_java202.Service.ChatLieuService;
import com.example.th06876_java202.Service.ExcelExportService;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/chatlieu")
public class ChatLieuController {

    private final ChatLieuService chatLieuService;
    private final ExcelExportService excelExportService;

    public ChatLieuController(ChatLieuService chatLieuService, ExcelExportService excelExportService) {
        this.chatLieuService = chatLieuService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/index")
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai,
            Model model) {

        Boolean status = null;
        if (trangThai != null && !trangThai.isEmpty()) {
            status = Boolean.parseBoolean(trangThai);
        }

        Page<ChatLieu> pageData = chatLieuService.searchAndFilter(
                keyword,
                status,
                PageRequest.of(page, size)
        );

        long totalActive = chatLieuService.countByTrangThai(true);
        long totalInactive = chatLieuService.countByTrangThai(false);

        model.addAttribute("listcl", pageData.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());
        model.addAttribute("totalActive", totalActive);
        model.addAttribute("totalInactive", totalInactive);

        String generatedCode = chatLieuService.generateMaChatLieu();

        if (!model.containsAttribute("chatlieu")) {
            ChatLieu newChatLieu = new ChatLieu();
            newChatLieu.setMaChatLieu(generatedCode);
            model.addAttribute("chatlieu", newChatLieu);
        } else {
            ChatLieu existing = (ChatLieu) model.getAttribute("chatlieu");
            if (existing != null && (existing.getMaChatLieu() == null || existing.getMaChatLieu().isEmpty())) {
                existing.setMaChatLieu(generatedCode);
            }
        }

        return "chatlieu/index";
    }

    // ===== API TÌM KIẾM AJAX (KHÔNG RELOAD) =====
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchAjax(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Boolean status = null;
        if (trangThai != null && !trangThai.isEmpty()) {
            status = Boolean.parseBoolean(trangThai);
        }

        Page<ChatLieu> pageData = chatLieuService.searchAndFilter(
                keyword,
                status,
                PageRequest.of(page, size)
        );

        long totalActive = chatLieuService.countByTrangThai(true);
        long totalInactive = chatLieuService.countByTrangThai(false);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("listcl", pageData.getContent());
        response.put("currentPage", page);
        response.put("totalPages", pageData.getTotalPages());
        response.put("totalItems", pageData.getTotalElements());
        response.put("totalActive", totalActive);
        response.put("totalInactive", totalInactive);

        return ResponseEntity.ok(response);
    }

    // ===== API TOGGLE STATUS BẰNG AJAX =====
    @PostMapping("/api/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleStatusAjax(@PathVariable("id") String id,
                                                                CsrfToken csrfToken) {
        Map<String, Object> response = new HashMap<>();

        try {
            ChatLieu chatLieu = chatLieuService.doiTrangThai(id);
            if (chatLieu != null) {
                String message = chatLieu.isTrangThai()
                        ? "Đã kích hoạt chất liệu '" + chatLieu.getTenChatLieu() + "' thành công!"
                        : "Đã ngừng hoạt động chất liệu '" + chatLieu.getTenChatLieu() + "' thành công!";
                response.put("success", true);
                response.put("message", message);
                response.put("trangThai", chatLieu.isTrangThai());
                response.put("tenChatLieu", chatLieu.getTenChatLieu());
                response.put("maChatLieu", chatLieu.getMaChatLieu());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy chất liệu!");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("chatlieu") @Valid ChatLieu chatLieu,
                      Errors errors,
                      Model model,
                      RedirectAttributes redirectAttributes) {

        String normalizedTen = chatLieuService.normalizeTenChatLieu(chatLieu.getTenChatLieu());
        chatLieu.setTenChatLieu(normalizedTen);

        if (errors.hasErrors()) {
            String newCode = chatLieuService.generateMaChatLieu();
            chatLieu.setMaChatLieu(newCode);

            Page<ChatLieu> pageData = chatLieuService.getallpage(PageRequest.of(0, 5));
            model.addAttribute("listcl", pageData.getContent());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", pageData.getTotalPages());
            model.addAttribute("totalItems", pageData.getTotalElements());
            model.addAttribute("chatlieu", chatLieu);
            model.addAttribute("showModal", true);
            return "chatlieu/index";
        }

        if (chatLieuService.existsByTenChatLieu(chatLieu.getTenChatLieu())) {
            String newCode = chatLieuService.generateMaChatLieu();
            chatLieu.setMaChatLieu(newCode);

            redirectAttributes.addFlashAttribute("mess", "Chất liệu '" + chatLieu.getTenChatLieu() + "' đã tồn tại!");
            redirectAttributes.addFlashAttribute("chatlieu", chatLieu);
            redirectAttributes.addFlashAttribute("showModal", true);
            return "redirect:/chatlieu/index";
        }

        if (chatLieu.getMaChatLieu() == null || chatLieu.getMaChatLieu().isEmpty()) {
            String newCode = chatLieuService.generateMaChatLieu();
            chatLieu.setMaChatLieu(newCode);
        }

        chatLieu.setTrangThai(true);
        chatLieuService.add(chatLieu);
        redirectAttributes.addFlashAttribute("successMess",
                "Thêm chất liệu '" + chatLieu.getTenChatLieu() + "' (mã: " + chatLieu.getMaChatLieu() + ") thành công!");
        return "redirect:/chatlieu/index";
    }

    // ===== API THÊM CHẤT LIỆU BẰNG AJAX =====
    @PostMapping("/add-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addAjax(@RequestBody ChatLieu chatLieu) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (chatLieu.getTenChatLieu() == null || chatLieu.getTenChatLieu().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Tên chất liệu không được để trống!");
                return ResponseEntity.badRequest().body(response);
            }

            String normalizedTen = chatLieuService.normalizeTenChatLieu(chatLieu.getTenChatLieu());
            chatLieu.setTenChatLieu(normalizedTen);

            if (chatLieuService.existsByTenChatLieu(chatLieu.getTenChatLieu())) {
                response.put("success", false);
                response.put("message", "Chất liệu '" + chatLieu.getTenChatLieu() + "' đã tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            if (chatLieu.getMaChatLieu() == null || chatLieu.getMaChatLieu().isEmpty()) {
                String newCode = chatLieuService.generateMaChatLieu();
                chatLieu.setMaChatLieu(newCode);
            }

            chatLieu.setTrangThai(true);
            ChatLieu saved = chatLieuService.add(chatLieu);

            response.put("success", true);
            response.put("message", "Thêm chất liệu '" + saved.getTenChatLieu() + "' (mã: " + saved.getMaChatLieu() + ") thành công!");
            response.put("data", saved);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ===== EXPORT EXCEL =====
    @GetMapping("/export-excel")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai) throws IOException {

        Boolean status = null;
        if (trangThai != null && !trangThai.isEmpty()) {
            status = Boolean.parseBoolean(trangThai);
        }

        List<ChatLieu> list = chatLieuService.searchAll(keyword, status);
        ByteArrayInputStream excelStream = excelExportService.exportChatLieuToExcel(list);

        if (excelStream == null) {
            throw new IOException("Không thể tạo file Excel");
        }

        String fileName = "Danh_sach_chat_lieu_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(excelStream));
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable("id") String id,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String trangThai,
                               RedirectAttributes redirectAttributes) {
        try {
            ChatLieu chatLieu = chatLieuService.doiTrangThai(id);
            if (chatLieu != null) {
                String message = chatLieu.isTrangThai()
                        ? "Đã kích hoạt chất liệu '" + chatLieu.getTenChatLieu() + "' thành công!"
                        : "Đã ngừng hoạt động chất liệu '" + chatLieu.getTenChatLieu() + "' thành công!";
                redirectAttributes.addFlashAttribute("successMess", message);
            } else {
                redirectAttributes.addFlashAttribute("mess", "Không tìm thấy chất liệu!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mess", "Cập nhật trạng thái thất bại: " + e.getMessage());
        }

        StringBuilder url = new StringBuilder("redirect:/chatlieu/index?page=" + page);
        if (keyword != null && !keyword.isEmpty()) {
            url.append("&keyword=").append(keyword);
        }
        if (trangThai != null && !trangThai.isEmpty()) {
            url.append("&trangThai=").append(trangThai);
        }

        return url.toString();
    }

    // ===== API CHO GỢI Ý TÌM KIẾM =====
    @GetMapping("/api/search-suggestions")
    @ResponseBody
    public ResponseEntity<List<ChatLieu>> searchSuggestions(@RequestParam("q") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<ChatLieu> suggestions = chatLieuService.searchSuggestions(keyword.trim());
        return ResponseEntity.ok(suggestions);
    }
}