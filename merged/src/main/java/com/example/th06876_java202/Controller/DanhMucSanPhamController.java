package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.DanhMucSanPham;
import com.example.th06876_java202.Service.DanhMucSanPhamService;
import com.example.th06876_java202.Service.ExcelExportService;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/danhmucsp")
public class DanhMucSanPhamController {

    private final DanhMucSanPhamService danhMucSanPhamService;
    private final ExcelExportService excelExportService;

    public DanhMucSanPhamController(DanhMucSanPhamService danhMucSanPhamService,
                                    ExcelExportService excelExportService) {
        this.danhMucSanPhamService = danhMucSanPhamService;
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

        // SỬA: Sắp xếp giảm dần theo ngày tạo
        Pageable pageable = PageRequest.of(page, size, Sort.by("ngayTao").descending());
        Page<DanhMucSanPham> pageData = danhMucSanPhamService.searchAndFilter(keyword, status, pageable);

        long totalActive = danhMucSanPhamService.countByTrangThai(true);
        long totalInactive = danhMucSanPhamService.countByTrangThai(false);

        model.addAttribute("listdmsp", pageData.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());
        model.addAttribute("totalActive", totalActive);
        model.addAttribute("totalInactive", totalInactive);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedTrangThai", trangThai);

        String generatedCode = danhMucSanPhamService.generateMaDanhMuc();

        if (!model.containsAttribute("danhmuc")) {
            DanhMucSanPham newDanhMuc = new DanhMucSanPham();
            newDanhMuc.setMaDanhMuc(generatedCode);
            model.addAttribute("danhmuc", newDanhMuc);
        } else {
            DanhMucSanPham existing = (DanhMucSanPham) model.getAttribute("danhmuc");
            if (existing != null && (existing.getMaDanhMuc() == null || existing.getMaDanhMuc().isEmpty())) {
                existing.setMaDanhMuc(generatedCode);
            }
        }

        return "danhmucsp/index";
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

        // SỬA: Sắp xếp giảm dần theo ngày tạo
        Pageable pageable = PageRequest.of(page, size, Sort.by("ngayTao").descending());
        Page<DanhMucSanPham> pageData = danhMucSanPhamService.searchAndFilter(keyword, status, pageable);

        long totalActive = danhMucSanPhamService.countByTrangThai(true);
        long totalInactive = danhMucSanPhamService.countByTrangThai(false);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("listdmsp", pageData.getContent());
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
        response.put("code", danhMucSanPhamService.generateMaDanhMuc());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleStatusAjax(@PathVariable("id") String id) {
        Map<String, Object> response = new HashMap<>();

        try {
            DanhMucSanPham dm = danhMucSanPhamService.doiTrangThai(id);
            if (dm != null) {
                String message = dm.isTrangThai()
                        ? "Đã kích hoạt danh mục '" + dm.getTenDanhMuc() + "' thành công!"
                        : "Đã ngừng hoạt động danh mục '" + dm.getTenDanhMuc() + "' thành công!";
                response.put("success", true);
                response.put("message", message);
                response.put("trangThai", dm.isTrangThai());
                response.put("tenDanhMuc", dm.getTenDanhMuc());
                response.put("maDanhMuc", dm.getMaDanhMuc());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy danh mục!");
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
    public ResponseEntity<Map<String, Object>> addAjax(@RequestBody DanhMucSanPham danhMuc) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (danhMuc.getTenDanhMuc() == null || danhMuc.getTenDanhMuc().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Tên danh mục không được để trống!");
                return ResponseEntity.badRequest().body(response);
            }

            String normalizedTen = danhMucSanPhamService.normalizeTenDanhMuc(danhMuc.getTenDanhMuc());
            danhMuc.setTenDanhMuc(normalizedTen);

            if (danhMucSanPhamService.existsByTenDanhMuc(danhMuc.getTenDanhMuc())) {
                response.put("success", false);
                response.put("message", "Danh mục '" + danhMuc.getTenDanhMuc() + "' đã tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            if (danhMuc.getMaDanhMuc() == null || danhMuc.getMaDanhMuc().isEmpty()) {
                String newCode = danhMucSanPhamService.generateMaDanhMuc();
                danhMuc.setMaDanhMuc(newCode);
            }

            danhMuc.setTrangThai(true);
            DanhMucSanPham saved = danhMucSanPhamService.them(danhMuc);

            response.put("success", true);
            response.put("message", "Thêm danh mục '" + saved.getTenDanhMuc() + "' thành công!");
            response.put("data", saved);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/export-excel")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai) throws IOException {

        Boolean status = null;
        if (trangThai != null && !trangThai.isEmpty()) {
            status = Boolean.parseBoolean(trangThai);
        }

        List<DanhMucSanPham> list = danhMucSanPhamService.searchAll(keyword, status);
        ByteArrayInputStream excelStream = excelExportService.exportDanhMucToExcel(list);

        if (excelStream == null) {
            throw new IOException("Không thể tạo file Excel");
        }

        String fileName = "Danh_sach_danh_muc_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(excelStream));
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("danhmuc") @Valid DanhMucSanPham dmsp,
                      Errors errors,
                      Model model,
                      RedirectAttributes redirectAttributes) {

        String normalizedTen = danhMucSanPhamService.normalizeTenDanhMuc(dmsp.getTenDanhMuc());
        dmsp.setTenDanhMuc(normalizedTen);

        if (errors.hasErrors()) {
            String newCode = danhMucSanPhamService.generateMaDanhMuc();
            dmsp.setMaDanhMuc(newCode);

            Page<DanhMucSanPham> pageData = danhMucSanPhamService.getallpage(PageRequest.of(0, 5));
            model.addAttribute("listdmsp", pageData.getContent());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", pageData.getTotalPages());
            model.addAttribute("totalItems", pageData.getTotalElements());
            model.addAttribute("danhmuc", dmsp);
            return "danhmucsp/index";
        }

        if (danhMucSanPhamService.existsByTenDanhMuc(dmsp.getTenDanhMuc())) {
            String newCode = danhMucSanPhamService.generateMaDanhMuc();
            dmsp.setMaDanhMuc(newCode);

            redirectAttributes.addFlashAttribute("mess", "Danh mục '" + dmsp.getTenDanhMuc() + "' đã tồn tại!");
            redirectAttributes.addFlashAttribute("danhmuc", dmsp);
            return "redirect:/danhmucsp/index";
        }

        if (dmsp.getMaDanhMuc() == null || dmsp.getMaDanhMuc().isEmpty()) {
            String newCode = danhMucSanPhamService.generateMaDanhMuc();
            dmsp.setMaDanhMuc(newCode);
        }

        dmsp.setTrangThai(true);
        danhMucSanPhamService.them(dmsp);

        redirectAttributes.addFlashAttribute("successMess",
                "Thêm danh mục '" + dmsp.getTenDanhMuc() + "' thành công!");
        return "redirect:/danhmucsp/index";
    }

    @GetMapping("/capnhatt/{id}")
    public String capnhatt(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            DanhMucSanPham dmsp = danhMucSanPhamService.doiTrangThai(id);

            if (dmsp != null) {
                if (dmsp.isTrangThai()) {
                    redirectAttributes.addFlashAttribute("successMess", "Đã kích hoạt danh mục '" + dmsp.getTenDanhMuc() + "'!");
                } else {
                    redirectAttributes.addFlashAttribute("successMess", "Đã ngừng hoạt động danh mục '" + dmsp.getTenDanhMuc() + "'!");
                }
            } else {
                redirectAttributes.addFlashAttribute("mess", "Không tìm thấy danh mục sản phẩm!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mess", "Cập nhật trạng thái thất bại: " + e.getMessage());
        }
        return "redirect:/danhmucsp/index";
    }

    @GetMapping("/api/search-suggestions")
    @ResponseBody
    public ResponseEntity<List<DanhMucSanPham>> searchSuggestions(@RequestParam("q") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<DanhMucSanPham> suggestions = danhMucSanPhamService.searchSuggestions(keyword.trim());
        return ResponseEntity.ok(suggestions);
    }
}