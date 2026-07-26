package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.KichThuoc;
import com.example.th06876_java202.Service.KichThuocService;
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
import org.springframework.validation.BindingResult;
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
@RequestMapping("/kichthuoc")
public class KichThuocController {

    private final KichThuocService kichThuocService;
    private final ExcelExportService excelExportService;

    public KichThuocController(KichThuocService kichThuocService, ExcelExportService excelExportService) {
        this.kichThuocService = kichThuocService;
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

        Page<KichThuoc> pageData = kichThuocService.searchAndFilter(
                keyword,
                status,
                PageRequest.of(page, size)
        );

        long totalActive = kichThuocService.countByTrangThai(true);
        long totalInactive = kichThuocService.countByTrangThai(false);

        model.addAttribute("listk", pageData.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());
        model.addAttribute("totalActive", totalActive);
        model.addAttribute("totalInactive", totalInactive);

        String generatedCode = kichThuocService.generateMaKichThuoc();

        if (!model.containsAttribute("kichthuoc")) {
            KichThuoc newKichThuoc = new KichThuoc();
            newKichThuoc.setMaKichThuoc(generatedCode);
            model.addAttribute("kichthuoc", newKichThuoc);
        } else {
            KichThuoc existing = (KichThuoc) model.getAttribute("kichthuoc");
            if (existing != null && (existing.getMaKichThuoc() == null || existing.getMaKichThuoc().isEmpty())) {
                existing.setMaKichThuoc(generatedCode);
            }
        }

        return "kichthuoc/index";
    }

    // ===== API TÌM KIẾM AJAX =====
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

        Page<KichThuoc> pageData = kichThuocService.searchAndFilter(
                keyword,
                status,
                PageRequest.of(page, size)
        );

        long totalActive = kichThuocService.countByTrangThai(true);
        long totalInactive = kichThuocService.countByTrangThai(false);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("listk", pageData.getContent());
        response.put("currentPage", page);
        response.put("totalPages", pageData.getTotalPages());
        response.put("totalItems", pageData.getTotalElements());
        response.put("totalActive", totalActive);
        response.put("totalInactive", totalInactive);

        return ResponseEntity.ok(response);
    }

    // ===== API TẠO MÃ TỰ ĐỘNG =====
    @GetMapping("/api/generate-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> generateCode() {
        Map<String, String> response = new HashMap<>();
        response.put("code", kichThuocService.generateMaKichThuoc());
        return ResponseEntity.ok(response);
    }

    // ===== API TOGGLE STATUS =====
    @PostMapping("/api/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleStatusAjax(@PathVariable("id") String id,
                                                                CsrfToken csrfToken) {
        Map<String, Object> response = new HashMap<>();

        try {
            KichThuoc kt = kichThuocService.doiTrangThai(id);
            if (kt != null) {
                String message = kt.isTrangThai()
                        ? "Đã kích hoạt kích thước '" + kt.getTenKichThuoc() + "' thành công!"
                        : "Đã ngừng hoạt động kích thước '" + kt.getTenKichThuoc() + "' thành công!";
                response.put("success", true);
                response.put("message", message);
                response.put("trangThai", kt.isTrangThai());
                response.put("tenKichThuoc", kt.getTenKichThuoc());
                response.put("maKichThuoc", kt.getMaKichThuoc());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy kích thước!");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ===== API THÊM KÍCH THƯỚC BẰNG AJAX - BẮT LỖI VALIDATION =====
    @PostMapping("/add-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addAjax(@Valid @RequestBody KichThuoc kichThuoc,
                                                       BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();

        // 🔥 BẮT LỖI VALIDATION TỪ ENTITY
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> {
                errors.put(error.getField(), error.getDefaultMessage());
            });
            response.put("success", false);
            response.put("message", "Dữ liệu không hợp lệ!");
            response.put("errors", errors);
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Chuẩn hóa tên
            String normalizedTen = kichThuocService.normalizeTenKichThuoc(kichThuoc.getTenKichThuoc());
            kichThuoc.setTenKichThuoc(normalizedTen);

            // Kiểm tra trùng tên
            if (kichThuocService.existsByTenKichThuoc(kichThuoc.getTenKichThuoc())) {
                response.put("success", false);
                response.put("message", "Kích thước '" + kichThuoc.getTenKichThuoc() + "' đã tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            // Tạo mã nếu chưa có
            if (kichThuoc.getMaKichThuoc() == null || kichThuoc.getMaKichThuoc().isEmpty()) {
                String newCode = kichThuocService.generateMaKichThuoc();
                kichThuoc.setMaKichThuoc(newCode);
            }

            kichThuoc.setTrangThai(true);
            KichThuoc saved = kichThuocService.add(kichThuoc);

            response.put("success", true);
            response.put("message", "Thêm kích thước '" + saved.getTenKichThuoc() + "' (mã: " + saved.getMaKichThuoc() + ") thành công!");
            response.put("data", saved);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
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

        List<KichThuoc> list = kichThuocService.searchAll(keyword, status);
        ByteArrayInputStream excelStream = excelExportService.exportKichThuocToExcel(list);

        if (excelStream == null) {
            throw new IOException("Không thể tạo file Excel");
        }

        String fileName = "Danh_sach_kich_thuoc_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(excelStream));
    }

    // ===== THÊM MỚI (FORM TRUYỀN THỐNG) =====
    @PostMapping("/add")
    public String add(@ModelAttribute("kichthuoc") @Valid KichThuoc kichThuoc,
                      Errors errors,
                      RedirectAttributes redirectAttributes,
                      Model model) {

        String normalizedTen = kichThuocService.normalizeTenKichThuoc(kichThuoc.getTenKichThuoc());
        kichThuoc.setTenKichThuoc(normalizedTen);

        if (errors.hasErrors()) {
            String newCode = kichThuocService.generateMaKichThuoc();
            kichThuoc.setMaKichThuoc(newCode);

            Page<KichThuoc> pageData = kichThuocService.getallpage(PageRequest.of(0, 5));
            model.addAttribute("listk", pageData.getContent());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", pageData.getTotalPages());
            model.addAttribute("totalItems", pageData.getTotalElements());
            model.addAttribute("kichthuoc", kichThuoc);
            return "kichthuoc/index";
        }

        if (kichThuocService.existsByTenKichThuoc(kichThuoc.getTenKichThuoc())) {
            String newCode = kichThuocService.generateMaKichThuoc();
            kichThuoc.setMaKichThuoc(newCode);

            redirectAttributes.addFlashAttribute("mess", "Kích thước '" + kichThuoc.getTenKichThuoc() + "' đã tồn tại!");
            redirectAttributes.addFlashAttribute("kichthuoc", kichThuoc);
            return "redirect:/kichthuoc/index";
        }

        if (kichThuoc.getMaKichThuoc() == null || kichThuoc.getMaKichThuoc().isEmpty()) {
            String newCode = kichThuocService.generateMaKichThuoc();
            kichThuoc.setMaKichThuoc(newCode);
        }

        kichThuoc.setTrangThai(true);
        kichThuocService.add(kichThuoc);

        redirectAttributes.addFlashAttribute("successMess",
                "Thêm kích thước '" + kichThuoc.getTenKichThuoc() + "' (mã: " + kichThuoc.getMaKichThuoc() + ") thành công!");
        return "redirect:/kichthuoc/index";
    }

    @GetMapping("/capnhatt/{id}")
    public String capnhatt(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            KichThuoc kt = kichThuocService.doiTrangThai(id);

            if (kt != null) {
                if (kt.isTrangThai()) {
                    redirectAttributes.addFlashAttribute("successMess", "Đã kích hoạt kích thước '" + kt.getTenKichThuoc() + "'!");
                } else {
                    redirectAttributes.addFlashAttribute("successMess", "Đã ngừng hoạt động kích thước '" + kt.getTenKichThuoc() + "'!");
                }
            } else {
                redirectAttributes.addFlashAttribute("mess", "Không tìm thấy kích thước!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mess", "Cập nhật trạng thái thất bại: " + e.getMessage());
        }
        return "redirect:/kichthuoc/index";
    }

    // ===== API CHO GỢI Ý TÌM KIẾM =====
    @GetMapping("/api/search-suggestions")
    @ResponseBody
    public ResponseEntity<List<KichThuoc>> searchSuggestions(@RequestParam("q") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<KichThuoc> suggestions = kichThuocService.searchSuggestions(keyword.trim());
        return ResponseEntity.ok(suggestions);
    }
}