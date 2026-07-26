package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.HoaDonChiTiet;
import com.example.th06876_java202.Entity.SanPhamChiTiet;
import com.example.th06876_java202.Repository.GiamGiaChiTietRepo;
import com.example.th06876_java202.Service.HoaDonChiTietService;
import com.example.th06876_java202.Repository.HoaDonChiTietRepository;
import com.example.th06876_java202.Repository.SanPhamChiTietRepository;
import com.example.th06876_java202.Service.DotGiamGiaService;
import com.example.th06876_java202.Service.HoaDonService;
import com.example.th06876_java202.Service.SanPhamChiTietService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/banhang")
public class BanHangApiController {

    /** Số quầy còn bán được = TỒN KHO THẬT (khoá biến thể vẫn chống tranh chấp). */
    private int khaDungQuay(com.example.th06876_java202.Entity.SanPhamChiTiet s) {
        return Math.max(0, s.getSoLuongTon() != null ? s.getSoLuongTon() : 0);
    }


    @Autowired
    private GiamGiaChiTietRepo giamGiaChiTietRepository;

    @Autowired
    private HoaDonChiTietService hoaDonChiTietService;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    SanPhamChiTietService sanPhamChiTietService;

    @Autowired
    private DotGiamGiaService dotGiamGiaService;

    @Autowired
    private HoaDonService hoaDonService;

    @PostMapping("/capnhatgia")
    @ResponseBody
    public ResponseEntity<?> capNhatGiaSanPham(
            @RequestParam("mahd") String maHoaDon,
            @RequestParam("mactsp") String maSPCT) {

        try {
            hoaDonChiTietService.capNhatGiaSanPham(maHoaDon, maSPCT);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật giá thành công!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/capnhatgiatatca")
    @ResponseBody
    public ResponseEntity<?> capNhatGiaTatCa(
            @RequestParam("mahd") String maHoaDon,
            @RequestParam("maSPCTs") String maSPCTsJson) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> maSPCTs = mapper.readValue(maSPCTsJson, new TypeReference<List<String>>() {});

            hoaDonChiTietService.capNhatGiaTatCa(maHoaDon, maSPCTs);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật " + maSPCTs.size() + " sản phẩm thành công!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/giohang/{maHoaDon}")
    @ResponseBody
    public ResponseEntity<?> getCartWithLatestPrice(@PathVariable String maHoaDon) {
        try {
            System.out.println("=== LẤY GIỎ HÀNG ===");
            System.out.println("maHoaDon: " + maHoaDon);

            List<HoaDonChiTiet> hdctList = hoaDonChiTietRepository.findByMaHoaDon_MaHoaDon(maHoaDon);

            if (hdctList == null || hdctList.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<Map<String, Object>> cartItems = hdctList.stream().map(hdct -> {
                Map<String, Object> item = new HashMap<>();

                SanPhamChiTiet sp = hdct.getSanPhamChiTiet();
                if (sp == null) {
                    return item;
                }

                // ===== THÔNG TIN CƠ BẢN =====
                item.put("maSPCT", sp.getMaSanPhamChiTiet());
                item.put("soLuong", hdct.getSoLuong());

                // ===== GIÁ CŨ (GIÁ LÚC THÊM VÀO HÓA ĐƠN) =====
                BigDecimal giaCu = hdct.getDonGia();
                item.put("giaCu", giaCu != null ? giaCu : BigDecimal.ZERO);

                // ===== TỒN KHO =====
                item.put("tonKho", sp.getSoLuongTon());

                // ===== THÔNG TIN SẢN PHẨM =====
                String tenSanPham = sp.getSanPham() != null ? sp.getSanPham().getTenSanPham() : "Sản phẩm";
                String mauSac = sp.getMauSac() != null ? sp.getMauSac().getTenMauSac() : "";
                String kichThuoc = sp.getKichThuoc() != null ? sp.getKichThuoc().getTenKichThuoc() : "";

                item.put("tenSanPham", tenSanPham);
                item.put("mauSac", mauSac);
                item.put("kichThuoc", kichThuoc);

                // ===== GIÁ GỐC HIỆN TẠI =====
                BigDecimal giaGoc = sp.getGiaBan();
                item.put("giaGoc", giaGoc != null ? giaGoc : BigDecimal.ZERO);

                // ===== KIỂM TRA CÓ ĐANG GIẢM GIÁ =====
                boolean coGiamGia = kiemTraCoGiamGia(sp);
                item.put("coGiamGia", coGiamGia);

                // ===== TÍNH GIÁ HIỆN TẠI =====
                BigDecimal giaHienTai = giaGoc != null ? giaGoc : BigDecimal.ZERO;
                if (coGiamGia) {
                    BigDecimal tienGiam = tinhTienGiam(sp);
                    if (tienGiam != null && tienGiam.compareTo(BigDecimal.ZERO) > 0) {
                        giaHienTai = giaGoc.subtract(tienGiam);
                        if (giaHienTai.compareTo(BigDecimal.ZERO) < 0) {
                            giaHienTai = BigDecimal.ZERO;
                        }
                    }
                }
                item.put("giaMoi", giaHienTai);

                // ⭐⭐ QUAN TRỌNG: LUÔN KIỂM TRA THAY ĐỔI GIÁ
                boolean daThayDoiGia = false;
                String lyDoThayDoi = null;

                if (giaCu != null && giaHienTai != null) {
                    // So sánh giá cũ và giá mới (có làm tròn để tránh lỗi số học)
                    BigDecimal roundedCu = giaCu.setScale(0, RoundingMode.HALF_UP);
                    BigDecimal roundedMoi = giaHienTai.setScale(0, RoundingMode.HALF_UP);
                    daThayDoiGia = roundedCu.compareTo(roundedMoi) != 0;

                    if (daThayDoiGia) {
                        if (coGiamGia) {
                            lyDoThayDoi = "Áp dụng giảm giá mới (còn hiệu lực)";
                        } else {
                            lyDoThayDoi = "Giá sản phẩm đã thay đổi";
                        }
                    }
                }
                item.put("daThayDoiGia", daThayDoiGia);
                item.put("lyDoThayDoi", lyDoThayDoi);

                int soLuongTrongHoaDon = hdct.getSoLuong();
                int tonKhoHienTai = sp.getSoLuongTon();
                boolean khongDuTonKho = tonKhoHienTai < soLuongTrongHoaDon;
                item.put("khongDuTonKho", khongDuTonKho);
                item.put("soLuongTrongHoaDon", soLuongTrongHoaDon);

                // ===== LOG CHI TIẾT =====
                System.out.println("  - SP: " + tenSanPham + " [" + mauSac + " - " + kichThuoc + "]");
                System.out.println("    Giá gốc: " + giaGoc);
                System.out.println("    Giá cũ (trong HD): " + giaCu);
                System.out.println("    Giá hiện tại: " + giaHienTai);
                System.out.println("    Có giảm giá: " + coGiamGia);
                System.out.println("    Đã thay đổi giá: " + daThayDoiGia);
                System.out.println("    Lý do: " + lyDoThayDoi);

                return item;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(cartItems);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }



    private List<com.example.th06876_java202.Entity.DotGiamGia> getActiveDotGiamGia(String maSanPham) {
        List<com.example.th06876_java202.Entity.DotGiamGia> dggList = dotGiamGiaService.getBymasp(maSanPham);
        if (dggList == null || dggList.isEmpty()) {
            return new ArrayList<>();
        }

        LocalDate today = LocalDate.now();
        List<com.example.th06876_java202.Entity.DotGiamGia> activeList = new ArrayList<>();

        for (com.example.th06876_java202.Entity.DotGiamGia dgg : dggList) {
            if ("Hoạt động".equals(dgg.getTrangThai())) {
                LocalDate ngayBatDau = (dgg.getNgayBatDau());
                LocalDate ngayKetThuc = (dgg.getNgayKetThuc());

                if (ngayBatDau != null && ngayKetThuc != null) {
                    boolean isActive = (today.isEqual(ngayBatDau) || today.isAfter(ngayBatDau)) &&
                            (today.isEqual(ngayKetThuc) || today.isBefore(ngayKetThuc));
                    if (isActive) {
                        activeList.add(dgg);
                    }
                }
            }
        }

        return activeList;
    }

    private boolean isDotGiamGiaActive(com.example.th06876_java202.Entity.DotGiamGia dgg) {
        if (dgg == null) return false;
        if (!"Hoạt động".equals(dgg.getTrangThai())) return false;

        LocalDate ngayBatDau = dgg.getNgayBatDau();
        LocalDate ngayKetThuc = dgg.getNgayKetThuc();

        if (ngayBatDau == null || ngayKetThuc == null) return false;

        LocalDate today = LocalDate.now();
        LocalDate start = (ngayBatDau);
        LocalDate end = (ngayKetThuc);

        if (start == null || end == null) return false;
        return (today.isEqual(start) || today.isAfter(start)) &&
                (today.isEqual(end) || today.isBefore(end));
    }

    private boolean kiemTraCoGiamGia(SanPhamChiTiet spct) {
        if (spct == null || spct.getSanPham() == null) {
            return false;
        }

        try {
            String maSanPham = spct.getSanPham().getMaSanPham();
            List<com.example.th06876_java202.Entity.DotGiamGia> dggList =
                    dotGiamGiaService.getBymasp(maSanPham);

            if (dggList != null && !dggList.isEmpty()) {
                LocalDate today = LocalDate.now();

                for (com.example.th06876_java202.Entity.DotGiamGia dgg : dggList) {
                    if ("Hoạt động".equals(dgg.getTrangThai())) {
                        LocalDate ngayBatDau = (dgg.getNgayBatDau());
                        LocalDate ngayKetThuc = (dgg.getNgayKetThuc());

                        if (ngayBatDau != null && ngayKetThuc != null) {
                            // ⭐ KIỂM TRA NGÀY HIỆN TẠI CÓ NẰM TRONG KHOẢNG KHÔNG
                            boolean isActive = (today.isEqual(ngayBatDau) || today.isAfter(ngayBatDau)) &&
                                    (today.isEqual(ngayKetThuc) || today.isBefore(ngayKetThuc));

                            if (isActive) {
                                System.out.println("    ✅ Sản phẩm đang trong đợt giảm giá: " + maSanPham);
                                System.out.println("    Ngày bắt đầu: " + ngayBatDau);
                                System.out.println("    Ngày kết thúc: " + ngayKetThuc);
                                System.out.println("    Ngày hiện tại: " + today);
                                return true;
                            }
                        }
                    }
                }
                System.out.println("    ❌ Sản phẩm KHÔNG trong đợt giảm giá: " + maSanPham);
                return false;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Lỗi kiểm tra giảm giá: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private BigDecimal tinhTienGiam(SanPhamChiTiet spct) {
        if (spct == null || spct.getSanPham() == null) {
            return BigDecimal.ZERO;
        }

        try {
            String maSanPham = spct.getSanPham().getMaSanPham();
            List<com.example.th06876_java202.Entity.DotGiamGia> dggList =
                    dotGiamGiaService.getBymasp(maSanPham);

            if (dggList == null || dggList.isEmpty()) {
                return BigDecimal.ZERO;
            }

            BigDecimal maxGiam = BigDecimal.ZERO;
            BigDecimal giaGoc = spct.getGiaBan();
            LocalDate today = LocalDate.now();

            for (com.example.th06876_java202.Entity.DotGiamGia dgg : dggList) {
                if ("Hoạt động".equals(dgg.getTrangThai())) {
                    // ⭐ KIỂM TRA NGÀY HIỆU LỰC
                    LocalDate ngayBatDau = (dgg.getNgayBatDau());
                    LocalDate ngayKetThuc = (dgg.getNgayKetThuc());

                    if (ngayBatDau != null && ngayKetThuc != null) {
                        boolean isActive = (today.isEqual(ngayBatDau) || today.isAfter(ngayBatDau)) &&
                                (today.isEqual(ngayKetThuc) || today.isBefore(ngayKetThuc));

                        if (isActive) {
                            BigDecimal giam = giaGoc.multiply(dgg.getGiaTriGiam()).divide(BigDecimal.valueOf(100));
                            if (giam.compareTo(maxGiam) > 0) {
                                maxGiam = giam;
                            }
                        }
                    }
                }
            }

            return maxGiam;
        } catch (Exception e) {
            System.err.println("Lỗi tính tiền giảm: " + e.getMessage());
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
    }

    @PostMapping("/xoatatca")
    @ResponseBody
    public ResponseEntity<?> xoaNhieuSanPham(
            @RequestParam("mahd") String maHoaDon,
            @RequestParam("maSPCTs") String maSPCTsJson) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> maSPCTs = mapper.readValue(maSPCTsJson, new TypeReference<List<String>>() {});

            System.out.println("=== XOA NHIEU SAN PHAM ===");
            System.out.println("Ma HD: " + maHoaDon);
            System.out.println("So luong SP can xoa: " + maSPCTs.size());

            int count = 0;
            int tongHoanLai = 0;

            for (String maSPCT : maSPCTs) {
                HoaDonChiTiet hdct = hoaDonChiTietRepository.findByMaHoaDon_MaHoaDonAndSanPhamChiTiet_MaSanPhamChiTiet(
                        maHoaDon, maSPCT
                );

                if (hdct != null) {
                    SanPhamChiTiet sp = sanPhamChiTietRepository.findById(maSPCT).orElse(null);
                    if (sp != null) {
                        sp.setSoLuongTon(sp.getSoLuongTon() + hdct.getSoLuong());
                        sanPhamChiTietRepository.save(sp);
                        tongHoanLai += hdct.getSoLuong();
                    }
                    hoaDonChiTietRepository.delete(hdct);
                    count++;
                }
            }

            System.out.println("✅ Da xoa " + count + " san pham, hoan " + tongHoanLai + " SP vao kho");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã xóa " + count + " sản phẩm khỏi hóa đơn, hoàn " + tongHoanLai + " SP vào kho!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/capnhatsoluong")
    @ResponseBody
    public ResponseEntity<?> capNhatSoLuongNhieu(
            @RequestParam("mahd") String maHoaDon,
            @RequestParam("updateData") String updateDataJson) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> updateData = mapper.readValue(updateDataJson, new TypeReference<List<Map<String, Object>>>() {});

            int count = 0;
            for (Map<String, Object> data : updateData) {
                String maSPCT = (String) data.get("maSPCT");
                Integer soLuongMoi = (Integer) data.get("soLuongMoi");

                if (maSPCT != null && soLuongMoi != null && soLuongMoi > 0) {
                    HoaDonChiTiet hdct = hoaDonChiTietRepository.findByMaHoaDon_MaHoaDonAndSanPhamChiTiet_MaSanPhamChiTiet(
                            maHoaDon, maSPCT
                    );

                    if (hdct != null) {
                        hdct.setSoLuong(soLuongMoi);
                        BigDecimal donGia = hdct.getDonGia();
                        BigDecimal thanhTienMoi = donGia.multiply(BigDecimal.valueOf(soLuongMoi));
                        hdct.setThanhTien(thanhTienMoi);
                        hoaDonChiTietRepository.save(hdct);
                        count++;
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã cập nhật " + count + " sản phẩm!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/capnhatsoluongmot")
    @ResponseBody
    public ResponseEntity<?> capNhatSoLuongMot(
            @RequestParam("mahd") String maHoaDon,
            @RequestParam("mactsp") String maSPCT,
            @RequestParam("soLuong") int soLuongMoi) {

        try {
            System.out.println("=== CAP NHAT SO LUONG ===");
            System.out.println("Ma HD: " + maHoaDon);
            System.out.println("Ma SPCT: " + maSPCT);
            System.out.println("So luong moi: " + soLuongMoi);

            HoaDonChiTiet hdct = hoaDonChiTietRepository.findByMaHoaDon_MaHoaDonAndSanPhamChiTiet_MaSanPhamChiTiet(
                    maHoaDon, maSPCT
            );

            if (hdct == null) {
                throw new RuntimeException("Không tìm thấy sản phẩm trong hóa đơn!");
            }

            SanPhamChiTiet spct = hdct.getSanPhamChiTiet();
            if (spct == null) {
                throw new RuntimeException("Không tìm thấy sản phẩm!");
            }

            int soLuongCu = hdct.getSoLuong();
            int soLuongMoiFinal = soLuongMoi > 0 ? soLuongMoi : 1;
            int chenhLech = soLuongMoiFinal - soLuongCu;

            System.out.println("So luong cu: " + soLuongCu);
            System.out.println("Chenh lech: " + chenhLech);
            System.out.println("Ton kho hien tai: " + spct.getSoLuongTon());

            // KHOÁ dòng biến thể — chống quầy ↔ online giành hàng song song
            sanPhamChiTietRepository.khoaBienTheDeDatHang(spct.getMaSanPhamChiTiet());
            if (chenhLech > 0) {
                if (khaDungQuay(spct) < chenhLech) {
                    throw new RuntimeException("Không đủ tồn kho! Còn có thể bán " + khaDungQuay(spct));
                }
                spct.setSoLuongTon(spct.getSoLuongTon() - chenhLech);
            } else if (chenhLech < 0) {
                spct.setSoLuongTon(spct.getSoLuongTon() + Math.abs(chenhLech));
            }

            sanPhamChiTietRepository.save(spct);

            hdct.setSoLuong(soLuongMoiFinal);
            BigDecimal donGia = hdct.getDonGia();
            BigDecimal thanhTienMoi = donGia.multiply(BigDecimal.valueOf(soLuongMoiFinal));
            hdct.setThanhTien(thanhTienMoi);

            hoaDonChiTietRepository.save(hdct);

            System.out.println("✅ Cap nhat so luong thanh cong!");
            System.out.println("Ton kho moi: " + spct.getSoLuongTon());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã cập nhật số lượng thành công!");
            response.put("tonKhoMoi", spct.getSoLuongTon());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

//    @PostMapping("/xoasp")
//    @ResponseBody
//    public ResponseEntity<?> xoaSanPham(
//            @RequestParam("mahd") String maHoaDon,
//            @RequestParam("mactsp") String maSPCT) {
//
//        try {
//            System.out.println("=== XOA SAN PHAM ===");
//            System.out.println("Ma HD: " + maHoaDon);
//            System.out.println("Ma SPCT: " + maSPCT);
//
//            HoaDonChiTiet hdct = hoaDonChiTietRepository
//                    .findByMaHoaDon_MaHoaDonAndSanPhamChiTiet_MaSanPhamChiTiet(maHoaDon, maSPCT);
//
//            if (hdct == null) {
//                Map<String, Object> response = new HashMap<>();
//                response.put("success", false);
//                response.put("message", "Không tìm thấy sản phẩm trong hóa đơn!");
//                return ResponseEntity.badRequest().body(response);
//            }
//
//            // Lấy số lượng để hoàn lại tồn kho
//            int soLuongHoan = hdct.getSoLuong();
//            SanPhamChiTiet spct = hdct.getSanPhamChiTiet();
//            spct.setSoLuongTon(spct.getSoLuongTon() + soLuongHoan);
//            sanPhamChiTietRepository.save(spct);
//
//            // Xóa chi tiết hóa đơn
//            hoaDonChiTietRepository.delete(hdct);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("message", "Đã xóa sản phẩm và hoàn " + soLuongHoan + " SP vào kho!");
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", false);
//            response.put("message", "Lỗi: " + e.getMessage());
//            return ResponseEntity.badRequest().body(response);
//        }
//    }



}

