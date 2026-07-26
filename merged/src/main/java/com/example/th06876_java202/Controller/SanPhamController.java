package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.*;
import com.example.th06876_java202.Repository.SanPhamRepository;
import com.example.th06876_java202.Service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/sanpham")
public class SanPhamController {

    @Autowired
    private ThuongHieuService thuongHieuService;

    @Autowired
    SanPhamRepository sanPhamRepository;

    @Autowired
    private KieuGiayService kieuGiayService;

    @Autowired
    private MauSacService mauSacService;

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private KichThuocService kichThuocService;

    @Autowired
    private ImageService imageService;

    private final DanhMucSanPhamService danhMucSanPhamService;
    private final SanPhamService sanPhamService;
    private final ChatLieuService chatLieuService;
    private final SanPhamChiTietService sanPhamChiTietService;

    public SanPhamController(DanhMucSanPhamService danhMucSanPhamService, SanPhamService sanPhamService, ChatLieuService chatLieuService,
                             SanPhamChiTietService sanPhamChiTietService) {
        this.danhMucSanPhamService = danhMucSanPhamService;
        this.sanPhamService = sanPhamService;
        this.chatLieuService = chatLieuService;
        this.sanPhamChiTietService = sanPhamChiTietService;
    }

    @GetMapping("/index")
    public String index(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) String maDanhMuc,
                        @RequestParam(required = false) Boolean tt,
                        @RequestParam(required = false) String maTH,
                        @RequestParam(required = false) String maKG,
                        @RequestParam(required = false) String t,
                        Model model) {

        // Chuyển chuỗi rỗng thành null
        if (maDanhMuc != null && maDanhMuc.trim().isEmpty()) {
            maDanhMuc = null;
        }
        if (maTH != null && maTH.trim().isEmpty()) {
            maTH = null;
        }
        if (maKG != null && maKG.trim().isEmpty()) {
            maKG = null;
        }
        if (t != null && t.trim().isEmpty()) {
            t = null;
        }

        System.out.println("=== FILTER PARAMS (AFTER CLEAN) ===");
        System.out.println("maDanhMuc: '" + maDanhMuc + "'");
        System.out.println("tt: " + tt);
        System.out.println("maTH: '" + maTH + "'");
        System.out.println("maKG: '" + maKG + "'");
        System.out.println("t: '" + t + "'");

        // ===== THÊM SORT GIẢM DẦN THEO NGÀY TẠO =====
        Page<SanPham> pageSanPham = sanPhamService.searchSanPham(
                maDanhMuc,
                tt,
                maTH,
                maKG,
                t,
                PageRequest.of(page, 5)
        );

        System.out.println("Total elements: " + pageSanPham.getTotalElements());
        System.out.println("Content size: " + pageSanPham.getContent().size());

        // In ra danh sách sản phẩm tìm được
        for (SanPham sp : pageSanPham.getContent()) {
            System.out.println("Found: " + sp.getMaSanPham() + " - " + sp.getTenSanPham() + " - TH: " +
                    (sp.getThuongHieu() != null ? sp.getThuongHieu().getMaThuongHieu() : "NULL"));
        }

        Optional<SanPham> sp0020 = sanPhamService.findById("SP0020");
        if (sp0020.isPresent()) {
            SanPham sp = sp0020.get();
            System.out.println("=== SP0020 ===");
            System.out.println("MaSanPham: " + sp.getMaSanPham());
            System.out.println("TenSanPham: " + sp.getTenSanPham());
            System.out.println("TrangThai: " + sp.getTrangThai());
            System.out.println("MaThuongHieu: " + (sp.getThuongHieu() != null ? sp.getThuongHieu().getMaThuongHieu() : "NULL"));
            System.out.println("DanhMuc: " + (sp.getDanhMucSanPham() != null ? sp.getDanhMucSanPham().getMaDanhMuc() : "NULL"));
            System.out.println("KieuGiay: " + (sp.getKieuGiay() != null ? sp.getKieuGiay().getMaKieuGiay() : "NULL"));
        } else {
            System.out.println("=== SP0020 NOT FOUND ===");
        }

        // ===== CHUYỂN ĐỔI SANG DTO VỚI THÔNG TIN GIÁ =====
        List<SanPhamDTO> sanPhamDTOList = new ArrayList<>();
        for (SanPham sp : pageSanPham.getContent()) {
            SanPhamDTO dto = new SanPhamDTO();
            dto.setMaSanPham(sp.getMaSanPham());
            dto.setTenSanPham(sp.getTenSanPham());
            dto.setMoTa(sp.getMoTa());
            dto.setTrangThai(sp.getTrangThai());
            dto.setMaDanhMuc(sp.getDanhMucSanPham() != null ? sp.getDanhMucSanPham().getMaDanhMuc() : null);
            dto.setMaThuongHieu(sp.getThuongHieu() != null ? sp.getThuongHieu().getMaThuongHieu() : null);
            dto.setMaKieuGiay(sp.getKieuGiay() != null ? sp.getKieuGiay().getMaKieuGiay() : null);
            dto.setMaChatLieu(sp.getChatLieu() != null ? sp.getChatLieu().getMaChatLieu() : null);

            // ===== LẤY TÊN THƯƠNG HIỆU =====
            if (sp.getThuongHieu() != null) {
                dto.setTenThuongHieu(sp.getThuongHieu().getTenThuongHieu());
            } else {
                dto.setTenThuongHieu("");
            }

            // Lấy tổng tồn kho
            dto.setTongTon(sp.getTongTon());

            // Lấy khoảng giá
            String maSanPham = sp.getMaSanPham();
            BigDecimal minPrice = sanPhamChiTietService.getGiaMin(maSanPham);
            BigDecimal maxPrice = sanPhamChiTietService.getGiaMax(maSanPham);

            dto.setGiaMin(minPrice);
            dto.setGiaMax(maxPrice);

            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            if (minPrice.compareTo(maxPrice) == 0) {
                dto.setGiaBanDisplay(formatter.format(minPrice) + "₫");
            } else {
                dto.setGiaBanDisplay(formatter.format(minPrice) + "₫ - " + formatter.format(maxPrice) + "₫");
            }

            sanPhamDTOList.add(dto);
        }

        model.addAttribute("listsp", sanPhamDTOList);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageSanPham.getTotalPages());
        model.addAttribute("totalItems", pageSanPham.getTotalElements());

        // Thống kê
        model.addAttribute("totalActive", sanPhamService.countByTrangThai(true));
        model.addAttribute("totalInactive", sanPhamService.countByTrangThai(false));

        // Các danh sách cho filter
        model.addAttribute("listdmsp", danhMucSanPhamService.getAll());
        model.addAttribute("listcl", chatLieuService.findAll());
        model.addAttribute("listkg", kieuGiayService.findAll());
        model.addAttribute("listth", thuongHieuService.findAll());

        return "sanpham/index";
    }

    @GetMapping("/export-excel")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam(required = false) String maDanhMuc,
            @RequestParam(required = false) Boolean tt,
            @RequestParam(required = false) String maTH,
            @RequestParam(required = false) String maKG,
            @RequestParam(required = false) String t) {

        try {
            // Clean parameters
            maDanhMuc = (maDanhMuc != null && !maDanhMuc.trim().isEmpty()) ? maDanhMuc : null;
            maTH = (maTH != null && !maTH.trim().isEmpty()) ? maTH : null;
            maKG = (maKG != null && !maKG.trim().isEmpty()) ? maKG : null;
            t = (t != null && !t.trim().isEmpty()) ? t : null;

            System.out.println("=== EXPORT EXCEL ===");
            System.out.println("maDanhMuc: " + maDanhMuc);
            System.out.println("tt: " + tt);
            System.out.println("maTH: " + maTH);
            System.out.println("maKG: " + maKG);
            System.out.println("t: " + t);

            // Lấy dữ liệu
            List<SanPham> sanPhamList = sanPhamService.findAllWithFilters(maDanhMuc, tt, maTH, maKG, t);

            if (sanPhamList == null || sanPhamList.isEmpty()) {
                System.out.println("⚠️ Không có dữ liệu để xuất!");
                return ResponseEntity.badRequest().build();
            }

            // Chuyển đổi sang DTO
            List<SanPhamDTO> sanPhamDTOList = convertToDTO(sanPhamList);

            if (sanPhamDTOList == null || sanPhamDTOList.isEmpty()) {
                System.out.println("⚠️ Không có DTO nào sau chuyển đổi!");
                return ResponseEntity.badRequest().build();
            }

            // Xuất Excel
            ByteArrayInputStream in = excelExportService.exportSanPhamToExcel(sanPhamDTOList);

            if (in == null) {
                System.err.println("❌ Lỗi: exportSanPhamToExcel trả về null");
                return ResponseEntity.badRequest().build();
            }

            // Tạo tên file
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "Danh_sach_san_pham_" + timestamp + ".xlsx";

            // Encoding tên file cho tiếng Việt
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            System.out.println("✅ Xuất Excel thành công! File: " + fileName);
            System.out.println("📊 Dung lượng: " + in.available() + " bytes");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(in));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }


    private List<SanPhamDTO> convertToDTO(List<SanPham> sanPhamList) {
        List<SanPhamDTO> dtoList = new ArrayList<>();
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        System.out.println("=== convertToDTO ===");
        System.out.println("Số lượng sản phẩm đầu vào: " + (sanPhamList != null ? sanPhamList.size() : 0));

        if (sanPhamList == null || sanPhamList.isEmpty()) {
            System.out.println("⚠️ Danh sách sản phẩm rỗng!");
            return dtoList;
        }

        int count = 0;
        for (SanPham sp : sanPhamList) {
            try {
                count++;
                SanPhamDTO dto = new SanPhamDTO();

                // ===== 1. THÔNG TIN CƠ BẢN =====
                dto.setMaSanPham(sp.getMaSanPham() != null ? sp.getMaSanPham() : "");
                dto.setTenSanPham(sp.getTenSanPham() != null ? sp.getTenSanPham() : "");
                dto.setMoTa(sp.getMoTa() != null ? sp.getMoTa() : "");
                dto.setTrangThai(sp.getTrangThai() != null ? sp.getTrangThai() : false);

                // Trạng thái hiển thị
                dto.setTrangThaiDisplay(sp.getTrangThai() != null && sp.getTrangThai() ? "Còn bán" : "Ngừng bán");

                // Ngày tạo
                if (sp.getNgayTao() != null) {
                    dto.setNgayTao(sp.getNgayTao());
                    dto.setNgayTaoDisplay(sp.getNgayTao().format(dateFormatter));
                } else {
                    dto.setNgayTaoDisplay("");
                }

                // ===== 2. THÔNG TIN DANH MỤC =====
                if (sp.getDanhMucSanPham() != null) {
                    dto.setMaDanhMuc(sp.getDanhMucSanPham().getMaDanhMuc());
                    dto.setTenDanhMuc(sp.getDanhMucSanPham().getTenDanhMuc() != null ?
                            sp.getDanhMucSanPham().getTenDanhMuc() : "");
                } else {
                    dto.setTenDanhMuc("");
                }

                // ===== 3. THÔNG TIN THƯƠNG HIỆU =====
                if (sp.getThuongHieu() != null) {
                    dto.setMaThuongHieu(sp.getThuongHieu().getMaThuongHieu());
                    dto.setTenThuongHieu(sp.getThuongHieu().getTenThuongHieu() != null ?
                            sp.getThuongHieu().getTenThuongHieu() : "");
                } else {
                    dto.setTenThuongHieu("");
                }

                // ===== 4. THÔNG TIN KIỂU GIÀY =====
                if (sp.getKieuGiay() != null) {
                    dto.setMaKieuGiay(sp.getKieuGiay().getMaKieuGiay());
                    dto.setTenKieuGiay(sp.getKieuGiay().getTenKieuGiay() != null ?
                            sp.getKieuGiay().getTenKieuGiay() : "");
                } else {
                    dto.setTenKieuGiay("");
                }

                // ===== 5. THÔNG TIN CHẤT LIỆU =====
                if (sp.getChatLieu() != null) {
                    dto.setMaChatLieu(sp.getChatLieu().getMaChatLieu());
                    dto.setTenChatLieu(sp.getChatLieu().getTenChatLieu() != null ?
                            sp.getChatLieu().getTenChatLieu() : "");
                } else {
                    dto.setTenChatLieu("");
                }

                // ===== 6. THÔNG TIN GIÁ VÀ TỒN KHO =====
                // Lấy tổng tồn kho
                dto.setTongTon(sp.getTongTon());

                // Lấy giá trung bình
                dto.setGiaBanTrungBinh(sp.getGiaBanTrungBinh());

                // Lấy khoảng giá từ chi tiết sản phẩm
                String maSanPham = sp.getMaSanPham();
                try {
                    BigDecimal minPrice = sanPhamChiTietService.getGiaMin(maSanPham);
                    BigDecimal maxPrice = sanPhamChiTietService.getGiaMax(maSanPham);

                    // Đếm số lượng biến thể
                    List<SanPhamChiTiet> variants = sanPhamChiTietService.getallsp(maSanPham);
                    dto.setSoLuongBienThe(variants != null ? variants.size() : 0);

                    dto.setGiaMin(minPrice != null ? minPrice : BigDecimal.ZERO);
                    dto.setGiaMax(maxPrice != null ? maxPrice : BigDecimal.ZERO);

                    // Format hiển thị giá
                    if (minPrice != null && maxPrice != null) {
                        if (minPrice.compareTo(maxPrice) == 0) {
                            dto.setGiaBanDisplay(formatter.format(minPrice) + "₫");
                        } else {
                            dto.setGiaBanDisplay(formatter.format(minPrice) + "₫ - " + formatter.format(maxPrice) + "₫");
                        }
                    } else {
                        dto.setGiaBanDisplay("0₫");
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Lỗi khi lấy giá cho sản phẩm " + maSanPham + ": " + e.getMessage());
                    dto.setGiaBanDisplay("0₫");
                    dto.setGiaMin(BigDecimal.ZERO);
                    dto.setGiaMax(BigDecimal.ZERO);
                    dto.setSoLuongBienThe(0);
                }

                dtoList.add(dto);

                if (count % 10 == 0) {
                    System.out.println("  ✅ Đã chuyển đổi " + count + " sản phẩm...");
                }

            } catch (Exception e) {
                System.err.println("❌ Lỗi khi chuyển đổi sản phẩm " + sp.getMaSanPham() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("✅ Đã chuyển đổi thành công " + dtoList.size() + " sản phẩm");
        return dtoList;
    }

    @GetMapping("/add-view")
    public String addView(Model model) {
        model.addAttribute("activeMenu", "sanpham");

        SanPhamDTO formObject = new SanPhamDTO();
        model.addAttribute("form", formObject);

        formObject.setMaSanPham(taoMaSanPham());


        model.addAttribute("listdmsp", danhMucSanPhamService.getAll());
        model.addAttribute("listcl", chatLieuService.findAll());
        model.addAttribute("listth", thuongHieuService.findAll());
        model.addAttribute("listkg", kieuGiayService.findAll());
        model.addAttribute("listmausac", mauSacService.findAll());
        model.addAttribute("listkichthuoc", kichThuocService.getall());

        return "sanpham/add";
    }


    @PostMapping("/api/upload-anh")
    @ResponseBody
    public String uploadAnh(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = FileUploadUtil.saveFile(file);
            return fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return "error";
        }
    }


    @PostMapping("/api/upload-image-url")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImageFromUrl(
            @RequestParam("imageUrl") String imageUrl,
            @RequestParam(value = "fileName", required = false) String customFileName) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "URL ảnh không hợp lệ!");
                return ResponseEntity.badRequest().body(response);
            }

            String fileName = customFileName;
            if (fileName == null || fileName.isEmpty()) {
                String[] parts = imageUrl.split("/");
                fileName = parts[parts.length - 1];
                if (!fileName.contains(".")) {
                    fileName = fileName + ".jpg";
                }
            }
            java.net.URL url = new java.net.URL(imageUrl);
            try (java.io.InputStream in = url.openStream()) {
                Path uploadPath = Paths.get("D:/AnhSP/");
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(fileName);
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            response.put("success", true);
            response.put("message", "Upload ảnh từ URL thành công!");
            response.put("fileName", fileName);
            response.put("fileUrl", "/images/" + fileName);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi upload ảnh từ URL: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/api/upload-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "product") String type,
            @RequestParam(value = "productCode", required = false) String productCode) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File không được để trống!");
                return ResponseEntity.badRequest().body(response);
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("message", "Chỉ chấp nhận file ảnh!");
                return ResponseEntity.badRequest().body(response);
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "Kích thước file không được vượt quá 5MB!");
                return ResponseEntity.badRequest().body(response);
            }

            String fileName = FileUploadUtil.saveFile(file);

            response.put("success", true);
            response.put("message", "Upload ảnh thành công!");
            response.put("fileName", fileName);
            response.put("fileUrl", "/images/" + fileName);
            response.put("type", type);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi khi upload ảnh: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ===== API LẤY DANH SÁCH ẢNH TỪ THƯ MỤC =====
    @GetMapping("/api/get-image-list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getImageList() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ImageInfo> images = imageService.getImageInfoList();
            response.put("success", true);
            response.put("images", images);
            response.put("count", images.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/image-info/{fileName}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getImageInfo(
            @PathVariable("fileName") String fileName) {

        Map<String, Object> response = new HashMap<>();

        try {
            boolean exists = FileUploadUtil.fileExists(fileName);

            if (!exists) {
                response.put("success", false);
                response.put("message", "Không tìm thấy file!");
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get("D:/AnhSP/", fileName);
            Map<String, Object> info = new HashMap<>();
            info.put("fileName", fileName);
            info.put("fileUrl", "/images/" + fileName);
            info.put("size", Files.size(filePath));
            info.put("lastModified", Files.getLastModifiedTime(filePath).toString());

            String extension = "";
            int lastDot = fileName.lastIndexOf(".");
            if (lastDot > 0) {
                extension = fileName.substring(lastDot + 1).toLowerCase();
            }
            info.put("extension", extension);

            response.put("success", true);
            response.put("info", info);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/api/product-images/{productCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProductImages(
            @PathVariable("productCode") String productCode) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<SanPhamChiTiet> variants = sanPhamChiTietService.getallsp(productCode);

            List<Map<String, Object>> images = new ArrayList<>();
            Set<String> uniqueImages = new HashSet<>();

            for (SanPhamChiTiet variant : variants) {
                if (variant.getDuongDanAnh() != null && !variant.getDuongDanAnh().isEmpty()) {
                    String fileName = variant.getDuongDanAnh();
                    if (!uniqueImages.contains(fileName)) {
                        uniqueImages.add(fileName);

                        Map<String, Object> imageInfo = new HashMap<>();
                        imageInfo.put("fileName", fileName);
                        imageInfo.put("fileUrl", "/images/" + fileName);
                        imageInfo.put("isMain", false);
                        imageInfo.put("variantCode", variant.getMaSanPhamChiTiet());
                        images.add(imageInfo);
                    }
                }

                if (variant.getDanhSachAnh() != null && !variant.getDanhSachAnh().isEmpty()) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        List<String> imageList = mapper.readValue(variant.getDanhSachAnh(),
                                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});

                        for (String fileName : imageList) {
                            if (!uniqueImages.contains(fileName)) {
                                uniqueImages.add(fileName);

                                Map<String, Object> imageInfo = new HashMap<>();
                                imageInfo.put("fileName", fileName);
                                imageInfo.put("fileUrl", "/images/" + fileName);
                                imageInfo.put("isMain", false);
                                imageInfo.put("variantCode", variant.getMaSanPhamChiTiet());
                                images.add(imageInfo);
                            }
                        }
                    } catch (Exception e) {
                        String[] imageArray = variant.getDanhSachAnh().split(",");
                        for (String fileName : imageArray) {
                            fileName = fileName.trim();
                            if (!fileName.isEmpty() && !uniqueImages.contains(fileName)) {
                                uniqueImages.add(fileName);

                                Map<String, Object> imageInfo = new HashMap<>();
                                imageInfo.put("fileName", fileName);
                                imageInfo.put("fileUrl", "/images/" + fileName);
                                imageInfo.put("isMain", false);
                                imageInfo.put("variantCode", variant.getMaSanPhamChiTiet());
                                images.add(imageInfo);
                            }
                        }
                    }
                }
            }

            if (!images.isEmpty()) {
                images.get(0).put("isMain", true);
            }

            response.put("success", true);
            response.put("productCode", productCode);
            response.put("images", images);
            response.put("count", images.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi lấy danh sách ảnh: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/api/delete-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteImage(
            @RequestParam("fileName") String fileName) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (fileName == null || fileName.isEmpty()) {
                response.put("success", false);
                response.put("message", "Tên file không hợp lệ!");
                return ResponseEntity.badRequest().body(response);
            }

            boolean deleted = FileUploadUtil.deleteFile(fileName);

            if (deleted) {
                response.put("success", true);
                response.put("message", "Xóa ảnh thành công!");
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy file để xóa!");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi khi xóa ảnh: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/api/check-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkImageExists(
            @RequestParam("fileName") String fileName) {

        Map<String, Object> response = new HashMap<>();

        try {
            boolean exists = FileUploadUtil.fileExists(fileName);

            response.put("success", true);
            response.put("exists", exists);
            response.put("fileName", fileName);
            response.put("fileUrl", "/images/" + fileName);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi kiểm tra ảnh: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/api/check-trung")
    @ResponseBody
    public ResponseEntity<Boolean> checkTrung(@RequestParam("ten") String ten) {
        boolean exists = sanPhamService.isTenSanPhamDuplicate(ten);
        return ResponseEntity.ok(exists);
    }

    @PostMapping("/api/upload-images")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadMultipleImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "productCode", required = false) String productCode) {

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> uploadedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            if (files == null || files.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không có file nào được upload!");
                return ResponseEntity.badRequest().body(response);
            }

            if (files.size() > 20) {
                response.put("success", false);
                response.put("message", "Chỉ được upload tối đa 20 ảnh cùng lúc!");
                return ResponseEntity.badRequest().body(response);
            }

            for (MultipartFile file : files) {
                try {
                    // Validate file
                    if (file.isEmpty()) {
                        errors.add("File rỗng: " + file.getOriginalFilename());
                        continue;
                    }

                    String contentType = file.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        errors.add("File không phải ảnh: " + file.getOriginalFilename());
                        continue;
                    }

                    if (file.getSize() > 5 * 1024 * 1024) {
                        errors.add("File quá 5MB: " + file.getOriginalFilename());
                        continue;
                    }

                    // Lưu file
                    String fileName = FileUploadUtil.saveFile(file);

                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("fileName", fileName);
                    fileInfo.put("fileUrl", "/images/" + fileName);
                    fileInfo.put("originalName", file.getOriginalFilename());
                    fileInfo.put("size", file.getSize());
                    uploadedFiles.add(fileInfo);

                } catch (IOException e) {
                    errors.add("Lỗi upload " + file.getOriginalFilename() + ": " + e.getMessage());
                }
            }

            response.put("success", true);
            response.put("message", "Upload " + uploadedFiles.size() + " ảnh thành công!");
            response.put("uploaded", uploadedFiles);
            response.put("errors", errors);
            response.put("totalUploaded", uploadedFiles.size());
            response.put("totalErrors", errors.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi upload ảnh: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(value = "/api/save-all", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<?> saveAll(@RequestBody SanPhamWrapperDTO wrapperDto) {

        String tenMoi = wrapperDto.getSanPham().getTenSanPham();
        if (sanPhamService.isTenSanPhamDuplicate(tenMoi)) {
            return ResponseEntity.badRequest().body("Tên sản phẩm đã tồn tại trong hệ thống!");
        }
        try {
            SanPhamDTO spDto = wrapperDto.getSanPham();

            SanPham sp = new SanPham();
            sp.setMaSanPham(spDto.getMaSanPham());
            sp.setTenSanPham(spDto.getTenSanPham());
            sp.setMoTa(spDto.getMoTa());

            sp.setDanhMucSanPham(danhMucSanPhamService.findById(spDto.getMaDanhMuc()).orElse(null));
            sp.setThuongHieu(thuongHieuService.findById(spDto.getMaThuongHieu()).orElse(null));
            sp.setKieuGiay(kieuGiayService.findById(spDto.getMaKieuGiay()).orElse(null));
            sp.setChatLieu(chatLieuService.findById(spDto.getMaChatLieu()).orElse(null));

            sp.setTrangThai(true);
            sp.setNgayTao(LocalDateTime.now());
            sanPhamService.save(sp);

            // ===== XỬ LÝ CHI TIẾT SẢN PHẨM VỚI NHIỀU ẢNH =====
            for (SanPhamChiTietDTO ctDto : wrapperDto.getChiTietList()) {

                SanPhamChiTiet ct = new SanPhamChiTiet();
                ct.setSanPham(sp);

                String maSP = sp.getMaSanPham();
                MauSac mauSac = mauSacService.findById(ctDto.getMaMauSac()).orElse(null);
                KichThuoc kichThuoc = kichThuocService.getKichThuocById(ctDto.getMaKichThuoc()).orElse(null);

                String maMau = (mauSac != null) ? mauSac.getMaMauSac() : "";
                String tenSize = (kichThuoc != null) ? kichThuoc.getTenKichThuoc() : "";
                String maBienThe = maSP + "-" + maMau + "-" + tenSize;
                ct.setMaSanPhamChiTiet(maBienThe);

                // Set thông tin cơ bản
                ct.setGiaBan(ctDto.getGiaBan());
                ct.setSoLuongTon(ctDto.getSoLuongTon());
                ct.setMauSac(mauSac);
                ct.setKichThuoc(kichThuoc);
                ct.setNgayTao(LocalDateTime.now());

                // ===== XỬ LÝ ẢNH =====
                List<String> danhSachAnh = ctDto.getDanhSachAnh();

                // 1. Lấy ảnh đại diện
                String anhDaiDien = ctDto.getDuongDanAnh();
                if (anhDaiDien == null || anhDaiDien.isEmpty()) {
                    if (danhSachAnh != null && !danhSachAnh.isEmpty()) {
                        anhDaiDien = danhSachAnh.get(0);
                    }
                }

                // Kiểm tra ảnh đại diện tồn tại
                if (anhDaiDien != null && !anhDaiDien.isEmpty()) {
                    if (!FileUploadUtil.fileExists(anhDaiDien)) {
                        // Nếu ảnh không tồn tại, tạo ảnh mặc định
                        anhDaiDien = "default-product-image.jpg";
                        System.out.println("⚠️ Ảnh không tồn tại, sử dụng ảnh mặc định: " + anhDaiDien);
                    }
                }
                ct.setDuongDanAnh(anhDaiDien);

                // 2. Lưu danh sách ảnh dạng JSON
                if (danhSachAnh != null && !danhSachAnh.isEmpty()) {
                    // Lọc ra các ảnh hợp lệ (tồn tại)
                    List<String> validImages = new ArrayList<>();
                    for (String img : danhSachAnh) {
                        if (FileUploadUtil.fileExists(img)) {
                            validImages.add(img);
                        } else {
                            System.out.println("⚠️ Ảnh không tồn tại, bỏ qua: " + img);
                        }
                    }

                    if (!validImages.isEmpty()) {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            String json = mapper.writeValueAsString(validImages);
                            ct.setDanhSachAnh(json);
                        } catch (Exception e) {
                            ct.setDanhSachAnh(String.join(",", validImages));
                        }
                    }
                }

                sanPhamChiTietService.capNhatTrangThaii(ct);
                sanPhamChiTietService.them(ct);
            }

            return ResponseEntity.ok("Thêm thành công");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") String id,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "source", defaultValue = "index") String source,
                       Model model) {

        SanPham sp = sanPhamService.findById(id).orElseThrow();
        model.addAttribute("sanpham", sp);

        model.addAttribute("listdmsp", danhMucSanPhamService.getAll());
        model.addAttribute("listcl", chatLieuService.findAll());
        model.addAttribute("listth", thuongHieuService.findAll());
        model.addAttribute("listkg", kieuGiayService.findAll());

        model.addAttribute("showModal", true);
        model.addAttribute("isEdit", true);
        model.addAttribute("source", source);
        if ("detail".equals(source)) {
            model.addAttribute("listBienThe", sanPhamChiTietService.getallsp(id));
            return "sanpham/detail";
        }

        Page<SanPham> pageSanPham = sanPhamService.getallpage(PageRequest.of(page, 5));
        model.addAttribute("listsp", pageSanPham.getContent());
        return "sanpham/index";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") String id,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(required = false) String maDanhMuc,
                         @RequestParam(required = false) Boolean tt,
                         @RequestParam(required = false) String maTH,
                         @RequestParam(required = false) String maKG,
                         @RequestParam(required = false) String t,
                         Model model) {


        SanPham sp = sanPhamService.findById(id).orElseThrow();
        model.addAttribute("sanpham", sp);
        model.addAttribute("listBienThe", sanPhamChiTietService.getallsp(id));
        model.addAttribute("sanphamct", new SanPhamChiTiet());
        model.addAttribute("listsp", sanPhamService.getAll());
        model.addAttribute("listms", mauSacService.findAll());
        model.addAttribute("lists", kichThuocService.getall());
        model.addAttribute("currentPage", page);
        model.addAttribute("maDanhMuc", maDanhMuc);
        model.addAttribute("tt", tt);
        model.addAttribute("maTH", maTH);
        model.addAttribute("maKG", maKG);
        model.addAttribute("t", t);

        return "sanpham/detail";
    }

    public String taoMaSanPham() {
        Random random = new Random();
        String maSP;

        do {
            int so = random.nextInt(10000);
            maSP = "SP" + String.format("%04d", so);
        } while (sanPhamRepository.existsByMaSanPham(maSP));

        return maSP;
    }

    @PostMapping("/update")
    public String update(@ModelAttribute SanPham sanpham,
                         @RequestParam(required = false) String maDanhMuc,
                         @RequestParam(required = false) String maThuongHieu,
                         @RequestParam(required = false) String maChatLieu,
                         @RequestParam(required = false) String maKieuGiay,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(required = false) String source,
                         RedirectAttributes redirectAttributes) {

        SanPham spOld = sanPhamService.findById(sanpham.getMaSanPham())
                .orElseThrow();

        spOld.setTenSanPham(sanpham.getTenSanPham());
        spOld.setMoTa(sanpham.getMoTa());

        if (maDanhMuc != null) {
            spOld.setDanhMucSanPham(
                    danhMucSanPhamService.findById(maDanhMuc).orElse(null)
            );
        }

        if (maThuongHieu != null) {
            spOld.setThuongHieu(
                    thuongHieuService.findById(maThuongHieu).orElse(null)
            );
        }

        if (maChatLieu != null) {
            spOld.setChatLieu(
                    chatLieuService.findById(maChatLieu).orElse(null)
            );
        }

        if (maKieuGiay != null) {
            spOld.setKieuGiay(
                    kieuGiayService.findById(maKieuGiay).orElse(null)
            );
        }

        sanPhamService.save(spOld);

        redirectAttributes.addFlashAttribute("successMess",
                "Cập nhật sản phẩm thành công!");

        if ("detail".equals(source)) {
            return "redirect:/sanpham/detail/" + sanpham.getMaSanPham();
        }

        return "redirect:/sanpham/index?page=" + page;
    }

    @GetMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable("id") String id,
                               @RequestParam("status") boolean status,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(required = false) String maDanhMuc,
                               @RequestParam(required = false) Boolean tt,
                               @RequestParam(required = false) String maTH,
                               @RequestParam(required = false) String maKG,
                               @RequestParam(required = false) String t,
                               RedirectAttributes redirectAttributes) {

        sanPhamService.updateTrangThai(id, status);

        if (!status) {
            sanPhamChiTietService.suaSanPham2(id);
        } else {
            sanPhamChiTietService.suaSanPham3(id);
        }

        // Xây dựng URL redirect với tất cả tham số filter
        String redirectUrl = "redirect:/sanpham/index?page=" + page;
        if (maDanhMuc != null && !maDanhMuc.isEmpty()) redirectUrl += "&maDanhMuc=" + maDanhMuc;
        if (tt != null) redirectUrl += "&tt=" + tt;
        if (maTH != null && !maTH.isEmpty()) redirectUrl += "&maTH=" + maTH;
        if (maKG != null && !maKG.isEmpty()) redirectUrl += "&maKG=" + maKG;
        if (t != null && !t.isEmpty()) redirectUrl += "&t=" + t;

        return redirectUrl;
    }

    @PostMapping("/api/check-and-add-variants")
    @ResponseBody
    public ResponseEntity<?> checkAndAddVariants(@RequestBody SanPhamWrapperDTO wrapperDto) {
        try {
            String tenSanPham = wrapperDto.getSanPham().getTenSanPham();

            boolean exists = sanPhamService.isTenSanPhamDuplicate(tenSanPham);

            if (!exists) {
                Map<String, Object> response = new HashMap<>();
                response.put("exists", false);
                response.put("message", "Tên sản phẩm chưa tồn tại, có thể tạo mới");
                return ResponseEntity.ok(response);
            }

            SanPham existingProduct = sanPhamService.findByTenSanPham(tenSanPham);
            if (existingProduct == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("exists", true);
                response.put("canAdd", false);
                response.put("message", "Không tìm thấy sản phẩm dù tên bị trùng!");
                return ResponseEntity.ok(response);
            }

            List<SanPhamChiTiet> existingVariants = sanPhamChiTietService.getallsp(existingProduct.getMaSanPham());
            Set<String> existingVariantCodes = existingVariants.stream()
                    .map(SanPhamChiTiet::getMaSanPhamChiTiet)
                    .collect(Collectors.toSet());

            List<String> newVariants = new ArrayList<>();
            List<String> duplicateVariants = new ArrayList<>();

            // ===== SỬA LỖI Ở ĐÂY =====
            for (SanPhamChiTietDTO ctDto : wrapperDto.getChiTietList()) {
                // Lấy mã biến thể từ DTO
                String maBienThe = ctDto.getMaBienThe();

                // Nếu không có maBienThe, tự tạo từ màu sắc và kích thước
                if (maBienThe == null || maBienThe.isEmpty()) {
                    String maMau = ctDto.getMaMauSac();
                    String maKichThuoc = ctDto.getMaKichThuoc();
                    String tenSize = kichThuocService.getKichThuocById(maKichThuoc)
                            .map(KichThuoc::getTenKichThuoc)
                            .orElse(maKichThuoc);
                    maBienThe = existingProduct.getMaSanPham() + "-" + maMau + "-" + tenSize;
                }

                if (existingVariantCodes.contains(maBienThe)) {
                    duplicateVariants.add(maBienThe);
                } else {
                    newVariants.add(maBienThe);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("exists", true);
            response.put("canAdd", !newVariants.isEmpty());
            response.put("productCode", existingProduct.getMaSanPham());
            response.put("productName", existingProduct.getTenSanPham());
            response.put("existingVariantCount", existingVariants.size());
            response.put("newVariantCount", newVariants.size());
            response.put("duplicateCount", duplicateVariants.size());
            response.put("duplicateVariants", duplicateVariants);
            response.put("newVariants", newVariants);
            response.put("message", "Sản phẩm đã tồn tại! Bạn có muốn thêm " + newVariants.size() + " biến thể mới vào sản phẩm này không?");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleStatus(
            @PathVariable String id,
            @RequestParam boolean active) {

        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("=== TOGGLE STATUS ===");
            System.out.println("ID: " + id);
            System.out.println("Active: " + active);

            // Tìm biến thể
            SanPhamChiTiet spct = sanPhamChiTietService.findbyId(id).orElse(null);
            if (spct == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy biến thể!");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra sản phẩm cha
            if (spct.getSanPham() != null && !spct.getSanPham().getTrangThai()) {
                response.put("success", false);
                response.put("message", "Sản phẩm cha đang ngừng bán, không thể thay đổi!");
                return ResponseEntity.badRequest().body(response);
            }

            // Cập nhật trạng thái
            String newStatus = active ? "Còn hàng" : "Ngừng bán";
            spct.setTrangThai(newStatus);

            // Lưu
            SanPhamChiTiet saved = sanPhamChiTietService.them(spct);

            response.put("success", true);
            response.put("message", "Cập nhật trạng thái thành công!");
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

    @PostMapping("/add-attribute")
    public ResponseEntity<Map<String, Object>> addAttribute(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            String type = request.get("type");
            String name = request.get("name");

            // Validate
            if (type == null || name == null || name.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Vui lòng nhập đầy đủ thông tin!");
                return ResponseEntity.badRequest().body(result);
            }

            String normalizedName = name.trim();
            Object saved = null;
            String id = "";
            String ma = "";
            LocalDateTime now = LocalDateTime.now();

            switch (type) {
                case "danhMuc":
                    if (danhMucSanPhamService.existsByTenDanhMuc(normalizedName)) {
                        result.put("success", false);
                        result.put("message", "Tên danh mục '" + normalizedName + "' đã tồn tại!");
                        return ResponseEntity.badRequest().body(result);
                    }

                    DanhMucSanPham dm = new DanhMucSanPham();
                    ma = danhMucSanPhamService.generateMaDanhMuc();
                    dm.setMaDanhMuc(ma);
                    dm.setTenDanhMuc(danhMucSanPhamService.normalizeTenDanhMuc(normalizedName)); // Sử dụng normalize có sẵn
                    dm.setTrangThai(true);
                    dm.setNgayTao(now);
                    saved = danhMucSanPhamService.them(dm);
                    id = ((DanhMucSanPham) saved).getMaDanhMuc();
                    break;

                case "thuongHieu":
                    // Kiểm tra tên trùng (sử dụng hàm có sẵn)
                    if (thuongHieuService.ktraten(normalizedName)) {
                        result.put("success", false);
                        result.put("message", "Tên thương hiệu '" + normalizedName + "' đã tồn tại!");
                        return ResponseEntity.badRequest().body(result);
                    }

                    ThuongHieu th = new ThuongHieu();
                    ma = thuongHieuService.generateMaThuongHieu();
                    th.setMaThuongHieu(ma);
                    th.setTenThuongHieu(thuongHieuService.normalizeTenThuongHieu(normalizedName)); // Sử dụng normalize có sẵn
                    th.setTrangThai(true);
                    th.setNgayTao(now);
                    saved = thuongHieuService.them(th);
                    id = ((ThuongHieu) saved).getMaThuongHieu();
                    break;

                case "kieuGiay":
                    // Kiểm tra tên trùng (sử dụng hàm có sẵn)
                    if (kieuGiayService.existsByTenKieuGiay(normalizedName)) {
                        result.put("success", false);
                        result.put("message", "Tên kiểu giày '" + normalizedName + "' đã tồn tại!");
                        return ResponseEntity.badRequest().body(result);
                    }

                    KieuGiay kg = new KieuGiay();
                    ma = kieuGiayService.generateMaKieuGiay();
                    kg.setMaKieuGiay(ma);
                    kg.setTenKieuGiay(kieuGiayService.normalizeTenKieuGiay(normalizedName)); // Sử dụng normalize có sẵn
                    kg.setTrangThai(true);
                    kg.setNgayTao(now);
                    saved = kieuGiayService.them(kg);
                    id = ((KieuGiay) saved).getMaKieuGiay();
                    break;

                case "chatLieu":
                    // Kiểm tra tên trùng (sử dụng hàm có sẵn)
                    if (chatLieuService.existsByTenChatLieu(normalizedName)) {
                        result.put("success", false);
                        result.put("message", "Tên chất liệu '" + normalizedName + "' đã tồn tại!");
                        return ResponseEntity.badRequest().body(result);
                    }

                    ChatLieu cl = new ChatLieu();
                    ma = chatLieuService.generateMaChatLieu();
                    cl.setMaChatLieu(ma);
                    cl.setTenChatLieu(chatLieuService.normalizeTenChatLieu(normalizedName)); // Sử dụng normalize có sẵn
                    cl.setTrangThai(true);
                    cl.setNgayTao(now);
                    saved = chatLieuService.add(cl);
                    id = ((ChatLieu) saved).getMaChatLieu();
                    break;

                case "mauSac":
                    // Kiểm tra tên trùng (sử dụng hàm có sẵn)
                    if (mauSacService.existsByTenMauSac(normalizedName)) {
                        result.put("success", false);
                        result.put("message", "Tên màu sắc '" + normalizedName + "' đã tồn tại!");
                        return ResponseEntity.badRequest().body(result);
                    }

                    MauSac ms = new MauSac();
                    ma = mauSacService.generateMaMauSac();
                    ms.setMaMauSac(ma);
                    ms.setTenMauSac(mauSacService.normalizeTenMauSac(normalizedName)); // Sử dụng normalize có sẵn
                    ms.setTrangThai(true);
                    ms.setNgayTao(now);
                    saved = mauSacService.add(ms);
                    id = ((MauSac) saved).getMaMauSac();
                    break;

                case "kichThuoc":
                    // Kiểm tra tên trùng (sử dụng hàm có sẵn)
                    if (kichThuocService.existsByTenKichThuoc(normalizedName)) {
                        result.put("success", false);
                        result.put("message", "Tên kích cỡ '" + normalizedName + "' đã tồn tại!");
                        return ResponseEntity.badRequest().body(result);
                    }

                    KichThuoc kt = new KichThuoc();
                    ma = kichThuocService.generateMaKichThuoc();
                    kt.setMaKichThuoc(ma);
                    kt.setTenKichThuoc(kichThuocService.normalizeTenKichThuoc(normalizedName)); // Sử dụng normalize có sẵn
                    kt.setTrangThai(true);
                    kt.setNgayTao(now);
                    saved = kichThuocService.add(kt);
                    id = ((KichThuoc) saved).getMaKichThuoc();
                    break;

                default:
                    result.put("success", false);
                    result.put("message", "Loại thuộc tính không hợp lệ!");
                    return ResponseEntity.badRequest().body(result);
            }

            result.put("success", true);
            result.put("message", "Thêm mới thành công! Mã: " + ma);
            result.put("id", id);
            result.put("ma", ma);
            result.put("name", normalizedName);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/api/search-suggestions")
    @ResponseBody
    public ResponseEntity<List<Map<String, String>>> getSearchSuggestions(
            @RequestParam("q") String keyword) {

        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            String cleanKeyword = keyword.trim();
            System.out.println("🔍 Search suggestions for: " + cleanKeyword);

            List<SanPham> suggestions = sanPhamService.findTop10ByTenSanPhamContainingOrMaSanPhamContaining(cleanKeyword);

            List<Map<String, String>> result = new ArrayList<>();
            for (SanPham sp : suggestions) {
                Map<String, String> item = new HashMap<>();
                item.put("maSanPham", sp.getMaSanPham() != null ? sp.getMaSanPham() : "");
                item.put("tenSanPham", sp.getTenSanPham() != null ? sp.getTenSanPham() : "");
                item.put("tenThuongHieu", sp.getThuongHieu() != null && sp.getThuongHieu().getTenThuongHieu() != null ?
                        sp.getThuongHieu().getTenThuongHieu() : "");
                result.add(item);
            }

            System.out.println("✅ Found " + result.size() + " suggestions");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi search suggestions: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Thêm vào SanPhamController.java

    @PostMapping("/api/add-variants-to-existing")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addVariantsToExisting(@RequestBody SanPhamWrapperDTO wrapperDto) {
        Map<String, Object> response = new HashMap<>();

        try {
            String tenSanPham = wrapperDto.getSanPham().getTenSanPham();

            // Tìm sản phẩm theo tên
            SanPham existingProduct = sanPhamService.findByTenSanPham(tenSanPham);
            if (existingProduct == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy sản phẩm: " + tenSanPham);
                return ResponseEntity.badRequest().body(response);
            }

            // Lấy danh sách biến thể hiện có
            List<SanPhamChiTiet> existingVariants = sanPhamChiTietService.getallsp(existingProduct.getMaSanPham());
            Set<String> existingVariantCodes = existingVariants.stream()
                    .map(SanPhamChiTiet::getMaSanPhamChiTiet)
                    .collect(Collectors.toSet());

            int addedCount = 0;
            int duplicateCount = 0;
            List<String> addedVariants = new ArrayList<>();
            List<String> duplicateVariants = new ArrayList<>();

            // Xử lý từng biến thể mới
            for (SanPhamChiTietDTO ctDto : wrapperDto.getChiTietList()) {
                String maMau = ctDto.getMaMauSac();
                String maKichThuoc = ctDto.getMaKichThuoc();
                String tenSize = kichThuocService.getKichThuocById(maKichThuoc)
                        .map(KichThuoc::getTenKichThuoc)
                        .orElse(maKichThuoc);
                String maBienThe = existingProduct.getMaSanPham() + "-" + maMau + "-" + tenSize;

                // Kiểm tra biến thể đã tồn tại chưa
                if (existingVariantCodes.contains(maBienThe)) {
                    duplicateCount++;
                    duplicateVariants.add(maBienThe);
                    continue;
                }

                // Tạo biến thể mới
                SanPhamChiTiet ct = new SanPhamChiTiet();
                ct.setMaSanPhamChiTiet(maBienThe);
                ct.setSanPham(existingProduct);

                MauSac mauSac = mauSacService.findById(maMau).orElse(null);
                KichThuoc kichThuoc = kichThuocService.getKichThuocById(maKichThuoc).orElse(null);

                ct.setMauSac(mauSac);
                ct.setKichThuoc(kichThuoc);
                ct.setGiaBan(ctDto.getGiaBan());
                ct.setSoLuongTon(ctDto.getSoLuongTon());
                ct.setNgayTao(LocalDateTime.now());

                // Xử lý ảnh
                String anhDaiDien = ctDto.getDuongDanAnh();
                if (anhDaiDien == null || anhDaiDien.isEmpty()) {
                    List<String> danhSachAnh = ctDto.getDanhSachAnh();
                    if (danhSachAnh != null && !danhSachAnh.isEmpty()) {
                        anhDaiDien = danhSachAnh.get(0);
                    }
                }

                if (anhDaiDien != null && !anhDaiDien.isEmpty()) {
                    if (!FileUploadUtil.fileExists(anhDaiDien)) {
                        anhDaiDien = "default-product-image.jpg";
                    }
                }
                ct.setDuongDanAnh(anhDaiDien);

                // Lưu danh sách ảnh
                List<String> danhSachAnh = ctDto.getDanhSachAnh();
                if (danhSachAnh != null && !danhSachAnh.isEmpty()) {
                    List<String> validImages = new ArrayList<>();
                    for (String img : danhSachAnh) {
                        if (FileUploadUtil.fileExists(img)) {
                            validImages.add(img);
                        }
                    }
                    if (!validImages.isEmpty()) {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            ct.setDanhSachAnh(mapper.writeValueAsString(validImages));
                        } catch (Exception e) {
                            ct.setDanhSachAnh(String.join(",", validImages));
                        }
                    }
                }

                sanPhamChiTietService.capNhatTrangThaii(ct);
                sanPhamChiTietService.them(ct);
                addedCount++;
                addedVariants.add(maBienThe);
            }

            sanPhamService.updateGiaTrungBinh(existingProduct.getMaSanPham());

            response.put("success", true);
            response.put("message", "Đã thêm " + addedCount + " biến thể mới cho sản phẩm " + existingProduct.getTenSanPham());
            response.put("addedCount", addedCount);
            response.put("duplicateCount", duplicateCount);
            response.put("addedVariants", addedVariants);
            response.put("duplicateVariants", duplicateVariants);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

}