package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.*;
import com.example.th06876_java202.Repository.DotGiamGiaRepo;
import com.example.th06876_java202.Service.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class DotGiamGiaCon {
    @Autowired
    private DotGiamGiaService dotGiamGiaService;

    @Autowired
    private SanPhamChiTietService sanPhamChiTietService;

    @Autowired
    private ChiTietDotGiamGiaService chiTietDotGiamGiaService;

    @Autowired
    private SanPhamService sanPhamservice;

    @Autowired
    private DotGiamGiaRepo dotGiamGiaRepo;

    @Autowired
    private MauSacService mauSacService;

    @Autowired
    private KichThuocService kichThuocService;

    @Autowired
    ExcelExportService excelExportService;

    @PersistenceContext
    private EntityManager entityManager;

    private String generateMaGiamGia() {
        Random random = new Random();
        String code;
        boolean exists;
        int attempts = 0;
        int maxAttempts = 100;

        do {
            int randomNumber = 1000 + random.nextInt(9000);
            code = "DGG" + randomNumber;
            exists = dotGiamGiaRepo.existsById(code);
            attempts++;

            if (attempts > maxAttempts) {
                code = "DGG" + System.currentTimeMillis();
                break;
            }
        } while (exists);

        return code;
    }

    private void addFilterData(Model model) {
        model.addAttribute("listMauSac", mauSacService.findAll());
        model.addAttribute("listKichThuoc", kichThuocService.getall());

        Double maxGia = sanPhamChiTietService.gia();
        if (maxGia == null) {
            maxGia = 1000000000.0;
        }
        model.addAttribute("minGia", 0);
        model.addAttribute("maxGia", maxGia);

        model.addAttribute("listSP", sanPhamservice.getAll());
    }

    @GetMapping("/dot-giam-gia/hien-thi")
    public String hienThi(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime denNgay,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        System.out.println("=== HIEN THI START ===");
        System.out.println("page: " + page);

        entityManager.clear();

        Pageable pageable = PageRequest.of(
                page,
                5,
                Sort.by(Sort.Direction.DESC, "ngayTao")
        );

        Page<DotGiamGia> dggPage = dotGiamGiaService.filterPaging(
                keyword,
                trangThai,
                tuNgay,
                denNgay,
                pageable
        );

        System.out.println("Total items: " + dggPage.getTotalElements());
        if (!dggPage.getContent().isEmpty()) {
            DotGiamGia first = dggPage.getContent().get(0);
            System.out.println("First item: " + first.getMaGiamGia() + " - " + first.getTrangThai());
        }

        model.addAttribute("listDGG", dggPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", dggPage.getTotalPages());
        model.addAttribute("totalItems", dggPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("tuNgay", tuNgay);
        model.addAttribute("denNgay", denNgay);
        model.addAttribute("dgg", new DotGiamGia());

        System.out.println("=== HIEN THI END ===");

        return "dotgiamgia/index";
    }

    // ===== THÊM MỚI =====
    @GetMapping("/chi-tiet-dot-giam-gia/them-moi")
    public String showPageThemMoi(Model model, @RequestParam(defaultValue = "0") int page) {
        System.out.println("=== SHOW THEM MOI START ===");
        System.out.println("page: " + page);

        DotGiamGiaDTO dto = new DotGiamGiaDTO();
        DotGiamGia dgg = new DotGiamGia();
        dgg.setMaGiamGia(generateMaGiamGia());
        dgg.setNgayTao(LocalDateTime.now());
        dto.setDotGiamGia(dgg);
        dto.setListMaSanPham(new ArrayList<>());
        dto.setListMaSanPhamChiTiet(new ArrayList<>());

        model.addAttribute("dggDTO", dto);
        model.addAttribute("isEdit", false);
        model.addAttribute("isView", false);
        model.addAttribute("currentPage", page);

        addFilterData(model);

        System.out.println("Generated maGiamGia: " + dgg.getMaGiamGia());
        System.out.println("=== SHOW THEM MOI END ===");

        return "chitietdotgiamgia/index";
    }

    // ===== XEM CHI TIẾT =====
    @GetMapping("/dot-giam-gia/detail/{id}")
    public String detail(@PathVariable("id") String id, Model model,
                         RedirectAttributes redirectAttributes,
                         @RequestParam(defaultValue = "0") int page) {

        System.out.println("=== DETAIL START ===");
        System.out.println("id: " + id);
        System.out.println("page: " + page);

        if (id == null || id.trim().isEmpty() || "null".equals(id)) {
            System.out.println("ERROR: ID is null or empty");
            redirectAttributes.addFlashAttribute("errorMess", "ID không hợp lệ!");
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }

        try {
            DotGiamGia dgg = dotGiamGiaService.getById(id);

            if (dgg == null) {
                System.out.println("ERROR: Không tìm thấy đợt giảm giá với ID: " + id);
                redirectAttributes.addFlashAttribute("errorMess", "Không tìm thấy đợt giảm giá!");
                return "redirect:/dot-giam-gia/hien-thi?page=" + page;
            }

            DotGiamGiaDTO dto = new DotGiamGiaDTO();
            dto.setDotGiamGia(dgg);

            List<String> listSP = chiTietDotGiamGiaService.getSanPhamByDot(id);
            List<String> listSPCT = chiTietDotGiamGiaService.getSanPhamChiTietByDot(id);
            dto.setListMaSanPham(listSP != null ? listSP : new ArrayList<>());
            dto.setListMaSanPhamChiTiet(listSPCT != null ? listSPCT : new ArrayList<>());

            System.out.println("listSP: " + listSP);
            System.out.println("listSPCT: " + listSPCT);

            model.addAttribute("dggDTO", dto);
            model.addAttribute("isView", true);
            model.addAttribute("isEdit", false);
            model.addAttribute("currentPage", page);

            addFilterData(model);

            System.out.println("=== DETAIL END ===");
            return "chitietdotgiamgia/index";

        } catch (Exception e) {
            System.out.println("=== DETAIL ERROR ===");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMess", "Lỗi khi tải dữ liệu: " + e.getMessage());
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }
    }

    // ===== CHỈNH SỬA =====
    @GetMapping("/dot-giam-gia/edit/{id}")
    public String edit(@PathVariable("id") String id, Model model,
                       RedirectAttributes redirectAttributes,
                       @RequestParam(defaultValue = "0") int page) {

        System.out.println("=== EDIT START ===");
        System.out.println("id: " + id);
        System.out.println("page: " + page);

        if (id == null || id.trim().isEmpty() || "null".equals(id)) {
            System.out.println("ERROR: ID is null or empty");
            redirectAttributes.addFlashAttribute("errorMess", "ID không hợp lệ!");
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }

        try {
            DotGiamGia dgg = dotGiamGiaService.getById(id);

            if (dgg == null) {
                System.out.println("ERROR: Không tìm thấy đợt giảm giá với ID: " + id);
                redirectAttributes.addFlashAttribute("errorMess", "Không tìm thấy đợt giảm giá!");
                return "redirect:/dot-giam-gia/hien-thi?page=" + page;
            }

            if (!"Sắp hoạt động".equals(dgg.getTrangThai())) {
                System.out.println("ERROR: Trạng thái không phải Sắp hoạt động: " + dgg.getTrangThai());
                redirectAttributes.addFlashAttribute("errorMess",
                        "Chỉ được chỉnh sửa khi đợt giảm giá ở trạng thái Sắp hoạt động!");
                return "redirect:/dot-giam-gia/hien-thi?page=" + page;
            }

            DotGiamGiaDTO dto = new DotGiamGiaDTO();
            dto.setDotGiamGia(dgg);

            List<String> listSP = chiTietDotGiamGiaService.getSanPhamByDot(id);
            List<String> listSPCT = chiTietDotGiamGiaService.getSanPhamChiTietByDot(id);
            dto.setListMaSanPham(listSP != null ? listSP : new ArrayList<>());
            dto.setListMaSanPhamChiTiet(listSPCT != null ? listSPCT : new ArrayList<>());

            System.out.println("listSP: " + listSP);
            System.out.println("listSPCT: " + listSPCT);

            model.addAttribute("dggDTO", dto);
            model.addAttribute("isEdit", true);
            model.addAttribute("isView", false);
            model.addAttribute("currentPage", page);

            addFilterData(model);

            System.out.println("=== EDIT END ===");
            return "chitietdotgiamgia/index";

        } catch (Exception e) {
            System.out.println("=== EDIT ERROR ===");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMess", "Lỗi khi tải dữ liệu: " + e.getMessage());
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }
    }

    // ===== KÍCH HOẠT SỚM =====
    @GetMapping("/dot-giam-gia/activate/{id}")
    public String activate(@PathVariable("id") String id, RedirectAttributes ra,
                           @RequestParam(defaultValue = "0") int page) {
        System.out.println("=== ACTIVATE START ===");
        System.out.println("id: " + id);
        System.out.println("page: " + page);

        if (id == null || id.trim().isEmpty() || "null".equals(id)) {
            System.out.println("ERROR: ID is null or empty");
            ra.addFlashAttribute("errorMess", "ID không hợp lệ!");
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }

        try {
            if (!dotGiamGiaRepo.existsById(id)) {
                System.out.println("ERROR: Không tìm thấy đợt giảm giá!");
                ra.addFlashAttribute("errorMess", "Không tìm thấy đợt giảm giá!");
                return "redirect:/dot-giam-gia/hien-thi?page=" + page;
            }

            DotGiamGia dgg = dotGiamGiaService.getById(id);

            if (dgg == null) {
                System.out.println("ERROR: Không tìm thấy đợt giảm giá!");
                ra.addFlashAttribute("errorMess", "Không tìm thấy đợt giảm giá!");
                return "redirect:/dot-giam-gia/hien-thi?page=" + page;
            }

            if (!"Sắp hoạt động".equals(dgg.getTrangThai())) {
                System.out.println("ERROR: Trạng thái không phải Sắp hoạt động: " + dgg.getTrangThai());
                ra.addFlashAttribute("errorMess", "Chỉ có thể kích hoạt khi trạng thái Sắp hoạt động!");
                return "redirect:/dot-giam-gia/hien-thi?page=" + page;
            }

            dotGiamGiaService.activateVoucher(id);
            entityManager.clear();
            ra.addFlashAttribute("successMess", "Đã kích hoạt đợt giảm giá thành công!");
            System.out.println("=== ACTIVATE END ===");
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;

        } catch (Exception e) {
            System.out.println("=== ACTIVATE ERROR ===");
            e.printStackTrace();
            ra.addFlashAttribute("errorMess", "Lỗi khi kích hoạt: " + e.getMessage());
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }
    }

    // ===== HUỶ =====
    @Transactional
    @GetMapping("/dot-giam-gia/cancel/{id}")
    public String cancel(@PathVariable("id") String id, RedirectAttributes ra,
                         @RequestParam(defaultValue = "0") int page) {
        System.out.println("=== CANCEL START ===");
        System.out.println("id: " + id);
        System.out.println("page: " + page);

        if (id == null || id.trim().isEmpty() || "null".equals(id)) {
            System.out.println("ERROR: ID is null or empty");
            ra.addFlashAttribute("errorMess", "ID không hợp lệ!");
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }

        try {
            String sql = "UPDATE DotGiamGia SET TrangThai = N'Đã huỷ' WHERE MaGiamGia = ? AND TrangThai = N'Sắp hoạt động'";
            int updated = entityManager.createNativeQuery(sql)
                    .setParameter(1, id)
                    .executeUpdate();

            System.out.println("Updated rows: " + updated);

            entityManager.flush();
            entityManager.clear();

            if (updated > 0) {
                ra.addFlashAttribute("successMess", "Đã huỷ đợt giảm giá thành công!");
            } else {
                ra.addFlashAttribute("errorMess", "Không thể huỷ đợt giảm giá! Vui lòng kiểm tra trạng thái.");
            }

            System.out.println("=== CANCEL END ===");
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;

        } catch (Exception e) {
            System.out.println("=== CANCEL ERROR ===");
            e.printStackTrace();
            ra.addFlashAttribute("errorMess", "Lỗi khi huỷ: " + e.getMessage());
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }
    }

    @GetMapping("/dot-giam-gia/stop/{id}")
    public String stop(@PathVariable("id") String id, RedirectAttributes ra,
                       @RequestParam(defaultValue = "0") int page) {
        System.out.println("=== STOP START ===");
        System.out.println("id: " + id);
        System.out.println("page: " + page);

        if (id == null || id.trim().isEmpty() || "null".equals(id)) {
            System.out.println("ERROR: ID is null or empty");
            ra.addFlashAttribute("errorMess", "ID không hợp lệ!");
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }

        try {
            if (!dotGiamGiaRepo.existsById(id)) {
                System.out.println("ERROR: Không tìm thấy đợt giảm giá!");
                ra.addFlashAttribute("errorMess", "Không tìm thấy đợt giảm giá!");
                return "redirect:/dot-giam-gia/hien-thi?page=" + page;
            }

            DotGiamGia dgg = dotGiamGiaService.getById(id);

            if (dgg == null) {
                System.out.println("ERROR: Không tìm thấy đợt giảm giá!");
                ra.addFlashAttribute("errorMess", "Không tìm thấy đợt giảm giá!");
                return "redirect:/dot-giam-gia/hien-thi?page=" + page;
            }

            if (!"Hoạt động".equals(dgg.getTrangThai())) {
                System.out.println("ERROR: Trạng thái không phải Hoạt động: " + dgg.getTrangThai());
                ra.addFlashAttribute("errorMess", "Chỉ có thể ngừng hoạt động khi trạng thái Hoạt động!");
                return "redirect:/dot-giam-gia/hien-thi?page=" + page;
            }

            dotGiamGiaService.updateTrangThaiToStop(id);
            entityManager.clear();
            ra.addFlashAttribute("successMess", "Đã ngừng hoạt động đợt giảm giá thành công!");
            System.out.println("=== STOP END ===");
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;

        } catch (Exception e) {
            System.out.println("=== STOP ERROR ===");
            e.printStackTrace();
            ra.addFlashAttribute("errorMess", "Lỗi khi ngừng hoạt động: " + e.getMessage());
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }
    }

    // ===== XOÁ =====
    @GetMapping("/dot-giam-gia/delete/{id}")
    public String delete(@PathVariable("id") String id, RedirectAttributes ra,
                         @RequestParam(defaultValue = "0") int page) {
        System.out.println("=== DELETE START ===");
        System.out.println("id: " + id);
        System.out.println("page: " + page);

        if (id == null || id.trim().isEmpty() || "null".equals(id)) {
            System.out.println("ERROR: ID is null or empty");
            ra.addFlashAttribute("errorMess", "ID không hợp lệ!");
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }

        try {
            if (!dotGiamGiaRepo.existsById(id)) {
                System.out.println("ERROR: Không tìm thấy đợt giảm giá!");
                ra.addFlashAttribute("errorMess", "Không tìm thấy đợt giảm giá!");
                return "redirect:/dot-giam-gia/hien-thi?page=" + page;
            }

            DotGiamGia dgg = dotGiamGiaService.getById(id);

            if (dgg == null) {
                System.out.println("ERROR: Không tìm thấy đợt giảm giá!");
                ra.addFlashAttribute("errorMess", "Không tìm thấy đợt giảm giá!");
                return "redirect:/dot-giam-gia/hien-thi?page=" + page;
            }

            if (!"Đã huỷ".equals(dgg.getTrangThai()) && !"Ngừng hoạt động".equals(dgg.getTrangThai())) {
                System.out.println("ERROR: Trạng thái không thể xoá: " + dgg.getTrangThai());
                ra.addFlashAttribute("errorMess", "Chỉ có thể xoá khi trạng thái Đã huỷ hoặc Ngừng hoạt động!");
                return "redirect:/dot-giam-gia/hien-thi?page=" + page;
            }

            dotGiamGiaService.suaa(id);
            entityManager.clear();
            ra.addFlashAttribute("successMess", "Đã xoá đợt giảm giá thành công!");
            System.out.println("=== DELETE END ===");
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;

        } catch (Exception e) {
            System.out.println("=== DELETE ERROR ===");
            e.printStackTrace();
            ra.addFlashAttribute("errorMess", "Lỗi khi xoá: " + e.getMessage());
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }
    }

    @PostMapping("/dot-giam-gia/save-all")
    public String saveAll(@Valid @ModelAttribute("dggDTO") DotGiamGiaDTO dggDTO,
                          BindingResult result,
                          Model model,
                          RedirectAttributes ra,
                          @RequestParam(defaultValue = "0") int page) {

        System.out.println("=== SAVE ALL START ===");
        System.out.println("📌 listMaSanPham received: " + dggDTO.getListMaSanPham());
        System.out.println("📌 listMaSanPhamChiTiet received: " + dggDTO.getListMaSanPhamChiTiet());
        System.out.println("page: " + page);

        if (dggDTO == null || dggDTO.getDotGiamGia() == null) {
            System.out.println("ERROR: dggDTO or dotGiamGia is null");
            ra.addFlashAttribute("errorMess", "Dữ liệu không hợp lệ!");
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }

        DotGiamGia dgg = dggDTO.getDotGiamGia();

        // Lấy mã từ DTO, loại bỏ dấu phẩy nếu có
        String maGiamGia = dgg.getMaGiamGia();
        if (maGiamGia != null && maGiamGia.contains(",")) {
            maGiamGia = maGiamGia.split(",")[0].trim();
            dgg.setMaGiamGia(maGiamGia);
            System.out.println("Fixed maGiamGia: " + maGiamGia);
        }

        // ===== VALIDATION =====
        // Validate tên
        if (dgg.getTenGiamGia() == null || dgg.getTenGiamGia().trim().isEmpty()) {
            result.rejectValue("dotGiamGia.tenGiamGia", "error.tenGiamGia", "Tên không được để trống");
        }

        // Validate giá trị giảm
        if (dgg.getGiaTriGiam() == null) {
            result.rejectValue("dotGiamGia.giaTriGiam", "error.giaTriGiam", "Giá trị giảm không được để trống");
        } else {
            double giaTri = dgg.getGiaTriGiam().doubleValue();
            if (giaTri <= 0) {
                result.rejectValue("dotGiamGia.giaTriGiam", "error.giaTriGiam", "Giá trị giảm phải lớn hơn 0");
            } else if (giaTri > 100) {
                result.rejectValue("dotGiamGia.giaTriGiam", "error.giaTriGiam", "Giá trị giảm không được vượt quá 100%");
            }
        }

        // Validate ngày bắt đầu
        if (dgg.getNgayBatDau() == null) {
            result.rejectValue("dotGiamGia.ngayBatDau", "error.ngayBatDau", "Ngày bắt đầu không được để trống");
        } else if (dgg.getNgayBatDau().isBefore(LocalDate.now())) {
            result.rejectValue("dotGiamGia.ngayBatDau", "error.ngayBatDau", "Ngày bắt đầu phải từ hôm nay trở đi");
        }

        // Validate ngày kết thúc
        if (dgg.getNgayKetThuc() == null) {
            result.rejectValue("dotGiamGia.ngayKetThuc", "error.ngayKetThuc", "Ngày kết thúc không được để trống");
        }

        // Validate ngày kết thúc > ngày bắt đầu
        if (dgg.getNgayBatDau() != null && dgg.getNgayKetThuc() != null
                && dgg.getNgayKetThuc().isBefore(dgg.getNgayBatDau())) {
            result.rejectValue("dotGiamGia.ngayKetThuc", "error.ngayKetThuc",
                    "Ngày kết thúc phải lớn hơn ngày bắt đầu");
        }

        // Validate biến thể
        if (dggDTO.getListMaSanPhamChiTiet() == null || dggDTO.getListMaSanPhamChiTiet().isEmpty()) {
            System.out.println("❌ ERROR: listMaSanPhamChiTiet is empty!");
            result.rejectValue("listMaSanPhamChiTiet", "error.listMaSanPhamChiTiet",
                    "Vui lòng chọn ít nhất một biến thể sản phẩm!");
        } else {
            System.out.println("✅ listMaSanPhamChiTiet: " + dggDTO.getListMaSanPhamChiTiet());
        }

        System.out.println("Has errors: " + result.hasErrors());
        if (result.hasErrors()) {
            System.out.println("Errors: " + result.getAllErrors());
            model.addAttribute("listSP", sanPhamservice.getAll());
            model.addAttribute("listMauSac", mauSacService.findAll());
            model.addAttribute("listKichThuoc", kichThuocService.getall());
            Double maxGia = sanPhamChiTietService.gia();
            model.addAttribute("minGia", 0);
            model.addAttribute("maxGia", maxGia != null ? maxGia : 1000000000);
            model.addAttribute("isEdit", dgg.getMaGiamGia() != null && dotGiamGiaRepo.existsById(dgg.getMaGiamGia()));
            model.addAttribute("isView", false);
            model.addAttribute("currentPage", page);
            return "chitietdotgiamgia/index";
        }

        System.out.println("Before save - maGiamGia: " + dgg.getMaGiamGia());

        // Kiểm tra nếu là edit (có mã và tồn tại trong DB)
        boolean isEdit = dgg.getMaGiamGia() != null && !dgg.getMaGiamGia().isEmpty()
                && dotGiamGiaRepo.existsById(dgg.getMaGiamGia());

        if (isEdit) {
            DotGiamGia existing = dotGiamGiaService.getById(dgg.getMaGiamGia());
            if (existing != null) {
                dgg.setNgayTao(existing.getNgayTao());
                if (dgg.getTrangThai() == null || dgg.getTrangThai().isEmpty()) {
                    dgg.setTrangThai(existing.getTrangThai());
                }
            }
            System.out.println("Updating existing DGG: " + dgg.getMaGiamGia());
        } else {
            String newMa = generateMaGiamGia();
            dgg.setMaGiamGia(newMa);
            dgg.setNgayTao(LocalDateTime.now());
            dgg.setTrangThai("Sắp hoạt động");
            System.out.println("Generated new maGiamGia: " + newMa);
        }

        if (dgg.getTrangThai() == null || dgg.getTrangThai().isEmpty()) {
            dgg.setTrangThai("Sắp hoạt động");
        }
        System.out.println("TrangThai: " + dgg.getTrangThai());

        try {
            // ===== 1. LƯU ĐỢT GIẢM GIÁ =====
            DotGiamGia savedDGG = dotGiamGiaService.save(dgg);
            System.out.println("Saved DGG: " + savedDGG.getMaGiamGia());

            // ===== 2. LÀM SẠCH DỮ LIỆU BIẾN THỂ =====
            List<String> listMaSPCT = new ArrayList<>();
            if (dggDTO.getListMaSanPhamChiTiet() != null) {
                for (String item : dggDTO.getListMaSanPhamChiTiet()) {
                    if (item == null || item.trim().isEmpty()) continue;

                    // ⭐ Tách các phần tử bị gộp bởi dấu phẩy
                    String[] parts = item.split(",");
                    for (String part : parts) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty() && !listMaSPCT.contains(trimmed)) {
                            listMaSPCT.add(trimmed);
                        }
                    }
                }
            }
            System.out.println("📌 Cleaned listMaSPCT: " + listMaSPCT);

// ===== 3. XÓA CHI TIẾT CŨ =====
            chiTietDotGiamGiaService.deleteByDotId(savedDGG.getMaGiamGia());
            System.out.println("Deleted old details for: " + savedDGG.getMaGiamGia());

// ===== 4. LƯU CHI TIẾT MỚI =====
            if (listMaSPCT != null && !listMaSPCT.isEmpty()) {
                System.out.println("Saving details...");
                int totalSaved = 0;

                // ⭐ TẠO MAP để nhóm biến thể theo sản phẩm
                Map<String, List<String>> mapSPToSPCT = new HashMap<>();

                for (String maSPCT : listMaSPCT) {
                    if (maSPCT == null || maSPCT.trim().isEmpty()) continue;
                    maSPCT = maSPCT.trim();

                    // ⭐ TRÍCH XUẤT MÃ SẢN PHẨM CHA
                    String maSP = extractMaSanPham(maSPCT);
                    if (maSP == null || maSP.isEmpty()) {
                        System.err.println("❌ Không thể extract mã sản phẩm từ: " + maSPCT);
                        continue;
                    }

                    mapSPToSPCT.computeIfAbsent(maSP, k -> new ArrayList<>()).add(maSPCT);
                }

                System.out.println("📌 Grouped by product: " + mapSPToSPCT);

                // Lưu từng nhóm
                for (Map.Entry<String, List<String>> entry : mapSPToSPCT.entrySet()) {
                    String maSP = entry.getKey();
                    List<String> spctList = entry.getValue();

                    System.out.println("  - Saving for product " + maSP + ": " + spctList);

                    int count = chiTietDotGiamGiaService.saveAllDetails(
                            savedDGG.getMaGiamGia(),
                            maSP,
                            spctList
                    );
                    totalSaved += count;
                }

                System.out.println("✅ Saved " + totalSaved + " details successfully");
            }

            entityManager.clear();

            ra.addFlashAttribute("successMess", isEdit ? "Cập nhật đợt giảm giá thành công!" : "Lưu đợt giảm giá thành công!");
            System.out.println("=== SAVE ALL SUCCESS ===");
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;

        } catch (Exception e) {
            System.out.println("=== SAVE ALL ERROR ===");
            e.printStackTrace();
            ra.addFlashAttribute("errorMess", "Lỗi khi lưu: " + e.getMessage());
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }
    }

    private String extractMaSanPham(String maSPCT) {
        if (maSPCT == null || maSPCT.isEmpty()) return null;

        // ⭐ NẾU CHỨA DẤU PHẨY, TÁCH VÀ LẤY PHẦN TỬ ĐẦU TIÊN
        if (maSPCT.contains(",")) {
            String[] parts = maSPCT.split(",");
            if (parts.length > 0) {
                maSPCT = parts[0].trim();
            }
        }

        // Cách 1: Lấy phần trước dấu "-" đầu tiên
        // Ví dụ: SP7555-MS5-43 -> SP7555
        int firstDash = maSPCT.indexOf("-");
        if (firstDash > 0) {
            return maSPCT.substring(0, firstDash);
        }

        // Cách 2: Nếu không có dấu "-", thử tìm theo pattern
        int firstUnderscore = maSPCT.indexOf("_");
        if (firstUnderscore > 0) {
            return maSPCT.substring(0, firstUnderscore);
        }

        // Cách 3: Nếu là số (ID), tìm sản phẩm từ database
        try {
            Integer id = Integer.parseInt(maSPCT);
            SanPhamChiTiet spct = sanPhamChiTietService.getById(String.valueOf(id));
            if (spct != null && spct.getSanPham() != null) {
                return spct.getSanPham().getMaSanPham();
            }
        } catch (NumberFormatException e) {
            // Không phải số, bỏ qua
        }

        // Không tìm thấy
        System.err.println("❌ Không thể extract mã sản phẩm từ: " + maSPCT);
        return null;
    }

    @PostMapping("/dot-giam-gia/update")
    public String update(@Valid @ModelAttribute("dggDTO") DotGiamGiaDTO dggDTO,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra,
                         @RequestParam(defaultValue = "0") int page) {

        System.out.println("=== UPDATE START ===");
        System.out.println("dggDTO: " + dggDTO);
        System.out.println("page: " + page);

        if (dggDTO == null || dggDTO.getDotGiamGia() == null) {
            ra.addFlashAttribute("errorMess", "Dữ liệu không hợp lệ!");
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;
        }

        DotGiamGia dgg = dggDTO.getDotGiamGia();

        String maGiamGia = dgg.getMaGiamGia();
        if (maGiamGia != null && maGiamGia.contains(",")) {
            maGiamGia = maGiamGia.split(",")[0].trim();
            dgg.setMaGiamGia(maGiamGia);
            System.out.println("Fixed maGiamGia: " + maGiamGia);
        }

        if (result.hasErrors()) {
            System.out.println("Errors: " + result.getAllErrors());
            ra.addFlashAttribute("errorMess", "Dữ liệu không hợp lệ: " + result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/dot-giam-gia/edit/" + dgg.getMaGiamGia() + "?page=" + page;
        }

        try {
            if (!dotGiamGiaRepo.existsById(dgg.getMaGiamGia())) {
                ra.addFlashAttribute("errorMess", "Không tìm thấy đợt giảm giá!");
                return "redirect:/dot-giam-gia/hien-thi?page=" + page;
            }

            DotGiamGia existing = dotGiamGiaService.getById(dgg.getMaGiamGia());
            if (existing == null) {
                ra.addFlashAttribute("errorMess", "Không tìm thấy đợt giảm giá!");
                return "redirect:/dot-giam-gia/hien-thi?page=" + page;
            }

            dgg.setNgayTao(existing.getNgayTao());
            dgg.setTrangThai(existing.getTrangThai());

            dotGiamGiaService.save(dgg);
            entityManager.clear();
            ra.addFlashAttribute("successMess", "Cập nhật đợt giảm giá thành công!");
            System.out.println("=== UPDATE END ===");
            return "redirect:/dot-giam-gia/hien-thi?page=" + page;

        } catch (Exception e) {
            System.out.println("=== UPDATE ERROR ===");
            e.printStackTrace();
            ra.addFlashAttribute("errorMess", "Lỗi khi cập nhật: " + e.getMessage());
            return "redirect:/dot-giam-gia/edit/" + dgg.getMaGiamGia() + "?page=" + page;
        }
    }

    // ===== API LẤY DANH SÁCH BIẾN THỂ =====
    @GetMapping("/api/get-bien-the-list")
    @ResponseBody
    public List<SanPhamChiTietDTOgg> getBienTheList(@RequestParam List<String> listMaSanPham) {
        System.out.println("=== API GET BIEN THE LIST START ===");
        System.out.println("listMaSanPham: " + listMaSanPham);

        if (listMaSanPham == null || listMaSanPham.isEmpty()) {
            System.out.println("listMaSanPham is empty");
            return new ArrayList<>();
        }

        try {
            List<String> maSanPhamList = new ArrayList<>();
            for (String id : listMaSanPham) {
                if (id != null && !id.trim().isEmpty()) {
                    maSanPhamList.add(id.trim());
                }
            }

            if (maSanPhamList.isEmpty()) {
                System.out.println("maSanPhamList is empty");
                return new ArrayList<>();
            }

            List<SanPhamChiTiet> listEntity = sanPhamChiTietService.findsp(maSanPhamList);
            System.out.println("Found " + listEntity.size() + " entities");

            List<SanPhamChiTietDTOgg> result = listEntity.stream().map(e -> {
                SanPhamChiTietDTOgg dto = new SanPhamChiTietDTOgg();
                dto.setMaSanPhamChiTiet(e.getMaSanPhamChiTiet());
                dto.setTenSanPham(e.getSanPham() != null ? e.getSanPham().getTenSanPham() : "");
                dto.setTenKichThuoc(e.getKichThuoc() != null ? e.getKichThuoc().getTenKichThuoc() : "");
                dto.setTenMauSac(e.getMauSac() != null ? e.getMauSac().getTenMauSac() : "");
                dto.setGiaBan(e.getGiaBan());
                dto.setSoLuongTon(e.getSoLuongTon());
                dto.setTrangThai(e.getTrangThai());
                dto.setDuongDanAnh(e.getDuongDanAnh());
                dto.setMaMauSac(e.getMauSac() != null ? e.getMauSac().getMaMauSac() : null);
                dto.setMaKichThuoc(e.getKichThuoc() != null ? e.getKichThuoc().getMaKichThuoc() : null);
                return dto;
            }).collect(Collectors.toList());

            System.out.println("Returning " + result.size() + " items");
            System.out.println("=== API GET BIEN THE LIST END ===");
            return result;

        } catch (Exception e) {
            System.out.println("=== API GET BIEN THE LIST ERROR ===");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @GetMapping("/api/get-selected-spct")
    @ResponseBody
    public List<String> getSelectedSPCT(@RequestParam String maGiamGia) {
        System.out.println("=== GET SELECTED SPCT ===");
        System.out.println("maGiamGia: " + maGiamGia);

        try {
            List<String> listSPCT = chiTietDotGiamGiaService.getSanPhamChiTietByDot(maGiamGia);
            System.out.println("Found " + (listSPCT != null ? listSPCT.size() : 0) + " items");
            return listSPCT != null ? listSPCT : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @GetMapping("/dot-giam-gia/export-excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime denNgay) {

        System.out.println("=== EXPORT EXCEL START ===");

        try {
            // Lấy danh sách đợt giảm giá theo filter (không phân trang)
            List<DotGiamGia> listDGG;

            // Nếu có filter, sử dụng filterPaging với pageable không giới hạn
            if (keyword != null || trangThai != null || tuNgay != null || denNgay != null) {
                Pageable unlimited = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "ngayTao"));
                Page<DotGiamGia> page = dotGiamGiaService.filterPaging(keyword, trangThai, tuNgay, denNgay, unlimited);
                listDGG = page.getContent();
            } else {
                // Lấy tất cả
                listDGG = dotGiamGiaService.getAll();
            }

            // Xuất Excel
            ByteArrayInputStream excelStream = excelExportService.exportDotGiamGiaToExcel(listDGG);

            if (excelStream == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // Tạo tên file
            String filename = "Danh_sach_dot_giam_gia_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + filename);
            headers.add("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            System.out.println("=== EXPORT EXCEL END ===");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(excelStream.readAllBytes());

        } catch (Exception e) {
            System.out.println("=== EXPORT EXCEL ERROR ===");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}