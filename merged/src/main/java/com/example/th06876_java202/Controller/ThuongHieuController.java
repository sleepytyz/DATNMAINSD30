package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.ThuongHieu;
import com.example.th06876_java202.Service.ThuongHieuService;
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
@RequestMapping("/thuonghieu")
public class ThuongHieuController {

    @Autowired
    private ThuongHieuService thuongHieuService;

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

        Page<ThuongHieu> pageData = thuongHieuService.searchAndFilter(
                keyword,
                status,
                PageRequest.of(page, size, Sort.by("ngayTao").descending())
        );

        long totalActive = thuongHieuService.countByTrangThai(true);
        long totalInactive = thuongHieuService.countByTrangThai(false);

        model.addAttribute("listth", pageData.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());
        model.addAttribute("totalActive", totalActive);
        model.addAttribute("totalInactive", totalInactive);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedTrangThai", trangThai);

        String generatedCode = thuongHieuService.generateMaThuongHieu();

        if (!model.containsAttribute("thuonghieu")) {
            ThuongHieu newThuongHieu = new ThuongHieu();
            newThuongHieu.setMaThuongHieu(generatedCode);
            model.addAttribute("thuonghieu", newThuongHieu);
        } else {
            ThuongHieu existing = (ThuongHieu) model.getAttribute("thuonghieu");
            if (existing != null && (existing.getMaThuongHieu() == null || existing.getMaThuongHieu().isEmpty())) {
                existing.setMaThuongHieu(generatedCode);
            }
        }

        return "thuonghieu/index";
    }

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

        Page<ThuongHieu> pageData = thuongHieuService.searchAndFilter(
                keyword,
                status,
                PageRequest.of(page, size, Sort.by("ngayTao").descending())
        );

        long totalActive = thuongHieuService.countByTrangThai(true);
        long totalInactive = thuongHieuService.countByTrangThai(false);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("listth", pageData.getContent());
        response.put("currentPage", page);
        response.put("totalPages", pageData.getTotalPages());
        response.put("totalItems", pageData.getTotalElements());
        response.put("totalActive", totalActive);
        response.put("totalInactive", totalInactive);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/generate-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> generateCode() {
        Map<String, String> response = new HashMap<>();
        response.put("code", thuongHieuService.generateMaThuongHieu());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleStatusAjax(@PathVariable("id") String id) {
        Map<String, Object> response = new HashMap<>();

        try {
            ThuongHieu th = thuongHieuService.doiTrangThai(id);
            if (th != null) {
                String message = th.isTrangThai()
                        ? "Đã kích hoạt thương hiệu '" + th.getTenThuongHieu() + "' thành công!"
                        : "Đã ngừng hoạt động thương hiệu '" + th.getTenThuongHieu() + "' thành công!";
                response.put("success", true);
                response.put("message", message);
                response.put("trangThai", th.isTrangThai());
                response.put("tenThuongHieu", th.getTenThuongHieu());
                response.put("maThuongHieu", th.getMaThuongHieu());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy thương hiệu!");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/add-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addAjax(@RequestBody ThuongHieu thuongHieu) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (thuongHieu.getTenThuongHieu() == null || thuongHieu.getTenThuongHieu().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Tên thương hiệu không được để trống!");
                return ResponseEntity.badRequest().body(response);
            }

            String normalizedTen = thuongHieuService.normalizeTenThuongHieu(thuongHieu.getTenThuongHieu());
            thuongHieu.setTenThuongHieu(normalizedTen);

            if (thuongHieuService.ktraten(thuongHieu.getTenThuongHieu())) {
                response.put("success", false);
                response.put("message", "Thương hiệu '" + thuongHieu.getTenThuongHieu() + "' đã tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            if (thuongHieu.getMaThuongHieu() == null || thuongHieu.getMaThuongHieu().isEmpty()) {
                String newCode = thuongHieuService.generateMaThuongHieu();
                thuongHieu.setMaThuongHieu(newCode);
            }

            thuongHieu.setTrangThai(true);
            ThuongHieu saved = thuongHieuService.them(thuongHieu);

            response.put("success", true);
            response.put("message", "Thêm thương hiệu '" + saved.getTenThuongHieu() + "' thành công!");
            response.put("data", saved);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/search-suggestions")
    @ResponseBody
    public ResponseEntity<List<ThuongHieu>> searchSuggestions(@RequestParam("q") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<ThuongHieu> suggestions = thuongHieuService.searchSuggestions(keyword.trim());
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("thuonghieu") @Valid ThuongHieu thuongHieu,
                      Errors errors,
                      RedirectAttributes redirectAttributes,
                      Model model) {

        String normalizedTen = thuongHieuService.normalizeTenThuongHieu(thuongHieu.getTenThuongHieu());
        thuongHieu.setTenThuongHieu(normalizedTen);

        if (errors.hasErrors()) {
            String newCode = thuongHieuService.generateMaThuongHieu();
            thuongHieu.setMaThuongHieu(newCode);

            Page<ThuongHieu> pageData = thuongHieuService.getallpage(PageRequest.of(0, 5));
            model.addAttribute("listth", pageData.getContent());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", pageData.getTotalPages());
            model.addAttribute("totalItems", pageData.getTotalElements());
            model.addAttribute("thuonghieu", thuongHieu);
            return "thuonghieu/index";
        }

        if (thuongHieuService.ktraten(thuongHieu.getTenThuongHieu())) {
            String newCode = thuongHieuService.generateMaThuongHieu();
            thuongHieu.setMaThuongHieu(newCode);

            redirectAttributes.addFlashAttribute("mess", "Thương hiệu '" + thuongHieu.getTenThuongHieu() + "' đã tồn tại!");
            redirectAttributes.addFlashAttribute("thuonghieu", thuongHieu);
            return "redirect:/thuonghieu/index";
        }

        if (thuongHieu.getMaThuongHieu() == null || thuongHieu.getMaThuongHieu().isEmpty()) {
            String newCode = thuongHieuService.generateMaThuongHieu();
            thuongHieu.setMaThuongHieu(newCode);
        }

        thuongHieu.setTrangThai(true);
        thuongHieuService.them(thuongHieu);

        redirectAttributes.addFlashAttribute("successMess",
                "Thêm thương hiệu '" + thuongHieu.getTenThuongHieu() + "' thành công!");
        return "redirect:/thuonghieu/index";
    }

    @GetMapping("/capnhatt/{id}")
    public String capnhatt(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            ThuongHieu dmsp = thuongHieuService.doiTrangThai(id);

            if (dmsp != null) {
                if (dmsp.isTrangThai()) {
                    redirectAttributes.addFlashAttribute("successMess", "Mở hoạt động thành công cho '" + dmsp.getTenThuongHieu() + "'");
                } else {
                    redirectAttributes.addFlashAttribute("successMess", "Ngừng hoạt động thành công cho '" + dmsp.getTenThuongHieu() + "'");
                }
            } else {
                redirectAttributes.addFlashAttribute("mess", "Không tìm thấy thương hiệu!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mess", "Cập nhật trạng thái thất bại: " + e.getMessage());
        }
        return "redirect:/thuonghieu/index";
    }

    @GetMapping("/export-excel")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai) throws IOException {

        Boolean status = null;
        if (trangThai != null && !trangThai.isEmpty()) {
            status = Boolean.parseBoolean(trangThai);
        }

        List<ThuongHieu> list = thuongHieuService.searchAll(keyword, status);
        ByteArrayInputStream excelStream = excelExportService.exportThuongHieuToExcel(list);

        if (excelStream == null) {
            throw new IOException("Không thể tạo file Excel");
        }

        String fileName = "Danh_sach_thuong_hieu_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(excelStream));
    }
}