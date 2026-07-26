package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.MauSac;
import com.example.th06876_java202.Service.MauSacService;
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
@RequestMapping("/mausac")
public class MauSacController {

    @Autowired
    private MauSacService mauSacService;

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

        // KHÔNG truyền Sort vào PageRequest
        Page<MauSac> pageData = mauSacService.searchAndFilter(
                keyword,
                status,
                PageRequest.of(page, size)
        );

        long totalActive = mauSacService.countByTrangThai(true);
        long totalInactive = mauSacService.countByTrangThai(false);

        model.addAttribute("listms", pageData.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());
        model.addAttribute("totalActive", totalActive);
        model.addAttribute("totalInactive", totalInactive);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedTrangThai", trangThai);

        String generatedCode = mauSacService.generateMaMauSac();

        if (!model.containsAttribute("mausac")) {
            MauSac newMauSac = new MauSac();
            newMauSac.setMaMauSac(generatedCode);
            model.addAttribute("mausac", newMauSac);
        } else {
            MauSac existing = (MauSac) model.getAttribute("mausac");
            if (existing != null && (existing.getMaMauSac() == null || existing.getMaMauSac().isEmpty())) {
                existing.setMaMauSac(generatedCode);
            }
        }

        return "mausac/index";
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

            // KHÔNG truyền Sort vào PageRequest
            Page<MauSac> pageData = mauSacService.searchAndFilter(
                    keyword,
                    status,
                    PageRequest.of(page, size)
            );

            long totalActive = mauSacService.countByTrangThai(true);
            long totalInactive = mauSacService.countByTrangThai(false);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("listms", pageData.getContent());
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
        response.put("code", mauSacService.generateMaMauSac());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleStatusAjax(@PathVariable("id") String id) {
        Map<String, Object> response = new HashMap<>();

        try {
            MauSac ms = mauSacService.doiTrangThai(id);
            if (ms != null) {
                String message = ms.isTrangThai()
                        ? "Đã kích hoạt màu sắc '" + ms.getTenMauSac() + "' thành công!"
                        : "Đã ngừng hoạt động màu sắc '" + ms.getTenMauSac() + "' thành công!";
                response.put("success", true);
                response.put("message", message);
                response.put("trangThai", ms.isTrangThai());
                response.put("tenMauSac", ms.getTenMauSac());
                response.put("maMauSac", ms.getMaMauSac());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy màu sắc!");
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
    public ResponseEntity<Map<String, Object>> addAjax(@RequestBody MauSac mauSac) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (mauSac.getTenMauSac() == null || mauSac.getTenMauSac().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Tên màu sắc không được để trống!");
                return ResponseEntity.badRequest().body(response);
            }

            String normalizedTen = mauSacService.normalizeTenMauSac(mauSac.getTenMauSac());
            mauSac.setTenMauSac(normalizedTen);

            if (mauSacService.existsByTenMauSac(mauSac.getTenMauSac())) {
                response.put("success", false);
                response.put("message", "Màu sắc '" + mauSac.getTenMauSac() + "' đã tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            if (mauSac.getMaMauSac() == null || mauSac.getMaMauSac().isEmpty()) {
                String newCode = mauSacService.generateMaMauSac();
                mauSac.setMaMauSac(newCode);
            }

            mauSac.setTrangThai(true);
            MauSac saved = mauSacService.add(mauSac);

            response.put("success", true);
            response.put("message", "Thêm màu sắc '" + saved.getTenMauSac() + "' thành công!");
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
    public ResponseEntity<List<MauSac>> searchSuggestions(@RequestParam("q") String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.ok(List.of());
            }
            List<MauSac> suggestions = mauSacService.searchSuggestions(keyword.trim());
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("mausac") @Valid MauSac mauSac,
                      Errors errors,
                      Model model,
                      RedirectAttributes redirectAttributes) {

        String normalizedTen = mauSacService.normalizeTenMauSac(mauSac.getTenMauSac());
        mauSac.setTenMauSac(normalizedTen);

        if (errors.hasErrors()) {
            String newCode = mauSacService.generateMaMauSac();
            mauSac.setMaMauSac(newCode);

            Page<MauSac> pageData = mauSacService.getallpage(PageRequest.of(0, 5));
            model.addAttribute("listms", pageData.getContent());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", pageData.getTotalPages());
            model.addAttribute("totalItems", pageData.getTotalElements());
            model.addAttribute("mausac", mauSac);
            return "mausac/index";
        }

        if (mauSacService.existsByTenMauSac(mauSac.getTenMauSac())) {
            String newCode = mauSacService.generateMaMauSac();
            mauSac.setMaMauSac(newCode);

            redirectAttributes.addFlashAttribute("mess", "Màu sắc '" + mauSac.getTenMauSac() + "' đã tồn tại!");
            redirectAttributes.addFlashAttribute("mausac", mauSac);
            return "redirect:/mausac/index";
        }

        if (mauSac.getMaMauSac() == null || mauSac.getMaMauSac().isEmpty()) {
            String newCode = mauSacService.generateMaMauSac();
            mauSac.setMaMauSac(newCode);
        }

        mauSac.setTrangThai(true);
        mauSacService.add(mauSac);

        redirectAttributes.addFlashAttribute("successMess",
                "Thêm màu sắc '" + mauSac.getTenMauSac() + "' thành công!");
        return "redirect:/mausac/index";
    }

    @GetMapping("/capnhatt/{id}")
    public String capnhatt(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            MauSac dmsp = mauSacService.doiTrangThai(id);

            if (dmsp != null) {
                if (dmsp.isTrangThai()) {
                    redirectAttributes.addFlashAttribute("successMess", "Mở hoạt động thành công cho '" + dmsp.getTenMauSac() + "'");
                } else {
                    redirectAttributes.addFlashAttribute("successMess", "Ngừng hoạt động thành công cho '" + dmsp.getTenMauSac() + "'");
                }
            } else {
                redirectAttributes.addFlashAttribute("mess", "Không tìm thấy màu sắc!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mess", "Cập nhật trạng thái thất bại: " + e.getMessage());
        }
        return "redirect:/mausac/index";
    }

    @GetMapping("/export-excel")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai) throws IOException {

        Boolean status = null;
        if (trangThai != null && !trangThai.isEmpty()) {
            status = Boolean.parseBoolean(trangThai);
        }

        List<MauSac> list = mauSacService.searchAll(keyword, status);
        ByteArrayInputStream excelStream = excelExportService.exportMauSacToExcel(list);

        if (excelStream == null) {
            throw new IOException("Không thể tạo file Excel");
        }

        String fileName = "Danh_sach_mau_sac_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(excelStream));
    }
}