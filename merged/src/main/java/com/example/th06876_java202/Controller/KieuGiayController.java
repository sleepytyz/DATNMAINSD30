package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.KieuGiay;
import com.example.th06876_java202.Service.KieuGiayService;
import com.example.th06876_java202.Service.ExcelExportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/kieugiay")
public class KieuGiayController {

    @Autowired
    private KieuGiayService kieuGiayService;

    @Autowired
    private ExcelExportService excelExportService;

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

        Page<KieuGiay> pageData = kieuGiayService.searchAndFilter(
                keyword,
                status,
                PageRequest.of(page, size, Sort.by("ngayTao").descending())
        );

        long totalActive = kieuGiayService.countByTrangThai(true);
        long totalInactive = kieuGiayService.countByTrangThai(false);

        model.addAttribute("listkg", pageData.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());
        model.addAttribute("totalActive", totalActive);
        model.addAttribute("totalInactive", totalInactive);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedTrangThai", trangThai);

        String generatedCode = kieuGiayService.generateMaKieuGiay();

        if (!model.containsAttribute("kieugiay")) {
            KieuGiay newKieuGiay = new KieuGiay();
            newKieuGiay.setMaKieuGiay(generatedCode);
            model.addAttribute("kieugiay", newKieuGiay);
        } else {
            KieuGiay existing = (KieuGiay) model.getAttribute("kieugiay");
            if (existing != null && (existing.getMaKieuGiay() == null || existing.getMaKieuGiay().isEmpty())) {
                existing.setMaKieuGiay(generatedCode);
            }
        }

        return "kieugiay/index";
    }

    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchAjax(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        try {
            Boolean status = null;
            if (trangThai != null && !trangThai.isEmpty()) {
                status = Boolean.parseBoolean(trangThai);
            }

            Page<KieuGiay> pageData = kieuGiayService.searchAndFilter(
                    keyword,
                    status,
                    PageRequest.of(page, size, Sort.by("ngayTao").descending())
            );

            long totalActive = kieuGiayService.countByTrangThai(true);
            long totalInactive = kieuGiayService.countByTrangThai(false);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("listkg", pageData.getContent());
            response.put("currentPage", page);
            response.put("totalPages", pageData.getTotalPages());
            response.put("totalItems", pageData.getTotalElements());
            response.put("totalActive", totalActive);
            response.put("totalInactive", totalInactive);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/generate-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> generateCode() {
        Map<String, String> response = new HashMap<>();
        response.put("code", kieuGiayService.generateMaKieuGiay());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleStatusAjax(@PathVariable("id") String id) {
        Map<String, Object> response = new HashMap<>();

        try {
            KieuGiay kg = kieuGiayService.doiTrangThai(id);
            if (kg != null) {
                String message = kg.isTrangThai()
                        ? "Đã kích hoạt kiểu giày '" + kg.getTenKieuGiay() + "' thành công!"
                        : "Đã ngừng hoạt động kiểu giày '" + kg.getTenKieuGiay() + "' thành công!";
                response.put("success", true);
                response.put("message", message);
                response.put("trangThai", kg.isTrangThai());
                response.put("tenKieuGiay", kg.getTenKieuGiay());
                response.put("maKieuGiay", kg.getMaKieuGiay());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy kiểu giày!");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/add-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addAjax(@RequestBody KieuGiay kieuGiay) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (kieuGiay.getTenKieuGiay() == null || kieuGiay.getTenKieuGiay().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Tên kiểu giày không được để trống!");
                return ResponseEntity.badRequest().body(response);
            }

            String normalizedTen = kieuGiayService.normalizeTenKieuGiay(kieuGiay.getTenKieuGiay());
            kieuGiay.setTenKieuGiay(normalizedTen);

            if (kieuGiayService.existsByTenKieuGiay(kieuGiay.getTenKieuGiay())) {
                response.put("success", false);
                response.put("message", "Kiểu giày '" + kieuGiay.getTenKieuGiay() + "' đã tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            if (kieuGiay.getMaKieuGiay() == null || kieuGiay.getMaKieuGiay().isEmpty()) {
                String newCode = kieuGiayService.generateMaKieuGiay();
                kieuGiay.setMaKieuGiay(newCode);
            }

            kieuGiay.setTrangThai(true);
            KieuGiay saved = kieuGiayService.them(kieuGiay);

            response.put("success", true);
            response.put("message", "Thêm kiểu giày '" + saved.getTenKieuGiay() + "' thành công!");
            response.put("data", saved);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/search-suggestions")
    @ResponseBody
    public ResponseEntity<List<KieuGiay>> searchSuggestions(@RequestParam("q") String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.ok(List.of());
            }
            List<KieuGiay> suggestions = kieuGiayService.searchSuggestions(keyword.trim());
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("kieugiay") @Valid KieuGiay kieuGiay,
                      Errors errors,
                      RedirectAttributes redirectAttributes,
                      Model model) {

        String normalizedTen = kieuGiayService.normalizeTenKieuGiay(kieuGiay.getTenKieuGiay());
        kieuGiay.setTenKieuGiay(normalizedTen);

        if (errors.hasErrors()) {
            String newCode = kieuGiayService.generateMaKieuGiay();
            kieuGiay.setMaKieuGiay(newCode);

            Page<KieuGiay> pageData = kieuGiayService.getallpage(PageRequest.of(0, 5));
            model.addAttribute("listkg", pageData.getContent());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", pageData.getTotalPages());
            model.addAttribute("totalItems", pageData.getTotalElements());
            model.addAttribute("kieugiay", kieuGiay);
            return "kieugiay/index";
        }

        if (kieuGiayService.existsByTenKieuGiay(kieuGiay.getTenKieuGiay())) {
            String newCode = kieuGiayService.generateMaKieuGiay();
            kieuGiay.setMaKieuGiay(newCode);

            redirectAttributes.addFlashAttribute("mess", "Kiểu giày '" + kieuGiay.getTenKieuGiay() + "' đã tồn tại!");
            redirectAttributes.addFlashAttribute("kieugiay", kieuGiay);
            return "redirect:/kieugiay/index";
        }

        if (kieuGiay.getMaKieuGiay() == null || kieuGiay.getMaKieuGiay().isEmpty()) {
            String newCode = kieuGiayService.generateMaKieuGiay();
            kieuGiay.setMaKieuGiay(newCode);
        }

        kieuGiay.setTrangThai(true);
        kieuGiayService.them(kieuGiay);

        redirectAttributes.addFlashAttribute("successMess",
                "Thêm kiểu giày '" + kieuGiay.getTenKieuGiay() + "' thành công!");
        return "redirect:/kieugiay/index";
    }

    @GetMapping("/capnhatt/{id}")
    public String capnhatt(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            KieuGiay dmsp = kieuGiayService.doiTrangThai(id);

            if (dmsp != null) {
                if (dmsp.isTrangThai()) {
                    redirectAttributes.addFlashAttribute("successMess", "Mở hoạt động thành công cho '" + dmsp.getTenKieuGiay() + "'");
                } else {
                    redirectAttributes.addFlashAttribute("successMess", "Ngừng hoạt động thành công cho '" + dmsp.getTenKieuGiay() + "'");
                }
            } else {
                redirectAttributes.addFlashAttribute("mess", "Không tìm thấy kiểu giày!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mess", "Cập nhật trạng thái thất bại: " + e.getMessage());
        }
        return "redirect:/kieugiay/index";
    }

    @GetMapping("/export-excel")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai) throws IOException {

        Boolean status = null;
        if (trangThai != null && !trangThai.isEmpty()) {
            status = Boolean.parseBoolean(trangThai);
        }

        List<KieuGiay> list = kieuGiayService.searchAll(keyword, status);
        ByteArrayInputStream excelStream = excelExportService.exportKieuGiayToExcel(list);

        if (excelStream == null) {
            throw new IOException("Không thể tạo file Excel");
        }

        String fileName = "Danh_sach_kieu_giay_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(excelStream));
    }
}