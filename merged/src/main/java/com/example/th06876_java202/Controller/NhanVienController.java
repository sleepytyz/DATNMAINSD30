package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.NhanVien;
import com.example.th06876_java202.Entity.TaiKhoan;
import com.example.th06876_java202.Repository.NhanVienRepository;
import com.example.th06876_java202.Service.EmailService;
import com.example.th06876_java202.Service.ExcelExportService;
import com.example.th06876_java202.Service.NhanVienService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/nhan-vien")
public class NhanVienController {

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private NhanVienService nhanVienService;

    @Autowired
    private EmailService emailService;

    @Autowired
    ExcelExportService excelExportService;

    @Autowired
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;

    @GetMapping
    public String index(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model,
            HttpSession session) {

        Pageable pageable = PageRequest.of(page, 10, Sort.by("ngayVaoLam").descending());
        Page<NhanVien> pageNhanVien = nhanVienRepository.filter(keyword, role, status, pageable);

        model.addAttribute("list", pageNhanVien.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageNhanVien.getTotalPages());
        model.addAttribute("totalItems", pageNhanVien.getTotalElements());
        model.addAttribute("activeMenu", "nhanvien");
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("status", status);

        // ===== LẤY THÔNG BÁO TỪ SESSION =====
        String successMsg = (String) session.getAttribute("successMess");
        String errorMsg = (String) session.getAttribute("errorMess");

        if (successMsg != null && !successMsg.isEmpty()) {
            model.addAttribute("successMess", successMsg);
            session.removeAttribute("successMess");
            System.out.println("📌 Đã lấy successMess từ session: " + successMsg);
        }
        if (errorMsg != null && !errorMsg.isEmpty()) {
            model.addAttribute("mess", errorMsg);
            session.removeAttribute("errorMess");
            System.out.println("📌 Đã lấy errorMess từ session: " + errorMsg);
        }

        return "nhanvien/index";
    }

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "status", required = false) String status) {

        try {
            // Lấy danh sách nhân viên theo filter
            List<NhanVien> nhanVienList = nhanVienRepository.findAllByFilter(keyword, role, status);

            // Xuất Excel
            ByteArrayInputStream excelStream = excelExportService.exportNhanVienToExcel(nhanVienList);

            if (excelStream == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            byte[] excelBytes = excelStream.readAllBytes();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Danh_sach_nhan_vien.xlsx");

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/form")
    public String form(
            @RequestParam(value = "id", required = false) String id,
            Model model,
            HttpSession session) {

        NhanVien nv;
        boolean isEdit = false;

        if (id != null && !id.isEmpty()) {
            nv = nhanVienRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên: " + id));
            isEdit = true;
            System.out.println("📌 EDIT - Mã NV: " + nv.getMaNhanVien());
        } else {
            nv = new NhanVien();
            nv.setTaiKhoan(new TaiKhoan());
            String newCode = nhanVienService.generateMaNhanVien();
            nv.setMaNhanVien(newCode);
            isEdit = false;
            System.out.println("📌 ADD - Mã NV mới: " + newCode);
        }

        model.addAttribute("nhanVien", nv);
        model.addAttribute("isEdit", isEdit);

        // Lấy thông báo từ session cho form
        String successMsg = (String) session.getAttribute("successMess");
        String errorMsg = (String) session.getAttribute("errorMess");

        if (successMsg != null && !successMsg.isEmpty()) {
            model.addAttribute("successMess", successMsg);
            session.removeAttribute("successMess");
        }
        if (errorMsg != null && !errorMsg.isEmpty()) {
            model.addAttribute("errorMess", errorMsg);
            session.removeAttribute("errorMess");
        }

        return "nhanvien/form";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") String id, Model model, HttpSession session) {
        return form(id, model, session);
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("nhanVien") NhanVien nv,
                       BindingResult result,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        if (nv.getMaNhanVien() != null && nv.getMaNhanVien().contains(",")) {
            nv.setMaNhanVien(nv.getMaNhanVien().split(",")[0].trim());
        }

        boolean isUpdate = false;
        if (nv.getMaNhanVien() != null && !nv.getMaNhanVien().isEmpty()) {
            isUpdate = nhanVienRepository.existsById(nv.getMaNhanVien());
        }

        System.out.println("=====================================");
        System.out.println("📌 SAVE - Mã NV: " + nv.getMaNhanVien());
        System.out.println("📌 isUpdate: " + isUpdate);
        System.out.println("📌 Email: " + nv.getEmail());
        System.out.println("📌 SDT: " + nv.getSoDienThoai());

        // Validate age
        if (nv.getNgaySinh() != null) {
            LocalDate today = LocalDate.now();
            Period age = Period.between(nv.getNgaySinh(), today);
            if (age.getYears() < 18) {
                result.rejectValue("ngaySinh", "error.nv", "Nhân viên phải đủ 18 tuổi trở lên!");
            }
        }

        // Kiểm tra trùng
        boolean emailExists = false;
        boolean sdtExists = false;

        if (isUpdate) {
            NhanVien existing = nhanVienRepository.findById(nv.getMaNhanVien()).orElse(null);
            if (existing != null) {
                String oldEmail = existing.getEmail();
                String newEmail = nv.getEmail();
                String oldSdt = existing.getSoDienThoai();
                String newSdt = nv.getSoDienThoai();

                if (!oldEmail.equals(newEmail)) {
                    emailExists = nhanVienRepository.existsByEmailAndNotMaNhanVien(newEmail, nv.getMaNhanVien());
                }
                if (!oldSdt.equals(newSdt)) {
                    sdtExists = nhanVienRepository.existsBySoDienThoaiAndNotMaNhanVien(newSdt, nv.getMaNhanVien());
                }
            }
        } else {
            emailExists = nhanVienRepository.existsByEmail(nv.getEmail());
            sdtExists = nhanVienRepository.existsBySoDienThoai(nv.getSoDienThoai());
        }

        if (emailExists) {
            result.rejectValue("email", "error.nv", "📧 Email này đã tồn tại trong hệ thống!");
        }
        if (sdtExists) {
            result.rejectValue("soDienThoai", "error.nv", "📱 Số điện thoại này đã được sử dụng!");
        }

        if (result.hasErrors()) {
            if (nv.getTaiKhoan() == null) nv.setTaiKhoan(new TaiKhoan());
            model.addAttribute("nhanVien", nv);
            model.addAttribute("isEdit", isUpdate);
            return "nhanvien/form";
        }

        // Lưu
        String message = "";
        if (isUpdate) {
            NhanVien existingNv = nhanVienRepository.findById(nv.getMaNhanVien()).orElse(null);
            if (existingNv != null) {
                existingNv.setHoTen(nv.getHoTen());
                existingNv.setSoDienThoai(nv.getSoDienThoai());
                existingNv.setEmail(nv.getEmail());
                existingNv.setNgaySinh(nv.getNgaySinh());
                existingNv.setDiaChi(nv.getDiaChi());
                existingNv.setGioiTinh(nv.getGioiTinh());
                existingNv.setChucVu(nv.getChucVu());
                existingNv.setLuongCoBan(nv.getLuongCoBan());
                existingNv.setGhiChu(nv.getGhiChu());

                nhanVienRepository.save(existingNv);
                message = "Cập nhật nhân viên thành công!";
                System.out.println("UPDATE thành công!");
            }
        } else {
            if (nv.getNgayVaoLam() == null) nv.setNgayVaoLam(LocalDateTime.now());
            nv.setTrangThai(true);

            String hoTen = nv.getHoTen().trim();
            String tenChinh = hoTen.substring(hoTen.lastIndexOf(" ") + 1);
            String autoUsername = tenChinh.toLowerCase() + (int)(Math.random() * 900 + 100);
            String rawPassword = "Pass" + (int)(Math.random() * 900000 + 100000);

            TaiKhoan tk = new TaiKhoan();
            tk.setTenDangNhap(autoUsername);
            tk.setMatKhau(passwordEncoder.encode(rawPassword));
            tk.setVaiTro("STAFF");
            tk.setTrangThai(true);
            nv.setTaiKhoan(tk);

            nhanVienRepository.save(nv);
            message = "Thêm nhân viên mới thành công!";
            System.out.println("ADD thành công!");

            try {
                emailService.sendAccountDetails(nv.getEmail(), nv.getHoTen(), autoUsername, rawPassword);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // ===== LƯU VÀO SESSION VÀ FLASH =====
        redirectAttributes.addFlashAttribute("successMess", message);
        System.out.println("Đã lưu thông báo: " + message);

        return "redirect:/nhan-vien";
    }

    @GetMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable("id") String id,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "role", required = false) String role,
                               @RequestParam(value = "status", required = false) String status,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {  // Thêm Authentication

        // Kiểm tra quyền ADMIN
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            redirectAttributes.addFlashAttribute("mess", "Bạn không có quyền thực hiện hành động này!");
            StringBuilder redirectUrl = new StringBuilder("redirect:/nhan-vien?page=" + page);
            if (keyword != null && !keyword.isEmpty()) {
                redirectUrl.append("&keyword=").append(keyword);
            }
            if (role != null && !role.isEmpty()) {
                redirectUrl.append("&role=").append(role);
            }
            if (status != null && !status.isEmpty()) {
                redirectUrl.append("&status=").append(status);
            }
            return redirectUrl.toString();
        }

        NhanVien nv = nhanVienRepository.findById(id).orElse(null);
        if (nv != null) {
            nv.setTrangThai(!nv.getTrangThai());
            nhanVienRepository.save(nv);
            String message = nv.getTrangThai() ?
                    "Đã chuyển trạng thái nhân viên sang Đang làm việc!" :
                    "Đã chuyển trạng thái nhân viên sang Nghỉ việc!";
            redirectAttributes.addFlashAttribute("successMess", message);
            System.out.println("📌 Đã lưu flash attribute toggle: " + message);
        } else {
            redirectAttributes.addFlashAttribute("mess", "Không tìm thấy nhân viên!");
        }

        StringBuilder redirectUrl = new StringBuilder("redirect:/nhan-vien?page=" + page);
        if (keyword != null && !keyword.isEmpty()) {
            redirectUrl.append("&keyword=").append(keyword);
        }
        if (role != null && !role.isEmpty()) {
            redirectUrl.append("&role=").append(role);
        }
        if (status != null && !status.isEmpty()) {
            redirectUrl.append("&status=").append(status);
        }
        return redirectUrl.toString();
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") String id) {
        nhanVienRepository.updateTrangThai(id);
        return "redirect:/nhan-vien";
    }

    @GetMapping("/api/search-suggestions")
    @ResponseBody
    public ResponseEntity<?> searchSuggestions(@RequestParam("q") String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<NhanVien> suggestions = nhanVienRepository.findTop10ByHoTenContainingOrMaNhanVienContaining(
                    keyword.trim()
            );
            if (suggestions.size() > 10) {
                suggestions = suggestions.subList(0, 10);
            }

            List<Map<String, String>> result = suggestions.stream()
                    .map(nv -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("maNhanVien", nv.getMaNhanVien());
                        map.put("hoTen", nv.getHoTen());
                        map.put("soDienThoai", nv.getSoDienThoai() != null ? nv.getSoDienThoai() : "");
                        map.put("chucVu", nv.getChucVu() != null ? nv.getChucVu() : "");
                        map.put("email", nv.getEmail() != null ? nv.getEmail() : "");
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
}