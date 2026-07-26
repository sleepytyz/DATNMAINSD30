package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.*;
import com.example.th06876_java202.Repository.SanPhamChiTietRepository;
import com.example.th06876_java202.Service.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/sanphamct")
public class SanPhamChiTietController {

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private ChiTietDotGiamGiaService chiTietDotGiamGiaService;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private ImageService imageService;

    private final SanPhamChiTietService sanPhamChiTietService;
    private final DanhMucSanPhamService danhMucSanPhamService;
    private final SanPhamService sanPhamService;
    private final MauSacService mauSacService;
    private final KichThuocService kichThuocService;

    public SanPhamChiTietController(SanPhamChiTietService sanPhamChiTietService,
                                    DanhMucSanPhamService danhMucSanPhamService,
                                    SanPhamService sanPhamService,
                                    MauSacService mauSacService,
                                    KichThuocService kichThuocService) {
        this.sanPhamChiTietService = sanPhamChiTietService;
        this.danhMucSanPhamService = danhMucSanPhamService;
        this.sanPhamService = sanPhamService;
        this.mauSacService = mauSacService;
        this.kichThuocService = kichThuocService;
    }

    private BigDecimal getMaxDiscountForVariant(String maSanPhamChiTiet) {
        try {
            List<ChiTietDotGiamGia> list = chiTietDotGiamGiaService.findBySanPhamChiTiet_MaSanPhamChiTiet(maSanPhamChiTiet);
            if (list == null || list.isEmpty()) {
                return BigDecimal.ZERO;
            }

            BigDecimal maxDiscount = BigDecimal.ZERO;
            LocalDate today = LocalDate.now();

            for (ChiTietDotGiamGia ct : list) {
                DotGiamGia dgg = ct.getDotGiamGia();
                if (dgg == null) continue;
                if (!"Hoạt động".equals(dgg.getTrangThai())) continue;

                if (dgg.getNgayBatDau() != null && dgg.getNgayKetThuc() != null) {
                    if (today.isBefore(dgg.getNgayBatDau()) || today.isAfter(dgg.getNgayKetThuc())) {
                        continue;
                    }
                }

                if (ct.getSanPhamChiTiet() != null &&
                        maSanPhamChiTiet.equals(ct.getSanPhamChiTiet().getMaSanPhamChiTiet())) {
                    BigDecimal giaTriGiam = dgg.getGiaTriGiam() != null ? dgg.getGiaTriGiam() : BigDecimal.ZERO;
                    if (giaTriGiam.compareTo(maxDiscount) > 0) {
                        maxDiscount = giaTriGiam;
                    }
                }
            }
            return maxDiscount;
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy giảm giá cho biến thể " + maSanPhamChiTiet + ": " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculatePriceAfterDiscount(BigDecimal giaBan, BigDecimal discountPercent) {
        if (giaBan == null || discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            return giaBan;
        }
        BigDecimal discountAmount = giaBan.multiply(discountPercent).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        return giaBan.subtract(discountAmount);
    }

    @GetMapping("/index")
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String msac,
            @RequestParam(required = false) String tt,
            @RequestParam(required = false) BigDecimal gia,
            @RequestParam(required = false) BigDecimal gia2,
            @RequestParam(required = false) String tonKho,
            Model model) {

        Pageable pageable = PageRequest.of(page, 5, Sort.by("maSanPhamChiTiet").descending());
        Page<SanPhamChiTiet> pageResult = sanPhamChiTietService.findAllWithFilters(
                size, msac, tt, gia, gia2, tonKho, pageable);

        List<SanPhamChiTietDTOWithDiscount> listWithDiscount = new ArrayList<>();

        Map<String, Integer> soLuongAnhPhuMap = new HashMap<>();

        for (SanPhamChiTiet spct : pageResult.getContent()) {
            SanPhamChiTietDTOWithDiscount dto = new SanPhamChiTietDTOWithDiscount();
            dto.setSanPhamChiTiet(spct);
            BigDecimal maxDiscount = getMaxDiscountForVariant(spct.getMaSanPhamChiTiet());
            dto.setMaxDiscount(maxDiscount);
            dto.setPriceAfterDiscount(calculatePriceAfterDiscount(spct.getGiaBan(), maxDiscount));
            dto.setHasDiscount(maxDiscount.compareTo(BigDecimal.ZERO) > 0);
            listWithDiscount.add(dto);

            int subCount = 0;
            try {
                List<String> images = spct.getDanhSachAnhList();
                if (images != null && !images.isEmpty()) {
                    String mainImage = spct.getDuongDanAnh();
                    if (mainImage != null && !mainImage.isEmpty() && images.contains(mainImage)) {
                        subCount = images.size() - 1;
                    } else {
                        subCount = images.size();
                    }
                }
            } catch (Exception e) {
                subCount = 0;
            }
            soLuongAnhPhuMap.put(spct.getMaSanPhamChiTiet(), subCount);
        }

        model.addAttribute("listspct", pageResult.getContent());
        model.addAttribute("listWithDiscount", listWithDiscount);
        model.addAttribute("currentPage", pageResult.getNumber());
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("totalItems", pageResult.getTotalElements());
        model.addAttribute("selectedSize", size);
        model.addAttribute("selectedMauSac", msac);
        model.addAttribute("selectedStatus", tt);
        model.addAttribute("selectedGia", gia);
        model.addAttribute("selectedGia2", gia2);
        model.addAttribute("selectedTonKho", tonKho);
        model.addAttribute("listms", mauSacService.findAll());
        model.addAttribute("lists", kichThuocService.getall());

        model.addAttribute("soLuongAnhPhuMap", soLuongAnhPhuMap);

        Double maxGia = sanPhamChiTietService.gia();
        model.addAttribute("maxGiaBan", maxGia != null ? maxGia : 1000000000);

        model.addAttribute("sanphamct", new SanPhamChiTiet());

        return "sanphamct/index";
    }

    @GetMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable("id") String id,
                               @RequestParam("status") String status,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(required = false) String size,
                               @RequestParam(required = false) String msac,
                               @RequestParam(required = false) String tt,
                               @RequestParam(required = false) String gia,
                               @RequestParam(required = false) String gia2,
                               @RequestParam(required = false) String tonKho,
                               RedirectAttributes redirectAttributes) {

        SanPhamChiTiet spct = sanPhamChiTietService.findbyId(id).orElseThrow();

        if (Boolean.FALSE.equals(spct.getSanPham().getTrangThai())) {
            redirectAttributes.addFlashAttribute("errorMess", "Không thể thay đổi trạng thái biến thể khi sản phẩm cha đang ngừng bán!");
            return buildRedirectUrl(page, size, msac, tt, gia, gia2, tonKho);
        }

        if ("Ngừng bán".equals(status)) {
            spct.setTrangThai("Ngừng bán");
        } else {
            spct.setTrangThai("Còn hàng");
            sanPhamChiTietService.capNhatTrangThaii(spct);
        }

        sanPhamChiTietService.them(spct);
        return buildRedirectUrl(page, size, msac, tt, gia, gia2, tonKho);
    }

    @PostMapping("/api/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleStatusApi(
            @PathVariable String id,
            @RequestParam boolean active) {

        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("=== TOGGLE STATUS API ===");
            System.out.println("ID: " + id);
            System.out.println("Active: " + active);

            Optional<SanPhamChiTiet> spctOpt = sanPhamChiTietService.findbyId(id);
            if (spctOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy biến thể!");
                return ResponseEntity.badRequest().body(response);
            }

            SanPhamChiTiet spct = spctOpt.get();

            // Kiểm tra nếu sản phẩm cha đã ngừng bán thì không cho phép thay đổi biến thể
            if (spct.getSanPham() != null && !spct.getSanPham().getTrangThai()) {
                response.put("success", false);
                response.put("message", "Sản phẩm cha đang ngừng bán, không thể thay đổi!");
                return ResponseEntity.badRequest().body(response);
            }

            // --- PHẦN LOGIC ĐÃ SỬA ---
            if (active) {
                // Khi bật (active = true):
                // Kiểm tra tồn kho để set trạng thái phù hợp thay vì gọi hàm capNhatTrangThaii
                if (spct.getSoLuongTon() == null || spct.getSoLuongTon() <= 0) {
                    spct.setTrangThai("Hết hàng");
                } else if (spct.getSoLuongTon() < 10) {
                    spct.setTrangThai("Sắp hết");
                } else {
                    spct.setTrangThai("Còn hàng");
                }
            } else {
                // Khi tắt (active = false):
                spct.setTrangThai("Ngừng bán");
            }
            // -------------------------

            // Lưu thay đổi
            SanPhamChiTiet saved = sanPhamChiTietService.them(spct);

            response.put("success", true);
            response.put("message", active ? "Đã bật sản phẩm!" : "Đã tắt sản phẩm!");
            response.put("trangThai", saved.getTrangThai());
            response.put("soLuongTon", saved.getSoLuongTon());
            response.put("maBienThe", saved.getMaSanPhamChiTiet());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") String id,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(required = false) String size,
                       @RequestParam(required = false) String msac,
                       @RequestParam(required = false) String tt,
                       @RequestParam(required = false) String gia,
                       @RequestParam(required = false) String gia2,
                       @RequestParam(required = false) String tonKho,
                       Model model) {

        System.out.println("=== EDIT ===");
        System.out.println("ID: " + id);

        Pageable pageable = PageRequest.of(page, 5, Sort.by("maSanPhamChiTiet").descending());
        Page<SanPhamChiTiet> p = sanPhamChiTietService.getall(pageable);
        model.addAttribute("listspct", p.getContent());
        setupPageModel(model, p, null, null);
        SanPhamChiTiet sanPhamChiTiet = sanPhamChiTietService.findbyIid(id).orElse(null);

        if (sanPhamChiTiet != null) {
            System.out.println("SanPhamChiTiet found: " + sanPhamChiTiet.getMaSanPhamChiTiet());
            if (sanPhamChiTiet.getSanPham() != null) {
                System.out.println("SanPham: " + sanPhamChiTiet.getSanPham().getMaSanPham() + " - " + sanPhamChiTiet.getSanPham().getTenSanPham());
            } else {
                System.out.println("SanPham is NULL!");
                if (sanPhamChiTiet.getSanPham() == null) {
                    String maSanPham = sanPhamChiTiet.getSanPham() != null ? sanPhamChiTiet.getSanPham().getMaSanPham() : null;
                    if (maSanPham != null) {
                        sanPhamService.findById(maSanPham).ifPresent(sanPhamChiTiet::setSanPham);
                    }
                }
            }
        }

        model.addAttribute("sanphamct", sanPhamChiTiet);
        model.addAttribute("listsp", sanPhamService.getAll());
        model.addAttribute("listms", mauSacService.findAll());
        model.addAttribute("lists", kichThuocService.getall());

        model.addAttribute("selectedSize", size);
        model.addAttribute("selectedMauSac", msac);
        model.addAttribute("selectedStatus", tt);
        model.addAttribute("selectedGia", gia);
        model.addAttribute("selectedGia2", gia2);
        model.addAttribute("selectedTonKho", tonKho);

        model.addAttribute("showModal", true);
        model.addAttribute("isEdit", true);
        Double maxGia = sanPhamChiTietService.gia();
        model.addAttribute("maxGiaBan", maxGia != null ? maxGia : 1000000000);

        return "sanphamct/index";
    }

    @PostMapping("/update-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateAjax(
            @RequestParam("maSanPhamChiTiet") String maSanPhamChiTiet,
            @RequestParam("sanPham.maSanPham") String maSanPham,
            @RequestParam("kichThuoc.maKichThuoc") String maKichThuoc,
            @RequestParam("mauSac.maMauSac") String maMauSac,
            @RequestParam("giaBan") BigDecimal giaBan,
            @RequestParam("soLuongTon") Integer soLuongTon,
            @RequestParam(value = "duongDanAnh", required = false) String duongDanAnh,
            @RequestParam(value = "danhSachAnh", required = false) String danhSachAnh,
            @RequestParam(value = "source", defaultValue = "detail") String source) {

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== UPDATE AJAX ===");
            System.out.println("MaSanPhamChiTiet: " + maSanPhamChiTiet);
            System.out.println("DuongDanAnh: " + duongDanAnh);
            System.out.println("DanhSachAnh: " + danhSachAnh);

            SanPhamChiTiet old = sanPhamChiTietService.findbyId(maSanPhamChiTiet)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể!"));

            if (duongDanAnh != null && !duongDanAnh.isEmpty()) {
                Path imagePath = Paths.get("D:/AnhSP/", duongDanAnh);
                if (Files.exists(imagePath) && Files.isRegularFile(imagePath)) {
                    old.setDuongDanAnh(duongDanAnh);
                    System.out.println("✅ Đã cập nhật ảnh chính: " + duongDanAnh);
                } else {
                    throw new RuntimeException("File ảnh chính không tồn tại: " + duongDanAnh);
                }
            }

            if (danhSachAnh != null && !danhSachAnh.isEmpty() && !danhSachAnh.equals("[]")) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    List<String> imageList = mapper.readValue(danhSachAnh, new TypeReference<List<String>>() {});

                    List<String> validImages = new ArrayList<>();
                    for (String img : imageList) {
                        Path imgPath = Paths.get("D:/AnhSP/", img);
                        if (Files.exists(imgPath) && Files.isRegularFile(imgPath)) {
                            validImages.add(img);
                        } else {
                            System.err.println(" Ảnh phụ không tồn tại: " + img);
                        }
                    }
                    old.setDanhSachAnhList(validImages);
                    System.out.println("✅ Đã cập nhật danh sách ảnh phụ: " + validImages.size() + " ảnh");
                } catch (Exception e) {
                    System.err.println("⚠️ Lỗi parse danh sách ảnh: " + e.getMessage());
                }
            } else {
                old.setDanhSachAnhList(new ArrayList<>());
                System.out.println("✅ Đã xóa danh sách ảnh phụ");
            }
            Optional<SanPham> sanPhamOpt = sanPhamService.findById(maSanPham);
            if (sanPhamOpt.isPresent()) {
                old.setSanPham(sanPhamOpt.get());
            } else {
                throw new RuntimeException("Không tìm thấy sản phẩm với mã: " + maSanPham);
            }
            Optional<KichThuoc> ktOpt = kichThuocService.getKichThuocById(maKichThuoc);
            if (ktOpt.isPresent()) {
                old.setKichThuoc(ktOpt.get());
            } else {
                throw new RuntimeException("Không tìm thấy kích thước với mã: " + maKichThuoc);
            }
            Optional<MauSac> msOpt = mauSacService.findById(maMauSac);
            if (msOpt.isPresent()) {
                old.setMauSac(msOpt.get());
            } else {
                throw new RuntimeException("Không tìm thấy màu sắc với mã: " + maMauSac);
            }
            if (giaBan != null && giaBan.compareTo(BigDecimal.ZERO) > 0) {
                old.setGiaBan(giaBan);
            }
            if (soLuongTon != null && soLuongTon >= 0) {
                old.setSoLuongTon(soLuongTon);
            }
            sanPhamChiTietService.capNhatTrangThaii(old);

            SanPhamChiTiet updated = sanPhamChiTietService.them(old);


            System.out.println("Update thành công! Biến thể: " + updated.getMaSanPhamChiTiet());
            response.put("success", true);
            response.put("message", "Cập nhật biến thể thành công!");
            response.put("maBienThe", updated.getMaSanPhamChiTiet());
            response.put("maKichThuoc", updated.getKichThuoc() != null ? updated.getKichThuoc().getMaKichThuoc() : "");
            response.put("tenKichThuoc", updated.getKichThuoc() != null ? updated.getKichThuoc().getTenKichThuoc() : "");
            response.put("maMauSac", updated.getMauSac() != null ? updated.getMauSac().getMaMauSac() : "");
            response.put("tenMauSac", updated.getMauSac() != null ? updated.getMauSac().getTenMauSac() : "");
            response.put("giaBan", updated.getGiaBan());
            response.put("soLuongTon", updated.getSoLuongTon());
            response.put("trangThai", updated.getTrangThai());
            response.put("duongDanAnh", updated.getDuongDanAnh() != null ? updated.getDuongDanAnh() : "");
            response.put("danhSachAnh", updated.getDanhSachAnhList());
            int subCount = 0;
            try {
                List<String> images = updated.getDanhSachAnhList();
                String mainImage = updated.getDuongDanAnh();
                if (images != null && !images.isEmpty()) {
                    if (mainImage != null && !mainImage.isEmpty() && images.contains(mainImage)) {
                        subCount = images.size() - 1;
                    } else {
                        subCount = images.size();
                    }
                }
            } catch (Exception e) {
                subCount = 0;
            }

            response.put("soLuongAnhPhu", subCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/update-main-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateMainImage(
            @RequestParam("maSanPhamChiTiet") String maSanPhamChiTiet,
            @RequestParam("duongDanAnh") String duongDanAnh) {

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== UPDATE MAIN IMAGE ===");
            System.out.println("MaSanPhamChiTiet: " + maSanPhamChiTiet);
            System.out.println("New Main Image: " + duongDanAnh);

            // Lấy biến thể
            SanPhamChiTiet spct = sanPhamChiTietService.findbyId(maSanPhamChiTiet)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể!"));

            Path imagePath = Paths.get("D:/AnhSP/", duongDanAnh);
            if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
                throw new RuntimeException("File ảnh không tồn tại trong folder: " + duongDanAnh);
            }
            List<String> images = spct.getDanhSachAnhList();
            if (!images.contains(duongDanAnh)) {
                images.add(0, duongDanAnh);
                spct.setDanhSachAnhList(images);
            }
            spct.setDuongDanAnh(duongDanAnh);

            // Lưu
            SanPhamChiTiet updated = sanPhamChiTietService.them(spct);

            response.put("success", true);
            response.put("message", "Đã đổi ảnh chính thành công!");
            response.put("maBienThe", updated.getMaSanPhamChiTiet());
            response.put("duongDanAnh", updated.getDuongDanAnh());
            response.put("danhSachAnh", updated.getDanhSachAnhList());
            response.put("soLuongAnhPhu", updated.getSoLuongAnhPhu());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private String buildRedirectUrl(int page, String size, String msac, String tt, String gia, String gia2, String tonKho) {
        StringBuilder url = new StringBuilder("redirect:/sanphamct/index?page=" + page);
        if (size != null && !size.isEmpty()) url.append("&size=").append(size);
        if (msac != null && !msac.isEmpty()) url.append("&msac=").append(msac);
        if (tt != null && !tt.isEmpty()) url.append("&tt=").append(tt);
        if (gia != null && !gia.isEmpty()) url.append("&gia=").append(gia);
        if (gia2 != null && !gia2.isEmpty()) url.append("&gia2=").append(gia2);
        if (tonKho != null && !tonKho.isEmpty()) url.append("&tonKho=").append(tonKho);
        return url.toString();
    }

    private void prepareModel(Model model) {
        model.addAttribute("listsp", sanPhamService.getAll());
        model.addAttribute("listdmsp", danhMucSanPhamService.getAll());
        model.addAttribute("listms", mauSacService.findAll());
        model.addAttribute("lists", kichThuocService.getall());
    }

    private void setupPageModel(Model model, Page<SanPhamChiTiet> page, String attrName, Object attrValue) {
        model.addAttribute("currentPage", page.getNumber());
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        if (attrName != null) model.addAttribute(attrName, attrValue);
        prepareModel(model);
        model.addAttribute("sanphamct", new SanPhamChiTiet());
    }

    @GetMapping("/generate-qr/{maBienThe}")
    public ResponseEntity<?> generateQR(@PathVariable("maBienThe") String maBienThe) {
        try {
            SanPhamChiTiet spct = sanPhamChiTietService.findbyId(maBienThe)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể!"));

            String qrPath = qrCodeService.generateVariantQRCode(maBienThe);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tạo QR Code thành công!");
            response.put("qrPath", qrPath);
            response.put("maBienThe", maBienThe);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/download-qr/{maBienThe}")
    public ResponseEntity<?> downloadQR(@PathVariable("maBienThe") String maBienThe) {
        try {
            SanPhamChiTiet spct = sanPhamChiTietService.findbyId(maBienThe)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể!"));

            String fileName = "QR_" + maBienThe + ".png";
            Path savePath = Paths.get("D:\\QRSanPham", fileName);

            if (Files.exists(savePath)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("duplicate", true);
                response.put("message", "Mã QR của biến thể '" + maBienThe + "' đã tồn tại!");
                response.put("filePath", savePath.toString());
                return ResponseEntity.ok(response);
            }

            String qrContent = "https://fsshop.com/sanpham/detail/" + maBienThe;
            byte[] qrBytes = qrCodeService.generateQRCodeAsBytes(qrContent);

            Files.createDirectories(savePath.getParent());
            Files.write(savePath, qrBytes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("duplicate", false);
            response.put("message", "QR Code đã được lưu vào D:\\QRSanPham\\" + fileName);
            response.put("filePath", savePath.toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/download-qr-batch")
    public ResponseEntity<?> downloadQRBatch(@RequestBody List<String> maBienTheList) {
        try {
            List<String> savedFiles = new ArrayList<>();
            List<String> duplicateFiles = new ArrayList<>();
            List<String> notFoundFiles = new ArrayList<>();

            for (String maBienThe : maBienTheList) {
                SanPhamChiTiet spct = sanPhamChiTietService.findbyId(maBienThe).orElse(null);
                if (spct == null) {
                    notFoundFiles.add(maBienThe);
                    continue;
                }

                String fileName = "QR_" + maBienThe + ".png";
                Path savePath = Paths.get("D:\\QRSanPham", fileName);

                if (Files.exists(savePath)) {
                    duplicateFiles.add(maBienThe);
                    continue;
                }

                String qrContent = "https://fsshop.com/sanpham/detail/" + maBienThe;
                byte[] qrBytes = qrCodeService.generateQRCodeAsBytes(qrContent);

                Files.createDirectories(savePath.getParent());
                Files.write(savePath, qrBytes);
                savedFiles.add(maBienThe);
            }

            String message = "";
            if (!savedFiles.isEmpty()) {
                message += "Đã lưu " + savedFiles.size() + " QR Code mới. ";
            }
            if (!duplicateFiles.isEmpty()) {
                message += "QR Code đã tồn tại cho " + duplicateFiles.size() + " biến thể. ";
            }
            if (!notFoundFiles.isEmpty()) {
                message += "Không tìm thấy " + notFoundFiles.size() + " biến thể. ";
            }
            if (message.isEmpty()) {
                message = "Không có biến thể nào được chọn!";
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", !savedFiles.isEmpty() || !duplicateFiles.isEmpty());
            response.put("message", message.trim());
            response.put("saved", savedFiles);
            response.put("duplicate", duplicateFiles);
            response.put("notFound", notFoundFiles);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/generate-qr-product/{maSanPham}")
    public ResponseEntity<?> generateQRForProduct(@PathVariable("maSanPham") String maSanPham) {
        try {
            List<SanPhamChiTiet> variants = sanPhamChiTietService.getallsp(maSanPham);
            if (variants.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Sản phẩm không có biến thể nào!");
                return ResponseEntity.badRequest().body(response);
            }

            List<Map<String, String>> qrResults = new ArrayList<>();
            for (SanPhamChiTiet variant : variants) {
                String qrPath = qrCodeService.generateVariantQRCode(variant.getMaSanPhamChiTiet());
                Map<String, String> result = new HashMap<>();
                result.put("maBienThe", variant.getMaSanPhamChiTiet());
                result.put("qrPath", qrPath);
                qrResults.add(result);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tạo QR Code cho " + qrResults.size() + " biến thể thành công!");
            response.put("data", qrResults);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/export-excel")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String msac,
            @RequestParam(required = false) String tt,
            @RequestParam(required = false) BigDecimal gia,
            @RequestParam(required = false) BigDecimal gia2,
            @RequestParam(required = false) String tonKho) {

        try {
            System.out.println("========== EXPORT EXCEL ==========");

            List<SanPhamChiTiet> list = sanPhamChiTietService.findAllWithFilters(size, msac, tt, gia, gia2, tonKho);

            if (list == null || list.isEmpty()) {
                System.out.println("⚠️ Không có dữ liệu để xuất!");
                return ResponseEntity.badRequest().build();
            }

            System.out.println("✅ Số lượng bản ghi tìm thấy: " + list.size());

            ByteArrayInputStream in = excelExportService.exportSanPhamChiTietToExcel(list);

            if (in == null) {
                System.err.println("❌ InputStream bị null!");
                return ResponseEntity.badRequest().build();
            }

            byte[] excelBytes = readAllBytes(in);

            if (excelBytes == null || excelBytes.length == 0) {
                System.err.println("❌ Dữ liệu Excel rỗng!");
                return ResponseEntity.badRequest().build();
            }

            System.out.println("📊 Dung lượng file: " + excelBytes.length + " bytes");

            String fileName = "Danh_sach_bien_the_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + fileName);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(excelBytes.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(new ByteArrayInputStream(excelBytes)));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi export Excel: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    private byte[] readAllBytes(ByteArrayInputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllProducts() {
        List<SanPhamChiTiet> products = sanPhamChiTietRepository.findAll();

        List<Map<String, Object>> result = products.stream()
                .map(sp -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("maSanPhamChiTiet", sp.getMaSanPhamChiTiet());
                    map.put("tenSanPham", sp.getSanPham().getTenSanPham());
                    map.put("giaBan", sp.getGiaBan());
                    map.put("soLuongTon", sp.getSoLuongTon());
                    map.put("mauSac", sp.getMauSac().getTenMauSac());
                    map.put("kichThuoc", sp.getKichThuoc().getTenKichThuoc());

                    BigDecimal giaSauGiam = sp.getGiaBan();
                    map.put("giaSauGiam", giaSauGiam);

                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}