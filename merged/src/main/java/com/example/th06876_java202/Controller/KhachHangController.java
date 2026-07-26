package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.DiaChi;
import com.example.th06876_java202.Entity.KhachHang;
import com.example.th06876_java202.Repository.KhachHangRepository;
import com.example.th06876_java202.Service.DiaChiService;
import com.example.th06876_java202.Service.ExcelExportService;
import com.example.th06876_java202.Service.KhachHangService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/khach-hang")
@RequiredArgsConstructor
public class KhachHangController {
    private final KhachHangService khachHangService;
    private final DiaChiService diaChiService;

    @Autowired
    ExcelExportService excelExportService;

    @Autowired
    KhachHangRepository khachHangRepository;

    @GetMapping("/hien-thi")
    public String khachHang(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status,
            Model model) {

        model.addAttribute("activeMenu", "khachhang");

        Pageable pageable = PageRequest.of(page, size, Sort.by("maKH").descending());
        Page<KhachHang> khachHangPage;

        if (status != null && !status.isEmpty()) {
            boolean trangThai = Boolean.parseBoolean(status);
            khachHangPage = khachHangService.findByTrangThai(trangThai, pageable);
            model.addAttribute("selectedStatus", status);
        } else {
            khachHangPage = khachHangService.getAllKhachHangPagin(pageable);
        }

        model.addAttribute("khachHangPage", khachHangPage);
        model.addAttribute("khachHangs", khachHangPage.getContent());
        model.addAttribute("currentPage", khachHangPage.getNumber());
        model.addAttribute("totalPages", khachHangPage.getTotalPages());
        model.addAttribute("totalItems", khachHangPage.getTotalElements());
        model.addAttribute("size", khachHangPage.getSize());
        model.addAttribute("showModal", false);
        model.addAttribute("isEdit", false);
        model.addAttribute("kh", new KhachHang());

        return "khachhang/index";
    }

    @GetMapping("/locsdt")
    public String locsdt(
            @RequestParam("sdt") String sdt,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        if (sdt == null || sdt.trim().isEmpty()) {
            return "redirect:/khach-hang/hien-thi";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("maKH").descending());
        Page<KhachHang> khachHangPage = khachHangService.findBySdt(sdt, pageable);

        model.addAttribute("activeMenu", "khachhang");
        model.addAttribute("khachHangPage", khachHangPage);
        model.addAttribute("khachHangs", khachHangPage.getContent());
        model.addAttribute("sdt", sdt);
        model.addAttribute("isFiltered", true);
        model.addAttribute("currentPage", khachHangPage.getNumber());
        model.addAttribute("totalPages", khachHangPage.getTotalPages());
        model.addAttribute("totalItems", khachHangPage.getTotalElements());
        model.addAttribute("size", pageable.getPageSize());
        model.addAttribute("kh", new KhachHang());

        return "khachhang/index";
    }

    @GetMapping("/edit/{maKH}")
    public String edit(@PathVariable String maKH, Model model, RedirectAttributes redirectAttributes) {
        KhachHang khachHang = khachHangService.getKhachHangById(maKH);
        if (khachHang == null) {
            redirectAttributes.addFlashAttribute("errorMess", "Không tìm thấy khách hàng!");
            return "redirect:/khach-hang/hien-thi";
        }
        List<DiaChi> dsDiaChi = diaChiService.findByKhachHang_MaKH(maKH);
        khachHang.setDanhSachDiaChi(dsDiaChi);

        model.addAttribute("kh", khachHang);
        model.addAttribute("isEdit", true);
        return "khachhang/add";
    }

    @GetMapping("/add-view")
    public String addView(Model model) {
        KhachHang kh = new KhachHang();
        kh.setDanhSachDiaChi(new ArrayList<>());

        // Tạo mã khách hàng random
        String newMaKH = khachHangService.generateMaKH();
        kh.setMaKH(newMaKH);

        model.addAttribute("kh", kh);
        model.addAttribute("isEdit", false);
        return "khachhang/add";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("kh") KhachHang form,
                      BindingResult result,
                      @RequestParam(value = "diaChiMacDinh", required = false) Integer indexMacDinh,
                      Model model,
                      RedirectAttributes redirectAttributes) {

        if (form.getMaKH() == null || form.getMaKH().isEmpty()) {
            form.setMaKH(khachHangService.generateMaKH());
        }

        String currentMaKH = form.getMaKH();

        khachHangService.validateKhachHang(form, result);

        if (result.hasErrors()) {
            form.setMaKH(currentMaKH);
            model.addAttribute("kh", form);
            model.addAttribute("isEdit", false);
            return "khachhang/add";
        }

        boolean isEdit = form.getMaKH() != null && !form.getMaKH().isEmpty()
                && khachHangService.getKhachHangById(form.getMaKH()) != null;

        KhachHang khachHang;
        if (isEdit) {
            khachHang = khachHangService.getKhachHangById(form.getMaKH());
            if (khachHang == null) {
                redirectAttributes.addFlashAttribute("errorMess", "Không tìm thấy khách hàng!");
                return "redirect:/khach-hang/hien-thi";
            }
            // Xóa địa chỉ cũ
            khachHang.getDanhSachDiaChi().clear();
        } else {
            khachHang = new KhachHang();
            khachHang.setMaKH(form.getMaKH());
            khachHang.setNgayDangKy(LocalDate.now());
            // Danh sách đã được khởi tạo trong Entity
        }

        // Copy dữ liệu
        khachHang.setHoTen(form.getHoTen());
        khachHang.setSdt(form.getSdt());
        khachHang.setEmail(form.getEmail());
        khachHang.setNgaySinh(form.getNgaySinh());
        khachHang.setGioiTinh(form.getGioiTinh());
        khachHang.setGhiChu(form.getGhiChu());

        // Thêm địa chỉ mới
        List<DiaChi> list = form.getDanhSachDiaChi();
        if (list != null && !list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                DiaChi dc = list.get(i);
                dc.setKhachHang(khachHang);
                dc.setDiaChiMacDinh(indexMacDinh != null && i == indexMacDinh);
                khachHang.getDanhSachDiaChi().add(dc);
            }
        }

        khachHangService.save(khachHang);

        if (isEdit) {
            redirectAttributes.addFlashAttribute("successMess", "Cập nhật khách hàng thành công!");
        } else {
            redirectAttributes.addFlashAttribute("successMess", "Thêm khách hàng mới thành công!");
        }

        return "redirect:/khach-hang/hien-thi";
    }

    @DeleteMapping("/xoa-dia-chi/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> xoaDiaChi(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        try {
            diaChiService.deleteById(id);
            response.put("success", true);
            response.put("message", "Xóa địa chỉ thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Xóa địa chỉ thất bại: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/lock/{maKH}")
    public String lock(@PathVariable String maKH,
                       @RequestParam(required = false) String status,
                       RedirectAttributes redirectAttributes) {
        khachHangService.lock(maKH);
        redirectAttributes.addFlashAttribute("successMess", "Đã khóa tài khoản khách hàng thành công!");
        return "redirect:/khach-hang/hien-thi" + (status != null ? "?status=" + status : "");
    }

    @GetMapping("/unlock/{maKH}")
    public String unlock(@PathVariable String maKH,
                         @RequestParam(required = false) String status,
                         RedirectAttributes redirectAttributes) {
        khachHangService.unlock(maKH);
        redirectAttributes.addFlashAttribute("successMess", "Đã mở khóa tài khoản khách hàng thành công!");
        return "redirect:/khach-hang/hien-thi" + (status != null ? "?status=" + status : "");
    }

    @GetMapping("/check-sdt")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkSdt(
            @RequestParam("sdt") String sdt,
            @RequestParam(value = "maKH", required = false) String maKH) {

        boolean exists;
        if (maKH != null && !maKH.isEmpty()) {
            exists = khachHangService.existsBySdtAndNotMaKH(sdt, maKH);
        } else {
            exists = khachHangService.existsBySdt(sdt);
        }

        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-email")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkEmail(
            @RequestParam("email") String email,
            @RequestParam(value = "maKH", required = false) String maKH) {

        boolean exists;
        if (maKH != null && !maKH.isEmpty()) {
            exists = khachHangService.existsByEmailAndNotMaKH(email, maKH);
        } else {
            exists = khachHangService.existsByEmail(email);
        }

        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel() {

        try {
            System.out.println("📊 Bắt đầu export Excel khách hàng...");
            List<KhachHang> khachHangList = khachHangRepository.findAll();

            System.out.println("🔍 Xuất FULL - Tất cả khách hàng: " + khachHangList.size() + " khách hàng");
            if (khachHangList == null || khachHangList.isEmpty()) {
                System.out.println("⚠️ Không có dữ liệu để xuất Excel");
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            ByteArrayInputStream excelStream = excelExportService.exportKhachHangToExcel(khachHangList);

            if (excelStream == null) {
                System.err.println("❌ Lỗi khi xuất Excel");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            byte[] excelBytes = excelStream.readAllBytes();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "Danh_sach_khach_hang_" + timestamp + ".xlsx";

            // Thiết lập headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(excelBytes.length);

            System.out.println("✅ Xuất Excel thành công! File: " + fileName);
            System.out.println("📊 Số lượng: " + khachHangList.size() + " khách hàng");
            System.out.println("📊 Dung lượng: " + excelBytes.length + " bytes");

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi xuất Excel: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/search-suggestions")
    @ResponseBody
    public ResponseEntity<?> searchSuggestions(@RequestParam("q") String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            Pageable pageable = PageRequest.of(0, 10);
            List<KhachHang> suggestions = khachHangRepository.findTop10ByHoTenContainingOrMaKHContaining(
                    keyword.trim(), pageable
            );
            List<Map<String, Object>> result = suggestions.stream()
                    .map(kh -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("maKH", kh.getMaKH());
                        map.put("hoTen", kh.getHoTen());
                        map.put("sdt", kh.getSdt() != null ? kh.getSdt() : "");
                        map.put("email", kh.getEmail() != null ? kh.getEmail() : "");
                        map.put("trangThai", kh.isTrangThai() ? "Hoạt động" : "Ngừng hoạt động");
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Có lỗi xảy ra khi tìm kiếm gợi ý"));
        }
    }

    @GetMapping("/tim-kiem")
    public String timKiem(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return "redirect:/khach-hang/hien-thi";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("maKH").descending());
        Page<KhachHang> khachHangPage = khachHangService.searchByKeyword(keyword.trim(), pageable);

        model.addAttribute("activeMenu", "khachhang");
        model.addAttribute("khachHangPage", khachHangPage);
        model.addAttribute("khachHangs", khachHangPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", khachHangPage.getNumber());
        model.addAttribute("totalPages", khachHangPage.getTotalPages());
        model.addAttribute("totalItems", khachHangPage.getTotalElements());
        model.addAttribute("size", pageable.getPageSize());
        model.addAttribute("kh", new KhachHang());

        return "khachhang/index";
    }
}