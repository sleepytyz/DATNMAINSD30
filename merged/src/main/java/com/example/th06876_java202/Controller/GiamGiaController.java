package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.*;
import com.example.th06876_java202.Repository.GiamGiaChiTietRepo;
import com.example.th06876_java202.Repository.GiamGiaRepository;
import com.example.th06876_java202.Repository.KhachHangRepository;
import com.example.th06876_java202.Service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/giamgia")
@SessionAttributes("giamGia")
public class GiamGiaController {

    @Autowired
    private GiamGiaService giamGiaService;

    @Autowired
    KhachHangService khachHangService;

    @Autowired
    KhachHangRepository khachHangRepo;

    @Autowired
    EmailService emailService;

    @Autowired
    GiamGiaChiTietRepo giamGiaChiTietRepo;

    @Autowired
    ExcelExportService excelExportService;

    @Autowired
    GiamGiaRepository giamGiaRepository;

    @GetMapping("/api/khachhang/suggest")
    @ResponseBody
    public List<KhachHang> suggest(@RequestParam String sdt) {
        return khachHangRepo.findTop10BySdtContaining(sdt);
    }

    @GetMapping("/index")
    public String index(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tt,
            @RequestParam(required = false) String lg,
            @RequestParam(required = false) Integer loaiApDung,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate ngaybdau,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate ngaykthuc,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        System.out.println("=== INDEX FILTER ===");
        System.out.println("keyword: " + keyword);
        System.out.println("trangThai: " + tt);
        System.out.println("loaiGiamGia: " + lg);
        System.out.println("loaiApDung: " + loaiApDung);
        System.out.println("ngaybdau: " + ngaybdau);
        System.out.println("ngaykthuc: " + ngaykthuc);

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (ngaybdau != null) {
            startDateTime = ngaybdau.atStartOfDay();
        }
        if (ngaykthuc != null) {
            endDateTime = ngaykthuc.atTime(23, 59, 59);
        }

        Page<GiamGia> pageData = giamGiaService.getFilteredGiamGia(keyword, tt, lg, loaiApDung, startDateTime, endDateTime, page);

        model.addAttribute("list", pageData.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());

        model.addAttribute("keyword", keyword);
        model.addAttribute("tt", tt);
        model.addAttribute("lg", lg);
        model.addAttribute("loaiApDung", loaiApDung);
        model.addAttribute("ngaybdau", ngaybdau);
        model.addAttribute("ngaykthuc", ngaykthuc);

        String successMess = (String) model.asMap().get("successMess");
        String errorMess = (String) model.asMap().get("errorMess");

        if (successMess != null) {
            model.addAttribute("successMess", successMess);
        }
        if (errorMess != null) {
            model.addAttribute("errorMess", errorMess);
        }

        return "giamgia/index";
    }

    @GetMapping("/create")
    public String create(
            @RequestParam(required = false) String sdt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String action,
            SessionStatus status,
            Model model) {

        if ("new".equals(action)) {
            status.setComplete();
        }

        Page<KhachHang> pageKh = khachHangService.searchByPhone(sdt, page);

        GiamGia giamGia = new GiamGia();
        String newCode = giamGiaService.generateMaGiamGia();
        giamGia.setMaGiamGia(newCode);
        giamGia.setLoaiGiamGia("PhanTram");
        giamGia.setTrangThai("Sắp hoạt động");
        giamGia.setLoaiApDung(1);
        giamGia.setIsVoHan(false);

        model.addAttribute("giamGia", giamGia);
        model.addAttribute("isEdit", false);
        model.addAttribute("listKhachHang", pageKh.getContent());
        model.addAttribute("currentPage", pageKh.getNumber());
        model.addAttribute("totalPages", pageKh.getTotalPages());
        model.addAttribute("totalItems", pageKh.getTotalElements());
        model.addAttribute("sdt", sdt);

        List<String> emptyList = new ArrayList<>();
        model.addAttribute("selectedKhachHangIds", emptyList);
        model.addAttribute("totalSelectedCount", 0);
        model.addAttribute("maGiamGia", "");

        return "giamgia/add";
    }

    @GetMapping("/edit/{id}")
    public String edit(
            @PathVariable("id") String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String sdt,
            Model model,
            RedirectAttributes ra) {

        GiamGia giamGia = giamGiaService.getGiamGiaById(id).orElse(null);

        if (giamGia == null) {
            ra.addFlashAttribute("errorMess", "Không tìm thấy chương trình giảm giá!");
            return "redirect:/giamgia/index";
        }

        String trangThai = giamGia.getTrangThai();
        if (giamGia == null || trangThai == null || (!"Sắp hoạt động".equals(trangThai.trim()) && !"Đã huỷ".equals(trangThai.trim()))) {
            ra.addFlashAttribute("errorMess", "Chỉ có thể chỉnh sửa chương trình ở trạng thái 'Sắp hoạt động' hoặc 'Đã huỷ'!");
            return "redirect:/giamgia/index";
        }

        model.addAttribute("giamGia", giamGia);
        model.addAttribute("isEdit", true);
        model.addAttribute("maGiamGia", id);

        Page<KhachHang> pageKh = khachHangService.searchByPhone(sdt, page);

        model.addAttribute("listKhachHang", pageKh.getContent());
        model.addAttribute("currentPage", pageKh.getNumber());
        model.addAttribute("totalPages", pageKh.getTotalPages());
        model.addAttribute("totalItems", pageKh.getTotalElements());
        model.addAttribute("sdt", sdt);

        List<String> selectedIds = giamGiaChiTietRepo.findMaKhachHangByMaGiamGia(id);
        if (selectedIds == null) {
            selectedIds = new ArrayList<>();
        }
        model.addAttribute("selectedKhachHangIds", selectedIds);

        long totalSelectedCount = giamGiaChiTietRepo.countByGiamGia_MaGiamGia(id);
        model.addAttribute("totalSelectedCount", totalSelectedCount);

        return "giamgia/add";
    }

    // ===== THÊM PHƯƠNG THỨC DETAIL =====
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") String id,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(required = false) String sdt,
                         Model model,
                         RedirectAttributes ra) {

        GiamGia giamGia = giamGiaService.getGiamGiaById(id).orElse(null);

        if (giamGia == null) {
            ra.addFlashAttribute("errorMess", "Không tìm thấy chương trình giảm giá!");
            return "redirect:/giamgia/index";
        }

        model.addAttribute("giamGia", giamGia);
        model.addAttribute("isEdit", false);
        model.addAttribute("isView", true);  // Đánh dấu là chế độ xem
        model.addAttribute("maGiamGia", id);
        model.addAttribute("sdt", sdt);

        // Lấy danh sách khách hàng đã chọn (nếu có)
        List<String> selectedIds = giamGiaChiTietRepo.findMaKhachHangByMaGiamGia(id);
        if (selectedIds == null) {
            selectedIds = new ArrayList<>();
        }
        model.addAttribute("selectedKhachHangIds", selectedIds);

        long totalSelectedCount = giamGiaChiTietRepo.countByGiamGia_MaGiamGia(id);
        model.addAttribute("totalSelectedCount", totalSelectedCount);

        // Lấy danh sách khách hàng để hiển thị (nếu có)
        Page<KhachHang> pageKh = khachHangService.searchByPhone(sdt, page);
        model.addAttribute("listKhachHang", pageKh.getContent());
        model.addAttribute("currentPage", pageKh.getNumber());
        model.addAttribute("totalPages", pageKh.getTotalPages());
        model.addAttribute("totalItems", pageKh.getTotalElements());

        return "giamgia/add";
    }

    @GetMapping("/search-customer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchCustomer(
            @RequestParam(required = false) String sdt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String maGiamGia,
            @RequestParam(required = false) Boolean isEdit,
            @RequestParam(required = false) Boolean isView) {  // THÊM THAM SỐ isView

        Map<String, Object> response = new HashMap<>();

        try {
            // ===== LẤY TẤT CẢ KHÁCH HÀNG (KHÔNG PHÂN TRANG) =====
            List<KhachHang> allCustomers;
            if (sdt == null || sdt.trim().isEmpty()) {
                allCustomers = khachHangService.getAllKhachHang();
            } else {
                allCustomers = khachHangService.findAllBySdt(sdt);
            }

            // ===== LẤY DANH SÁCH KHÁCH HÀNG ĐÃ ĐƯỢC ÁP DỤNG =====
            List<String> selectedIds = new ArrayList<>();
            // ===== SỬA: Kiểm tra cả isEdit và isView =====
            if (maGiamGia != null && !maGiamGia.isEmpty() && (isEdit != null && isEdit || isView != null && isView)) {
                selectedIds = giamGiaChiTietRepo.findMaKhachHangByMaGiamGia(maGiamGia);
            }

            // ===== SẮP XẾP: ĐƯA KHÁCH HÀNG ĐÃ ĐƯỢC ÁP DỤNG LÊN ĐẦU =====
            List<KhachHang> selectedCustomers = new ArrayList<>();
            List<KhachHang> unselectedCustomers = new ArrayList<>();

            for (KhachHang kh : allCustomers) {
                if (selectedIds.contains(kh.getMaKH())) {
                    selectedCustomers.add(kh);
                } else {
                    unselectedCustomers.add(kh);
                }
            }

            // Gộp lại: đã chọn trước, chưa chọn sau
            List<KhachHang> sortedCustomers = new ArrayList<>();
            sortedCustomers.addAll(selectedCustomers);
            sortedCustomers.addAll(unselectedCustomers);

            // ===== PHÂN TRANG SAU KHI SẮP XẾP =====
            int totalItems = sortedCustomers.size();
            int start = page * size;
            int end = Math.min(start + size, totalItems);
            List<KhachHang> pageContent = start < totalItems ? sortedCustomers.subList(start, end) : new ArrayList<>();

            List<Map<String, Object>> customerList = new ArrayList<>();
            for (KhachHang kh : pageContent) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("maKH", kh.getMaKH());
                customer.put("hoTen", kh.getHoTen() != null ? kh.getHoTen() : "");
                customer.put("sdt", kh.getSdt() != null ? kh.getSdt() : "");
                customer.put("email", kh.getEmail() != null ? kh.getEmail() : "");
                customer.put("ngaySinh", kh.getNgaySinh() != null ? kh.getNgaySinh().toString() : "");
                customerList.add(customer);
            }

            response.put("content", customerList);
            response.put("currentPage", page);
            response.put("totalPages", (int) Math.ceil((double) totalItems / size));
            response.put("totalItems", totalItems);
            response.put("size", size);
            response.put("sdt", sdt);
            response.put("selectedIds", selectedIds);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", e.getMessage());
            response.put("content", new ArrayList<>());
            response.put("currentPage", 0);
            response.put("totalPages", 0);
            response.put("totalItems", 0);
            response.put("size", size);
            response.put("sdt", sdt);
            response.put("selectedIds", new ArrayList<>());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===== ADD =====
    @PostMapping("/add")
    @Transactional
    public String add(@Valid @ModelAttribute("giamGia") GiamGia giamGia,
                      BindingResult result,
                      SessionStatus status,
                      @RequestParam(value = "selectedKhachHang", required = false) List<String> selectedKhachHang,
                      @RequestParam(value = "selectedKhachHangIds", required = false) String selectedKhachHangIdsStr,
                      @RequestParam(defaultValue = "0") int page,
                      Model model,
                      RedirectAttributes ra) {

        System.out.println("=== ADD METHOD CALLED ===");
        System.out.println("maGiamGia from model: " + giamGia.getMaGiamGia());
        System.out.println("selectedKhachHang (checkbox): " + selectedKhachHang);
        System.out.println("selectedKhachHangIds (hidden): " + selectedKhachHangIdsStr);

        List<String> allSelectedIds = new ArrayList<>();
        if (selectedKhachHangIdsStr != null && !selectedKhachHangIdsStr.trim().isEmpty()) {
            String[] ids = selectedKhachHangIdsStr.split(",");
            for (String id : ids) {
                if (id != null && !id.trim().isEmpty()) {
                    allSelectedIds.add(id.trim());
                }
            }
        }
        if (allSelectedIds.isEmpty() && selectedKhachHang != null && !selectedKhachHang.isEmpty()) {
            allSelectedIds = selectedKhachHang;
        }

        System.out.println("Final allSelectedIds: " + allSelectedIds);
        System.out.println("allSelectedIds size: " + allSelectedIds.size());

        String originalMaGiamGia = giamGia.getMaGiamGia();
        if (originalMaGiamGia != null && originalMaGiamGia.contains(",")) {
            String[] parts = originalMaGiamGia.split(",");
            originalMaGiamGia = parts[0].trim();
            giamGia.setMaGiamGia(originalMaGiamGia);
        }

        if (giamGia.getMaGiamGia() == null || giamGia.getMaGiamGia().trim().isEmpty()) {
            String newCode = giamGiaService.generateMaGiamGia();
            giamGia.setMaGiamGia(newCode);
            originalMaGiamGia = newCode;
        }

        if (giamGia.getLoaiApDung() == 2) {
            if (allSelectedIds != null && !allSelectedIds.isEmpty()) {
                giamGia.setSoLuong(allSelectedIds.size());
            } else {
                giamGia.setSoLuong(0);
            }
            giamGia.setIsVoHan(false);
        } else if (giamGia.getLoaiApDung() == 1) {
            if (giamGia.getIsVoHan() != null && giamGia.getIsVoHan()) {
                giamGia.setSoLuong(null);
            } else {
                if (giamGia.getSoLuong() == null || giamGia.getSoLuong() <= 0) {
                    result.rejectValue("soLuong", "error.giamGia", "Vui lòng nhập số lượng hoặc chọn vô hạn!");
                }
            }
        }

        if (giamGia.getLoaiApDung() == 2 && (allSelectedIds == null || allSelectedIds.isEmpty())) {
            result.rejectValue("loaiApDung", "error.giamGia", "Vui lòng chọn ít nhất một khách hàng!");
        }

        if (result.hasErrors()) {
            System.out.println("=== ERRORS FOUND ===");
            result.getAllErrors().forEach(error -> {
                System.out.println("Error: " + error.toString());
            });

            giamGia.setMaGiamGia(originalMaGiamGia);

            Page<KhachHang> pageKh = khachHangService.searchByPhone(null, page);
            model.addAttribute("listKhachHang", pageKh.getContent());
            model.addAttribute("currentPage", pageKh.getNumber());
            model.addAttribute("totalPages", pageKh.getTotalPages());
            model.addAttribute("totalItems", pageKh.getTotalElements());
            model.addAttribute("isEdit", false);
            model.addAttribute("sdt", null);

            model.addAttribute("selectedKhachHangIds", allSelectedIds);
            model.addAttribute("totalSelectedCount", allSelectedIds.size());
            model.addAttribute("maGiamGia", originalMaGiamGia);
            model.addAttribute("giamGia", giamGia);

            return "giamgia/add";
        }

        try {
            giamGia.setNgayTao(LocalDateTime.now());
            giamGia.setTrangThai(giamGiaService.tinhToanTrangThai(giamGia));

            GiamGia saved = giamGiaService.save(giamGia);

            if (giamGia.getLoaiApDung() == 2 && allSelectedIds != null && !allSelectedIds.isEmpty()) {
                VoucherEmailDTO dto = new VoucherEmailDTO();
                dto.setTenGiamGia(saved.getTenGiamGia());
                dto.setLoaiGiamGia(saved.getLoaiGiamGia());
                dto.setGiaTri(saved.getGiaTriGiam());
                dto.setNgayBatDau(saved.getNgayBatDau());
                dto.setNgayKetThuc(saved.getNgayKetThuc());
                dto.setMaGiamGia(saved.getMaGiamGia());
                dto.setDonToiThieu(saved.getDonToiThieu());
                dto.setGiamToiDa(saved.getGiamToiDa());
                dto.setLoaiApDung(saved.getLoaiApDung() == 1 ? "Công khai" : "Cá nhân");

                for (String maKH : allSelectedIds) {
                    var kh = khachHangService.getKhachHangById(maKH);
                    if (kh == null) continue;

                    GiamGiaChiTiet ct = new GiamGiaChiTiet(
                            new GiamGiaChiTietId(maKH, saved.getMaGiamGia()),
                            kh, saved, LocalDateTime.now(), 0
                    );
                    giamGiaChiTietRepo.save(ct);

                    try {
                        emailService.sendVoucherEmail(kh.getEmail(), dto);
                    } catch (Exception e) {
                        System.out.println("Send mail fail: " + kh.getEmail());
                    }
                }
            }

            status.setComplete();
            ra.addFlashAttribute("successMess", "Thêm mới chương trình giảm giá thành công!");
            return "redirect:/giamgia/index";

        } catch (Exception e) {
            e.printStackTrace();
            giamGia.setMaGiamGia(originalMaGiamGia);
            model.addAttribute("errorMess", "Lỗi: " + e.getMessage());
            return "giamgia/add";
        }
    }

    @PostMapping("/update")
    @Transactional
    public String update(@ModelAttribute("giamGia") @Valid GiamGia giamGia,
                         BindingResult result,
                         SessionStatus status,
                         @RequestParam(value = "selectedKhachHang", required = false) List<String> selectedKhachHang,
                         @RequestParam(value = "selectedKhachHangIds", required = false) String selectedKhachHangIdsStr,
                         @RequestParam(defaultValue = "0") int page,
                         RedirectAttributes ra,
                         Model model) {

        System.out.println("=== UPDATE METHOD CALLED ===");
        System.out.println("maGiamGia from model: " + giamGia.getMaGiamGia());
        System.out.println("selectedKhachHang (checkbox): " + selectedKhachHang);
        System.out.println("selectedKhachHangIds (hidden): " + selectedKhachHangIdsStr);

        // ===== XỬ LÝ TỪ HIDDEN INPUT =====
        List<String> newSelectedIds = new ArrayList<>();
        if (selectedKhachHangIdsStr != null && !selectedKhachHangIdsStr.trim().isEmpty()) {
            String[] ids = selectedKhachHangIdsStr.split(",");
            for (String id : ids) {
                if (id != null && !id.trim().isEmpty()) {
                    newSelectedIds.add(id.trim());
                }
            }
        }
        // Nếu hidden input trống nhưng có checkbox, dùng checkbox (trang hiện tại)
        if (newSelectedIds.isEmpty() && selectedKhachHang != null && !selectedKhachHang.isEmpty()) {
            newSelectedIds = selectedKhachHang;
        }

        System.out.println("New selected IDs: " + newSelectedIds);
        System.out.println("New selected size: " + newSelectedIds.size());

        // ===== LẤY DANH SÁCH KHÁCH HÀNG CŨ =====
        List<String> oldSelectedIds = giamGiaChiTietRepo.findMaKhachHangByMaGiamGia(giamGia.getMaGiamGia());
        System.out.println("Old selected IDs: " + oldSelectedIds);

        // ===== XỬ LÝ MÃ GIẢM GIÁ =====
        String originalMaGiamGia = giamGia.getMaGiamGia();
        if (originalMaGiamGia != null && originalMaGiamGia.contains(",")) {
            String[] parts = originalMaGiamGia.split(",");
            originalMaGiamGia = parts[0].trim();
            giamGia.setMaGiamGia(originalMaGiamGia);
        }

        // Xử lý số lượng
        if (giamGia.getLoaiApDung() == 2) {
            if (newSelectedIds != null && !newSelectedIds.isEmpty()) {
                giamGia.setSoLuong(newSelectedIds.size());
            } else {
                giamGia.setSoLuong(0);
            }
            giamGia.setIsVoHan(false);
        } else if (giamGia.getLoaiApDung() == 1) {
            if (giamGia.getIsVoHan() != null && giamGia.getIsVoHan()) {
                giamGia.setSoLuong(null);
            } else {
                if (giamGia.getSoLuong() == null || giamGia.getSoLuong() <= 0) {
                    result.rejectValue("soLuong", "error.giamGia", "Vui lòng nhập số lượng hoặc chọn vô hạn!");
                }
            }
        }

        if (giamGia.getLoaiApDung() == 2 && (newSelectedIds == null || newSelectedIds.isEmpty())) {
            result.rejectValue("loaiApDung", "error.giamGia", "Vui lòng chọn ít nhất một khách hàng!");
        }

        if (result.hasErrors()) {
            System.out.println("=== ERRORS FOUND IN UPDATE ===");
            result.getAllErrors().forEach(error -> {
                System.out.println("Error: " + error.toString());
            });

            giamGia.setMaGiamGia(originalMaGiamGia);

            model.addAttribute("isEdit", true);
            Page<KhachHang> pageKh = khachHangService.searchByPhone(null, page);
            model.addAttribute("listKhachHang", pageKh.getContent());
            model.addAttribute("currentPage", pageKh.getNumber());
            model.addAttribute("totalPages", pageKh.getTotalPages());
            model.addAttribute("totalItems", pageKh.getTotalElements());
            model.addAttribute("sdt", null);

            model.addAttribute("selectedKhachHangIds", newSelectedIds);
            model.addAttribute("totalSelectedCount", newSelectedIds.size());
            model.addAttribute("maGiamGia", originalMaGiamGia);
            model.addAttribute("giamGia", giamGia);

            return "giamgia/add";
        }

        try {
            // Cập nhật trạng thái
            giamGia.setTrangThai(giamGiaService.tinhToanTrangThai(giamGia));
            giamGiaService.save(giamGia);

            // ===== QUAN TRỌNG: XÓA NHỮNG KHÁCH HÀNG KHÔNG CÒN TRONG DANH SÁCH =====
            // Tìm những ID cũ không có trong danh sách mới
            List<String> idsToRemove = new ArrayList<>();
            if (oldSelectedIds != null && newSelectedIds != null) {
                for (String oldId : oldSelectedIds) {
                    if (!newSelectedIds.contains(oldId)) {
                        idsToRemove.add(oldId);
                    }
                }
            }

            // Xóa những khách hàng đã bị bỏ chọn
            if (!idsToRemove.isEmpty()) {
                System.out.println("Removing customers: " + idsToRemove);
                for (String maKH : idsToRemove) {
                    giamGiaChiTietRepo.deleteById(new GiamGiaChiTietId(maKH, giamGia.getMaGiamGia()));
                }
            }

            // ===== THÊM KHÁCH HÀNG MỚI (NHỮNG ID CHƯA CÓ) =====
            if (giamGia.getLoaiApDung() == 2 && newSelectedIds != null && !newSelectedIds.isEmpty()) {
                VoucherEmailDTO dto = new VoucherEmailDTO();
                dto.setTenGiamGia(giamGia.getTenGiamGia());
                dto.setLoaiGiamGia(giamGia.getLoaiGiamGia());
                dto.setGiaTri(giamGia.getGiaTriGiam());
                dto.setNgayBatDau(giamGia.getNgayBatDau());
                dto.setNgayKetThuc(giamGia.getNgayKetThuc());
                dto.setMaGiamGia(giamGia.getMaGiamGia());
                dto.setDonToiThieu(giamGia.getDonToiThieu());
                dto.setGiamToiDa(giamGia.getGiamToiDa());
                dto.setLoaiApDung(giamGia.getLoaiApDung() == 1 ? "Công khai" : "Cá nhân");

                for (String maKH : newSelectedIds) {
                    // Kiểm tra xem khách hàng đã có chưa (tránh trùng)
                    boolean exists = giamGiaChiTietRepo.existsById(new GiamGiaChiTietId(maKH, giamGia.getMaGiamGia()));
                    if (!exists) {
                        var kh = khachHangService.getKhachHangById(maKH);
                        if (kh == null) continue;

                        GiamGiaChiTiet ct = new GiamGiaChiTiet(
                                new GiamGiaChiTietId(maKH, giamGia.getMaGiamGia()),
                                kh, giamGia, LocalDateTime.now(), 0
                        );
                        giamGiaChiTietRepo.save(ct);

                        try {
                            emailService.sendVoucherEmail(kh.getEmail(), dto);
                            System.out.println("Email sent to new customer: " + kh.getEmail());
                        } catch (Exception e) {
                            System.out.println("Send mail fail: " + kh.getEmail());
                        }
                    }
                }
            }

            status.setComplete();
            ra.addFlashAttribute("successMess", "Cập nhật chương trình giảm giá thành công!");
            return "redirect:/giamgia/index";

        } catch (Exception e) {
            e.printStackTrace();
            giamGia.setMaGiamGia(originalMaGiamGia);
            model.addAttribute("errorMess", "Lỗi: " + e.getMessage());
            return "giamgia/add";
        }
    }

    @GetMapping("/cancel/{id}")
    public String cancel(@PathVariable("id") String id, RedirectAttributes ra) {
        GiamGia giamGia = giamGiaService.getGiamGiaById(id).orElse(null);

        if (giamGia == null) {
            ra.addFlashAttribute("errorMess", "Không tìm thấy chương trình giảm giá!");
            return "redirect:/giamgia/index";
        }

        if (!"Sắp hoạt động".equals(giamGia.getTrangThai())) {
            ra.addFlashAttribute("errorMess", "Chỉ có thể huỷ chương trình ở trạng thái 'Sắp hoạt động'!");
            return "redirect:/giamgia/index";
        }

        giamGia.setTrangThai("Đã huỷ");
        giamGiaService.save(giamGia);

        ra.addFlashAttribute("successMess", "Đã huỷ chương trình giảm giá thành công!");
        return "redirect:/giamgia/index";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") String id, RedirectAttributes ra) {
        GiamGia giamGia = giamGiaService.getGiamGiaById(id).orElse(null);

        if (giamGia == null) {
            ra.addFlashAttribute("errorMess", "Không tìm thấy chương trình giảm giá!");
            return "redirect:/giamgia/index";
        }

        if (!"Đã huỷ".equals(giamGia.getTrangThai()) && !"Ngừng hoạt động".equals(giamGia.getTrangThai())) {
            ra.addFlashAttribute("errorMess", "Chỉ có thể xoá chương trình ở trạng thái 'Đã huỷ' hoặc 'Ngừng hoạt động'!");
            return "redirect:/giamgia/index";
        }

        giamGia.setTrangThai("Ngừng hoạt động");
        giamGiaService.save(giamGia);

        ra.addFlashAttribute("successMess", "Đã xoá chương trình giảm giá thành công!");
        return "redirect:/giamgia/index";
    }

    @GetMapping("loctt")
    public String loctt(@RequestParam("tt") String tt, Model model) {
        List<GiamGia> list = giamGiaService.loctt(tt);
        model.addAttribute("list", list);
        model.addAttribute("giamGia", new GiamGia());
        return "giamgia/index";
    }

    @GetMapping("loclg")
    public String loctloai(@RequestParam("lg") String tt, Model model) {
        List<GiamGia> list = giamGiaService.loclg(tt);
        model.addAttribute("list", list);
        model.addAttribute("giamGia", new GiamGia());
        return "giamgia/index";
    }

    @GetMapping("locten")
    public String locten(@RequestParam("ten") String tt, Model model) {
        List<GiamGia> list = giamGiaService.timkiem(tt);
        model.addAttribute("list", list);
        model.addAttribute("giamGia", new GiamGia());
        return "giamgia/index";
    }

    @GetMapping("locngay")
    public String loctt(@RequestParam("ngaybdau") LocalDateTime ngay1,
                        @RequestParam("ngaykthuc") LocalDateTime ngay2,
                        Model model) {
        List<GiamGia> list = giamGiaService.locng(ngay1, ngay2);
        model.addAttribute("list", list);
        model.addAttribute("giamGia", new GiamGia());
        return "giamgia/index";
    }

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tt,
            @RequestParam(required = false) String lg,
            @RequestParam(required = false) Integer loaiApDung,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ngaybdau,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ngaykthuc) {

        try {
            List<GiamGia> list = giamGiaService.findAllFiltered(keyword, tt, lg, loaiApDung, ngaybdau, ngaykthuc);

            System.out.println("=== EXPORT EXCEL ===");
            System.out.println("keyword: " + keyword);
            System.out.println("trangThai: " + tt);
            System.out.println("loaiGiamGia: " + lg);
            System.out.println("loaiApDung: " + loaiApDung);
            System.out.println("Số lượng bản ghi xuất: " + list.size());

            if (list.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            ByteArrayInputStream excelStream = excelExportService.exportGiamGiaToExcel(list);

            if (excelStream == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            String filename = "Danh_sach_giam_gia_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(excelStream.readAllBytes());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stop/{id}")
    public String stop(@PathVariable("id") String id, RedirectAttributes ra) {
        GiamGia giamGia = giamGiaService.getGiamGiaById(id).orElse(null);

        if (giamGia == null) {
            ra.addFlashAttribute("errorMess", "Không tìm thấy chương trình giảm giá!");
            return "redirect:/giamgia/index";
        }

        if (!"Hoạt động".equals(giamGia.getTrangThai())) {
            ra.addFlashAttribute("errorMess", "Chỉ có thể ngừng hoạt động chương trình đang ở trạng thái 'Hoạt động'!");
            return "redirect:/giamgia/index";
        }

        // ===== NẾU LÀ CÁ NHÂN, GỬI EMAIL THÔNG BÁO CHO KHÁCH HÀNG =====
        if (giamGia.getLoaiApDung() == 2) {
            try {
                List<String> customerIds = giamGiaChiTietRepo.findMaKhachHangByMaGiamGia(id);

                if (customerIds != null && !customerIds.isEmpty()) {
                    System.out.println("Sending stop notification to " + customerIds.size() + " customers");

                    // Tạo DTO
                    VoucherEmailDTO dto = new VoucherEmailDTO();
                    dto.setTenGiamGia(giamGia.getTenGiamGia());
                    dto.setLoaiGiamGia(giamGia.getLoaiGiamGia());
                    dto.setGiaTri(giamGia.getGiaTriGiam());
                    dto.setNgayBatDau(giamGia.getNgayBatDau());
                    dto.setNgayKetThuc(giamGia.getNgayKetThuc());
                    dto.setMaGiamGia(giamGia.getMaGiamGia());
                    dto.setDonToiThieu(giamGia.getDonToiThieu());
                    dto.setGiamToiDa(giamGia.getGiamToiDa());
                    dto.setLoaiApDung(giamGia.getLoaiApDung() == 1 ? "Công khai" : "Cá nhân");

                    for (String maKH : customerIds) {
                        var kh = khachHangService.getKhachHangById(maKH);
                        if (kh == null) continue;

                        try {
                            emailService.sendVoucherStopEmail(kh.getEmail(), dto);
                            System.out.println("Stop notification sent to: " + kh.getEmail());
                        } catch (Exception e) {
                            System.out.println("Send stop notification fail: " + kh.getEmail());
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error sending stop notifications: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Cập nhật trạng thái sang Ngừng hoạt động
        giamGiaService.updateTrangThaiToStop(id);

        ra.addFlashAttribute("successMess", "Đã chuyển trạng thái sang 'Ngừng hoạt động' thành công!");
        return "redirect:/giamgia/index";
    }

    // Thêm vào GiamGiaController.java

    // Trong GiamGiaController.java - Sửa method activate
    @GetMapping("/activate/{id}")
    public String activate(@PathVariable("id") String id, RedirectAttributes ra) {
        GiamGia giamGia = giamGiaService.getGiamGiaById(id).orElse(null);

        if (giamGia == null) {
            ra.addFlashAttribute("errorMess", "Không tìm thấy chương trình giảm giá!");
            return "redirect:/giamgia/index";
        }

        if (!"Sắp hoạt động".equals(giamGia.getTrangThai())) {
            ra.addFlashAttribute("errorMess", "Chỉ có thể kích hoạt chương trình ở trạng thái 'Sắp hoạt động'!");
            return "redirect:/giamgia/index";
        }

        try {
            // Cập nhật trực tiếp bằng Native Query (bỏ qua validation)
            giamGiaService.activateVoucher(id);

            // ===== NẾU LÀ CÁ NHÂN, GỬI EMAIL THÔNG BÁO =====
            if (giamGia.getLoaiApDung() == 2) {
                // Lấy lại dữ liệu mới nhất
                GiamGia updatedGiamGia = giamGiaService.getGiamGiaById(id).orElse(null);
                if (updatedGiamGia != null) {
                    sendActivationEmails(updatedGiamGia);
                }
            }

            ra.addFlashAttribute("successMess", "Đã kích hoạt chương trình giảm giá thành công!");

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("errorMess", "Lỗi khi kích hoạt: " + e.getMessage());
        }

        return "redirect:/giamgia/index";
    }

    private void sendActivationEmails(GiamGia giamGia) {
        try {
            List<String> customerIds = giamGiaChiTietRepo.findMaKhachHangByMaGiamGia(giamGia.getMaGiamGia());

            if (customerIds != null && !customerIds.isEmpty()) {
                VoucherEmailDTO dto = new VoucherEmailDTO();
                dto.setTenGiamGia(giamGia.getTenGiamGia());
                dto.setLoaiGiamGia(giamGia.getLoaiGiamGia());
                dto.setGiaTri(giamGia.getGiaTriGiam());
                dto.setNgayBatDau(giamGia.getNgayBatDau());
                dto.setNgayKetThuc(giamGia.getNgayKetThuc());
                dto.setMaGiamGia(giamGia.getMaGiamGia());
                dto.setDonToiThieu(giamGia.getDonToiThieu());
                dto.setGiamToiDa(giamGia.getGiamToiDa());

                for (String maKH : customerIds) {
                    var kh = khachHangService.getKhachHangById(maKH);
                    if (kh == null) continue;

                    try {
                        emailService.sendVoucherActivationEmail(kh.getEmail(), dto);
                        System.out.println("Activation notification sent to: " + kh.getEmail());
                    } catch (Exception e) {
                        System.out.println("Send activation notification fail: " + kh.getEmail());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error sending activation notifications: " + e.getMessage());
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
            List<GiamGia> suggestions = giamGiaRepository.findTop10ByMaGiamGiaContainingOrTenGiamGiaContaining(
                    keyword.trim(), pageable
            );

            List<Map<String, String>> result = suggestions.stream()
                    .map(gg -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("maGiamGia", gg.getMaGiamGia());
                        map.put("tenGiamGia", gg.getTenGiamGia());
                        map.put("trangThai", gg.getTrangThai());
                        map.put("loaiGiamGia", gg.getLoaiGiamGia());
                        map.put("giaTriGiam", String.valueOf(gg.getGiaTriGiam()));
                        map.put("loaiApDung", gg.getLoaiApDung() == 1 ? "Công khai" : "Cá nhân");
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


