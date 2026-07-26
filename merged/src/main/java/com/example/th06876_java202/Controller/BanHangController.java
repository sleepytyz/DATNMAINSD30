package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.*;
import com.example.th06876_java202.Repository.*;
import com.example.th06876_java202.Service.*;
import com.example.th06876_java202.config.CustomUserDetails;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.EncodeHintType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.math.BigDecimal;
import org.slf4j.Logger;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/banhang")
@Transactional
public class BanHangController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    GiamGiaChiTietRepo giamGiaChiTietRepository;

    @Autowired
    SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    DiaChiRepo diaChiRepo;

    @Autowired
    private GHNShippingService ghnShippingService;

    @Autowired
    HoaDonRepo hoaDonRepo;

    @Autowired
    private DiaChiService diaChiService;

    private static final Logger logger = LoggerFactory.getLogger(BanHangController.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private GHNLocationService ghnLocationService;


    private String bank = "MB";

    private String account = "0344552008";
    // @Value("${vietqr.account-name}")
    private String accountName = "TRUONG HAI MINH";

    private final SanPhamService sanPhamService;
    private final SanPhamChiTietService sanPhamChiTietService;
    private final KhachHangService khachHangService;
    private final HoaDonService hoaDonService;
    private final HoaDonChiTietService hoaDonChiTietService;
    private final DotGiamGiaService dotGiamGiaService;
    private final TaiKhoanService taiKhoanService;
    private final GiamGiaService giamGiaService;
    private static final BigDecimal PHI_SHIP_MAC_DINH = BigDecimal.ZERO;
    private static final int MAX_HOA_DON_CHO = 5;

    public BanHangController(SanPhamChiTietService sanPhamChiTietService,
                             KhachHangService khachHangService,
                             HoaDonService hoaDonService,
                             HoaDonChiTietService hoaDonChiTietService,
                             DotGiamGiaService dotGiamGiaService,
                             SanPhamService sanPhamService,
                             TaiKhoanService taiKhoanService,
                             GiamGiaService giamGiaService) {
        this.sanPhamChiTietService = sanPhamChiTietService;
        this.khachHangService = khachHangService;
        this.hoaDonService = hoaDonService;
        this.hoaDonChiTietService = hoaDonChiTietService;
        this.dotGiamGiaService = dotGiamGiaService;
        this.sanPhamService = sanPhamService;
        this.taiKhoanService = taiKhoanService;
        this.giamGiaService = giamGiaService;
    }

    public BigDecimal tinhMucGiamVoucher(GiamGia gg, BigDecimal tongTien) {
        if (gg == null || tongTien == null || tongTien.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        System.out.println("=== TINH MUC GIAM VOUCHER ===");
        System.out.println("Ma: " + gg.getMaGiamGia());
        System.out.println("Loai: " + gg.getLoaiGiamGia());
        System.out.println("GiaTriGiam: " + gg.getGiaTriGiam());
        System.out.println("GiamToiDa: " + gg.getGiamToiDa());
        System.out.println("TongTien: " + tongTien);

        // ⭐ KIỂM TRA LOẠI GIẢM GIÁ
        if ("PhanTram".equalsIgnoreCase(gg.getLoaiGiamGia())) {
            BigDecimal phanTram = gg.getGiaTriGiam();
            if (phanTram == null || phanTram.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("-> Phan tram null hoac <= 0, tra ve 0");
                return BigDecimal.ZERO;
            }

            // ⭐ TÍNH GIẢM THEO PHẦN TRĂM
            BigDecimal giam = tongTien.multiply(phanTram).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

            // ⭐ KIỂM TRA GIẢM TỐI ĐA
            if (gg.getGiamToiDa() != null && gg.getGiamToiDa().compareTo(BigDecimal.ZERO) > 0) {
                giam = giam.min(gg.getGiamToiDa());
            }

            System.out.println("-> Giam tinh: " + giam);
            return giam;

        } else if ("SoTien".equalsIgnoreCase(gg.getLoaiGiamGia()) || "Tien".equalsIgnoreCase(gg.getLoaiGiamGia())) {
            BigDecimal giam = gg.getGiaTriGiam();
            if (giam == null) {
                System.out.println("-> Gia tri giam null, tra ve 0");
                return BigDecimal.ZERO;
            }

            // Kiểm tra giảm tối đa (nếu có)
            if (gg.getGiamToiDa() != null && gg.getGiamToiDa().compareTo(BigDecimal.ZERO) > 0) {
                giam = giam.min(gg.getGiamToiDa());
            }

            System.out.println("-> Giam tinh: " + giam);
            return giam;
        }

        System.out.println("-> Khong xac dinh loai giam gia, tra ve 0");
        return BigDecimal.ZERO;
    }

    private KhachHang getOrCreateKhachLe() {
        List<KhachHang> listKH = khachHangService.findAllBySdt("0000000000");

        if (listKH != null && !listKH.isEmpty()) {
            return listKH.get(0);
        }

        KhachHang khachLe = new KhachHang();
        khachLe.setMaKH("KH" + System.currentTimeMillis());
        khachLe.setHoTen("Khách lẻ");
        khachLe.setSdt("0000000000");
        khachLe.setEmail("khachle@fsshop.com");
        khachLe.setNgayDangKy(LocalDate.now());
        khachLe.setTrangThai(true);
        khachLe.setGioiTinh(true);

        khachHangService.save(khachLe);
        return khachLe;
    }

    @GetMapping("/index")
    public String index(@RequestParam(value = "mahd", required = false) String mahd,
                        @RequestParam(value = "qr", required = false) String qrData,
                        Model model) {

        System.out.println("========== BAN HANG INDEX ==========");
        System.out.println("📋 Loading page with mahd: " + mahd);

        List<DiaChi> listDiaChi = new ArrayList<>();

        List<HoaDon> hoaDonCho = hoaDonService.findByTrangThai("Đang xử lý");
        if (hoaDonCho == null) hoaDonCho = new ArrayList<>();
        if (hoaDonCho.size() > MAX_HOA_DON_CHO) {
            hoaDonCho = hoaDonCho.subList(0, MAX_HOA_DON_CHO);
        }

        HoaDon hoadonHienTai = null;
        List<HoaDonChiTiet> hdct = new ArrayList<>();
        BigDecimal tongTienGioHang = BigDecimal.ZERO;
        BigDecimal tienGiamVoucher = BigDecimal.ZERO;
        BigDecimal tongThanhToan = BigDecimal.ZERO;
        BigDecimal tienShip = BigDecimal.ZERO;
        String qrCodeBase64 = null;
        DiaChi diaChiMacDinh = null;

        if (mahd != null && !mahd.trim().isEmpty()) {
            hoadonHienTai = hoaDonService.findById(mahd);
            System.out.println("📄 Hoa don found: " + (hoadonHienTai != null ? "YES" : "NO"));

            if (hoadonHienTai != null) {
                List<HoaDonChiTiet> temp = hoaDonChiTietService.findById(mahd);
                hdct = (temp != null) ? temp : new ArrayList<>();
                System.out.println("📦 So luong san pham: " + hdct.size());

                // ⭐ TÍNH TỔNG TIỀN HÀNG
                tongTienGioHang = hdct.stream()
                        .map(HoaDonChiTiet::getThanhTien)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // ⭐ TÍNH TIỀN GIẢM VOUCHER
                tienGiamVoucher = tinhTienGiamVoucher(hoadonHienTai, tongTienGioHang);

                // ⭐ LẤY ĐỊA CHỈ
                if (hoadonHienTai.getMaKhachHang() != null) {
                    diaChiMacDinh = diaChiService.findDefaultByMaKH(
                            hoadonHienTai.getMaKhachHang().getMaKH()
                    );

                    if (diaChiMacDinh == null) {
                        List<DiaChi> danhSachDiaChi = diaChiService.findByKhachHang(
                                hoadonHienTai.getMaKhachHang()
                        );
                        if (danhSachDiaChi != null && !danhSachDiaChi.isEmpty()) {
                            diaChiMacDinh = danhSachDiaChi.get(0);
                        }
                    }

                    System.out.println("📍 Dia chi mac dinh: " + (diaChiMacDinh != null ? "YES" : "NO"));
                    if (diaChiMacDinh != null) {
                        System.out.println("   - " + diaChiMacDinh.getDiaChiCuThe() + ", " +
                                diaChiMacDinh.getPhuongXa() + ", " +
                                diaChiMacDinh.getQuanHuyen());
                    }

                    // ⭐⭐ LẤY DANH SÁCH ĐỊA CHỈ CỦA KHÁCH HÀNG ⭐⭐
                    KhachHang kh = hoadonHienTai.getMaKhachHang();
                    System.out.println("👤 Khách hàng: " + kh.getMaKH() + " - " + kh.getHoTen());

                    // Lấy tất cả địa chỉ của khách hàng
                    listDiaChi = diaChiService.findByKhachHang(kh);
                    System.out.println("📍 Số địa chỉ tìm thấy: " + (listDiaChi != null ? listDiaChi.size() : 0));

                    if (listDiaChi != null && !listDiaChi.isEmpty()) {
                        for (DiaChi dc : listDiaChi) {
                            System.out.println("   - ID: " + dc.getMaDiaChi() +
                                    ", Tên: " + dc.getTenNguoiNhan() +
                                    ", Địa chỉ: " + dc.getDiaChiCuThe() +
                                    ", Mặc định: " + dc.getDiaChiMacDinh());
                        }
                    }
                } else {
                    System.out.println("⚠️ Không có khách hàng");
                    listDiaChi = new ArrayList<>();
                }

                // ⭐ TÍNH PHÍ SHIP (CHỈ CHO ONLINE)
                if ("Online".equalsIgnoreCase(hoadonHienTai.getLoaiBan())) {
                    tienShip = tinhPhiShipGHN(hoadonHienTai);
                    if (tienShip == null || tienShip.compareTo(BigDecimal.ZERO) <= 0) {
                        tienShip = PHI_SHIP_MAC_DINH;
                    }
                    hoadonHienTai.setTienShip(tienShip);
                    System.out.println("🚚 Tien ship: " + tienShip);
                } else {
                    tienShip = BigDecimal.ZERO;
                    hoadonHienTai.setTienShip(BigDecimal.ZERO);
                }

                tongThanhToan = tongTienGioHang
                        .subtract(tienGiamVoucher != null ? tienGiamVoucher : BigDecimal.ZERO)
                        .add(tienShip != null ? tienShip : BigDecimal.ZERO);

                if (tongThanhToan.compareTo(BigDecimal.ZERO) < 0) {
                    tongThanhToan = BigDecimal.ZERO;
                }
                hoadonHienTai.setTongTien(tongThanhToan);
                hoaDonService.save(hoadonHienTai);

                System.out.println("💰 Tong tien hang: " + tongTienGioHang);
                System.out.println("💰 Tien giam voucher: " + tienGiamVoucher);
                System.out.println("🚚 Tien ship: " + tienShip);
                System.out.println("💰 Tong thanh toan: " + tongThanhToan);
                System.out.println("💰 Tien giam voucher sau khi tinh: " + tienGiamVoucher);
                System.out.println("💰 Voucher hien tai: " + (hoadonHienTai.getMaGiamGia() != null ? hoadonHienTai.getMaGiamGia().getMaGiamGia() : "null"));
            }
        }
        if (qrData != null && !qrData.isEmpty()) {
            qrCodeBase64 = generateQRCodeBase64(qrData);
        }

        List<SanPhamChiTiet> sanPhamList = sanPhamChiTietService.getallll();
        if (sanPhamList == null) sanPhamList = new ArrayList<>();

        Map<String, BigDecimal> mapGiamGia = new HashMap<>();
        Map<String, BigDecimal> mapGiaSauGiam = new HashMap<>();

        for (SanPhamChiTiet spct : sanPhamList) {
            BigDecimal giaGoc = spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO;
            BigDecimal giamGia = tinhGiamGiaSanPham(spct);
            BigDecimal giaSauGiam = giaGoc.subtract(giamGia).max(BigDecimal.ZERO);

            mapGiamGia.put(spct.getMaSanPhamChiTiet(), giamGia);
            mapGiaSauGiam.put(spct.getMaSanPhamChiTiet(), giaSauGiam);
        }

        List<GiamGia> listVoucherHoatDong = new ArrayList<>();
        GiamGia voucherTotNhat = null;

        if (hoadonHienTai != null) {
            KhachHang khachHang = hoadonHienTai.getMaKhachHang();

            if ("Online".equalsIgnoreCase(hoadonHienTai.getLoaiBan())) {
                tienShip = tinhPhiShipGHN(hoadonHienTai);
                if (tienShip == null || tienShip.compareTo(BigDecimal.ZERO) < 0) {
                    tienShip = BigDecimal.ZERO;
                }
                hoadonHienTai.setTienShip(tienShip);
                hoaDonService.save(hoadonHienTai);
                System.out.println("🚚 Tien ship: " + tienShip);
            } else {
                tienShip = BigDecimal.ZERO;
                hoadonHienTai.setTienShip(BigDecimal.ZERO);
            }

            if (khachHang != null && !"0000000000".equals(khachHang.getSdt())) {
                listVoucherHoatDong = getVoucherChoKhachHang(khachHang);
                System.out.println("👤 Khách hàng: " + khachHang.getHoTen() +
                        " (" + khachHang.getMaKH() + ") - Số voucher: " + listVoucherHoatDong.size());
            } else {
                listVoucherHoatDong = getVoucherCongKhai();
                System.out.println("👤 Khách lẻ - Số voucher: " + listVoucherHoatDong.size());
            }
            if (!listVoucherHoatDong.isEmpty() && !hdct.isEmpty()) {
                voucherTotNhat = timVoucherTotNhatChoHoaDon(hoadonHienTai, tongTienGioHang);
                if (voucherTotNhat != null) {
                    System.out.println("⭐ Voucher tốt nhất: " + voucherTotNhat.getMaGiamGia() +
                            " - " + voucherTotNhat.getTenGiamGia());
                }
            }
        } else {
            listVoucherHoatDong = getVoucherCongKhai();
            System.out.println("👤 Chưa có hóa đơn - Số voucher: " + listVoucherHoatDong.size());
        }

        model.addAttribute("listDiaChi", listDiaChi != null ? listDiaChi : new ArrayList<>());
        System.out.println("✅ Đã thêm listDiaChi vào model: " + (listDiaChi != null ? listDiaChi.size() : 0) + " địa chỉ");

        model.addAttribute("diaChi", new DiaChi());
        model.addAttribute("diaChiMacDinh", diaChiMacDinh);
        model.addAttribute("hoaDonCho", hoaDonCho);
        model.addAttribute("hoadonHienTai", hoadonHienTai);
        model.addAttribute("listhdct", hdct);
        model.addAttribute("tongTienGioHang", tongTienGioHang);
        model.addAttribute("tienGiamVoucher", tienGiamVoucher);
        model.addAttribute("tongThanhToan", tongThanhToan);
        model.addAttribute("tienShip", tienShip);
        model.addAttribute("qrCodeBase64", qrCodeBase64);
        model.addAttribute("hoadonct", new HoaDonChiTiet());
        model.addAttribute("kh", new KhachHang());
        model.addAttribute("hoadon", new HoaDon());

        model.addAttribute("listgg", listVoucherHoatDong);
        model.addAttribute("voucherTotNhat", voucherTotNhat);

        model.addAttribute("khachHangHienTai", hoadonHienTai != null ? hoadonHienTai.getMaKhachHang() : null);

        model.addAttribute("listkh", khachHangService.getkh());
        model.addAttribute("listsanpham", sanPhamList);
        model.addAttribute("listsanphamms", sanPhamChiTietService.getMsac());
        model.addAttribute("listsanphams", sanPhamChiTietService.getSize());
        model.addAttribute("mapGiamGia", mapGiamGia);
        model.addAttribute("mapGiaSauGiam", mapGiaSauGiam);

        System.out.println("📋 Tổng số voucher hiển thị: " + listVoucherHoatDong.size());
        System.out.println("========== END INDEX ==========");
        return "banhang/index";
    }

    @GetMapping("/hoa-don-chi-tiet")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHoaDonChiTiet(
            @RequestParam("mahd") String maHoaDon) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Lấy hóa đơn
            HoaDon hoaDon = hoaDonService.findById(maHoaDon);
            if (hoaDon == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy hóa đơn");
                return ResponseEntity.ok(response);
            }

            // 2. Lấy chi tiết hóa đơn
            List<HoaDonChiTiet> chiTietList = hoaDonChiTietService.findById(maHoaDon);
            if (chiTietList == null) chiTietList = new ArrayList<>();

            // 3. Tính tổng tiền
            BigDecimal tongTienHang = chiTietList.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 4. Tính tiền giảm voucher
            BigDecimal tienGiamVoucher = tinhTienGiamVoucher(hoaDon, tongTienHang);

            // 5. Tính phí ship
            BigDecimal tienShip = BigDecimal.ZERO;
            if ("Online".equalsIgnoreCase(hoaDon.getLoaiBan())) {
                tienShip = tinhPhiShipGHN(hoaDon);
                if (tienShip == null || tienShip.compareTo(BigDecimal.ZERO) <= 0) {
                    tienShip = PHI_SHIP_MAC_DINH;
                }
            }

            // 6. Tính tổng thanh toán
            BigDecimal tongThanhToan = tongTienHang
                    .subtract(tienGiamVoucher != null ? tienGiamVoucher : BigDecimal.ZERO)
                    .add(tienShip != null ? tienShip : BigDecimal.ZERO);

            if (tongThanhToan.compareTo(BigDecimal.ZERO) < 0) {
                tongThanhToan = BigDecimal.ZERO;
            }

            // 7. Lấy địa chỉ mặc định (nếu có)
            DiaChi diaChiMacDinh = null;
            List<DiaChi> listDiaChi = new ArrayList<>();
            if (hoaDon.getMaKhachHang() != null) {
                diaChiMacDinh = diaChiService.findDefaultByMaKH(
                        hoaDon.getMaKhachHang().getMaKH()
                );

                if (diaChiMacDinh == null) {
                    List<DiaChi> danhSachDiaChi = diaChiService.findByKhachHang(
                            hoaDon.getMaKhachHang()
                    );
                    if (danhSachDiaChi != null && !danhSachDiaChi.isEmpty()) {
                        diaChiMacDinh = danhSachDiaChi.get(0);
                    }
                }

                listDiaChi = diaChiService.findByKhachHang(hoaDon.getMaKhachHang());
            }

            // 8. Lấy voucher tốt nhất
            List<GiamGia> listVoucher = new ArrayList<>();
            GiamGia voucherTotNhat = null;

            if (hoaDon.getMaKhachHang() != null &&
                    !"0000000000".equals(hoaDon.getMaKhachHang().getSdt())) {
                listVoucher = getVoucherChoKhachHang(hoaDon.getMaKhachHang());
            } else {
                listVoucher = getVoucherCongKhai();
            }

            if (!listVoucher.isEmpty() && !chiTietList.isEmpty()) {
                voucherTotNhat = timVoucherTotNhatChoHoaDon(hoaDon, tongTienHang);
            }

            // 9. Build response data
            Map<String, Object> data = new HashMap<>();
            data.put("maHoaDon", hoaDon.getMaHoaDon());
            data.put("loaiBan", hoaDon.getLoaiBan());
            data.put("trangThai", hoaDon.getTrangThai());
            data.put("ngayTao", hoaDon.getNgayTao() != null ?
                    hoaDon.getNgayTao().format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy")) : "");
            data.put("tongTienHang", tongTienHang);
            data.put("tienGiamVoucher", tienGiamVoucher);
            data.put("tienShip", tienShip);
            data.put("tongThanhToan", tongThanhToan);
            data.put("phuongThucThanhToan", hoaDon.getPhuongThucThanhToan());
            data.put("ghiChu", hoaDon.getGhiChu());
            data.put("diaChiGiaoHang", hoaDon.getDiaChiGiaoHang());

            // Thông tin khách hàng
            if (hoaDon.getMaKhachHang() != null) {
                Map<String, Object> khachHang = new HashMap<>();
                khachHang.put("maKH", hoaDon.getMaKhachHang().getMaKH());
                khachHang.put("ten", hoaDon.getMaKhachHang().getHoTen());
                khachHang.put("sdt", hoaDon.getMaKhachHang().getSdt());
                data.put("khachHang", khachHang);
            }

            // Thông tin nhân viên
            if (hoaDon.getMaNhanVien() != null) {
                Map<String, Object> nhanVien = new HashMap<>();
                nhanVien.put("ten", hoaDon.getMaNhanVien().getHoTen());
                data.put("nhanVien", nhanVien);
            }

            // Chi tiết sản phẩm
            List<Map<String, Object>> chiTietSanPham = new ArrayList<>();
            for (HoaDonChiTiet ct : chiTietList) {
                Map<String, Object> ctMap = new HashMap<>();
                ctMap.put("maSanPhamChiTiet", ct.getSanPhamChiTiet() != null ?
                        ct.getSanPhamChiTiet().getMaSanPhamChiTiet() : "");
                ctMap.put("tenSanPham", ct.getSanPhamChiTiet() != null &&
                        ct.getSanPhamChiTiet().getSanPham() != null ?
                        ct.getSanPhamChiTiet().getSanPham().getTenSanPham() : "Không xác định");
                ctMap.put("soLuong", ct.getSoLuong());
                ctMap.put("donGia", ct.getDonGia());
                ctMap.put("thanhTien", ct.getThanhTien());
                ctMap.put("tienGiam", ct.getTienGiam());

                // Thêm màu sắc, kích thước nếu có
                if (ct.getSanPhamChiTiet() != null) {
                    if (ct.getSanPhamChiTiet().getMauSac() != null) {
                        ctMap.put("mauSac", ct.getSanPhamChiTiet().getMauSac().getTenMauSac());
                    }
                    if (ct.getSanPhamChiTiet().getKichThuoc() != null) {
                        ctMap.put("kichThuoc", ct.getSanPhamChiTiet().getKichThuoc().getTenKichThuoc());
                    }
                }

                chiTietSanPham.add(ctMap);
            }
            data.put("chiTietSanPham", chiTietSanPham);

            // Địa chỉ
            data.put("diaChiMacDinh", diaChiMacDinh);
            data.put("listDiaChi", listDiaChi);

            // Voucher
            data.put("listVoucher", listVoucher);
            data.put("voucherTotNhat", voucherTotNhat);

            response.put("success", true);
            response.put("data", data);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/taohd/ajax")
    @ResponseBody
    public Map<String, Object> taoHoaDonAjax(@RequestParam("loaiBan") String loaiBan,
                                             Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Kiểm tra đăng nhập
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập!");
                return response;
            }

            // Kiểm tra số lượng hóa đơn chờ
            List<HoaDon> hoaDonCho = hoaDonService.findByTrangThai("Đang xử lý");
            if (hoaDonCho != null && hoaDonCho.size() >= MAX_HOA_DON_CHO) {
                response.put("success", false);
                response.put("message", "Đã đạt tối đa " + MAX_HOA_DON_CHO + " hóa đơn chờ!");
                return response;
            }

            // Lấy thông tin nhân viên
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            TaiKhoan account = userDetails.getTaiKhoan();

            if (account.getNhanVien() == null) {
                response.put("success", false);
                response.put("message", "Tài khoản chưa được gán nhân viên!");
                return response;
            }

            // Tạo hóa đơn mới
            KhachHang khachLe = getOrCreateKhachLe();
            String maHoaDon = taoMaHoaDon();

            HoaDon hoaDon = new HoaDon();
            hoaDon.setMaHoaDon(maHoaDon);
            hoaDon.setNgayTao(LocalDateTime.now());
            hoaDon.setTrangThai("Đang xử lý");
            hoaDon.setLoaiBan(loaiBan);
            hoaDon.setMaNhanVien(account.getNhanVien());
            hoaDon.setMaKhachHang(khachLe);
            hoaDon.setTienShip(BigDecimal.ZERO);

            HoaDon hdVuaLuu = hoaDonService.save(hoaDon);

            response.put("success", true);
            response.put("message", "Tạo hoá đơn " + loaiBan + " thành công!");
            response.put("maHD", hdVuaLuu.getMaHoaDon());
            response.put("maHoaDon", hdVuaLuu.getMaHoaDon());

            return response;

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi tạo hóa đơn: " + e.getMessage());
            return response;
        }
    }

    @PostMapping("/taohd")
    public String taoHoaDon(@ModelAttribute("hoadon") HoaDon hoaDon,
                            @RequestParam("loaiBan") String loaiBan,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("mess", "Vui lòng đăng nhập!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/login";
        }

        try {
            List<HoaDon> hoaDonCho = hoaDonService.findByTrangThai("Đang xử lý");
            if (hoaDonCho != null && hoaDonCho.size() >= MAX_HOA_DON_CHO) {
                redirectAttributes.addFlashAttribute("mess",
                        "Đã đạt tối đa " + MAX_HOA_DON_CHO + " hóa đơn chờ! Vui lòng xử lý hoặc hủy bớt.");
                redirectAttributes.addFlashAttribute("messageType", "warning");
                return "redirect:/banhang/index";
            }

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof CustomUserDetails)) {
                redirectAttributes.addFlashAttribute("mess", "Lỗi xác thực người dùng!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/login";
            }

            CustomUserDetails userDetails = (CustomUserDetails) principal;
            TaiKhoan account = userDetails.getTaiKhoan();

            if (account.getNhanVien() == null) {
                redirectAttributes.addFlashAttribute("mess", "Tài khoản chưa được gán nhân viên!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/banhang/index";
            }

            KhachHang khachLe = getOrCreateKhachLe();

            String maHoaDon = taoMaHoaDon();
            hoaDon.setMaHoaDon(maHoaDon);
            hoaDon.setNgayTao(LocalDateTime.now());
            hoaDon.setTrangThai("Đang xử lý");
            hoaDon.setLoaiBan(loaiBan);
            hoaDon.setMaNhanVien(account.getNhanVien());
            hoaDon.setMaKhachHang(khachLe);
            hoaDon.setTienShip(BigDecimal.ZERO);

            HoaDon hdVuaLuu = hoaDonService.save(hoaDon);

            redirectAttributes.addFlashAttribute("mess",
                    "Tạo hóa đơn " + loaiBan + " thành công! Mã: " + hdVuaLuu.getMaHoaDon());
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/banhang/index?mahd=" + hdVuaLuu.getMaHoaDon();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mess", "Lỗi tạo hóa đơn: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/banhang/index";
        }
    }

    @GetMapping("/get-ds-sanpham-modal")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDSSanPhamModal(
            @RequestParam("mahd") String maHD) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<SanPhamChiTiet> sanPhamList = sanPhamChiTietService.getallll();
            List<Map<String, Object>> sanPhamData = new ArrayList<>();
            for (SanPhamChiTiet sp : sanPhamList) {
                int tonKho = sp.getSoLuongTon();

                Map<String, Object> item = new HashMap<>();
                item.put("maSpct", sp.getMaSanPhamChiTiet());
                item.put("tenSanPham", sp.getSanPham().getTenSanPham());
                item.put("mauSac", sp.getMauSac().getTenMauSac());
                item.put("kichThuoc", sp.getKichThuoc().getTenKichThuoc());
                item.put("giaBan", sp.getGiaBan());
                item.put("giaSauGiam", sp.getGiaBan());
                item.put("tonKho", tonKho);
                item.put("ngayTao", sp.getNgayTao());

                sanPhamData.add(item);
            }

            response.put("success", true);
            response.put("data", sanPhamData);
            response.put("total", sanPhamData.size());

            return ResponseEntity.ok()
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .body(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .body(response);
        }
    }

    private String taoMaHoaDon() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return "HD" + LocalDateTime.now().format(formatter);
    }

    @PostMapping("/themsphd")
    public String themSanPhamVaoHoaDon(@RequestParam("mahd") String mahd,
                                       @RequestParam("mactsp") String mactsp,
                                       @RequestParam("sluong") Integer sluong,
                                       RedirectAttributes redirectAttributes) {

        if (mahd == null || mahd.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("mess", "Vui lòng tạo hoặc chọn hóa đơn trước!");
            redirectAttributes.addFlashAttribute("messageType", "warning");
            return "redirect:/banhang/index";
        }

        if (sluong == null || sluong <= 0) {
            redirectAttributes.addFlashAttribute("mess", "Số lượng phải lớn hơn 0!");
            redirectAttributes.addFlashAttribute("messageType", "warning");
            return "redirect:/banhang/index?mahd=" + mahd;
        }

        HoaDon hdd = hoaDonService.findById(mahd);
        if (hdd == null) {
            redirectAttributes.addFlashAttribute("mess", "Hóa đơn không tồn tại!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/banhang/index";
        }

        if ("Đã thanh toán".equals(hdd.getTrangThai()) || "Đã huỷ".equals(hdd.getTrangThai())) {
            redirectAttributes.addFlashAttribute("mess",
                    "Không thể thêm sản phẩm. Hóa đơn đã " + hdd.getTrangThai() + "!");
            redirectAttributes.addFlashAttribute("messageType", "warning");
            return "redirect:/banhang/index?mahd=" + mahd;
        }

        SanPhamChiTiet spct = sanPhamChiTietService.findbyId(mactsp).orElse(null);
        if (spct == null) {
            redirectAttributes.addFlashAttribute("mess", "Sản phẩm không tồn tại!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/banhang/index?mahd=" + mahd;
        }

        if (spct.getSoLuongTon() == null || spct.getSoLuongTon() <= 0) {
            redirectAttributes.addFlashAttribute("mess", "Sản phẩm đã hết hàng!");
            redirectAttributes.addFlashAttribute("messageType", "warning");
            return "redirect:/banhang/index?mahd=" + mahd;
        }

        try {
            HoaDonChiTiet hdct = hoaDonChiTietService.findAll(mahd, mactsp);

            BigDecimal giaGoc = spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO;
            BigDecimal giamDonVi = tinhGiamGiaSanPham(spct);
            BigDecimal giaDonVi = giaGoc.subtract(giamDonVi).max(BigDecimal.ZERO);

            if (hdct == null) {
                if (sluong > spct.getSoLuongTon()) {
                    redirectAttributes.addFlashAttribute("mess",
                            "Số lượng vượt tồn kho! Còn: " + spct.getSoLuongTon());
                    redirectAttributes.addFlashAttribute("messageType", "warning");
                    return "redirect:/banhang/index?mahd=" + mahd;
                }

                hdct = new HoaDonChiTiet();
                hdct.setMaHoaDon(hdd);
                hdct.setSanPhamChiTiet(spct);
                hdct.setSoLuong(sluong);
                hdct.setDonGia(giaDonVi);
                hdct.setTienGiam(giamDonVi.multiply(BigDecimal.valueOf(sluong)));
                hdct.setThanhTien(giaDonVi.multiply(BigDecimal.valueOf(sluong)));

                spct.setSoLuongTon(spct.getSoLuongTon() - sluong);

            } else {
                int slMoi = hdct.getSoLuong() + sluong;
                int tonKhoThuc = spct.getSoLuongTon();

                if (slMoi > tonKhoThuc + hdct.getSoLuong()) {
                    redirectAttributes.addFlashAttribute("mess",
                            "Chỉ có thể thêm tối đa " + tonKhoThuc + " sản phẩm nữa!");
                    redirectAttributes.addFlashAttribute("messageType", "warning");
                    return "redirect:/banhang/index?mahd=" + mahd;
                }

                hdct.setSoLuong(slMoi);
                hdct.setDonGia(giaDonVi);
                hdct.setTienGiam(giamDonVi.multiply(BigDecimal.valueOf(slMoi)));
                hdct.setThanhTien(giaDonVi.multiply(BigDecimal.valueOf(slMoi)));

                spct.setSoLuongTon(spct.getSoLuongTon() - sluong);
            }

            sanPhamChiTietService.capNhatTrangThaii(spct);
            sanPhamChiTietService.them(spct);
            hoaDonChiTietService.luu(hdct);

            if ("Online".equalsIgnoreCase(hdd.getLoaiBan())) {
                BigDecimal shipMoi = tinhPhiShipGHN(hdd);
                if (shipMoi != null && shipMoi.compareTo(BigDecimal.ZERO) > 0) {
                    hdd.setTienShip(shipMoi);
                    hoaDonService.save(hdd);
                }
            }

            redirectAttributes.addFlashAttribute("mess",
                    "Thêm sản phẩm thành công! Đã trừ " + sluong + " SP khỏi kho.");
            redirectAttributes.addFlashAttribute("messageType", "success");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("mess", "Lỗi khi thêm sản phẩm: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        BigDecimal giaGoc = spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO;
        BigDecimal giamDonVi = tinhGiamGiaSanPham(spct);
        BigDecimal giaDonVi = giaGoc.subtract(giamDonVi).max(BigDecimal.ZERO);

        System.out.println("=== THEM SAN PHAM ===");
        System.out.println("Gia goc: " + giaGoc);
        System.out.println("Giam don vi: " + giamDonVi);
        System.out.println("Gia don vi sau giam: " + giaDonVi);
        System.out.println("So luong: " + sluong);
        System.out.println("Thanh tien: " + giaDonVi.multiply(BigDecimal.valueOf(sluong)));
        return "redirect:/banhang/index?mahd=" + mahd;
    }

    @PostMapping("/themsphd-ajax")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> themSanPhamAjax(@RequestParam("mahd") String mahd,
                                             @RequestParam("mactsp") String mactsp,
                                             @RequestParam("sluong") Integer sluong) {
        try {
            System.out.println("=== THEM SAN PHAM AJAX ===");
            System.out.println("mahd: " + mahd);
            System.out.println("mactsp: " + mactsp);
            System.out.println("sluong: " + sluong);

            if (mahd == null || mahd.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Vui lòng tạo hoặc chọn hóa đơn trước!"
                ));
            }

            if (sluong == null || sluong <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Số lượng phải lớn hơn 0!"
                ));
            }

            HoaDon hdd = hoaDonService.findById(mahd);
            if (hdd == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Hóa đơn không tồn tại!"
                ));
            }

            if ("Đã thanh toán".equals(hdd.getTrangThai()) || "Đã huỷ".equals(hdd.getTrangThai())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không thể thêm sản phẩm. Hóa đơn đã " + hdd.getTrangThai() + "!"
                ));
            }

            // ⭐ 2. KIỂM TRA SẢN PHẨM
            SanPhamChiTiet spct = sanPhamChiTietService.findbyId(mactsp).orElse(null);
            if (spct == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Sản phẩm không tồn tại!"
                ));
            }

            if (spct.getSoLuongTon() == null || spct.getSoLuongTon() <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Sản phẩm đã hết hàng!"
                ));
            }

            // ⭐ 3. TÍNH GIÁ
            BigDecimal giaGoc = spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO;
            BigDecimal giamDonVi = tinhGiamGiaSanPham(spct);
            BigDecimal giaDonVi = giaGoc.subtract(giamDonVi).max(BigDecimal.ZERO);

            // ⭐ 4. KIỂM TRA SẢN PHẨM ĐÃ CÓ TRONG GIỎ HÀNG CHƯA
            HoaDonChiTiet hdct = hoaDonChiTietService.findAll(mahd, mactsp);
            int tonKhoTruocKhiBan = spct.getSoLuongTon();

            if (hdct == null) {
                // ⭐ 4A. THÊM MỚI
                System.out.println("📦 Sản phẩm chưa có trong giỏ -> THÊM MỚI");

                if (sluong > spct.getSoLuongTon()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Số lượng vượt tồn kho! Còn: " + spct.getSoLuongTon()
                    ));
                }

                hdct = new HoaDonChiTiet();
                hdct.setMaHoaDon(hdd);
                hdct.setSanPhamChiTiet(spct);
                hdct.setSoLuong(sluong);
                hdct.setDonGia(giaDonVi);
                hdct.setTienGiam(giamDonVi.multiply(BigDecimal.valueOf(sluong)));
                hdct.setThanhTien(giaDonVi.multiply(BigDecimal.valueOf(sluong)));

                spct.setSoLuongTon(spct.getSoLuongTon() - sluong);
                sanPhamChiTietService.them(spct);
                hoaDonChiTietService.luu(hdct);

            } else {
                System.out.println("📦 Sản phẩm đã có trong giỏ -> CẬP NHẬT SỐ LƯỢNG");

                int slHienTai = hdct.getSoLuong();
                int slMoi = slHienTai + sluong;
                int tonKhoHienTai = spct.getSoLuongTon();
                int tongTonKho = tonKhoHienTai + slHienTai;

                System.out.println("=== DEBUG ===");
                System.out.println("Số lượng hiện tại trong giỏ: " + slHienTai);
                System.out.println("Số lượng muốn thêm: " + sluong);
                System.out.println("Số lượng mới: " + slMoi);
                System.out.println("Tồn kho hiện tại: " + tonKhoHienTai);
                System.out.println("Tổng tồn kho: " + tongTonKho);

                if (slMoi > tongTonKho) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Số lượng vượt quá tồn kho! Tối đa: " + tongTonKho
                    ));
                }
                hdct.setSoLuong(slMoi);
                hdct.setDonGia(giaDonVi);
                hdct.setTienGiam(giamDonVi.multiply(BigDecimal.valueOf(slMoi)));
                hdct.setThanhTien(giaDonVi.multiply(BigDecimal.valueOf(slMoi)));
                spct.setSoLuongTon(tonKhoHienTai - sluong);
                sanPhamChiTietService.them(spct);
                hoaDonChiTietService.luu(hdct);
            }

            SanPhamChiTiet spctDaCapNhat = sanPhamChiTietService.kiemTraVaCapNhatTrangThaiSauKhiBan(mactsp);

            boolean daHetHang = false;
            String trangThaiMoi = "Còn hàng";
            if (spctDaCapNhat != null) {
                trangThaiMoi = spctDaCapNhat.getTrangThai();
                daHetHang = "Hết hàng".equals(trangThaiMoi);
                if (daHetHang) {
                    System.out.println("🔥🔥🔥 SẢN PHẨM " + mactsp + " ĐÃ HẾT HÀNG! 🔥🔥🔥");
                }
            }

            // ⭐ 6. CẬP NHẬT PHÍ SHIP (NẾU LÀ ĐƠN ONLINE)
            if ("Online".equalsIgnoreCase(hdd.getLoaiBan())) {
                BigDecimal shipMoi = tinhPhiShipGHN(hdd);
                if (shipMoi == null || shipMoi.compareTo(BigDecimal.ZERO) < 0) {
                    shipMoi = BigDecimal.ZERO;
                }
                hdd.setTienShip(shipMoi);
                hoaDonService.save(hdd);
            }

            // ⭐ 7. LẤY DỮ LIỆU GIỎ HÀNG MỚI
            List<HoaDonChiTiet> chiTiets = hoaDonChiTietRepository.findByMaHoaDon_MaHoaDon(mahd);
            List<Map<String, Object>> danhSachSanPham = new ArrayList<>();
            BigDecimal tongTienHang = BigDecimal.ZERO;

            for (HoaDonChiTiet ct : chiTiets) {
                Map<String, Object> item = new HashMap<>();
                SanPhamChiTiet sp = ct.getSanPhamChiTiet();
                item.put("maSanPhamChiTiet", sp.getMaSanPhamChiTiet());
                item.put("tenSanPham", sp.getSanPham().getTenSanPham()
                        + " [" + sp.getMauSac().getTenMauSac()
                        + " - " + sp.getKichThuoc().getTenKichThuoc() + "]");
                item.put("soLuong", ct.getSoLuong());
                item.put("donGia", ct.getDonGia());
                item.put("thanhTien", ct.getThanhTien());
                item.put("tonKho", sp.getSoLuongTon() != null ? sp.getSoLuongTon() : 0);
                danhSachSanPham.add(item);
                tongTienHang = tongTienHang.add(ct.getThanhTien());
            }

            // ⭐ 8. TÍNH TIỀN GIẢM VOUCHER
            BigDecimal tienGiamVoucher = BigDecimal.ZERO;
            GiamGia voucher = hdd.getMaGiamGia();

            if (voucher != null) {
                tienGiamVoucher = tinhMucGiamVoucher(voucher, tongTienHang);
            }

            // ⭐ 9. TÍNH TỔNG TIỀN (BAO GỒM SHIP)
            BigDecimal tienShip = hdd.getTienShip() != null ? hdd.getTienShip() : BigDecimal.ZERO;
            BigDecimal tongTien = tongTienHang.subtract(tienGiamVoucher).add(tienShip);

            // Cập nhật tổng tiền vào hóa đơn
            hdd.setTongTien(tongTien);
            hoaDonService.save(hdd);

            // ⭐ 10. TRẢ VỀ RESPONSE
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Thêm sản phẩm thành công!");
            response.put("danhSachSanPham", danhSachSanPham);
            response.put("tongTienHang", tongTienHang);
            response.put("tienGiamVoucher", tienGiamVoucher);
            response.put("tienShip", tienShip);
            response.put("tongTien", tongTien);
            response.put("tongTienMoi", tongTien);
            response.put("maHoaDon", mahd);
            response.put("soLuongSanPham", chiTiets.size());

            // ⭐ THÊM THÔNG TIN TRẠNG THÁI SẢN PHẨM
            response.put("daHetHang", daHetHang);
            response.put("trangThaiSanPham", trangThaiMoi);
            response.put("tonKhoConLai", spct.getSoLuongTon());

            if (voucher != null) {
                Map<String, Object> voucherInfo = new HashMap<>();
                voucherInfo.put("maGiamGia", voucher.getMaGiamGia());
                voucherInfo.put("tenGiamGia", voucher.getTenGiamGia());
                voucherInfo.put("tienGiam", tienGiamVoucher);
                response.put("voucher", voucherInfo);
            }

            System.out.println("✅ Thêm sản phẩm thành công!");
            System.out.println("   Tổng tiền hàng: " + tongTienHang);
            System.out.println("   Tiền giảm voucher: " + tienGiamVoucher);
            System.out.println("   Tiền ship: " + tienShip);
            System.out.println("   Tổng tiền: " + tongTien);
            System.out.println("   Trạng thái sản phẩm: " + trangThaiMoi);
            System.out.println("=== END THEM SAN PHAM AJAX ===");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/giamsp-ajax")
    @ResponseBody
    public ResponseEntity<?> giamSanPhamAjax(@RequestParam("mahd") String mahd,
                                             @RequestParam("mactsp") String mactsp,
                                             @RequestParam(value = "sluong", defaultValue = "1") Integer sluong) {
        try {
            System.out.println("=== GIẢM SẢN PHẨM ===");
            System.out.println("mahd: " + mahd);
            System.out.println("mactsp: " + mactsp);
            System.out.println("sluong: " + sluong);

            HoaDon hdd = hoaDonService.findById(mahd);
            if (hdd == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Hóa đơn không tồn tại!"
                ));
            }

            HoaDonChiTiet hdct = hoaDonChiTietService.findAll(mahd, mactsp);
            if (hdct == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Sản phẩm không có trong hóa đơn!"
                ));
            }

            int soLuongHienTai = hdct.getSoLuong();
            int soLuongGiam = Math.min(sluong, soLuongHienTai);

            if (soLuongGiam <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Số lượng giảm phải lớn hơn 0!"
                ));
            }

            int soLuongMoi = soLuongHienTai - soLuongGiam;

            if (soLuongMoi <= 0) {
                // ⭐ XÓA SẢN PHẨM
                SanPhamChiTiet spct = hdct.getSanPhamChiTiet();
                spct.setSoLuongTon(spct.getSoLuongTon() + soLuongHienTai);
                sanPhamChiTietService.them(spct);
                hoaDonChiTietService.xoa(hdct.getId());

                // ⭐ CẬP NHẬT TRẠNG THÁI SẢN PHẨM (Có thể từ Hết hàng -> Còn hàng)
                sanPhamChiTietService.kiemTraVaCapNhatTrangThaiSauKhiBan(mactsp);

            } else {
                // ⭐ GIẢM SỐ LƯỢNG
                hdct.setSoLuong(soLuongMoi);

                BigDecimal giaDonVi = hdct.getDonGia();
                BigDecimal giamDonVi = hdct.getTienGiam().divide(BigDecimal.valueOf(soLuongHienTai), 2, RoundingMode.HALF_UP);
                hdct.setTienGiam(giamDonVi.multiply(BigDecimal.valueOf(soLuongMoi)));
                hdct.setThanhTien(giaDonVi.multiply(BigDecimal.valueOf(soLuongMoi)));

                SanPhamChiTiet spct = hdct.getSanPhamChiTiet();
                spct.setSoLuongTon(spct.getSoLuongTon() + soLuongGiam);
                sanPhamChiTietService.them(spct);
                hoaDonChiTietService.luu(hdct);

                // ⭐ CẬP NHẬT TRẠNG THÁI SẢN PHẨM
                sanPhamChiTietService.kiemTraVaCapNhatTrangThaiSauKhiBan(mactsp);
            }

            // ⭐ CẬP NHẬT PHÍ SHIP (NẾU LÀ ĐƠN ONLINE)
            if ("Online".equalsIgnoreCase(hdd.getLoaiBan())) {
                BigDecimal shipMoi = tinhPhiShipGHN(hdd);
                if (shipMoi == null || shipMoi.compareTo(BigDecimal.ZERO) < 0) {
                    shipMoi = BigDecimal.ZERO;
                }
                hdd.setTienShip(shipMoi);
                hoaDonService.save(hdd);
            }

            // Cập nhật tổng tiền
            capNhatTongTienHoaDon(mahd);

            // Lấy dữ liệu giỏ hàng
            Map<String, Object> response = getGioHangData(mahd);
            response.put("success", true);
            response.put("message", "Đã giảm " + soLuongGiam + " sản phẩm!");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/xoasp")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> xoaSanPham(@RequestParam("mahd") String mahd,
                                        @RequestParam("mactsp") String mactsp) {
        try {
            System.out.println("=== XÓA SẢN PHẨM ===");
            System.out.println("mahd: " + mahd);
            System.out.println("mactsp: " + mactsp);

            HoaDon hdd = hoaDonService.findById(mahd);
            if (hdd == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Hóa đơn không tồn tại!"
                ));
            }

            HoaDonChiTiet hdct = hoaDonChiTietService.findAll(mahd, mactsp);
            if (hdct == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Sản phẩm không có trong hóa đơn!"
                ));
            }

            int soLuongHoan = hdct.getSoLuong();

            // Hoàn lại tồn kho
            SanPhamChiTiet spct = hdct.getSanPhamChiTiet();
            spct.setSoLuongTon(spct.getSoLuongTon() + soLuongHoan);
            sanPhamChiTietService.them(spct);

            // Xóa chi tiết
            hoaDonChiTietService.xoa(hdct.getId());

            // ⭐ CẬP NHẬT TRẠNG THÁI SẢN PHẨM (Có thể từ Hết hàng -> Còn hàng)
            sanPhamChiTietService.kiemTraVaCapNhatTrangThaiSauKhiBan(mactsp);

            // ⭐ CẬP NHẬT PHÍ SHIP (NẾU LÀ ĐƠN ONLINE)
            if ("Online".equalsIgnoreCase(hdd.getLoaiBan())) {
                BigDecimal shipMoi = tinhPhiShipGHN(hdd);
                if (shipMoi == null || shipMoi.compareTo(BigDecimal.ZERO) < 0) {
                    shipMoi = BigDecimal.ZERO;
                }
                hdd.setTienShip(shipMoi);
                hoaDonService.save(hdd);
            }

            // Cập nhật tổng tiền
            capNhatTongTienHoaDon(mahd);

            // Lấy dữ liệu giỏ hàng
            Map<String, Object> response = getGioHangData(mahd);
            response.put("success", true);
            response.put("message", "Đã xóa sản phẩm!");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }


    @PostMapping("/giamsp")
    public String giamSoLuongSanPham(@RequestParam("mahd") String mahd,
                                     @RequestParam("mactsp") String mactsp,
                                     RedirectAttributes redirectAttributes) {

        HoaDonChiTiet hdct = hoaDonChiTietService.findAll(mahd, mactsp);
        if (hdct == null) {
            redirectAttributes.addFlashAttribute("mess", "Không tìm thấy sản phẩm trong hóa đơn!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/banhang/index?mahd=" + mahd;
        }

        SanPhamChiTiet spct = hdct.getSanPhamChiTiet();
        int slMoi = hdct.getSoLuong() - 1;

        if (slMoi <= 0) {
            spct.setSoLuongTon(spct.getSoLuongTon() + hdct.getSoLuong());
            sanPhamChiTietService.capNhatTrangThaii(spct);
            sanPhamChiTietService.them(spct);
            hoaDonChiTietService.xoa(hdct);
            redirectAttributes.addFlashAttribute("mess",
                    "Đã xóa sản phẩm khỏi hóa đơn và hoàn lại kho!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } else {
            spct.setSoLuongTon(spct.getSoLuongTon() + 1);
            sanPhamChiTietService.capNhatTrangThaii(spct);
            sanPhamChiTietService.them(spct);

            BigDecimal giaDonVi = hdct.getDonGia() != null ? hdct.getDonGia() : BigDecimal.ZERO;
            BigDecimal giamDonVi = timGiamGiaTotNhat(spct.getSanPham().getMaSanPham());

            hdct.setSoLuong(slMoi);
            hdct.setThanhTien(giaDonVi.multiply(BigDecimal.valueOf(slMoi)));
            hdct.setTienGiam(giamDonVi.multiply(BigDecimal.valueOf(slMoi)));
            hoaDonChiTietService.luu(hdct);

            redirectAttributes.addFlashAttribute("mess",
                    "Đã giảm số lượng và hoàn 1 SP vào kho!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        }

        return "redirect:/banhang/index?mahd=" + mahd;
    }


    @PostMapping("/chongg")
    public String chonGiamGia(@RequestParam("mahd") String mahd,
                              @RequestParam("magg") String magg,
                              RedirectAttributes redirectAttributes) {
        try {
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                redirectAttributes.addFlashAttribute("mess", "Không tìm thấy hóa đơn!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/banhang/index";
            }

            GiamGia giamGia = giamGiaService.getGiamGiaById(magg).orElse(null);
            if (giamGia == null) {
                redirectAttributes.addFlashAttribute("mess", "Mã giảm giá không tồn tại!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/banhang/index?mahd=" + mahd;
            }

            // ⭐ Kiểm tra vô hạn
            if (giamGia.getIsVoHan() == null || !giamGia.getIsVoHan()) {
                if (giamGia.getSoLuong() == null || giamGia.getSoLuong() <= 0) {
                    redirectAttributes.addFlashAttribute("mess", "Mã giảm giá đã hết lượt sử dụng!");
                    redirectAttributes.addFlashAttribute("messageType", "warning");
                    return "redirect:/banhang/index?mahd=" + mahd;
                }
            }

            String trangThai = giamGiaService.tinhToanTrangThai(giamGia);
            if (!"Hoạt động".equals(trangThai)) {
                redirectAttributes.addFlashAttribute("mess", "Mã giảm giá không còn hiệu lực!");
                redirectAttributes.addFlashAttribute("messageType", "warning");
                return "redirect:/banhang/index?mahd=" + mahd;
            }

            hoaDon.setMaGiamGia(giamGia);
            hoaDonService.save(hoaDon);

            // ⭐ TÍNH LẠI TIỀN GIẢM VOUCHER
            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            BigDecimal tongTien = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal tienGiam = tinhMucGiamVoucher(giamGia, tongTien);

            redirectAttributes.addFlashAttribute("mess",
                    "Đã áp dụng mã: " + giamGia.getTenGiamGia() + " (Giảm " + formatCurrency(tienGiam) + ")");
            redirectAttributes.addFlashAttribute("messageType", "success");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mess", "Lỗi áp dụng mã giảm giá: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/banhang/index?mahd=" + mahd;
    }

    @PostMapping("/bogg")
    @ResponseBody
    @Transactional
    public Map<String, Object> boGiamGia(
            @RequestParam("mahd") String mahd,
            @RequestParam(value = "phuongthuc", required = false, defaultValue = "default") String phuongthuc,
            @RequestParam(value = "_csrf", required = false) String csrf,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {

        Map<String, Object> response = new HashMap<>();
        try {
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy hóa đơn!");
                return response;
            }

            // ⭐ CHỈ CẦN SET NULL, KHÔNG TRỪ SỐ LƯỢNG
            hoaDon.setMaGiamGia(null);
            hoaDonService.save(hoaDon);

            response.put("success", true);
            response.put("message", "Đã bỏ mã giảm giá thành công!");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi bỏ voucher: " + e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }

        return response;
    }

    @PostMapping("/bokh")
    public String boKhachHang(@RequestParam("mahd") String mahd,
                              RedirectAttributes redirectAttributes) {
        HoaDon hoaDon = hoaDonService.findById(mahd);
        if (hoaDon == null) {
            redirectAttributes.addFlashAttribute("mess", "Không tìm thấy hóa đơn!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/banhang/index";
        }

        if ("Đã thanh toán".equals(hoaDon.getTrangThai())) {
            redirectAttributes.addFlashAttribute("mess", "Không thể bỏ khách hàng của hóa đơn đã thanh toán!");
            redirectAttributes.addFlashAttribute("messageType", "warning");
            return "redirect:/banhang/index?mahd=" + mahd;
        }

        KhachHang khachLe = getOrCreateKhachLe();
        hoaDon.setMaKhachHang(khachLe);
        hoaDonService.save(hoaDon);
        redirectAttributes.addFlashAttribute("mess", "Đã chuyển về khách lẻ!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/banhang/index?mahd=" + mahd;
    }

    @PostMapping("/taoqr")
    @ResponseBody
    public Map<String,Object> taoQR(@RequestParam("mahd") String mahd){
        Map<String,Object> response = new HashMap<>();
        try{
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if(hoaDon==null){
                response.put("error","Không tìm thấy hóa đơn");
                response.put("success", false);
                return response;
            }

            List<HoaDonChiTiet> list = hoaDonChiTietService.findById(mahd);
            if(list == null || list.isEmpty()){
                response.put("error","Hóa đơn không có sản phẩm");
                response.put("success", false);
                return response;
            }

            BigDecimal tongTien = tinhTongTienHoaDon(hoaDon,list);

            String bankCode = bank;
            String accountNo = account;
            String accountNameEncoded = URLEncoder.encode(accountName.trim(), StandardCharsets.UTF_8.name());
            String orderIdEncoded = URLEncoder.encode(hoaDon.getMaHoaDon(), StandardCharsets.UTF_8.name());

            long timestamp = System.currentTimeMillis();

            // Tạo URL VietQR
            String qrUrl = String.format(
                    "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s&t=%d",
                    bankCode,
                    accountNo,
                    tongTien.intValue(),
                    orderIdEncoded,
                    accountNameEncoded,
                    timestamp
            );

            System.out.println("📱 QR URL: " + qrUrl);

            // ⭐ TRẢ VỀ URL TRỰC TIẾP
            response.put("qrUrl", qrUrl);
            response.put("amount", tongTien);
            response.put("orderId", hoaDon.getMaHoaDon());
            response.put("success", true);

        }catch(Exception e){
            e.printStackTrace();
            response.put("error", e.getMessage());
            response.put("success", false);
        }
        return response;
    }

    private String generateQRCodeBase64(String data) {
        try {
            if (data == null || data.isEmpty()) {
                System.err.println("❌ Data QR rỗng!");
                return null;
            }

            System.out.println("📱 Generating QR for: " + data);

            // Tạo QR code với kích thước 400x400
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // Thêm error correction level cao
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H);

            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 400, 400, hints);

            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            String base64 = java.util.Base64.getEncoder().encodeToString(outputStream.toByteArray());
            System.out.println("✅ QR generated, size: " + base64.length() + " bytes");

            return base64;
        } catch (Exception e) {
            System.err.println("❌ Lỗi tạo QR: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 VNĐ";
        java.text.NumberFormat fmt = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        return fmt.format(amount) + " VNĐ";
    }

    @PostMapping("/thanhtoan")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> thanhToan(
            @RequestParam("mahd") String mahd,
            @RequestParam("method") String method,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(value = "tenNguoiNhan", required = false) String tenNguoiNhan,
            @RequestParam(value = "sdtNguoiNhan", required = false) String sdtNguoiNhan,
            @RequestParam(value = "diaChiGiaoHang", required = false) String diaChiGiaoHang) {

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("========== THANH TOAN (AJAX) ==========");
            System.out.println("Ma HD: " + mahd);
            System.out.println("Phuong thuc: " + method);
            System.out.println("So tien: " + amount);
            System.out.println("Ten nguoi nhan: " + tenNguoiNhan);
            System.out.println("SDT nguoi nhan: " + sdtNguoiNhan);
            System.out.println("Dia chi giao hang: " + diaChiGiaoHang);

            // 1. Kiểm tra hóa đơn
            HoaDon hd = hoaDonService.findById(mahd);
            if (hd == null) {
                response.put("success", false);
                response.put("message", "Hóa đơn không tồn tại!");
                return ResponseEntity.ok(response);
            }

            if ("Đã thanh toán".equals(hd.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Hóa đơn đã được thanh toán!");
                return ResponseEntity.ok(response);
            }

            if ("Đã huỷ".equals(hd.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Hóa đơn đã bị hủy!");
                return ResponseEntity.ok(response);
            }

            // 2. Kiểm tra sản phẩm
            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            if (listhdct == null || listhdct.isEmpty()) {
                response.put("success", false);
                response.put("message", "Hóa đơn chưa có sản phẩm!");
                return ResponseEntity.ok(response);
            }

            // 3. Tính tổng tiền hàng
            BigDecimal tongTienHang = BigDecimal.ZERO;
            for (HoaDonChiTiet ct : listhdct) {
                if (ct.getThanhTien() != null) {
                    tongTienHang = tongTienHang.add(ct.getThanhTien());
                }
            }
            System.out.println("Tong tien hang: " + tongTienHang);

            // 4. Xử lý voucher
            GiamGia voucher = hd.getMaGiamGia();
            BigDecimal tienGiam = BigDecimal.ZERO;
            String maKhachHang = null;

            if (hd.getMaKhachHang() != null) {
                maKhachHang = hd.getMaKhachHang().getMaKH();
            }

            if (voucher != null) {
                try {
                    boolean isValid = giamGiaService.kiemTraVoucherHopLeChoThanhToan(
                            voucher.getMaGiamGia(), maKhachHang);

                    if (isValid) {
                        tienGiam = giamGiaService.tinhSoTienGiam(voucher, tongTienHang);
                        System.out.println("Giam voucher: " + tienGiam);
                    } else {
                        hd.setMaGiamGia(null);
                        voucher = null;
                        System.out.println("Da bo voucher khong hop le");
                    }
                } catch (Exception e) {
                    System.out.println("Loi xu ly voucher: " + e.getMessage());
                    hd.setMaGiamGia(null);
                    voucher = null;
                }
            }

            // 5. Tính tổng tiền sau voucher
            BigDecimal tongTien = tongTienHang.subtract(tienGiam);

            // 6. Xử lý phí ship (KHÔNG SET CỨNG 30k)
            boolean isOnline = "Online".equalsIgnoreCase(hd.getLoaiBan());
            BigDecimal tienShip = BigDecimal.ZERO;

            if (isOnline) {
                // ⭐ CHỈ LẤY SHIP ĐÃ TÍNH TỪ DB, KHÔNG SET CỨNG
                tienShip = hd.getTienShip() != null ? hd.getTienShip() : BigDecimal.ZERO;
                if (tienShip.compareTo(BigDecimal.ZERO) > 0) {
                    tongTien = tongTien.add(tienShip);
                    System.out.println("Phi ship: " + tienShip);
                } else {
                    System.out.println("Khong co phi ship (FREE SHIP hoac chua tinh)");
                }
            }

            // Đảm bảo không âm
            if (tongTien.compareTo(BigDecimal.ZERO) < 0) {
                tongTien = BigDecimal.ZERO;
            }

            System.out.println("Tong tien cuoi cung: " + tongTien);

            // 7. Xác định loại thanh toán
            boolean isCOD = "cod".equalsIgnoreCase(method);

            // ⭐ LƯU THÔNG TIN NGƯỜI NHẬN (CHỈ KHI ONLINE)
            if (isOnline) {
                if (tenNguoiNhan != null && !tenNguoiNhan.trim().isEmpty()) {
                    hd.setTenNguoiNhan(tenNguoiNhan.trim());
                    System.out.println("Da luu ten nguoi nhan: " + tenNguoiNhan);
                }
                if (sdtNguoiNhan != null && !sdtNguoiNhan.trim().isEmpty()) {
                    hd.setSdtNguoiNhan(sdtNguoiNhan.trim());
                    System.out.println("Da luu SDT nguoi nhan: " + sdtNguoiNhan);
                }
                if (diaChiGiaoHang != null && !diaChiGiaoHang.trim().isEmpty()) {
                    hd.setDiaChiGiaoHang(diaChiGiaoHang.trim());
                    System.out.println("Da luu dia chi giao hang: " + diaChiGiaoHang);
                }
            }

            // 8. Xử lý ngày thanh toán và trạng thái
            LocalDateTime ngayThanhToan = null;
            String trangThaiMoi = null;
            String successMessage = "";

            if (isOnline && isCOD) {
                // ONLINE + COD: Chưa thanh toán
                ngayThanhToan = null;
                trangThaiMoi = "Đã xác nhận";
                hd.setTienKhachDua(BigDecimal.ZERO);
                hd.setTienThua(BigDecimal.ZERO);
                successMessage = "Đơn hàng đã xác nhận! Khách sẽ thanh toán khi nhận hàng.";
                System.out.println("Online COD - Chua thanh toan");

            } else if (isOnline && !isCOD) {
                // ONLINE + Chuyển khoản/Tiền mặt: Đã thanh toán
                ngayThanhToan = LocalDateTime.now();
                trangThaiMoi = "Đã xác nhận";
                successMessage = "Đơn hàng đã thanh toán online thành công!";
                System.out.println("Online da thanh toan qua: " + method);

                if ("cash".equalsIgnoreCase(method) || "tienmat".equalsIgnoreCase(method)) {
                    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        response.put("success", false);
                        response.put("message", "Vui lòng nhập số tiền khách đưa!");
                        return ResponseEntity.ok(response);
                    }

                    if (amount.compareTo(tongTien) < 0) {
                        BigDecimal conThieu = tongTien.subtract(amount);
                        response.put("success", false);
                        response.put("message", "Tiền khách đưa không đủ! Còn thiếu: " + formatCurrency(conThieu));
                        return ResponseEntity.ok(response);
                    }

                    hd.setTienKhachDua(amount);
                    hd.setTienThua(amount.subtract(tongTien));
                } else {
                    hd.setTienKhachDua(tongTien);
                    hd.setTienThua(BigDecimal.ZERO);
                }

            } else {
                // TẠI QUÁN: Đã thanh toán
                ngayThanhToan = LocalDateTime.now();
                trangThaiMoi = "Đã thanh toán";
                successMessage = "Thanh toán thành công!";
                System.out.println("Tai quan da thanh toan");

                if ("cash".equalsIgnoreCase(method) || "tienmat".equalsIgnoreCase(method)) {
                    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        response.put("success", false);
                        response.put("message", "Vui lòng nhập số tiền khách đưa!");
                        return ResponseEntity.ok(response);
                    }

                    if (amount.compareTo(tongTien) < 0) {
                        BigDecimal conThieu = tongTien.subtract(amount);
                        response.put("success", false);
                        response.put("message", "Tiền khách đưa không đủ! Còn thiếu: " + formatCurrency(conThieu));
                        return ResponseEntity.ok(response);
                    }

                    hd.setTienKhachDua(amount);
                    hd.setTienThua(amount.subtract(tongTien));
                } else {
                    hd.setTienKhachDua(tongTien);
                    hd.setTienThua(BigDecimal.ZERO);
                }
            }

            // 9. Giảm số lượng voucher
            if (voucher != null) {
                try {
                    giamGiaService.giamSoLuongVoucherChoThanhToan(
                            voucher.getMaGiamGia(), maKhachHang);
                    System.out.println("Da xu ly giam so luong voucher: " + voucher.getMaGiamGia());
                } catch (Exception e) {
                    System.out.println("Loi giam so luong voucher (bo qua): " + e.getMessage());
                }
            }

            // 10. Cập nhật hóa đơn
            hd.setTongTien(tongTien);
            hd.setPhuongThucThanhToan(getPhuongThucText(method));
            hd.setNgayThanhToan(ngayThanhToan);
            hd.setTrangThai(trangThaiMoi);

            hoaDonService.save(hd);

            System.out.println("Thanh toan thanh cong!");
            System.out.println("Trang thai: " + hd.getTrangThai());
            System.out.println("Ngay thanh toan: " + (ngayThanhToan != null ? ngayThanhToan : "Chua thanh toan"));
            System.out.println("========== END THANH TOAN ==========");

            // 11. Trả về response
            response.put("success", true);
            response.put("message", successMessage);
            response.put("maHoaDon", mahd);
            response.put("trangThai", hd.getTrangThai());
            response.put("tongTien", tongTien);
            response.put("tienThua", hd.getTienThua() != null ? hd.getTienThua() : BigDecimal.ZERO);
            response.put("daThanhToan", ngayThanhToan != null);
            response.put("ngayThanhToan", ngayThanhToan != null ? ngayThanhToan.toString() : "Chưa thanh toán");
            response.put("tienShip", tienShip);
            response.put("tenNguoiNhan", hd.getTenNguoiNhan() != null ? hd.getTenNguoiNhan() : "");
            response.put("sdtNguoiNhan", hd.getSdtNguoiNhan() != null ? hd.getSdtNguoiNhan() : "");
            response.put("diaChiGiaoHang", hd.getDiaChiGiaoHang() != null ? hd.getDiaChiGiaoHang() : "");

            if (voucher != null) {
                response.put("voucherUsed", voucher.getMaGiamGia());
                response.put("tienGiam", tienGiam);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("LOI: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi thanh toán: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/thanhtoanhd")
    public String thanhToanHoaDon(
            @RequestParam("mahd") String mahd,
            @RequestParam(value = "tienkhachdua", required = false) BigDecimal tienkhachdua,
            @RequestParam("phuongthuc") String phuongthuc,
            @RequestParam(value = "tenNguoiNhan", required = false) String tenNguoiNhan,
            @RequestParam(value = "sdtNguoiNhan", required = false) String sdtNguoiNhan,
            @RequestParam(value = "diaChiGiaoHang", required = false) String diaChiGiaoHang,
            RedirectAttributes redirectAttributes) {

        try {
            System.out.println("========== THANH TOAN (MVC) ==========");
            System.out.println("Ma HD: " + mahd);
            System.out.println("Phuong thuc: " + phuongthuc);
            System.out.println("Tien khach dua: " + tienkhachdua);
            System.out.println("Ten nguoi nhan: " + tenNguoiNhan);
            System.out.println("SDT nguoi nhan: " + sdtNguoiNhan);
            System.out.println("Dia chi giao hang: " + diaChiGiaoHang);

            // 1. Kiểm tra hóa đơn
            HoaDon hd = hoaDonService.findById(mahd);
            if (hd == null) {
                redirectAttributes.addFlashAttribute("mess", "Hóa đơn không tồn tại!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/banhang/index";
            }

            if ("Đã thanh toán".equals(hd.getTrangThai())) {
                redirectAttributes.addFlashAttribute("mess", "Hóa đơn đã được thanh toán!");
                redirectAttributes.addFlashAttribute("messageType", "warning");
                return "redirect:/banhang/index?mahd=" + mahd;
            }

            if ("Đã huỷ".equals(hd.getTrangThai())) {
                redirectAttributes.addFlashAttribute("mess", "Hóa đơn đã bị hủy!");
                redirectAttributes.addFlashAttribute("messageType", "warning");
                return "redirect:/banhang/index";
            }

            // 2. Kiểm tra sản phẩm
            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            if (listhdct == null || listhdct.isEmpty()) {
                redirectAttributes.addFlashAttribute("mess", "Hóa đơn chưa có sản phẩm!");
                redirectAttributes.addFlashAttribute("messageType", "warning");
                return "redirect:/banhang/index?mahd=" + mahd;
            }

            // 3. Tính tổng tiền hàng
            BigDecimal tongTienHang = BigDecimal.ZERO;
            for (HoaDonChiTiet ct : listhdct) {
                if (ct.getThanhTien() != null) {
                    tongTienHang = tongTienHang.add(ct.getThanhTien());
                }
            }

            // 4. Xử lý voucher
            GiamGia voucher = hd.getMaGiamGia();
            BigDecimal tienGiam = BigDecimal.ZERO;
            String maKhachHang = null;

            if (hd.getMaKhachHang() != null) {
                maKhachHang = hd.getMaKhachHang().getMaKH();
            }

            if (voucher != null) {
                try {
                    boolean isValid = giamGiaService.kiemTraVoucherHopLeChoThanhToan(
                            voucher.getMaGiamGia(), maKhachHang);

                    if (isValid) {
                        tienGiam = giamGiaService.tinhSoTienGiam(voucher, tongTienHang);
                        System.out.println("Giam voucher: " + tienGiam);
                    } else {
                        hd.setMaGiamGia(null);
                        voucher = null;
                        System.out.println("Da bo voucher khong hop le");
                    }
                } catch (Exception e) {
                    System.out.println("Loi xu ly voucher: " + e.getMessage());
                    hd.setMaGiamGia(null);
                    voucher = null;
                }
            }

            // 5. Tính tổng tiền
            BigDecimal tongTien = tongTienHang.subtract(tienGiam);

            // 6. Xác định phương thức thanh toán
            boolean isCOD = "COD".equalsIgnoreCase(phuongthuc);
            boolean isChuyenKhoan = "Chuyển khoản".equalsIgnoreCase(phuongthuc);

            if (isCOD) {
                tienkhachdua = BigDecimal.ZERO;
            }
            if (isChuyenKhoan) {
                tienkhachdua = tongTien;
            }

            // 7. Kiểm tra tiền khách đưa
            if (!isCOD && !isChuyenKhoan) {
                if (tienkhachdua == null || tienkhachdua.compareTo(tongTien) < 0) {
                    BigDecimal conThieu = tongTien.subtract(tienkhachdua != null ? tienkhachdua : BigDecimal.ZERO);
                    redirectAttributes.addFlashAttribute("mess",
                            "Tiền khách đưa không đủ! Còn thiếu: " + formatCurrency(conThieu));
                    redirectAttributes.addFlashAttribute("messageType", "warning");
                    return "redirect:/banhang/index?mahd=" + mahd;
                }
            }

            // 8. Xử lý phí ship (KHÔNG SET CỨNG 30k)
            boolean isOnline = "Online".equalsIgnoreCase(hd.getLoaiBan());
            BigDecimal tienShip = BigDecimal.ZERO;

            if (isOnline && !isCOD) {
                // ⭐ CHỈ LẤY SHIP ĐÃ TÍNH TỪ DB, KHÔNG SET CỨNG
                tienShip = hd.getTienShip() != null ? hd.getTienShip() : BigDecimal.ZERO;
                if (tienShip.compareTo(BigDecimal.ZERO) > 0) {
                    tongTien = tongTien.add(tienShip);
                    System.out.println("Phi ship: " + tienShip);
                } else {
                    System.out.println("Khong co phi ship (FREE SHIP hoac chua tinh)");
                }
            }

            // Đảm bảo không âm
            if (tongTien.compareTo(BigDecimal.ZERO) < 0) {
                tongTien = BigDecimal.ZERO;
            }

            // 9. Lưu thông tin người nhận (CHỈ KHI ONLINE)
            if (isOnline) {
                if (tenNguoiNhan != null && !tenNguoiNhan.trim().isEmpty()) {
                    hd.setTenNguoiNhan(tenNguoiNhan.trim());
                }
                if (sdtNguoiNhan != null && !sdtNguoiNhan.trim().isEmpty()) {
                    hd.setSdtNguoiNhan(sdtNguoiNhan.trim());
                }
                if (diaChiGiaoHang != null && !diaChiGiaoHang.trim().isEmpty()) {
                    hd.setDiaChiGiaoHang(diaChiGiaoHang.trim());
                }
            }

            // 10. Xử lý ngày thanh toán và trạng thái
            LocalDateTime ngayThanhToan = null;
            String trangThaiMoi = null;
            String successMessage = "";

            if (isOnline && isCOD) {
                ngayThanhToan = null;
                trangThaiMoi = "Đã xác nhận";
                hd.setTienKhachDua(BigDecimal.ZERO);
                hd.setTienThua(BigDecimal.ZERO);
                successMessage = "Đơn hàng đã xác nhận! Khách sẽ thanh toán khi nhận hàng.";
                System.out.println("Online COD - Chua thanh toan");

            } else if (isOnline && !isCOD) {
                ngayThanhToan = LocalDateTime.now();
                trangThaiMoi = "Đã xác nhận";

                if (isChuyenKhoan) {
                    hd.setTienKhachDua(tongTien);
                    hd.setTienThua(BigDecimal.ZERO);
                    successMessage = "Đơn hàng đã thanh toán qua chuyển khoản!";
                } else {
                    hd.setTienKhachDua(tienkhachdua);
                    hd.setTienThua(tienkhachdua.subtract(tongTien));
                    successMessage = "Thanh toán thành công! Tiền thừa: " + formatCurrency(hd.getTienThua());
                }
                System.out.println("Online da thanh toan");

            } else {
                ngayThanhToan = LocalDateTime.now();
                trangThaiMoi = "Đã thanh toán";

                if (isCOD) {
                    hd.setTienKhachDua(BigDecimal.ZERO);
                    hd.setTienThua(BigDecimal.ZERO);
                    successMessage = "Đơn hàng đã xác nhận!";
                } else {
                    hd.setTienKhachDua(tienkhachdua);
                    hd.setTienThua(tienkhachdua.subtract(tongTien));
                    successMessage = "Thanh toán thành công! Tiền thừa: " + formatCurrency(hd.getTienThua());
                }
                System.out.println("Tai quan da thanh toan");
            }

            // 11. Giảm số lượng voucher
            if (voucher != null) {
                try {
                    giamGiaService.giamSoLuongVoucherChoThanhToan(
                            voucher.getMaGiamGia(), maKhachHang);
                    System.out.println("Da xu ly giam so luong voucher: " + voucher.getMaGiamGia());
                } catch (Exception e) {
                    System.out.println("Loi giam so luong voucher (bo qua): " + e.getMessage());
                }
            }

            // 12. Cập nhật hóa đơn
            hd.setTongTien(tongTien);
            hd.setPhuongThucThanhToan(phuongthuc);
            hd.setNgayThanhToan(ngayThanhToan);
            hd.setTrangThai(trangThaiMoi);

            hoaDonService.save(hd);

            System.out.println("Thanh toan MVC thanh cong!");
            System.out.println("Trang thai: " + hd.getTrangThai());
            System.out.println("Ngay thanh toan: " + (ngayThanhToan != null ? ngayThanhToan : "Chua thanh toan"));
            System.out.println("========== END THANH TOAN MVC ==========");

            redirectAttributes.addFlashAttribute("mess", successMessage);
            redirectAttributes.addFlashAttribute("messageType", "success");
            redirectAttributes.addFlashAttribute("daThanhToan", ngayThanhToan != null);
            redirectAttributes.addFlashAttribute("ngayThanhToan",
                    ngayThanhToan != null ? ngayThanhToan.toString() : "Chưa thanh toán");

        } catch (Exception e) {
            System.out.println("LOI: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("mess", "Lỗi thanh toán: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/banhang/index?mahd=" + mahd;
        }

        return "redirect:/banhang/index";
    }

    private String getPhuongThucText(String method) {
        if ("cash".equalsIgnoreCase(method) || "tienmat".equalsIgnoreCase(method)) {
            return "Tiền mặt";
        } else if ("transfer".equalsIgnoreCase(method) || "chuyenkhoan".equalsIgnoreCase(method)) {
            return "Chuyển khoản";
        } else if ("cod".equalsIgnoreCase(method)) {
            return "COD";
        }
        return method;
    }

    @PostMapping("/huyhd")
    public String huyHoaDon(@RequestParam("mahd") String mahd,
                            RedirectAttributes redirectAttributes) {
        HoaDon hd = hoaDonService.findById(mahd);

        if (hd == null) {
            redirectAttributes.addFlashAttribute("mess", "Hóa đơn không tồn tại!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/banhang/index";
        }

        if ("Đã thanh toán".equals(hd.getTrangThai())) {
            redirectAttributes.addFlashAttribute("mess",
                    "Không thể hủy hóa đơn đã thanh toán!");
            redirectAttributes.addFlashAttribute("messageType", "warning");
            return "redirect:/banhang/index?mahd=" + mahd;
        }

        if ("Đã huỷ".equals(hd.getTrangThai())) {
            redirectAttributes.addFlashAttribute("mess", "Hóa đơn này đã bị hủy rồi!");
            redirectAttributes.addFlashAttribute("messageType", "warning");
            return "redirect:/banhang/index";
        }

        List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
        int tongSPHoanLai = 0;
        if (listhdct != null && !listhdct.isEmpty()) {
            for (HoaDonChiTiet dc : listhdct) {
                SanPhamChiTiet spct = dc.getSanPhamChiTiet();
                spct.setSoLuongTon(spct.getSoLuongTon() + dc.getSoLuong());
                sanPhamChiTietService.capNhatTrangThaii(spct);
                sanPhamChiTietService.them(spct);
                tongSPHoanLai += dc.getSoLuong();
            }
        }

        hd.setTrangThai("Đã huỷ");
        hoaDonService.save(hd);
        redirectAttributes.addFlashAttribute("mess",
                "Hủy hóa đơn thành công! Đã hoàn " + tongSPHoanLai + " SP vào kho.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/banhang/index";
    }

    @PostMapping("/api/huyhd")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> huyHoaDonAjax(
            @RequestParam("mahd") String mahd,
            @RequestParam(value = "_csrf", required = false) String csrf) {

        Map<String, Object> response = new HashMap<>();

        try {
            HoaDon hd = hoaDonService.findById(mahd);

            if (hd == null) {
                response.put("success", false);
                response.put("message", "Hóa đơn không tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            if ("Đã thanh toán".equals(hd.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Không thể hủy hóa đơn đã thanh toán!");
                return ResponseEntity.badRequest().body(response);
            }

            if ("Đã huỷ".equals(hd.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Hóa đơn này đã bị hủy rồi!");
                return ResponseEntity.badRequest().body(response);
            }

            // Hoàn lại tồn kho và cập nhật trạng thái
            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            int tongSPHoanLai = 0;
            List<String> danhSachMaSpHoan = new ArrayList<>();

            if (listhdct != null && !listhdct.isEmpty()) {
                for (HoaDonChiTiet dc : listhdct) {
                    SanPhamChiTiet spct = dc.getSanPhamChiTiet();
                    int soLuongHoan = dc.getSoLuong();

                    // Hoàn lại tồn kho
                    spct.setSoLuongTon(spct.getSoLuongTon() + soLuongHoan);
                    sanPhamChiTietService.them(spct);

                    // Lưu mã sản phẩm để cập nhật trạng thái sau
                    danhSachMaSpHoan.add(spct.getMaSanPhamChiTiet());
                    tongSPHoanLai += soLuongHoan;

                    System.out.println("🔄 Hoàn " + soLuongHoan + " SP " + spct.getMaSanPhamChiTiet() +
                            ", tồn kho mới: " + spct.getSoLuongTon());
                }
            }

            // ⭐ CẬP NHẬT TRẠNG THÁI CHO TẤT CẢ SẢN PHẨM ĐÃ HOÀN
            // (Có thể từ "Hết hàng" -> "Còn hàng" nếu tồn kho > 0)
            if (!danhSachMaSpHoan.isEmpty()) {
                for (String maSpct : danhSachMaSpHoan) {
                    SanPhamChiTiet spct = sanPhamChiTietService.kiemTraVaCapNhatTrangThaiSauKhiBan(maSpct);
                    if (spct != null) {
                        System.out.println("📦 Trạng thái sản phẩm " + maSpct + ": " + spct.getTrangThai());
                    }
                }
            }

            // Cập nhật trạng thái hóa đơn
            hd.setTrangThai("Đã huỷ");
            hoaDonService.save(hd);

            // Trả về thành công
            response.put("success", true);
            response.put("message", "Hủy hóa đơn thành công! Đã hoàn " + tongSPHoanLai + " SP vào kho.");
            response.put("tongSPHoanLai", tongSPHoanLai);
            response.put("maHD", mahd);
            response.put("soSanPhamDaHoan", danhSachMaSpHoan.size());

            System.out.println("✅ Hủy hóa đơn " + mahd + " thành công!");
            System.out.println("   Đã hoàn " + tongSPHoanLai + " sản phẩm");
            System.out.println("   Cập nhật trạng thái cho " + danhSachMaSpHoan.size() + " sản phẩm");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/themkh")
    @Transactional
    public String themKhachHang(@Valid @ModelAttribute("kh") KhachHang kh,
                                BindingResult bindingResult,
                                @RequestParam(value = "mahd", required = false) String mahd,
                                @RequestParam(value = "ghiChuGiaoHang", required = false) String ghiChuGiaoHang,
                                RedirectAttributes redirectAttributes) {

        String redirectUrl = (mahd != null && !mahd.trim().isEmpty())
                ? "redirect:/banhang/index?mahd=" + mahd
                : "redirect:/banhang/index";

        if ("0000000000".equals(kh.getSdt())) {
            redirectAttributes.addFlashAttribute("mess", "Không thể tạo khách hàng với SĐT mặc định!");
            redirectAttributes.addFlashAttribute("messageType", "warning");
            return redirectUrl;
        }

        if (bindingResult.hasErrors()) {
            String loiDauTien = bindingResult.getFieldErrors().stream()
                    .map(fe -> fe.getDefaultMessage())
                    .findFirst().orElse("Dữ liệu không hợp lệ");
            redirectAttributes.addFlashAttribute("mess", "Lỗi: " + loiDauTien);
            redirectAttributes.addFlashAttribute("messageType", "error");
            return redirectUrl;
        }

        if (khachHangService.existsBySdt(kh.getSdt())) {
            redirectAttributes.addFlashAttribute("mess",
                    "Số điện thoại " + kh.getSdt() + " đã được đăng ký!");
            redirectAttributes.addFlashAttribute("messageType", "warning");
            return redirectUrl;
        }

        if (kh.getNgayDangKy() == null) {
            kh.setNgayDangKy(LocalDate.now());
        }

        kh.setMaKH(taoMaKhachHang());

        KhachHang khachHangDaLuu = khachHangService.save(kh);

        if (mahd != null && !mahd.trim().isEmpty()) {
            HoaDon hd = hoaDonService.findById(mahd);
            if (hd != null) {
                if (hd.getMaKhachHang() != null && "0000000000".equals(hd.getMaKhachHang().getSdt())) {
                    hd.setMaKhachHang(khachHangDaLuu);
                    if (ghiChuGiaoHang != null && !ghiChuGiaoHang.trim().isEmpty()) {
                        hd.setGhiChu(ghiChuGiaoHang);
                    }
                    hoaDonService.save(hd);
                    redirectAttributes.addFlashAttribute("mess",
                            "Thêm & gán khách hàng " + khachHangDaLuu.getHoTen() + " vào hóa đơn thành công!");
                    redirectAttributes.addFlashAttribute("messageType", "success");
                    return redirectUrl;
                }
            }
        }

        redirectAttributes.addFlashAttribute("mess", "Thêm khách hàng mới thành công!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return redirectUrl;
    }

    private String taoMaKhachHang() {
        Random random = new Random();
        int soNgauNhien = 1000 + random.nextInt(9000);
        return "KH" + soNgauNhien;
    }

    @PostMapping("/chonkh")
    public String chonKhachHang(@RequestParam("mahd") String mahd,
                                @RequestParam("makh") String makh,
                                RedirectAttributes redirectAttributes) {
        HoaDon hd = hoaDonService.findById(mahd);
        if (hd == null) {
            redirectAttributes.addFlashAttribute("mess", "Hóa đơn không tồn tại!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/banhang/index";
        }

        KhachHang kh = khachHangService.getKhachHangById(makh);
        if (kh == null) {
            redirectAttributes.addFlashAttribute("mess", "Khách hàng không tồn tại!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/banhang/index?mahd=" + mahd;
        }

        if ("0000000000".equals(kh.getSdt())) {
            redirectAttributes.addFlashAttribute("mess", "Không thể chọn khách hàng mặc định!");
            redirectAttributes.addFlashAttribute("messageType", "warning");
            return "redirect:/banhang/index?mahd=" + mahd;
        }

        hd.setMaKhachHang(kh);
        hoaDonService.save(hd);

        // ⭐ THÊM: Lưu thông tin khách hàng vào session để JS lấy
        redirectAttributes.addFlashAttribute("khachHangSelected", kh.getMaKH());
        redirectAttributes.addFlashAttribute("khachHangTen", kh.getHoTen());
        redirectAttributes.addFlashAttribute("mess", "Đã chọn khách hàng: " + kh.getHoTen());
        redirectAttributes.addFlashAttribute("messageType", "success");

        return "redirect:/banhang/index?mahd=" + mahd;
    }

    @PostMapping("/capnhatghichu")
    public String capNhatGhiChu(@RequestParam("mahd") String mahd,
                                @RequestParam("ghiChu") String ghiChu,
                                RedirectAttributes redirectAttributes) {
        HoaDon hd = hoaDonService.findById(mahd);
        if (hd != null) {
            hd.setGhiChu(ghiChu);
            hoaDonService.save(hd);
            redirectAttributes.addFlashAttribute("mess", "Đã cập nhật ghi chú!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        }
        return "redirect:/banhang/index?mahd=" + mahd;
    }


    @GetMapping("/get-payment-info")
    @ResponseBody
    public Map<String, Object> getPaymentInfo(@RequestParam("mahd") String maHD) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Lấy hóa đơn từ database
            HoaDon hoaDon = hoaDonService.findById(maHD);
            if (hoaDon == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy hóa đơn!");
                return response;
            }

            // Lấy danh sách chi tiết hóa đơn
            List<HoaDonChiTiet> chiTietList = hoaDonChiTietRepository.findByMaHoaDon(hoaDon);

            // Tính tổng tiền hàng
            BigDecimal tongTienHang = BigDecimal.ZERO;
            if (chiTietList != null) {
                for (HoaDonChiTiet ct : chiTietList) {
                    if (ct.getDonGia() != null) {
                        tongTienHang = tongTienHang.add(ct.getDonGia().multiply(BigDecimal.valueOf(ct.getSoLuong())));
                    }
                }
            }

            // Lấy tiền ship (đã lưu trong database)
            BigDecimal tienShip = hoaDon.getTienShip() != null ? hoaDon.getTienShip() : BigDecimal.ZERO;

            // Lấy loại bán
            String loaiBan = hoaDon.getLoaiBan() != null ? hoaDon.getLoaiBan() : "Tại quầy";

            // Tính tổng thanh toán (chưa tính giảm giá)
            BigDecimal tongThanhToan = tongTienHang.add(tienShip);

            response.put("success", true);
            response.put("tongTienHang", tongTienHang);
            response.put("tienShip", tienShip);
            response.put("loaiBan", loaiBan);
            response.put("tongGiamGia", BigDecimal.ZERO);
            response.put("tongThanhToan", tongThanhToan);
            response.put("maHoaDon", hoaDon.getMaHoaDon());
            response.put("trangThai", hoaDon.getTrangThai() != null ? hoaDon.getTrangThai() : "");

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return response;
    }


    @GetMapping("/inhoadon/{id}")
    public String inHoaDon(@PathVariable("id") String id, Model model) {
        HoaDon hoaDon = hoaDonService.findById(id);
        if (hoaDon == null) {
            return "redirect:/banhang/index";
        }

        List<HoaDonChiTiet> listHdct = hoaDonChiTietService.findByHoaDOn(hoaDon);
        if (listHdct == null) listHdct = new ArrayList<>();

        // ⭐ TÍNH TỔNG TIỀN HÀNG
        BigDecimal tongTienHang = listHdct.stream()
                .map(item -> item.getThanhTien() != null ? item.getThanhTien() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ⭐ TÍNH TIỀN GIẢM VOUCHER
        BigDecimal tienGiamVoucher = BigDecimal.ZERO;
        if (hoaDon.getMaGiamGia() != null) {
            tienGiamVoucher = tinhMucGiamVoucher(hoaDon.getMaGiamGia(), tongTienHang);
        }

        // ⭐ TIỀN SHIP
        BigDecimal tienShip = hoaDon.getTienShip() != null ? hoaDon.getTienShip() : BigDecimal.ZERO;

        // ⭐ TỔNG TIỀN = TIỀN HÀNG - GIẢM VOUCHER + SHIP
        BigDecimal tongTien = tongTienHang.subtract(tienGiamVoucher).add(tienShip);

        // ⭐ ĐỊNH DẠNG NGÀY THÁNG
        String ngayTao = "";
        String ngayThanhToan = "";
        if (hoaDon.getNgayTao() != null) {
            ngayTao = hoaDon.getNgayTao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
        if (hoaDon.getNgayThanhToan() != null) {
            ngayThanhToan = hoaDon.getNgayThanhToan().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }

        // ⭐ ĐẢM BẢO KHÔNG NULL
        if (tongTienHang == null) tongTienHang = BigDecimal.ZERO;
        if (tienGiamVoucher == null) tienGiamVoucher = BigDecimal.ZERO;
        if (tienShip == null) tienShip = BigDecimal.ZERO;
        if (tongTien == null) tongTien = BigDecimal.ZERO;

        model.addAttribute("hd", hoaDon);
        model.addAttribute("listHdct", listHdct);
        model.addAttribute("tongTienHang", tongTienHang);
        model.addAttribute("tienGiamVoucher", tienGiamVoucher);
        model.addAttribute("tienShip", tienShip);
        model.addAttribute("tongTien", tongTien);
        model.addAttribute("ngayTao", ngayTao);
        model.addAttribute("ngayThanhToan", ngayThanhToan);
        model.addAttribute("soLuongSanPham", listHdct.size());

        return "inhoadon";
    }

    @GetMapping("/khachhang")
    public String khachhang(@RequestParam(value = "sdt", required = false) String sdt, Model model) {
        List<KhachHang> kh = (sdt == null || sdt.trim().isEmpty())
                ? khachHangService.getAllKhachHang()
                : khachHangService.findAllBySdt(sdt);
        model.addAttribute("listkh", kh != null ? kh : new ArrayList<>());
        model.addAttribute("kh", new KhachHang());
        return "banhang/index";
    }


    @GetMapping("/tinh-phi-ship-invoice")
    @ResponseBody
    public ResponseEntity<?> tinhPhiShipInvoice(@RequestParam String mahd) {
        Map<String, Object> response = new HashMap<>();
        try {
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy hóa đơn!");
                return ResponseEntity.badRequest().body(response);
            }

            // Lấy địa chỉ từ hóa đơn
            DiaChi diaChi = null;
            if (hoaDon.getMaKhachHang() != null) {
                diaChi = diaChiService.findDefaultByMaKH(hoaDon.getMaKhachHang().getMaKH());
            }

            if (diaChi == null) {
                response.put("success", false);
                response.put("message", "Khách hàng chưa có địa chỉ!");
                return ResponseEntity.badRequest().body(response);
            }

            // Lấy danh sách sản phẩm
            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            if (listhdct == null || listhdct.isEmpty()) {
                response.put("success", false);
                response.put("message", "Hóa đơn chưa có sản phẩm!");
                return ResponseEntity.badRequest().body(response);
            }

            // ⭐ TÍNH TỔNG CÂN NẶNG: mỗi sản phẩm 500g
            int totalWeight = listhdct.stream()
                    .mapToInt(HoaDonChiTiet::getSoLuong)
                    .sum() * 500;

            BigDecimal tongTien = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // ⭐ TÍNH PHÍ SHIP VỚI ĐẦY ĐỦ THÔNG TIN
            BigDecimal shippingFee = ghnShippingService.calculateShippingFee(
                    diaChi.getQuanHuyen(),
                    diaChi.getPhuongXa(),
                    diaChi.getTinhThanh(),
                    totalWeight,
                    tongTien
            );

            // Cập nhật vào hóa đơn
            hoaDon.setTienShip(shippingFee);
            hoaDonService.save(hoaDon);

            response.put("success", true);
            response.put("shippingFee", shippingFee);
            response.put("shippingFeeFormatted", formatCurrency(shippingFee) + "đ");
            response.put("weight", totalWeight);
            response.put("address", diaChi.getDiaChiCuThe() + ", " +
                    diaChi.getPhuongXa() + ", " +
                    diaChi.getQuanHuyen() + ", " +
                    diaChi.getTinhThanh());

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-address/{maKH}")
    @ResponseBody
    public ResponseEntity<?> getAddressByKhachHang(@PathVariable("maKH") String maKH) {
        Map<String, Object> response = new HashMap<>();
        try {
            DiaChi diaChi = diaChiService.findDefaultByMaKH(maKH);
            if (diaChi == null) {
                List<DiaChi> list = diaChiService.findByKhachHang_MaKH(maKH);
                if (list != null && !list.isEmpty()) {
                    diaChi = list.get(0);
                }
            }

            if (diaChi == null) {
                response.put("success", false);
                response.put("message", "Khách hàng chưa có địa chỉ!");
                return ResponseEntity.ok(response);
            }

            Map<String, Object> address = new HashMap<>();
            address.put("maDiaChi", diaChi.getMaDiaChi());
            address.put("tenNguoiNhan", diaChi.getTenNguoiNhan());
            address.put("soDienThoai", diaChi.getSoDienThoaiNguoiNhan());
            address.put("diaChiCuThe", diaChi.getDiaChiCuThe());
            address.put("phuongXa", diaChi.getPhuongXa());
            address.put("quanHuyen", diaChi.getQuanHuyen());
            address.put("tinhThanh", diaChi.getTinhThanh());
            address.put("diaChiMacDinh", diaChi.getDiaChiMacDinh());

            response.put("success", true);
            response.put("address", address);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-shipping-address")
    @ResponseBody
    public ResponseEntity<?> updateShippingAddress(
            @RequestParam("mahd") String mahd,
            @RequestParam("maDiaChi") Integer maDiaChi) {
        try {
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy hóa đơn!"
                ));
            }

            DiaChi diaChi = diaChiService.findById(maDiaChi).orElse(null);
            if (diaChi == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy địa chỉ!"
                ));
            }

            // Cập nhật địa chỉ giao hàng
            String diaChiDayDu = diaChi.getDiaChiCuThe() + ", " +
                    diaChi.getPhuongXa() + ", " +
                    diaChi.getQuanHuyen() + ", " +
                    diaChi.getTinhThanh();

            hoaDon.setDiaChiGiaoHang(diaChiDayDu);
            hoaDon.setGhiChu("Địa chỉ giao hàng: " + diaChiDayDu +
                    " | Người nhận: " + diaChi.getTenNguoiNhan() +
                    " | SĐT: " + diaChi.getSoDienThoaiNguoiNhan());
            hoaDonService.save(hoaDon);

            // Tính lại phí ship
            BigDecimal shippingFee = tinhPhiShipGHN(hoaDon);
            hoaDon.setTienShip(shippingFee);
            hoaDonService.save(hoaDon);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã cập nhật địa chỉ giao hàng!",
                    "shippingFee", shippingFee,
                    "shippingFeeFormatted", formatCurrency(shippingFee) + "đ"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/test-ghn-token")
    @ResponseBody
    public Map<String, Object> testGHNToken() {
        Map<String, Object> result = new HashMap<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", ghnShippingService.getApiToken());
            headers.set("ShopId", String.valueOf(ghnShippingService.getShopId()));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shop/all",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            result.put("status", response.getStatusCode().value());
            result.put("body", response.getBody());
            result.put("success", response.getStatusCode().value() == 200);

            if (response.getStatusCode().value() == 200) {
                JsonNode root = objectMapper.readTree(response.getBody());
                result.put("shopName", root.path("data").path("shop_name").asText());
                result.put("message", "Token hợp lệ!");
            } else {
                result.put("message", "Token không hợp lệ!");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "Lỗi kiểm tra token: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/ghn/districts")
    @ResponseBody
    public Map<String, Object> getGHNDistricts(@RequestParam(defaultValue = "1") int provinceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", ghnShippingService.getApiToken());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/district?province_id=" + provinceId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            result.put("success", true);
            result.put("status", response.getStatusCode().value());

            if (response.getStatusCode().value() == 200) {
                JsonNode root = objectMapper.readTree(response.getBody());
                result.put("data", root.path("data"));
                result.put("message", "Lấy danh sách quận/huyện thành công!");
            } else {
                result.put("message", "Lỗi lấy danh sách quận/huyện!");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    @GetMapping("/test-ship-fee")
    @ResponseBody
    public Map<String, Object> testShipFee(
            @RequestParam(defaultValue = "Ba Đình") String toDistrict,
            @RequestParam(defaultValue = "Trúc Bạch") String toWard,
            @RequestParam(defaultValue = "Hà Nội") String toProvince,
            @RequestParam(defaultValue = "1000") int weight,
            @RequestParam(defaultValue = "500000") int amount) {

        Map<String, Object> result = new HashMap<>();
        result.put("request", Map.of(
                "toDistrict", toDistrict,
                "toWard", toWard,
                "toProvince", toProvince,
                "weight", weight,
                "amount", amount
        ));

        try {
            System.out.println("========== TEST SHIP FEE ==========");
            System.out.println("📍 Quận: " + toDistrict);
            System.out.println("📍 Phường: " + toWard);
            System.out.println("📍 Tỉnh: " + toProvince);
            System.out.println("⚖️ Cân nặng: " + weight + "g");
            System.out.println("💰 Số tiền: " + amount + "đ");

            // ⭐ GỌI VỚI PROVINCE
            BigDecimal ghnFee = ghnShippingService.calculateShippingFee(
                    toDistrict,
                    toWard,
                    toProvince,
                    weight,
                    BigDecimal.valueOf(amount)
            );


            result.put("ghnShippingFee", ghnFee);
            result.put("success", true);
            result.put("message", "Tính phí ship thành công!");

            System.out.println("🚚 GHN Fee: " + ghnFee);
            System.out.println("========== END TEST ==========");

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "Lỗi tính phí ship: " + e.getMessage());
        }

        return result;
    }

    @GetMapping("/test-ship-invoice")
    @ResponseBody
    public Map<String, Object> testShipInvoice(@RequestParam String mahd) {
        Map<String, Object> result = new HashMap<>();

        try {
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                result.put("success", false);
                result.put("message", "Không tìm thấy hóa đơn!");
                return result;
            }

            result.put("invoice", Map.of(
                    "maHoaDon", hoaDon.getMaHoaDon(),
                    "loaiBan", hoaDon.getLoaiBan(),
                    "khachHang", hoaDon.getMaKhachHang() != null ? hoaDon.getMaKhachHang().getHoTen() : "null"
            ));

            // Lấy địa chỉ
            DiaChi diaChi = null;
            if (hoaDon.getMaKhachHang() != null) {
                diaChi =diaChiService.findDefaultByMaKH(
                        hoaDon.getMaKhachHang().getMaKH()
                );
                if (diaChi == null) {
                    List<DiaChi> list = diaChiService.findByKhachHang(hoaDon.getMaKhachHang());
                    if (list != null && !list.isEmpty()) {
                        diaChi = list.get(0);
                    }
                }
            }

            if (diaChi == null) {
                result.put("success", false);
                result.put("message", "Khách hàng chưa có địa chỉ!");
                return result;
            }

            result.put("address", Map.of(
                    "diaChi", diaChi.getDiaChiCuThe(),
                    "phuongXa", diaChi.getPhuongXa(),
                    "quanHuyen", diaChi.getQuanHuyen(),
                    "tinhThanh", diaChi.getTinhThanh()
            ));

            // Tính phí ship
            BigDecimal fee = tinhPhiShipGHN(hoaDon);
            result.put("shippingFee", fee);
            result.put("success", true);
            result.put("message", "Tính phí ship thành công!");

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }


    @GetMapping("/tinh-phi-ship")
    @ResponseBody
    public ResponseEntity<?> tinhPhiShip(@RequestParam("mahd") String mahd)     {
        Map<String, Object> response = new HashMap<>();
        try {
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy hóa đơn!");
                return ResponseEntity.badRequest().body(response);
            }
            BigDecimal shippingFee = tinhPhiShipGHN(hoaDon);
            HoaDon hoaDonMoi = hoaDonService.findById(mahd);
            BigDecimal tienShip = hoaDonMoi.getTienShip() != null ? hoaDonMoi.getTienShip() : BigDecimal.ZERO;

            System.out.println("📦 Phí ship trong DB: " + tienShip);
            System.out.println("📦 Phí ship tính được: " + shippingFee);

            response.put("success", true);
            response.put("shippingFee", tienShip);

            if (tienShip.compareTo(BigDecimal.ZERO) == 0) {
                response.put("shippingFeeFormatted", "🎉 FREE SHIP");
            } else {
                response.put("shippingFeeFormatted", formatCurrency(tienShip) + "đ");
            }
            capNhatTongTienHoaDon(mahd);
            HoaDon hoaDonCapNhat = hoaDonService.findById(mahd);
            response.put("tongTien", hoaDonCapNhat.getTongTien());
            response.put("tongTienFormatted", formatCurrency(hoaDonCapNhat.getTongTien()) + "đ");

            response.put("message", "Tính phí ship thành công!");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi tính phí ship: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    private BigDecimal tinhPhiShipGHN(HoaDon hoaDon) {
        try {
            System.out.println("========== TÍNH PHÍ SHIP GHN ==========");
            System.out.println("📦 Hóa đơn: " + hoaDon.getMaHoaDon());

            if (hoaDon.getMaKhachHang() == null || !"Online".equalsIgnoreCase(hoaDon.getLoaiBan())) {
                System.out.println("⚠️ Không phải đơn Online hoặc chưa có khách hàng");
                hoaDon.setTienShip(BigDecimal.ZERO);
                hoaDonService.save(hoaDon);
                return BigDecimal.ZERO;
            }

            KhachHang khachHang = hoaDon.getMaKhachHang();
            if ("0000000000".equals(khachHang.getSdt())) {
                System.out.println("⚠️ Khách hàng là khách lẻ");
                hoaDon.setTienShip(BigDecimal.ZERO);
                hoaDonService.save(hoaDon);
                return BigDecimal.ZERO;
            }

            DiaChi diaChiMacDinh = diaChiService.findDefaultByMaKH(khachHang.getMaKH());
            if (diaChiMacDinh == null) {
                List<DiaChi> danhSachDiaChi = diaChiService.findByKhachHang(khachHang);
                if (danhSachDiaChi == null || danhSachDiaChi.isEmpty()) {
                    System.out.println("⚠️ Không có địa chỉ nào");
                    hoaDon.setTienShip(BigDecimal.ZERO);
                    hoaDonService.save(hoaDon);
                    return BigDecimal.ZERO;
                }
                diaChiMacDinh = danhSachDiaChi.get(0);
            }
            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(hoaDon.getMaHoaDon());
            if (listhdct == null || listhdct.isEmpty()) {
                System.out.println("⚠️ Hóa đơn không có sản phẩm");
                hoaDon.setTienShip(BigDecimal.ZERO);
                hoaDonService.save(hoaDon);
                return BigDecimal.ZERO;
            }
            BigDecimal tongTienHang = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal FREE_SHIP_THRESHOLD = new BigDecimal("500000");
            if (tongTienHang.compareTo(FREE_SHIP_THRESHOLD) >= 0) {
                System.out.println("🎉 Đơn hàng trên 500.000đ -> FREE SHIP!");
                hoaDon.setTienShip(BigDecimal.ZERO);
                hoaDonService.save(hoaDon);
                return BigDecimal.ZERO;
            }

            int totalWeight = listhdct.stream()
                    .mapToInt(HoaDonChiTiet::getSoLuong)
                    .sum() * 500;

            String quanHuyen = diaChiMacDinh.getQuanHuyen();
            String phuongXa = diaChiMacDinh.getPhuongXa();
            String tinhThanh = diaChiMacDinh.getTinhThanh();

            System.out.println("📍 Quận/Huyện: " + quanHuyen);
            System.out.println("📍 Phường/Xã: " + phuongXa);
            System.out.println("📍 Tỉnh/Thành: " + tinhThanh);
            System.out.println("⚖️ Tổng cân nặng: " + totalWeight + "g");
            System.out.println("💰 Tổng tiền: " + tongTienHang);

            BigDecimal shippingFee = ghnShippingService.calculateShippingFee(
                    quanHuyen,
                    phuongXa,
                    tinhThanh,
                    totalWeight,
                    tongTienHang
            );

            if (shippingFee == null || shippingFee.compareTo(BigDecimal.ZERO) < 0) {
                shippingFee = BigDecimal.ZERO;
            }

            hoaDon.setTienShip(shippingFee);
            hoaDonService.save(hoaDon);

            System.out.println("✅ Đã lưu phí ship: " + shippingFee + "đ vào DB");
            System.out.println("========== END TÍNH PHÍ SHIP ==========");

            return shippingFee;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Lỗi tính phí ship: " + e.getMessage());
            hoaDon.setTienShip(BigDecimal.ZERO);
            hoaDonService.save(hoaDon);
            return BigDecimal.ZERO;
        }
    }

    private void capNhatTongTienHoaDon(String mahd) {
        HoaDon hoaDon = hoaDonService.findById(mahd);
        if (hoaDon == null) return;

        List<HoaDonChiTiet> chiTiets = hoaDonChiTietRepository.findByMaHoaDon_MaHoaDon(mahd);
        BigDecimal tongTienHang = BigDecimal.ZERO;

        for (HoaDonChiTiet ct : chiTiets) {
            if (ct.getThanhTien() != null) {
                tongTienHang = tongTienHang.add(ct.getThanhTien());
            }
        }

        // Trừ tiền voucher
        BigDecimal tienGiam = BigDecimal.ZERO;
        if (hoaDon.getMaGiamGia() != null) {
            tienGiam = tinhMucGiamVoucher(hoaDon.getMaGiamGia(), tongTienHang);
        }

        // ⭐ CỘNG PHÍ SHIP (LẤY TỪ DATABASE)
        BigDecimal tienShip = hoaDon.getTienShip() != null ? hoaDon.getTienShip() : BigDecimal.ZERO;

        BigDecimal tongTienMoi = tongTienHang.subtract(tienGiam).add(tienShip);
        hoaDon.setTongTien(tongTienMoi);
        hoaDonService.save(hoaDon);

        System.out.println("✅ Cập nhật tổng tiền: " + tongTienMoi);
        System.out.println("   Tiền hàng: " + tongTienHang);
        System.out.println("   Giảm voucher: " + tienGiam);
        System.out.println("   Phí ship: " + tienShip);
    }

    private Map<String, Object> getGioHangData(String mahd) {
        Map<String, Object> data = new HashMap<>();

        HoaDon hoaDon = hoaDonService.findById(mahd);
        if (hoaDon == null) {
            data.put("danhSachSanPham", new ArrayList<>());
            data.put("tongTienHang", BigDecimal.ZERO);
            data.put("tienShip", BigDecimal.ZERO);
            data.put("maHoaDon", mahd);
            return data;
        }

        List<HoaDonChiTiet> chiTiets = hoaDonChiTietRepository.findByMaHoaDon_MaHoaDon(mahd);
        if (chiTiets == null) chiTiets = new ArrayList<>();

        List<Map<String, Object>> danhSach = new ArrayList<>();
        BigDecimal tongTienHang = BigDecimal.ZERO;

        for (HoaDonChiTiet ct : chiTiets) {
            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
            Map<String, Object> item = new HashMap<>();
            item.put("maSanPhamChiTiet", spct.getMaSanPhamChiTiet());
            String tenSanPham = spct.getSanPham().getTenSanPham()
                    + " [" + spct.getMauSac().getTenMauSac()
                    + " - " + spct.getKichThuoc().getTenKichThuoc() + "]";
            item.put("tenSanPham", tenSanPham);
            item.put("soLuong", ct.getSoLuong());
            item.put("donGia", ct.getDonGia());
            item.put("thanhTien", ct.getThanhTien());

            // ⭐ THÊM TỒN KHO VÀ TRẠNG THÁI
            int tonKho = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
            item.put("tonKho", tonKho);
            item.put("trangThai", spct.getTrangThai() != null ? spct.getTrangThai() : "Còn hàng");
            item.put("hetHang", tonKho <= 0);

            danhSach.add(item);
            tongTienHang = tongTienHang.add(ct.getThanhTien());
        }

        // Tính tiền giảm voucher
        BigDecimal tienGiamVoucher = BigDecimal.ZERO;
        GiamGia voucher = hoaDon.getMaGiamGia();

        if (voucher != null) {
            tienGiamVoucher = tinhMucGiamVoucher(voucher, tongTienHang);
        }

        // Tiền ship (lấy từ database)
        BigDecimal tienShip = hoaDon.getTienShip() != null ? hoaDon.getTienShip() : BigDecimal.ZERO;

        // Tính tổng tiền
        BigDecimal tongTien = tongTienHang.subtract(tienGiamVoucher).add(tienShip);

        // Cập nhật hóa đơn
        hoaDon.setTongTien(tongTien);
        hoaDonService.save(hoaDon);

        data.put("danhSachSanPham", danhSach);
        data.put("tongTienHang", tongTienHang);
        data.put("tienGiamVoucher", tienGiamVoucher);
        data.put("tienShip", tienShip);
        data.put("tongTien", tongTien);
        data.put("tongTienMoi", tongTien);
        data.put("maHoaDon", mahd);

        // ⭐ THÊM DANH SÁCH SẢN PHẨM HẾT HÀNG (ĐỂ FE XỬ LÝ)
        List<String> sanPhamHetHang = new ArrayList<>();
        for (Map<String, Object> item : danhSach) {
            Boolean hetHang = (Boolean) item.get("hetHang");
            if (hetHang != null && hetHang) {
                sanPhamHetHang.add((String) item.get("maSanPhamChiTiet"));
            }
        }
        data.put("sanPhamHetHang", sanPhamHetHang);

        if (voucher != null) {
            Map<String, Object> voucherInfo = new HashMap<>();
            voucherInfo.put("maGiamGia", voucher.getMaGiamGia());
            voucherInfo.put("tenGiamGia", voucher.getTenGiamGia());
            voucherInfo.put("tienGiam", tienGiamVoucher);
            data.put("voucher", voucherInfo);
        }

        return data;
    }

    private BigDecimal timGiamGiaTotNhat(String maSanPham) {
        List<DotGiamGia> listDgg = dotGiamGiaService.getBymasp(maSanPham);
        if (listDgg == null || listDgg.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return listDgg.stream()
                .filter(dgg -> "Hoạt động".equals(dgg.getTrangThai()))
                .filter(dgg -> dgg.getGiaTriGiam() != null)
                .filter(dgg -> dgg.getGiaTriGiam().compareTo(BigDecimal.ZERO) > 0)
                .map(dgg -> {
                    BigDecimal giam = dgg.getGiaTriGiam();
                    if (giam.compareTo(BigDecimal.valueOf(100)) <= 0) {
                        return giam;
                    }
                    return giam;
                })
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal tinhTienGiamVoucher(HoaDon hoaDon, BigDecimal tongTien) {
        if (hoaDon == null || hoaDon.getMaGiamGia() == null) return BigDecimal.ZERO;

        GiamGia gg = hoaDon.getMaGiamGia();

        // ⭐ LOG ĐỂ DEBUG
        System.out.println("=== TINH TIEN GIAM VOUCHER ===");
        System.out.println("Ma Giam Gia: " + gg.getMaGiamGia());
        System.out.println("Ten Giam Gia: " + gg.getTenGiamGia());
        System.out.println("Loai Giam Gia: " + gg.getLoaiGiamGia());
        System.out.println("Gia Tri Giam: " + gg.getGiaTriGiam());
        System.out.println("Giam Toi Da: " + gg.getGiamToiDa());
        System.out.println("Tong Tien: " + tongTien);

        // ⭐ KIỂM TRA VÔ HẠN - BỎ QUA KIỂM TRA SỐ LƯỢNG
        if (gg.getIsVoHan() == null || !gg.getIsVoHan()) {
            if (gg.getSoLuong() == null || gg.getSoLuong() <= 0) {
                System.out.println("-> Voucher het luot, tra ve 0");
                return BigDecimal.ZERO;
            }
        } else {
            System.out.println("-> Voucher vo han, bo qua kiem tra so luong");
        }

        // ⭐ KIỂM TRA TRẠNG THÁI
        String trangThai = giamGiaService.tinhToanTrangThai(gg);
        System.out.println("Trang Thai: " + trangThai);
        if (!"Hoạt động".equals(trangThai)) {
            System.out.println("-> Voucher khong hoat dong, tra ve 0");
            return BigDecimal.ZERO;
        }

        BigDecimal tienGiam = tinhMucGiamVoucher(gg, tongTien);
        System.out.println("-> Tien giam: " + tienGiam);
        System.out.println("=== END TINH TIEN GIAM VOUCHER ===");

        return tienGiam;
    }

    private BigDecimal tinhTongTienHoaDon(HoaDon hd, List<HoaDonChiTiet> listhdct) {
        if (listhdct == null || listhdct.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal tongTienHang = listhdct.stream()
                .map(HoaDonChiTiet::getThanhTien)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tienGiamVoucher = tinhTienGiamVoucher(hd, tongTienHang);
        BigDecimal tongTien = tongTienHang.subtract(tienGiamVoucher);

        if ("Online".equalsIgnoreCase(hd.getLoaiBan())) {
            // ⭐ LẤY PHÍ SHIP TỪ DB (CÓ THỂ LÀ 0)
            BigDecimal ship = hd.getTienShip();
            if (ship == null) {
                // ⭐ CHỈ KHI CHƯA CÓ PHÍ SHIP MỚI TÍNH
                ship = tinhPhiShipGHN(hd);
            }
            // ⭐ CỘNG PHÍ SHIP (CÓ THỂ LÀ 0)
            tongTien = tongTien.add(ship);

            System.out.println("🚚 Phí ship: " + ship);
        }

        return tongTien.max(BigDecimal.ZERO);
    }

    private BigDecimal tinhGiamGiaSanPham(SanPhamChiTiet spct) {
        if (spct == null || spct.getSanPham() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal giaGoc = spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO;
        BigDecimal giamMax = BigDecimal.ZERO;

        List<DotGiamGia> listDgg = dotGiamGiaService.getBymasp(spct.getSanPham().getMaSanPham());
        if (listDgg == null || listDgg.isEmpty()) {
            System.out.println("-> Khong co dot giam gia cho san pham: " + spct.getSanPham().getMaSanPham());
            return BigDecimal.ZERO;
        }

        System.out.println("=== TINH GIAM GIA SAN PHAM ===");
        System.out.println("Ma SP: " + spct.getSanPham().getMaSanPham());
        System.out.println("Gia goc: " + giaGoc);

        for (DotGiamGia dgg : listDgg) {
            System.out.println("  Dot giam: " + dgg.getMaGiamGia());
            System.out.println("  Gia tri giam: " + dgg.getGiaTriGiam());
            System.out.println("  Trang thai: " + dgg.getTrangThai());

            if (!"Hoạt động".equals(dgg.getTrangThai())) {
                System.out.println("  -> Bo qua (khong hoat dong)");
                continue;
            }

            if (dgg.getGiaTriGiam() == null || dgg.getGiaTriGiam().compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("  -> Bo qua (gia tri giam <= 0)");
                continue;
            }

            BigDecimal giam = dgg.getGiaTriGiam();
            BigDecimal giamTinh = BigDecimal.ZERO;

            // Nếu giá trị giảm <= 100 -> là phần trăm
            if (giam.compareTo(BigDecimal.valueOf(100)) <= 0) {
                BigDecimal phanTram = giam.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                giamTinh = giaGoc.multiply(phanTram);
                System.out.println("  Giam phan tram: " + giam + "% => " + giamTinh);
            } else {
                giamTinh = giam;
                System.out.println("  Giam so tien: " + giamTinh);
            }

            if (giamTinh.compareTo(giamMax) > 0) {
                giamMax = giamTinh;
                System.out.println("  -> Giam max hien tai: " + giamMax);
            }
        }

        System.out.println("=> Giam max: " + giamMax);
        System.out.println("=== END TINH GIAM GIA SAN PHAM ===");

        return giamMax.setScale(0, RoundingMode.HALF_UP);
    }

    @GetMapping("/test-location")
    @ResponseBody
    public ResponseEntity<?> testLocation(
            @RequestParam String ward,
            @RequestParam String district,
            @RequestParam String province) {

        Map<String, Object> result = ghnLocationService.getLocationInfo(ward, district, province);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/clear-cache")
    @ResponseBody
    public ResponseEntity<?> clearCache() {
        ghnLocationService.clearCache();
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa cache"));
    }

    @PostMapping("/tinh-phi-ship")
    @ResponseBody
    public Map<String, Object> tinhPhiShip(
            @RequestParam String district,
            @RequestParam String ward,
            @RequestParam String province,
            @RequestParam BigDecimal tongTien,
            @RequestParam(defaultValue = "1000") int weight) {

        Map<String, Object> map = new HashMap<>();

        try {
            BigDecimal phiShip = ghnShippingService.calculateShippingFee(
                    district,
                    ward,
                    province,  // ⭐ TRUYỀN PROVINCE
                    weight,
                    tongTien
            );

            map.put("shippingFee", phiShip);
            map.put("success", true);
            map.put("message", "Tính phí ship thành công!");

        } catch (Exception e) {
            map.put("success", false);
            map.put("message", "Lỗi: " + e.getMessage());
            map.put("shippingFee", BigDecimal.valueOf(30000));
        }

        return map;
    }

    private GiamGia timVoucherTotNhatChoHoaDon(HoaDon hoaDon, BigDecimal tongTien) {
        List<GiamGia> vouchers;
        KhachHang khachHang = hoaDon != null ? hoaDon.getMaKhachHang() : null;

        if (khachHang != null && !"0000000000".equals(khachHang.getSdt())) {
            vouchers = getVoucherChoKhachHang(khachHang);
        } else {
            vouchers = getVoucherCongKhai();
        }

        if (vouchers.isEmpty() || tongTien == null || tongTien.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        System.out.println("=== TIM VOUCHER TOT NHAT ===");
        System.out.println("👤 Khach hang: " + (khachHang != null ? khachHang.getMaKH() : "null"));
        System.out.println("💰 Tong tien: " + tongTien);
        System.out.println("📋 So voucher: " + vouchers.size());

        GiamGia bestVoucher = null;
        BigDecimal maxGiam = BigDecimal.ZERO;
        GiamGia currentVoucher = hoaDon != null ? hoaDon.getMaGiamGia() : null;

        // ⭐ LƯU TIỀN GIẢM CỦA VOUCHER HIỆN TẠI
        BigDecimal currentDiscount = BigDecimal.ZERO;
        if (currentVoucher != null) {
            currentDiscount = tinhMucGiamVoucher(currentVoucher, tongTien);
            System.out.println("📌 Voucher hien tai: " + currentVoucher.getMaGiamGia() +
                    " - Tien giam: " + currentDiscount);
        }

        for (GiamGia gg : vouchers) {
            System.out.println("--- Kiem tra voucher: " + gg.getMaGiamGia() + " - " + gg.getTenGiamGia());

            // ⭐⭐⭐ KHÔNG BỎ QUA VOUCHER HIỆN TẠI NỮA
            // Bỏ comment dòng này đi
            // if (currentVoucher != null && currentVoucher.getMaGiamGia().equals(gg.getMaGiamGia())) {
            //     System.out.println("  ❌ Da la voucher hien tai, bo qua");
            //     continue;
            // }

            // Kiểm tra trạng thái
            Map<String, Object> checkResult = kiemTraVoucherHienTai(gg, tongTien, hoaDon);
            if (checkResult != null) {
                System.out.println("  ❌ " + checkResult.get("message"));
                continue;
            }

            BigDecimal giam = tinhMucGiamVoucher(gg, tongTien);
            System.out.println("  ✅ Voucher hop le, tien giam: " + giam);

            // ⭐ SO SÁNH VỚI TIỀN GIẢM HIỆN TẠI
            if (giam.compareTo(maxGiam) > 0) {
                maxGiam = giam;
                bestVoucher = gg;
                System.out.println("  ⭐ Voucher nay dang tot nhat!");
            }
        }

        // ⭐ NẾU KHÔNG TÌM THẤY VOUCHER NÀO TỐT HƠN HOẶC BẰNG VOUCHER HIỆN TẠI,
        // TRẢ VỀ VOUCHER HIỆN TẠI NẾU NÓ VẪN HỢP LỆ
        if (bestVoucher == null && currentVoucher != null) {
            // Kiểm tra voucher hiện tại vẫn hợp lệ
            Map<String, Object> checkResult = kiemTraVoucherHienTai(currentVoucher, tongTien, hoaDon);
            if (checkResult == null) {
                System.out.println("📌 Giu voucher hien tai: " + currentVoucher.getMaGiamGia());
                return currentVoucher;
            } else {
                System.out.println("⚠️ Voucher hien tai khong con hop le: " + checkResult.get("message"));
                return null;
            }
        }

        System.out.println("=== KET QUA: " + (bestVoucher != null ? bestVoucher.getMaGiamGia() : "KHONG CO") + " ===");
        return bestVoucher;
    }

    private List<GiamGia> getVoucherChoKhachHang(KhachHang khachHang) {
        List<GiamGia> allVouchers = giamGiaService.getGiamGia1();
        if (allVouchers == null || allVouchers.isEmpty()) {
            return new ArrayList<>();
        }

        List<GiamGia> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // ⭐ Lấy danh sách voucher cá nhân đã sử dụng của khách hàng
        List<String> usedPersonalVoucherIds = new ArrayList<>();
        if (khachHang != null) {
            try {
                List<GiamGiaChiTiet> personalVouchers = giamGiaChiTietRepository.findByKhachHang_MaKH(khachHang.getMaKH());
                if (personalVouchers != null) {
                    usedPersonalVoucherIds = personalVouchers.stream()
                            .filter(ct -> ct.getTrangThaiSuDung() != null && ct.getTrangThaiSuDung() == 1)
                            .map(ct -> ct.getGiamGia().getMaGiamGia())
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                System.out.println("⚠️ Khong the lay danh sach voucher da su dung: " + e.getMessage());
            }
        }

        for (GiamGia gg : allVouchers) {
            // Kiểm tra trạng thái
            String trangThai = giamGiaService.tinhToanTrangThai(gg);
            if (!"Hoạt động".equals(trangThai)) {
                continue;
            }

            // Kiểm tra ngày hiệu lực
            if (gg.getNgayBatDau() != null && gg.getNgayBatDau().isAfter(now)) {
                continue;
            }
            if (gg.getNgayKetThuc() != null && gg.getNgayKetThuc().isBefore(now)) {
                continue;
            }

            // Kiểm tra số lượng
            if (gg.getIsVoHan() == null || !gg.getIsVoHan()) {
                if (gg.getSoLuong() == null || gg.getSoLuong() <= 0) {
                    continue;
                }
            }

            // Loại 1 = Công khai
            if (gg.getLoaiApDung() != null && gg.getLoaiApDung() == 1) {
                result.add(gg);
            }
            // Loại 2 = Cá nhân
            else if (gg.getLoaiApDung() != null && gg.getLoaiApDung() == 2) {
                if (khachHang != null && !"0000000000".equals(khachHang.getSdt())) {
                    // ⭐ BỎ QUA VOUCHER ĐÃ SỬ DỤNG
                    if (usedPersonalVoucherIds.contains(gg.getMaGiamGia())) {
                        System.out.println("⏭️ Bo qua voucher ca nhan da su dung: " + gg.getMaGiamGia());
                        continue;
                    }

                    // ⭐ KIỂM TRA KHÁCH HÀNG CÓ ĐƯỢC ÁP DỤNG KHÔNG
                    boolean isEligible = kiemTraVoucherChoKhachHang(gg, khachHang);
                    if (isEligible) {
                        result.add(gg);
                    }
                }
            } else {
                // Không xác định loại -> mặc định công khai
                result.add(gg);
            }
        }

        // Sắp xếp
        result.sort((a, b) -> {
            BigDecimal giamA = a.getGiaTriGiam() != null ? a.getGiaTriGiam() : BigDecimal.ZERO;
            BigDecimal giamB = b.getGiaTriGiam() != null ? b.getGiaTriGiam() : BigDecimal.ZERO;

            if ("Tien".equalsIgnoreCase(a.getLoaiGiamGia()) && !"Tien".equalsIgnoreCase(b.getLoaiGiamGia())) {
                return -1;
            }
            if (!"Tien".equalsIgnoreCase(a.getLoaiGiamGia()) && "Tien".equalsIgnoreCase(b.getLoaiGiamGia())) {
                return 1;
            }
            return giamB.compareTo(giamA);
        });

        return result;
    }

    private List<GiamGia> getVoucherCongKhai() {
        List<GiamGia> allVouchers = giamGiaService.getGiamGia1();
        if (allVouchers == null || allVouchers.isEmpty()) {
            return new ArrayList<>();
        }

        List<GiamGia> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (GiamGia gg : allVouchers) {
            // ⭐ KIỂM TRA TRẠNG THÁI: Chỉ lấy "Hoạt động"
            String trangThai = giamGiaService.tinhToanTrangThai(gg);
            if (!"Hoạt động".equals(trangThai)) {
                continue;
            }

            // Kiểm tra ngày hiệu lực
            if (gg.getNgayBatDau() != null && gg.getNgayBatDau().isAfter(now)) {
                continue;
            }
            if (gg.getNgayKetThuc() != null && gg.getNgayKetThuc().isBefore(now)) {
                continue;
            }

            // Kiểm tra số lượng
            if (gg.getIsVoHan() == null || !gg.getIsVoHan()) {
                if (gg.getSoLuong() == null || gg.getSoLuong() <= 0) {
                    continue;
                }
            }

            // Chỉ lấy voucher công khai (loại 1) hoặc không xác định
            if (gg.getLoaiApDung() == null || gg.getLoaiApDung() == 1) {
                result.add(gg);
            }
        }

        // Sắp xếp
        result.sort((a, b) -> {
            BigDecimal giamA = a.getGiaTriGiam() != null ? a.getGiaTriGiam() : BigDecimal.ZERO;
            BigDecimal giamB = b.getGiaTriGiam() != null ? b.getGiaTriGiam() : BigDecimal.ZERO;

            if ("Tien".equalsIgnoreCase(a.getLoaiGiamGia()) && !"Tien".equalsIgnoreCase(b.getLoaiGiamGia())) {
                return -1;
            }
            if (!"Tien".equalsIgnoreCase(a.getLoaiGiamGia()) && "Tien".equalsIgnoreCase(b.getLoaiGiamGia())) {
                return 1;
            }
            return giamB.compareTo(giamA);
        });

        return result;
    }

    private boolean kiemTraVoucherChoKhachHang(GiamGia voucher, KhachHang khachHang) {
        if (voucher == null || khachHang == null) {
            return false;
        }

        try {
            // Loại 1 = Công khai: ai cũng được áp dụng
            if (voucher.getLoaiApDung() != null && voucher.getLoaiApDung() == 1) {
                return true;
            }

            // Loại 2 = Cá nhân: kiểm tra trong bảng KHACHHANG_VOUCHER
            if (voucher.getLoaiApDung() != null && voucher.getLoaiApDung() == 2) {
                boolean exists = giamGiaChiTietRepository.existsById_MaGiamGiaAndId_MaKhachHang(
                        voucher.getMaGiamGia(),
                        khachHang.getMaKH()
                );
                return exists;
            }

            // Nếu không xác định loại, mặc định là công khai
            return true;
        } catch (Exception e) {
            System.err.println("❌ Lỗi kiểm tra voucher: " + e.getMessage());
            return false;
        }
    }

    @GetMapping("/voucher-goi-y")
    @ResponseBody
    public Map<String, Object> getVoucherGoiY(@RequestParam("mahd") String mahd) {
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("========== GET VOUCHER GOI Y ==========");
            System.out.println("📋 mahd: " + mahd);

            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy hóa đơn!");
                return response;
            }

            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            if (listhdct == null || listhdct.isEmpty()) {
                response.put("success", false);
                response.put("message", "Hóa đơn chưa có sản phẩm!");
                return response;
            }

            BigDecimal tongTien = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // ⭐ LẤY TẤT CẢ VOUCHER HOẠT ĐỘNG
            List<GiamGia> allVouchers = giamGiaService.getGiamGia1();
            KhachHang khachHang = hoaDon.getMaKhachHang();

            // ⭐ LẤY DANH SÁCH VOUCHER CÁ NHÂN CỦA KHÁCH HÀNG (ĐỂ CHECK daSuDung)
            List<GiamGiaChiTiet> personalVoucherDetails = new ArrayList<>();
            if (khachHang != null && !"0000000000".equals(khachHang.getSdt())) {
                personalVoucherDetails = giamGiaChiTietRepository.findByKhachHang_MaKH(khachHang.getMaKH());
                System.out.println("📌 Số voucher cá nhân của KH: " + personalVoucherDetails.size());
            }

            // ⭐ PHÂN LOẠI VOUCHER
            List<GiamGia> publicVouchers = new ArrayList<>();
            List<GiamGia> personalVouchers = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (GiamGia gg : allVouchers) {
                // Kiểm tra trạng thái
                String trangThai = giamGiaService.tinhToanTrangThai(gg);
                if (!"Hoạt động".equals(trangThai)) continue;

                // Kiểm tra số lượng
                if (gg.getIsVoHan() == null || !gg.getIsVoHan()) {
                    if (gg.getSoLuong() == null || gg.getSoLuong() <= 0) continue;
                }

                // ⭐ PHÂN LOẠI THEO LoaiApDung
                if (gg.getLoaiApDung() == null || gg.getLoaiApDung() == 1) {
                    publicVouchers.add(gg);
                } else if (gg.getLoaiApDung() == 2) {
                    if (khachHang != null && !"0000000000".equals(khachHang.getSdt())) {
                        boolean isEligible = kiemTraVoucherChoKhachHang(gg, khachHang);
                        if (isEligible) {
                            personalVouchers.add(gg);
                        }
                    }
                }
            }

            System.out.println("========== VOUCHER GOI Y ==========");
            System.out.println("Voucher công khai: " + publicVouchers.size());
            System.out.println("Voucher cá nhân: " + personalVouchers.size());
            System.out.println("Tổng: " + (publicVouchers.size() + personalVouchers.size()));

            // ⭐ GỘP DANH SÁCH
            List<GiamGia> combinedVouchers = new ArrayList<>();
            combinedVouchers.addAll(publicVouchers);
            combinedVouchers.addAll(personalVouchers);

            // ⭐ TẠO RESPONSE
            List<Map<String, Object>> voucherList = new ArrayList<>();
            GiamGia currentVoucher = hoaDon.getMaGiamGia();

            for (GiamGia gg : combinedVouchers) {
                Map<String, Object> item = new HashMap<>();
                item.put("maGiamGia", gg.getMaGiamGia());
                item.put("tenGiamGia", gg.getTenGiamGia());
                item.put("loaiGiamGia", gg.getLoaiGiamGia());
                item.put("giaTriGiam", gg.getGiaTriGiam());
                item.put("giamToiDa", gg.getGiamToiDa());
                item.put("soLuong", gg.getSoLuong());
                item.put("isVoHan", gg.getIsVoHan());
                item.put("donToiThieu", gg.getDonToiThieu());

                if (gg.getNgayKetThuc() != null) {
                    item.put("ngayKetThuc", gg.getNgayKetThuc().format(formatter));
                } else {
                    item.put("ngayKetThuc", null);
                }

                if (gg.getNgayBatDau() != null) {
                    item.put("ngayBatDau", gg.getNgayBatDau().format(formatter));
                } else {
                    item.put("ngayBatDau", null);
                }

                item.put("loaiApDung", gg.getLoaiApDung());
                item.put("loaiApDungText", gg.getLoaiApDung() != null && gg.getLoaiApDung() == 1 ? "Công khai" : "Cá nhân");

                String typeBadge = "";
                if (gg.getLoaiApDung() != null && gg.getLoaiApDung() == 2) {
                    typeBadge = "<span class='badge bg-danger ms-1' style='font-size:8px;'>👤 Cá nhân</span>";
                } else {
                    typeBadge = "<span class='badge bg-primary ms-1' style='font-size:8px;'>🌐 Công khai</span>";
                }
                item.put("typeBadge", typeBadge);

                BigDecimal tienGiam = tinhMucGiamVoucher(gg, tongTien);
                item.put("tienGiam", tienGiam);

                // ⭐⭐ QUAN TRỌNG: KIỂM TRA VOUCHER CÁ NHÂN ĐÃ SỬ DỤNG
                boolean daSuDung = false;
                boolean isEligible = true;
                String status = "Sẵn sàng áp dụng";
                String statusClass = "success";
                BigDecimal canThem = BigDecimal.ZERO;

                // ⭐ NẾU LÀ VOUCHER CÁ NHÂN, KIỂM TRA ĐÃ SỬ DỤNG
                if (gg.getLoaiApDung() != null && gg.getLoaiApDung() == 2) {
                    // Tìm trong danh sách voucher cá nhân của khách hàng
                    for (GiamGiaChiTiet ct : personalVoucherDetails) {
                        if (ct.getGiamGia() != null && ct.getGiamGia().getMaGiamGia().equals(gg.getMaGiamGia())) {
                            // ⭐ KIỂM TRA TRẠNG THÁI SỬ DỤNG
                            if (ct.getTrangThaiSuDung() != null && ct.getTrangThaiSuDung() == 1) {
                                daSuDung = true;
                                isEligible = false;
                                status = "Đã sử dụng";
                                statusClass = "secondary";
                                System.out.println("⚠️ Voucher cá nhân đã sử dụng: " + gg.getMaGiamGia() + " - " + gg.getTenGiamGia());
                            }
                            break;
                        }
                    }
                }

                // ⭐ NẾU CHƯA SỬ DỤNG, KIỂM TRA ĐIỀU KIỆN KHÁC
                if (!daSuDung) {
                    // Kiểm tra đơn tối thiểu
                    if (gg.getDonToiThieu() != null && gg.getDonToiThieu().compareTo(BigDecimal.ZERO) > 0
                            && tongTien.compareTo(gg.getDonToiThieu()) < 0) {
                        isEligible = false;
                        canThem = gg.getDonToiThieu().subtract(tongTien);
                        status = "Cần thêm " + formatCurrency(canThem);
                        statusClass = "warning";
                        item.put("canThem", canThem);
                    }

                    // Kiểm tra nếu đang áp dụng
                    if (currentVoucher != null && currentVoucher.getMaGiamGia().equals(gg.getMaGiamGia())) {
                        status = "✅ Đang áp dụng";
                        statusClass = "info";
                        isEligible = true;
                        item.put("isApplied", true);
                    } else {
                        item.put("isApplied", false);
                    }
                } else {
                    // Đã sử dụng -> không thể áp dụng
                    item.put("isApplied", false);
                }

                String soLuongDisplay = "";
                if (gg.getIsVoHan() != null && gg.getIsVoHan()) {
                    soLuongDisplay = "♾️ Không giới hạn";
                } else {
                    soLuongDisplay = "Còn: " + (gg.getSoLuong() != null ? gg.getSoLuong() : 0) + " lượt";
                }
                item.put("soLuongDisplay", soLuongDisplay);
                item.put("isEligible", isEligible);
                item.put("status", status);
                item.put("statusClass", statusClass);

                // ⭐⭐ QUAN TRỌNG: THÊM FIELD daSuDung VÀO RESPONSE
                item.put("daSuDung", daSuDung);

                voucherList.add(item);
            }

            // ⭐ SẮP XẾP
            voucherList.sort((a, b) -> {
                // 1. Đang áp dụng lên đầu
                boolean aApplied = (boolean) a.get("isApplied");
                boolean bApplied = (boolean) b.get("isApplied");
                if (aApplied && !bApplied) return -1;
                if (!aApplied && bApplied) return 1;

                // 2. Công khai lên trước cá nhân
                Integer aLoai = (Integer) a.get("loaiApDung");
                Integer bLoai = (Integer) b.get("loaiApDung");
                if (aLoai != null && bLoai != null) {
                    if (aLoai == 1 && bLoai == 2) return -1;
                    if (aLoai == 2 && bLoai == 1) return 1;
                }

                // 3. Đã sử dụng xuống cuối
                boolean aUsed = (boolean) a.getOrDefault("daSuDung", false);
                boolean bUsed = (boolean) b.getOrDefault("daSuDung", false);
                if (aUsed && !bUsed) return 1;
                if (!aUsed && bUsed) return -1;

                // 4. Có thể áp dụng lên sau
                boolean aEligible = (boolean) a.get("isEligible");
                boolean bEligible = (boolean) b.get("isEligible");
                if (aEligible && !bEligible) return -1;
                if (!aEligible && bEligible) return 1;

                // 5. Chưa đủ điều kiện: sắp xếp theo số tiền thiếu
                if (!aEligible && !bEligible) {
                    BigDecimal aCanThem = (BigDecimal) a.getOrDefault("canThem", BigDecimal.ZERO);
                    BigDecimal bCanThem = (BigDecimal) b.getOrDefault("canThem", BigDecimal.ZERO);
                    return aCanThem.compareTo(bCanThem);
                }

                // 6. Cả 2 đều đủ điều kiện: sắp xếp theo tiền giảm
                BigDecimal aGiam = (BigDecimal) a.get("tienGiam");
                BigDecimal bGiam = (BigDecimal) b.get("tienGiam");
                return bGiam.compareTo(aGiam);
            });

            response.put("success", true);
            response.put("vouchers", voucherList);
            response.put("tongTien", tongTien);
            response.put("currentVoucher", currentVoucher != null ? currentVoucher.getMaGiamGia() : null);
            response.put("totalVouchers", combinedVouchers.size());
            response.put("displayVouchers", voucherList.size());
            response.put("publicCount", publicVouchers.size());
            response.put("personalCount", personalVouchers.size());

            // ⭐ LOG KẾT QUẢ
            long usedCount = voucherList.stream().filter(v -> (Boolean) v.get("daSuDung")).count();
            System.out.println("📊 Voucher đã sử dụng (bị ẩn): " + usedCount);
            System.out.println("========== END GET VOUCHER GOI Y ==========");

            // ⭐ Tìm voucher tốt nhất (bỏ qua voucher đã sử dụng)
            GiamGia bestVoucher = null;
            BigDecimal maxGiam = BigDecimal.ZERO;

            for (GiamGia gg : combinedVouchers) {
                // ⭐ BỎ QUA VOUCHER ĐÃ SỬ DỤNG
                boolean isUsed = false;
                for (GiamGiaChiTiet ct : personalVoucherDetails) {
                    if (ct.getGiamGia() != null &&
                            ct.getGiamGia().getMaGiamGia().equals(gg.getMaGiamGia()) &&
                            ct.getTrangThaiSuDung() != null &&
                            ct.getTrangThaiSuDung() == 1) {
                        isUsed = true;
                        break;
                    }
                }
                if (isUsed) continue;

                if (gg.getDonToiThieu() != null && gg.getDonToiThieu().compareTo(BigDecimal.ZERO) > 0
                        && tongTien.compareTo(gg.getDonToiThieu()) < 0) {
                    continue;
                }

                BigDecimal giam = tinhMucGiamVoucher(gg, tongTien);
                if (giam.compareTo(maxGiam) > 0) {
                    maxGiam = giam;
                    bestVoucher = gg;
                }
            }

            if (bestVoucher != null) {
                response.put("bestVoucher", Map.of(
                        "maGiamGia", bestVoucher.getMaGiamGia(),
                        "tenGiamGia", bestVoucher.getTenGiamGia(),
                        "tienGiam", tinhMucGiamVoucher(bestVoucher, tongTien),
                        "isCurrent", currentVoucher != null && currentVoucher.getMaGiamGia().equals(bestVoucher.getMaGiamGia())
                ));
            } else {
                response.put("bestVoucher", null);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/tao-qr-sanpham")
    @ResponseBody
    public Map<String, Object> taoQRSanPham(@RequestParam("mactsp") String mactsp,
                                            @RequestParam(value = "sluong", defaultValue = "1") int sluong) {
        Map<String, Object> response = new HashMap<>();
        try {
            String qrData = mactsp + "|" + sluong;
            String encoded = java.util.Base64.getEncoder().encodeToString(qrData.getBytes());
            String qrUrl = "http://localhost:8080/banhang/quet-qr?data=" + encoded;
            String qrBase64 = generateQRCodeBase64(qrUrl);
            response.put("success", true);
            response.put("qrData", qrData);
            response.put("qrCode", qrBase64);
            response.put("qrUrl", qrUrl);
            response.put("mactsp", mactsp);
            response.put("sluong", sluong);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }

    @GetMapping("/getInvoiceTotal/{mahd}")
    @ResponseBody
    public Map<String, Object> getInvoiceTotal(@PathVariable("mahd") String mahd) {
        Map<String, Object> response = new HashMap<>();
        try {
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy hóa đơn!");
                return response;
            }

            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            if (listhdct == null || listhdct.isEmpty()) {
                response.put("success", false);
                response.put("message", "Hóa đơn chưa có sản phẩm!");
                return response;
            }

            BigDecimal tongTien = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Trừ voucher
            if (hoaDon.getMaGiamGia() != null) {
                BigDecimal tienGiam = tinhMucGiamVoucher(hoaDon.getMaGiamGia(), tongTien);
                tongTien = tongTien.subtract(tienGiam);
            }

            // Cộng phí ship
            if ("Online".equalsIgnoreCase(hoaDon.getLoaiBan())) {
                BigDecimal ship = hoaDon.getTienShip() != null ? hoaDon.getTienShip() : BigDecimal.ZERO;
                tongTien = tongTien.add(ship);
            }

            response.put("success", true);
            response.put("total", tongTien);
            response.put("tongTienHang", tongTien);
            response.put("maHoaDon", mahd);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/kiemtravoucher/{maHoaDon}")
    @ResponseBody
    public Map<String, Object> kiemTraVoucher(@PathVariable("maHoaDon") String maHoaDon) {
        Map<String, Object> response = new HashMap<>();
        try {
            HoaDon hoaDon = hoaDonService.findById(maHoaDon);
            if (hoaDon == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy hóa đơn!");
                return response;
            }

            // Lấy danh sách sản phẩm
            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(maHoaDon);
            if (listhdct == null || listhdct.isEmpty()) {
                response.put("success", false);
                response.put("message", "Hóa đơn chưa có sản phẩm!");
                return response;
            }

            BigDecimal tongTien = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<Map<String, Object>> warnings = new ArrayList<>();
            boolean hasWarning = false;
            GiamGia currentVoucher = hoaDon.getMaGiamGia();

            // ===== 1. Nếu CÓ voucher đang áp dụng =====
            if (currentVoucher != null) {
                Map<String, Object> checkResult = kiemTraVoucherHienTai(currentVoucher, tongTien, hoaDon);
                if (checkResult != null) {
                    hasWarning = true;
                    warnings.add(checkResult);
                }

                // ===== 2. Kiểm tra voucher TỐT HƠN =====
                if (!hasWarning) {
                    GiamGia betterVoucher = timVoucherTotNhatChoHoaDon(hoaDon, tongTien);
                    if (betterVoucher != null && !betterVoucher.getMaGiamGia().equals(currentVoucher.getMaGiamGia())) {
                        BigDecimal currentDiscount = tinhMucGiamVoucher(currentVoucher, tongTien);
                        BigDecimal betterDiscount = tinhMucGiamVoucher(betterVoucher, tongTien);

                        if (betterDiscount.compareTo(currentDiscount) > 0) {
                            hasWarning = true;
                            Map<String, Object> warning = new HashMap<>();
                            warning.put("type", "BETTER_VOUCHER");
                            warning.put("maVoucher", betterVoucher.getMaGiamGia());
                            warning.put("tenVoucher", betterVoucher.getTenGiamGia());
                            warning.put("message", "Có voucher tốt hơn: " + betterVoucher.getTenGiamGia());
                            warning.put("currentDiscount", currentDiscount);
                            warning.put("betterDiscount", betterDiscount);
                            warnings.add(warning);
                        }
                    }
                }
            } else {
                // ===== 3. Nếu KHÔNG có voucher, kiểm tra có voucher nào khả dụng không =====
                GiamGia bestVoucher = timVoucherTotNhatChoHoaDon(hoaDon, tongTien);
                if (bestVoucher != null) {
                    hasWarning = true;
                    Map<String, Object> warning = new HashMap<>();
                    warning.put("type", "VOUCHER_AVAILABLE");
                    warning.put("maVoucher", bestVoucher.getMaGiamGia());
                    warning.put("tenVoucher", bestVoucher.getTenGiamGia());
                    warning.put("message", "Có voucher khả dụng: " + bestVoucher.getTenGiamGia());
                    warning.put("tienGiam", tinhMucGiamVoucher(bestVoucher, tongTien));
                    warnings.add(warning);
                }
            }

            response.put("success", true);
            response.put("hasWarning", hasWarning);
            response.put("warnings", warnings);
            response.put("voucherHienTai", currentVoucher != null ?
                    currentVoucher.getMaGiamGia() + " - " + currentVoucher.getTenGiamGia() :
                    "Chưa có voucher");
            response.put("tongTien", tongTien);
            response.put("message", hasWarning ? "Có " + warnings.size() + " cảnh báo voucher" : "Voucher hợp lệ");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/update-vouchers")
    @ResponseBody
    public Map<String, Object> updateVouchers(
            @RequestParam("mahd") String mahd,
            @RequestParam(value = "maKH", required = false) String maKH) {

        Map<String, Object> response = new HashMap<>();
        try {
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy hóa đơn!");
                return response;
            }

            // Cập nhật khách hàng nếu có maKH
            if (maKH != null && !maKH.isEmpty()) {
                KhachHang khachHang = khachHangService.getKhachHangById(maKH);
                if (khachHang != null) {
                    hoaDon.setMaKhachHang(khachHang);
                    hoaDonService.save(hoaDon);
                }
            }

            // Lấy danh sách voucher theo khách hàng
            List<GiamGia> vouchers = getVoucherChoHoaDon(hoaDon);

            // Lấy tổng tiền hiện tại
            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            BigDecimal tongTien = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Chuyển đổi sang DTO
            List<Map<String, Object>> voucherList = new ArrayList<>();
            GiamGia currentVoucher = hoaDon.getMaGiamGia();

            for (GiamGia gg : vouchers) {
                Map<String, Object> item = new HashMap<>();
                item.put("maGiamGia", gg.getMaGiamGia());
                item.put("tenGiamGia", gg.getTenGiamGia());
                item.put("loaiGiamGia", gg.getLoaiGiamGia());
                item.put("giaTriGiam", gg.getGiaTriGiam());
                item.put("giamToiDa", gg.getGiamToiDa());
                item.put("soLuong", gg.getSoLuong());
                item.put("isVoHan", gg.getIsVoHan());
                item.put("donToiThieu", gg.getDonToiThieu());
                item.put("loaiApDung", gg.getLoaiApDung());
                item.put("loaiApDungText", gg.getLoaiApDung() == 1 ? "Công khai" : "Cá nhân");
                item.put("isApplied", currentVoucher != null && currentVoucher.getMaGiamGia().equals(gg.getMaGiamGia()));

                // Tính tiền giảm
                BigDecimal tienGiam = tinhMucGiamVoucher(gg, tongTien);
                item.put("tienGiam", tienGiam);

                // Kiểm tra điều kiện áp dụng
                boolean isEligible = true;
                String status = "Sẵn sàng áp dụng";
                String statusClass = "success";
                BigDecimal canThem = BigDecimal.ZERO;

                if (gg.getDonToiThieu() != null && gg.getDonToiThieu().compareTo(BigDecimal.ZERO) > 0
                        && tongTien.compareTo(gg.getDonToiThieu()) < 0) {
                    isEligible = false;
                    canThem = gg.getDonToiThieu().subtract(tongTien);
                    status = "Cần thêm " + formatCurrency(canThem);
                    statusClass = "warning";
                    item.put("canThem", canThem);
                }

                // Kiểm tra nếu đang áp dụng voucher này
                if (item.get("isApplied") == Boolean.TRUE) {
                    status = "✅ Đang áp dụng";
                    statusClass = "info";
                    isEligible = true;
                }

                item.put("isEligible", isEligible);
                item.put("status", status);
                item.put("statusClass", statusClass);

                voucherList.add(item);
            }

            // Tìm voucher tốt nhất
            GiamGia bestVoucher = timVoucherTotNhatChoHoaDon(hoaDon, tongTien);

            response.put("success", true);
            response.put("vouchers", voucherList);
            response.put("count", voucherList.size());
            response.put("khachHang", hoaDon.getMaKhachHang() != null ?
                    hoaDon.getMaKhachHang().getHoTen() : "Khách lẻ");
            response.put("maKH", hoaDon.getMaKhachHang() != null ?
                    hoaDon.getMaKhachHang().getMaKH() : null);
            response.put("currentVoucher", currentVoucher != null ? currentVoucher.getMaGiamGia() : null);

            if (bestVoucher != null) {
                response.put("bestVoucher", Map.of(
                        "maGiamGia", bestVoucher.getMaGiamGia(),
                        "tenGiamGia", bestVoucher.getTenGiamGia(),
                        "tienGiam", tinhMucGiamVoucher(bestVoucher, tongTien),
                        "isCurrent", currentVoucher != null && currentVoucher.getMaGiamGia().equals(bestVoucher.getMaGiamGia())
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    private List<GiamGia> getVoucherChoHoaDon(HoaDon hoaDon) {
        if (hoaDon == null) {
            return getVoucherCongKhai();
        }

        KhachHang khachHang = hoaDon.getMaKhachHang();
        if (khachHang != null && !"0000000000".equals(khachHang.getSdt())) {
            return getVoucherChoKhachHang(khachHang);
        }
        return getVoucherCongKhai();
    }

    @PostMapping("/chonkh-ajax")
    @ResponseBody
    public Map<String, Object> chonKhachHangAjax(@RequestParam("mahd") String mahd,
                                                 @RequestParam("makh") String makh) {
        Map<String, Object> response = new HashMap<>();
        try {
            HoaDon hd = hoaDonService.findById(mahd);
            if (hd == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy hóa đơn!");
                return response;
            }

            KhachHang kh = khachHangService.getKhachHangById(makh);
            if (kh == null) {
                response.put("success", false);
                response.put("message", "Khách hàng không tồn tại!");
                return response;
            }

            if ("0000000000".equals(kh.getSdt())) {
                response.put("success", false);
                response.put("message", "Không thể chọn khách hàng mặc định!");
                return response;
            }

            // Cập nhật khách hàng cho hóa đơn
            hd.setMaKhachHang(kh);
            hoaDonService.save(hd);

            // Lấy danh sách voucher cho khách hàng
            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            BigDecimal tongTien = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Lấy voucher theo khách hàng (công khai + cá nhân)
            List<GiamGia> vouchers = getVoucherChoKhachHang(kh);

            // Chuyển đổi sang DTO
            List<Map<String, Object>> voucherList = new ArrayList<>();
            GiamGia currentVoucher = hd.getMaGiamGia();

            for (GiamGia gg : vouchers) {
                Map<String, Object> item = new HashMap<>();
                item.put("maGiamGia", gg.getMaGiamGia());
                item.put("tenGiamGia", gg.getTenGiamGia());
                item.put("loaiGiamGia", gg.getLoaiGiamGia());
                item.put("giaTriGiam", gg.getGiaTriGiam());
                item.put("giamToiDa", gg.getGiamToiDa());
                item.put("soLuong", gg.getSoLuong());
                item.put("isVoHan", gg.getIsVoHan());
                item.put("donToiThieu", gg.getDonToiThieu());
                item.put("loaiApDung", gg.getLoaiApDung());
                item.put("loaiApDungText", gg.getLoaiApDung() != null && gg.getLoaiApDung() == 1 ? "Công khai" : "Cá nhân");
                item.put("isApplied", currentVoucher != null && currentVoucher.getMaGiamGia().equals(gg.getMaGiamGia()));

                // Tính tiền giảm
                BigDecimal tienGiam = tinhMucGiamVoucher(gg, tongTien);
                item.put("tienGiam", tienGiam);

                // Kiểm tra điều kiện áp dụng
                boolean isEligible = true;
                String status = "Sẵn sàng áp dụng";
                String statusClass = "success";
                BigDecimal canThem = BigDecimal.ZERO;

                if (gg.getDonToiThieu() != null && gg.getDonToiThieu().compareTo(BigDecimal.ZERO) > 0
                        && tongTien.compareTo(gg.getDonToiThieu()) < 0) {
                    isEligible = false;
                    canThem = gg.getDonToiThieu().subtract(tongTien);
                    status = "Cần thêm " + formatCurrency(canThem);
                    statusClass = "warning";
                    item.put("canThem", canThem);
                }

                if (item.get("isApplied") == Boolean.TRUE) {
                    status = "✅ Đang áp dụng";
                    statusClass = "info";
                    isEligible = true;
                }

                item.put("isEligible", isEligible);
                item.put("status", status);
                item.put("statusClass", statusClass);

                voucherList.add(item);
            }

            // Tìm voucher tốt nhất
            GiamGia bestVoucher = timVoucherTotNhatChoHoaDon(hd, tongTien);

            response.put("success", true);
            response.put("message", "Đã chọn khách hàng: " + kh.getHoTen());
            response.put("vouchers", voucherList);
            response.put("count", voucherList.size());
            response.put("khachHang", Map.of(
                    "maKH", kh.getMaKH(),
                    "hoTen", kh.getHoTen(),
                    "sdt", kh.getSdt()
            ));
            response.put("loaiKhachHang", "khachhang");
            response.put("currentVoucher", currentVoucher != null ? currentVoucher.getMaGiamGia() : null);

            if (bestVoucher != null) {
                response.put("bestVoucher", Map.of(
                        "maGiamGia", bestVoucher.getMaGiamGia(),
                        "tenGiamGia", bestVoucher.getTenGiamGia(),
                        "tienGiam", tinhMucGiamVoucher(bestVoucher, tongTien),
                        "isCurrent", currentVoucher != null && currentVoucher.getMaGiamGia().equals(bestVoucher.getMaGiamGia())
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
        }
        return response;
    }

    @GetMapping("/get-vouchers")
    @ResponseBody
    public Map<String, Object> getVouchers(@RequestParam("mahd") String mahd,
                                           @RequestParam(value = "maKH", required = false) String maKH) {
        Map<String, Object> response = new HashMap<>();
        try {
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy hóa đơn!");
                return response;
            }

            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            BigDecimal tongTien = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Lấy danh sách voucher
            List<GiamGia> vouchers;
            KhachHang khachHang = null;
            String tenKhachHang = "Khách lẻ";
            String loaiKhachHang = "vanglai";

            if (maKH != null && !maKH.isEmpty()) {
                khachHang = khachHangService.getKhachHangById(maKH);
                if (khachHang != null && !"0000000000".equals(khachHang.getSdt())) {
                    vouchers = getVoucherChoKhachHang(khachHang);
                    tenKhachHang = khachHang.getHoTen();
                    loaiKhachHang = "khachhang";
                } else {
                    vouchers = getVoucherCongKhai();
                    loaiKhachHang = "vanglai";
                }
            } else {
                // Kiểm tra xem hóa đơn đã có khách hàng chưa
                if (hoaDon.getMaKhachHang() != null && !"0000000000".equals(hoaDon.getMaKhachHang().getSdt())) {
                    khachHang = hoaDon.getMaKhachHang();
                    vouchers = getVoucherChoKhachHang(khachHang);
                    tenKhachHang = khachHang.getHoTen();
                    loaiKhachHang = "khachhang";
                } else {
                    vouchers = getVoucherCongKhai();
                    loaiKhachHang = "vanglai";
                }
            }

            // Chuyển đổi sang DTO
            List<Map<String, Object>> voucherList = new ArrayList<>();
            GiamGia currentVoucher = hoaDon.getMaGiamGia();

            for (GiamGia gg : vouchers) {
                Map<String, Object> item = new HashMap<>();
                item.put("maGiamGia", gg.getMaGiamGia());
                item.put("tenGiamGia", gg.getTenGiamGia());
                item.put("loaiGiamGia", gg.getLoaiGiamGia());
                item.put("giaTriGiam", gg.getGiaTriGiam());
                item.put("giamToiDa", gg.getGiamToiDa());
                item.put("soLuong", gg.getSoLuong());
                item.put("isVoHan", gg.getIsVoHan());
                item.put("donToiThieu", gg.getDonToiThieu());
                item.put("loaiApDung", gg.getLoaiApDung());
                item.put("loaiApDungText", gg.getLoaiApDung() != null && gg.getLoaiApDung() == 1 ? "Công khai" : "Cá nhân");
                item.put("isApplied", currentVoucher != null && currentVoucher.getMaGiamGia().equals(gg.getMaGiamGia()));

                BigDecimal tienGiam = tinhMucGiamVoucher(gg, tongTien);
                item.put("tienGiam", tienGiam);

                boolean isEligible = true;
                String status = "Sẵn sàng áp dụng";
                String statusClass = "success";
                BigDecimal canThem = BigDecimal.ZERO;

                if (gg.getDonToiThieu() != null && gg.getDonToiThieu().compareTo(BigDecimal.ZERO) > 0
                        && tongTien.compareTo(gg.getDonToiThieu()) < 0) {
                    isEligible = false;
                    canThem = gg.getDonToiThieu().subtract(tongTien);
                    status = "Cần thêm " + formatCurrency(canThem);
                    statusClass = "warning";
                    item.put("canThem", canThem);
                }

                if (item.get("isApplied") == Boolean.TRUE) {
                    status = "✅ Đang áp dụng";
                    statusClass = "info";
                    isEligible = true;
                }

                item.put("isEligible", isEligible);
                item.put("status", status);
                item.put("statusClass", statusClass);

                voucherList.add(item);
            }

            GiamGia bestVoucher = timVoucherTotNhatChoHoaDon(hoaDon, tongTien);

            response.put("success", true);
            response.put("vouchers", voucherList);
            response.put("count", voucherList.size());
            response.put("tenKhachHang", tenKhachHang);
            response.put("loaiKhachHang", loaiKhachHang);
            response.put("currentVoucher", currentVoucher != null ? currentVoucher.getMaGiamGia() : null);

            if (bestVoucher != null) {
                response.put("bestVoucher", Map.of(
                        "maGiamGia", bestVoucher.getMaGiamGia(),
                        "tenGiamGia", bestVoucher.getTenGiamGia(),
                        "tienGiam", tinhMucGiamVoucher(bestVoucher, tongTien),
                        "isCurrent", currentVoucher != null && currentVoucher.getMaGiamGia().equals(bestVoucher.getMaGiamGia())
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/themdiachi")
    @ResponseBody
    public ResponseEntity<?> themDiaChi(@RequestBody Map<String, Object> payload) {
        try {
            logger.info("📝 Thêm địa chỉ mới: {}", payload);

            // ⭐ LẤY DỮ LIỆU
            String maKH = (String) payload.get("maKH");
            String tenNguoiNhan = (String) payload.get("tenNguoiNhan");
            String soDienThoai = (String) payload.get("soDienThoaiNguoiNhan");
            String diaChiCuThe = (String) payload.get("diaChiCuThe");
            String phuongXa = (String) payload.get("phuongXa");
            String quanHuyen = (String) payload.get("quanHuyen");
            String tinhThanh = (String) payload.get("tinhThanh");
            Boolean diaChiMacDinh = (Boolean) payload.get("diaChiMacDinh");

            // ⭐ VALIDATE
            if (maKH == null || maKH.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Mã khách hàng không hợp lệ!"));
            }

            if (tenNguoiNhan == null || tenNguoiNhan.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Vui lòng nhập tên người nhận!"));
            }

            if (diaChiCuThe == null || diaChiCuThe.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Vui lòng nhập địa chỉ cụ thể!"));
            }

            // ⭐ LẤY KHÁCH HÀNG
            KhachHang khachHang = khachHangService.findByMaKH(maKH);
            if (khachHang == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Khách hàng không tồn tại!"));
            }

            // ⭐ TẠO ĐỊA CHỈ MỚI
            DiaChi diaChi = new DiaChi();
            diaChi.setKhachHang(khachHang);
            diaChi.setTenNguoiNhan(tenNguoiNhan);
            diaChi.setSoDienThoaiNguoiNhan(soDienThoai != null ? soDienThoai : "");
            diaChi.setDiaChiCuThe(diaChiCuThe);
            diaChi.setPhuongXa(phuongXa != null ? phuongXa : "");
            diaChi.setQuanHuyen(quanHuyen != null ? quanHuyen : "");
            diaChi.setTinhThanh(tinhThanh != null ? tinhThanh : "");
            diaChi.setDiaChiMacDinh(diaChiMacDinh != null && diaChiMacDinh);

            // ⭐ LƯU - SERVICE TỰ RESET NẾU LÀ MẶC ĐỊNH
            DiaChi saved = diaChiService.save(diaChi);

            logger.info("✅ Thêm địa chỉ thành công! ID: {}", saved.getMaDiaChi());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thêm địa chỉ thành công!",
                    "maDiaChi", saved.getMaDiaChi(),
                    "diaChiMacDinh", saved.getDiaChiMacDinh()
            ));

        } catch (Exception e) {
            logger.error("❌ Lỗi thêm địa chỉ: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "Lỗi: " + e.getMessage()
                    ));
        }
    }

    @PutMapping("/suadiachi/{maDiaChi}")
    @ResponseBody
    public ResponseEntity<?> suaDiaChi(@PathVariable("maDiaChi") Integer maDiaChi,
                                       @RequestBody Map<String, Object> payload) {
        try {
            logger.info("📝 Sửa địa chỉ ID: {}, data: {}", maDiaChi, payload);

            if (maDiaChi == null || maDiaChi <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Mã địa chỉ không hợp lệ!"));
            }

            Optional<DiaChi> diaChiOpt = diaChiService.findById(maDiaChi);
            if (!diaChiOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Địa chỉ không tồn tại!"));
            }

            DiaChi existing = diaChiOpt.get();

            // ⭐ CẬP NHẬT THÔNG TIN
            String tenNguoiNhan = (String) payload.get("tenNguoiNhan");
            String soDienThoai = (String) payload.get("soDienThoaiNguoiNhan");
            String diaChiCuThe = (String) payload.get("diaChiCuThe");
            String phuongXa = (String) payload.get("phuongXa");
            String quanHuyen = (String) payload.get("quanHuyen");
            String tinhThanh = (String) payload.get("tinhThanh");
            Boolean diaChiMacDinh = (Boolean) payload.get("diaChiMacDinh");

            if (tenNguoiNhan != null && !tenNguoiNhan.trim().isEmpty()) {
                existing.setTenNguoiNhan(tenNguoiNhan);
            }
            if (soDienThoai != null && !soDienThoai.isEmpty()) {
                existing.setSoDienThoaiNguoiNhan(soDienThoai);
            }
            if (diaChiCuThe != null && !diaChiCuThe.trim().isEmpty()) {
                existing.setDiaChiCuThe(diaChiCuThe);
            }
            if (phuongXa != null) {
                existing.setPhuongXa(phuongXa);
            }
            if (quanHuyen != null) {
                existing.setQuanHuyen(quanHuyen);
            }
            if (tinhThanh != null) {
                existing.setTinhThanh(tinhThanh);
            }

            // ⭐ XỬ LÝ ĐỊA CHỈ MẶC ĐỊNH
            if (diaChiMacDinh != null) {
                if (diaChiMacDinh) {
                    // Nếu đặt mặc định, reset các địa chỉ khác
                    diaChiService.resetDiaChiMacDinh(existing.getKhachHang().getMaKH());
                }
                existing.setDiaChiMacDinh(diaChiMacDinh);
            }

            // ⭐ LƯU
            DiaChi saved = diaChiService.save(existing);

            logger.info("✅ Sửa địa chỉ thành công! ID: {}", saved.getMaDiaChi());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật địa chỉ thành công!",
                    "maDiaChi", saved.getMaDiaChi(),
                    "diaChiMacDinh", saved.getDiaChiMacDinh()
            ));

        } catch (Exception e) {
            logger.error("❌ Lỗi sửa địa chỉ: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "Lỗi: " + e.getMessage()
                    ));
        }
    }

    @DeleteMapping("/xoadiachi/{maDiaChi}")
    @ResponseBody
    public ResponseEntity<?> xoaDiaChi(@PathVariable("maDiaChi") Integer maDiaChi) {
        try {
            logger.info("📝 Xóa địa chỉ ID: {}", maDiaChi);

            if (maDiaChi == null || maDiaChi <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Mã địa chỉ không hợp lệ!"));
            }

            Optional<DiaChi> diaChiOpt = diaChiService.findById(maDiaChi);
            if (!diaChiOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Địa chỉ không tồn tại!"));
            }

            // ⭐ KIỂM TRA NẾU LÀ ĐỊA CHỈ MẶC ĐỊNH
            DiaChi diaChi = diaChiOpt.get();
            if (Boolean.TRUE.equals(diaChi.getDiaChiMacDinh())) {
                // Nếu xóa địa chỉ mặc định, cần set địa chỉ khác làm mặc định
                String maKH = diaChi.getKhachHang().getMaKH();
                List<DiaChi> otherAddresses = diaChiService.findByKhachHang_MaKH(maKH)
                        .stream()
                        .filter(d -> !d.getMaDiaChi().equals(maDiaChi))
                        .collect(Collectors.toList());

                if (!otherAddresses.isEmpty()) {
                    // Đặt địa chỉ đầu tiên làm mặc định
                    DiaChi newDefault = otherAddresses.get(0);
                    newDefault.setDiaChiMacDinh(true);
                    diaChiService.save(newDefault);
                    logger.info("📌 Đã đặt địa chỉ {} làm mặc định thay thế", newDefault.getMaDiaChi());
                }
            }

            // ⭐ XÓA ĐỊA CHỈ
            diaChiService.deleteById(maDiaChi);

            logger.info("✅ Xóa địa chỉ thành công! ID: {}", maDiaChi);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xóa địa chỉ thành công!"
            ));

        } catch (Exception e) {
            logger.error("❌ Lỗi xóa địa chỉ: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "Lỗi: " + e.getMessage()
                    ));
        }
    }

    @PostMapping("/setdefault/{maDiaChi}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> setDefaultDiaChi(
            @PathVariable("maDiaChi") Integer maDiaChi,
            @RequestParam(value = "mahd", required = false) String mahd) {

        try {
            logger.info("📝 Đặt mặc định địa chỉ ID: {}", maDiaChi);

            if (maDiaChi == null || maDiaChi <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Mã địa chỉ không hợp lệ!"));
            }

            Optional<DiaChi> diaChiOpt = diaChiService.findById(maDiaChi);
            if (!diaChiOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Địa chỉ không tồn tại!"));
            }

            DiaChi diaChi = diaChiOpt.get();
            KhachHang khachHang = diaChi.getKhachHang();
            if (khachHang == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Không tìm thấy khách hàng!"));
            }

            String maKH = khachHang.getMaKH();
            logger.info("👤 Đặt mặc định cho khách hàng: {}", maKH);

            // ⭐ RESET TẤT CẢ VỀ FALSE
            diaChiService.resetDiaChiMacDinh(maKH);
            logger.info("✅ Đã reset địa chỉ mặc định cho KH: {}", maKH);

            // ⭐ ĐẶT ĐỊA CHỈ NÀY LÀM MẶC ĐỊNH
            diaChi.setDiaChiMacDinh(true);
            DiaChi saved = diaChiService.save(diaChi);
            logger.info("✅ Đã đặt địa chỉ {} làm mặc định, giá trị trong DB: {}", maDiaChi, saved.getDiaChiMacDinh());

            // ⭐ KIỂM TRA XEM ĐÃ CẬP NHẬT CHƯA
            int count = diaChiService.countDefaultAddressByKhachHang(maKH);
            logger.info("📊 Số địa chỉ mặc định của KH {}: {}", maKH, count);

            // ⭐ NẾU COUNT = 0, DÙNG CÁCH 2: UPDATE TRỰC TIẾP
            if (count == 0) {
                logger.warn("⚠️ COUNT = 0, sử dụng update trực tiếp...");
                diaChiService.resetDiaChiMacDinh(maKH);
                // Set trực tiếp
                diaChiRepo.setDefaultAddressDirectly(maKH, maDiaChi);
                int countAfter = diaChiService.countDefaultAddressByKhachHang(maKH);
                logger.info("📊 Số địa chỉ mặc định sau update trực tiếp: {}", countAfter);

                // Load lại địa chỉ từ DB
                diaChi = diaChiService.findById(maDiaChi).orElse(diaChi);
            }

            // ⭐ CẬP NHẬT HÓA ĐƠN NẾU CÓ
            if (mahd != null && !mahd.isEmpty()) {
                HoaDon hd = hoaDonService.findById(mahd);
                if (hd != null) {
                    String diaChiDayDu = buildFullAddress(diaChi);
                    hd.setDiaChiGiaoHang(diaChiDayDu);
                    hd.setGhiChu("Địa chỉ giao hàng: " + diaChiDayDu +
                            " | Người nhận: " + diaChi.getTenNguoiNhan() +
                            " | SĐT: " + diaChi.getSoDienThoaiNguoiNhan());
                    hoaDonService.save(hd);
                    logger.info("✅ Đã cập nhật địa chỉ cho hóa đơn: {}", mahd);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã đặt địa chỉ làm mặc định!");
            response.put("maDiaChi", maDiaChi);
            response.put("maKH", maKH);
            response.put("diaChi", diaChi.getDiaChiCuThe());
            response.put("tenNguoiNhan", diaChi.getTenNguoiNhan());
            response.put("soDienThoai", diaChi.getSoDienThoaiNguoiNhan());
            response.put("diaChiDayDu", buildFullAddress(diaChi));
            response.put("countDefault", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Lỗi đặt mặc định: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "Lỗi: " + e.getMessage()
                    ));
        }
    }

    @GetMapping("/diachi/{maKH}")
    @ResponseBody
    public ResponseEntity<?> getDiaChiByKhachHang(@PathVariable("maKH") String maKH) {
        try {
            logger.info("📝 Lấy địa chỉ của KH: {}", maKH);

            if (maKH == null || maKH.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Mã khách hàng không hợp lệ!"));
            }

            List<DiaChi> diaChiList = diaChiService.findByKhachHang_MaKH(maKH);

            if (diaChiList == null || diaChiList.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            // ⭐ CHUYỂN ĐỔI DTO
            List<Map<String, Object>> result = diaChiList.stream()
                    .map(dc -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("maDiaChi", dc.getMaDiaChi());
                        map.put("diaChiCuThe", dc.getDiaChiCuThe() != null ? dc.getDiaChiCuThe() : "");
                        map.put("phuongXa", dc.getPhuongXa() != null ? dc.getPhuongXa() : "");
                        map.put("quanHuyen", dc.getQuanHuyen() != null ? dc.getQuanHuyen() : "");
                        map.put("tinhThanh", dc.getTinhThanh() != null ? dc.getTinhThanh() : "");
                        map.put("tenNguoiNhan", dc.getTenNguoiNhan() != null ? dc.getTenNguoiNhan() : "");
                        map.put("soDienThoaiNguoiNhan", dc.getSoDienThoaiNguoiNhan() != null ? dc.getSoDienThoaiNguoiNhan() : "");
                        map.put("diaChiMacDinh", dc.getDiaChiMacDinh() != null && dc.getDiaChiMacDinh());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ Lỗi lấy địa chỉ: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/diachi/detail/{maDiaChi}")
    @ResponseBody
    public ResponseEntity<?> getDiaChiDetail(@PathVariable("maDiaChi") Integer maDiaChi) {
        try {
            Optional<DiaChi> diaChiOpt = diaChiService.findById(maDiaChi);
            if (!diaChiOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Địa chỉ không tồn tại!"));
            }

            DiaChi dc = diaChiOpt.get();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("maDiaChi", dc.getMaDiaChi());
            result.put("diaChiCuThe", dc.getDiaChiCuThe() != null ? dc.getDiaChiCuThe() : "");
            result.put("phuongXa", dc.getPhuongXa() != null ? dc.getPhuongXa() : "");
            result.put("quanHuyen", dc.getQuanHuyen() != null ? dc.getQuanHuyen() : "");
            result.put("tinhThanh", dc.getTinhThanh() != null ? dc.getTinhThanh() : "");
            result.put("tenNguoiNhan", dc.getTenNguoiNhan() != null ? dc.getTenNguoiNhan() : "");
            result.put("soDienThoaiNguoiNhan", dc.getSoDienThoaiNguoiNhan() != null ? dc.getSoDienThoaiNguoiNhan() : "");
            result.put("diaChiMacDinh", dc.getDiaChiMacDinh() != null && dc.getDiaChiMacDinh());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ Lỗi lấy chi tiết địa chỉ: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/chondiachi")
    @ResponseBody
    public ResponseEntity<?> chonDiaChi(@RequestBody Map<String, Object> payload) {
        try {
            String mahd = (String) payload.get("mahd");
            Integer maDiaChi = (Integer) payload.get("maDiaChi");

            if (mahd == null || mahd.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Mã hóa đơn không hợp lệ!"));
            }

            if (maDiaChi == null || maDiaChi <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Mã địa chỉ không hợp lệ!"));
            }

            HoaDon hd = hoaDonService.findById(mahd);
            if (hd == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Hóa đơn không tồn tại!"));
            }

            Optional<DiaChi> diaChiOpt = diaChiService.findById(maDiaChi);
            if (!diaChiOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Địa chỉ không tồn tại!"));
            }

            DiaChi diaChi = diaChiOpt.get();

            // ⭐ CẬP NHẬT HÓA ĐƠN
            String diaChiDayDu = buildFullAddress(diaChi);
            hd.setDiaChiGiaoHang(diaChiDayDu);
            hd.setGhiChu("Địa chỉ giao hàng: " + diaChiDayDu +
                    " | Người nhận: " + diaChi.getTenNguoiNhan() +
                    " | SĐT: " + diaChi.getSoDienThoaiNguoiNhan());
            hoaDonService.save(hd);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã chọn địa chỉ giao hàng thành công!",
                    "diaChi", diaChiDayDu,
                    "tenNguoiNhan", diaChi.getTenNguoiNhan(),
                    "soDienThoai", diaChi.getSoDienThoaiNguoiNhan(),
                    "maDiaChi", diaChi.getMaDiaChi()
            ));

        } catch (Exception e) {
            logger.error("❌ Lỗi chọn địa chỉ: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "Lỗi: " + e.getMessage()
                    ));
        }
    }

    private String buildFullAddress(DiaChi diaChi) {
        if (diaChi == null) return "";

        StringBuilder sb = new StringBuilder();
        if (diaChi.getDiaChiCuThe() != null && !diaChi.getDiaChiCuThe().isEmpty()) {
            sb.append(diaChi.getDiaChiCuThe());
        }
        if (diaChi.getPhuongXa() != null && !diaChi.getPhuongXa().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(diaChi.getPhuongXa());
        }
        if (diaChi.getQuanHuyen() != null && !diaChi.getQuanHuyen().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(diaChi.getQuanHuyen());
        }
        if (diaChi.getTinhThanh() != null && !diaChi.getTinhThanh().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(diaChi.getTinhThanh());
        }
        return sb.toString();
    }

    @PostMapping("/update/{mahd}")
    @ResponseBody
    public ResponseEntity<?> updateHoaDon(@PathVariable("mahd") String mahd) {
        try {
            HoaDon hd = hoaDonService.findById(mahd);
            if (hd == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Hóa đơn không tồn tại!"));
            }

            if (hd.getMaKhachHang() != null) {
                String maKH = hd.getMaKhachHang().getMaKH();
                DiaChi diaChiMacDinh = diaChiService.findDefaultByMaKH(maKH);

                if (diaChiMacDinh != null) {
                    String diaChiDayDu = diaChiMacDinh.getDiaChiCuThe();
                    if (diaChiMacDinh.getPhuongXa() != null && !diaChiMacDinh.getPhuongXa().isEmpty()) {
                        diaChiDayDu += ", " + diaChiMacDinh.getPhuongXa();
                    }
                    if (diaChiMacDinh.getQuanHuyen() != null && !diaChiMacDinh.getQuanHuyen().isEmpty()) {
                        diaChiDayDu += ", " + diaChiMacDinh.getQuanHuyen();
                    }
                    if (diaChiMacDinh.getTinhThanh() != null && !diaChiMacDinh.getTinhThanh().isEmpty()) {
                        diaChiDayDu += ", " + diaChiMacDinh.getTinhThanh();
                    }

                    hd.setDiaChiGiaoHang(diaChiDayDu);
                    hd.setGhiChu("Địa chỉ giao hàng: " + diaChiDayDu +
                            " | Người nhận: " + diaChiMacDinh.getTenNguoiNhan() +
                            " | SĐT: " + diaChiMacDinh.getSoDienThoaiNguoiNhan());

                    hoaDonService.save(hd);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật hóa đơn thành công!"
            ));

        } catch (Exception e) {
            logger.error("❌ Lỗi cập nhật hóa đơn: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/api/voucher-ca-nhan")
    @ResponseBody
    public ResponseEntity<?> getVoucherCaNhan(@RequestParam String maKhachHang) {
        try {
            System.out.println("========== GET VOUCHER CÁ NHÂN ==========");
            System.out.println("📋 maKhachHang: " + maKhachHang);

            if (maKhachHang == null || maKhachHang.trim().isEmpty()) {
                System.out.println("⚠️ maKhachHang rỗng hoặc null");
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<GiamGiaChiTiet> chiTietList = giamGiaChiTietRepository.findByKhachHang_MaKH(maKhachHang);

            System.out.println("📊 Số chi tiết tìm thấy: " + (chiTietList != null ? chiTietList.size() : 0));

            // ⭐ LOG CHI TIẾT TỪNG VOUCHER
            if (chiTietList != null && !chiTietList.isEmpty()) {
                System.out.println("📌 DANH SÁCH VOUCHER CÁ NHÂN:");
                for (int i = 0; i < chiTietList.size(); i++) {
                    GiamGiaChiTiet ct = chiTietList.get(i);
                    GiamGia giamGia = ct.getGiamGia();
                    if (giamGia != null) {
                        System.out.println("  [" + (i + 1) + "] Ma: " + giamGia.getMaGiamGia());
                        System.out.println("      Ten: " + giamGia.getTenGiamGia());
                        System.out.println("      TrangThaiSuDung: " + ct.getTrangThaiSuDung() +
                                (ct.getTrangThaiSuDung() != null && ct.getTrangThaiSuDung() == 1 ? " ⚠️ ĐÃ SỬ DỤNG" : " ✅ CHƯA DÙNG"));
                        System.out.println("      TrangThai: " + giamGia.getTrangThai());
                        System.out.println("      NgayKetThuc: " + giamGia.getNgayKetThuc());
                        System.out.println("      -----------------------------------");
                    }
                }
            } else {
                System.out.println("📌 Không có voucher cá nhân nào");
            }

            List<VoucherDTO> result = new ArrayList<>();
            LocalDate today = LocalDate.now();
            int countDaSuDung = 0;
            int countChuaSuDung = 0;
            int countExpired = 0;
            int countInactive = 0;

            for (GiamGiaChiTiet ct : chiTietList) {
                GiamGia giamGia = ct.getGiamGia();
                if (giamGia == null) {
                    System.out.println("⏭️ Bỏ qua: GiamGia null");
                    continue;
                }

                // ⭐ LẤY TRẠNG THÁI SỬ DỤNG
                Integer trangThaiSuDung = ct.getTrangThaiSuDung() != null ? ct.getTrangThaiSuDung() : 0;

                // Kiểm tra trạng thái hoạt động
                if (!"Hoạt động".equals(giamGia.getTrangThai())) {
                    countInactive++;
                    System.out.println("⏭️ Bỏ qua voucher không hoạt động: " + giamGia.getMaGiamGia() +
                            " | TrangThai: " + giamGia.getTrangThai());
                    continue;
                }

                // Kiểm tra ngày hết hạn
                if (giamGia.getNgayKetThuc() != null &&
                        giamGia.getNgayKetThuc().toLocalDate().isBefore(today)) {
                    countExpired++;
                    System.out.println("⏭️ Bỏ qua voucher đã hết hạn: " + giamGia.getMaGiamGia() +
                            " | NgayKetThuc: " + giamGia.getNgayKetThuc());
                    continue;
                }

                // ⭐ ĐẾM VOUCHER ĐÃ SỬ DỤNG
                if (trangThaiSuDung == 1) {
                    countDaSuDung++;
                    System.out.println("📌 Voucher đã sử dụng: " + giamGia.getMaGiamGia() +
                            " - " + giamGia.getTenGiamGia());
                } else {
                    countChuaSuDung++;
                    System.out.println("✅ Voucher chưa sử dụng: " + giamGia.getMaGiamGia() +
                            " - " + giamGia.getTenGiamGia());
                }

                VoucherDTO dto = new VoucherDTO();
                dto.setMaGiamGia(giamGia.getMaGiamGia());
                dto.setTenGiamGia(giamGia.getTenGiamGia());
                dto.setLoaiGiamGia(giamGia.getLoaiGiamGia());
                dto.setGiaTriGiam(giamGia.getGiaTriGiam());
                dto.setDonToiThieu(giamGia.getDonToiThieu());
                dto.setGiamToiDa(giamGia.getGiamToiDa());
                dto.setNgayBatDau(giamGia.getNgayBatDau());
                dto.setNgayKetThuc(giamGia.getNgayKetThuc());
                dto.setLoaiApDung(giamGia.getLoaiApDung());
                dto.setTrangThaiSuDung(trangThaiSuDung);

                // ⭐ QUAN TRỌNG: Đánh dấu đã sử dụng
                dto.setDaSuDung(trangThaiSuDung == 1);

                result.add(dto);
            }

            // ⭐ LOG TỔNG KẾT
            System.out.println("========== TỔNG KẾT ==========");
            System.out.println("📊 Tổng số chi tiết: " + chiTietList.size());
            System.out.println("✅ Voucher chưa sử dụng: " + countChuaSuDung);
            System.out.println("⚠️ Voucher đã sử dụng: " + countDaSuDung);
            System.out.println("⏰ Voucher hết hạn (bỏ qua): " + countExpired);
            System.out.println("⛔ Voucher không hoạt động (bỏ qua): " + countInactive);
            System.out.println("📦 Voucher trả về FE: " + result.size());
            System.out.println("================================");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ LỖI: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Lỗi: " + e.getMessage()
                    ));
        }
    }

    @GetMapping("/api/check-voucher-used")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkVoucherUsed(
            @RequestParam String maKhachHang,
            @RequestParam String maGiamGia) {

        Map<String, Object> response = new HashMap<>();
        try {
            GiamGiaChiTietId id = new GiamGiaChiTietId(maKhachHang, maGiamGia);
            GiamGiaChiTiet ct = giamGiaChiTietRepository.findById(id).orElse(null);

            boolean used = false;
            if (ct != null && ct.getTrangThaiSuDung() != null && ct.getTrangThaiSuDung() == 1) {
                used = true;
            }

            response.put("success", true);
            response.put("used", used);
            response.put("message", used ? "Bạn đã sử dụng voucher này rồi!" : "Voucher còn hiệu lực");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi kiểm tra: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/mark-voucher-used")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markVoucherUsed(
            @RequestParam String maKhachHang,
            @RequestParam String maGiamGia) {

        Map<String, Object> response = new HashMap<>();
        try {
            giamGiaService.markVoucherAsUsed(maKhachHang, maGiamGia);

            response.put("success", true);
            response.put("message", "Đã đánh dấu voucher đã sử dụng");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/get-voucher-info")
    @ResponseBody
    public Map<String, Object> getVoucherInfo(@RequestParam("mahd") String mahd) {
        Map<String, Object> response = new HashMap<>();
        try {
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy hóa đơn!");
                return response;
            }
            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            if (listhdct == null) listhdct = new ArrayList<>();

            BigDecimal tongTienHang = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            GiamGia voucher = hoaDon.getMaGiamGia();

            if (voucher == null) {
                response.put("success", true);
                response.put("voucher", null);
                response.put("tongTien", tongTienHang);
                response.put("tienGiam", BigDecimal.ZERO);
                return response;
            }

            // ⭐ KIỂM TRA VOUCHER HỢP LỆ
            Map<String, Object> checkResult = kiemTraVoucherHienTai(voucher, tongTienHang, hoaDon);
            if (checkResult != null) {
                // VOUCHER KHÔNG HỢP LỆ -> TỰ ĐỘNG BỎ
                hoaDon.setMaGiamGia(null);
                hoaDonService.save(hoaDon);

                response.put("success", true);
                response.put("voucher", null);
                response.put("tongTien", tongTienHang);
                response.put("tienGiam", BigDecimal.ZERO);
                response.put("message", "Voucher không còn hợp lệ, đã tự động bỏ!");
                return response;
            }

            // ⭐ TÍNH TIỀN GIẢM
            BigDecimal tienGiam = tinhMucGiamVoucher(voucher, tongTienHang);
            BigDecimal tongTien = tongTienHang.subtract(tienGiam);

            // ⭐ CỘNG PHÍ SHIP NẾU ONLINE
            if ("Online".equalsIgnoreCase(hoaDon.getLoaiBan())) {
                BigDecimal ship = hoaDon.getTienShip() != null ? hoaDon.getTienShip() : BigDecimal.ZERO;
                tongTien = tongTien.add(ship);
            }

            Map<String, Object> voucherInfo = new HashMap<>();
            voucherInfo.put("maGiamGia", voucher.getMaGiamGia());
            voucherInfo.put("tenGiamGia", voucher.getTenGiamGia());
            voucherInfo.put("tienGiam", tienGiam);
            voucherInfo.put("loaiGiamGia", voucher.getLoaiGiamGia());
            voucherInfo.put("giaTriGiam", voucher.getGiaTriGiam());

            response.put("success", true);
            response.put("voucher", voucherInfo);
            response.put("tongTien", tongTien);
            response.put("tienGiam", tienGiam);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/kiemtra-voucher-thanhtoan/{mahd}")
    @ResponseBody
    public ResponseEntity<?> kiemTraVoucherThanhToan(@PathVariable("mahd") String mahd) {
        try {
            System.out.println("========== KIỂM TRA VOUCHER TRƯỚC THANH TOÁN ==========");
            System.out.println("📋 Mã hóa đơn: " + mahd);

            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy hóa đơn!"
                ));
            }

            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            if (listhdct == null || listhdct.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Hóa đơn chưa có sản phẩm!"
                ));
            }

            BigDecimal tongTien = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            GiamGia currentVoucher = hoaDon.getMaGiamGia();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("hasVoucher", currentVoucher != null);
            result.put("tongTien", tongTien);
            result.put("voucherRemoved", false);
            result.put("hasBetterVoucher", false);
            result.put("isValid", false);
            result.put("voucherInfo", null);
            result.put("removedVoucher", null);
            result.put("replacementVoucher", null);
            result.put("bestVoucher", null);
            result.put("autoApplied", false);
            result.put("voucherChanged", false);

            if (currentVoucher == null) {
                result.put("message", "Không có voucher");
                result.put("tongTienMoi", tongTien);
                result.put("isValid", false);

                GiamGia bestVoucher = timVoucherTotNhatChoHoaDon(hoaDon, tongTien);
                if (bestVoucher != null) {
                    result.put("bestVoucher", Map.of(
                            "maGiamGia", bestVoucher.getMaGiamGia(),
                            "tenGiamGia", bestVoucher.getTenGiamGia(),
                            "tienGiam", tinhMucGiamVoucher(bestVoucher, tongTien)
                    ));
                    result.put("autoApplied", true);
                    result.put("message", "Đã tự động áp dụng voucher tốt nhất!");

                    hoaDon.setMaGiamGia(bestVoucher);
                    hoaDonService.save(hoaDon);

                    result.put("voucherChanged", true);
                    result.put("replacementVoucher", Map.of(
                            "maGiamGia", bestVoucher.getMaGiamGia(),
                            "tenGiamGia", bestVoucher.getTenGiamGia(),
                            "tienGiam", tinhMucGiamVoucher(bestVoucher, tongTien)
                    ));
                    result.put("tongTienMoi", tongTien.subtract(tinhMucGiamVoucher(bestVoucher, tongTien)));
                }
                return ResponseEntity.ok(result);
            }

            // ⭐ DÙNG CHUNG HÀM KIỂM TRA
            Map<String, Object> checkResult = kiemTraVoucherHienTai(currentVoucher, tongTien, hoaDon);

            if (checkResult != null) {
                // Voucher không hợp lệ
                System.out.println("⚠️ Voucher không hợp lệ: " + checkResult.get("message"));

                // ⭐ LƯU THÔNG TIN VOUCHER BỊ XÓA
                Map<String, Object> removedInfo = new HashMap<>();
                removedInfo.put("maGiamGia", currentVoucher.getMaGiamGia());
                removedInfo.put("tenGiamGia", currentVoucher.getTenGiamGia());
                removedInfo.put("reason", checkResult.get("message"));
                removedInfo.put("type", checkResult.get("type"));
                result.put("removedVoucher", removedInfo);

                // ⭐ XÓA VOUCHER KHỎI HÓA ĐƠN
                hoaDon.setMaGiamGia(null);
                hoaDonService.save(hoaDon);

                result.put("isValid", false);
                result.put("voucherRemoved", true);
                result.put("message", "Voucher không hợp lệ: " + checkResult.get("message"));

                // ⭐⭐⭐ TÌM VOUCHER THAY THẾ TỐT NHẤT
                GiamGia bestVoucher = timVoucherTotNhatChoHoaDon(hoaDon, tongTien);
                BigDecimal tongTienMoi = tongTien;

                if (bestVoucher != null) {
                    System.out.println("⭐ Tìm thấy voucher thay thế: " + bestVoucher.getMaGiamGia() + " - " + bestVoucher.getTenGiamGia());

                    // ⭐ ÁP DỤNG VOUCHER THAY THẾ
                    hoaDon.setMaGiamGia(bestVoucher);
                    hoaDonService.save(hoaDon);

                    BigDecimal tienGiam = tinhMucGiamVoucher(bestVoucher, tongTien);
                    tongTienMoi = tongTien.subtract(tienGiam);

                    result.put("hasReplacement", true);
                    result.put("replacementVoucher", Map.of(
                            "maGiamGia", bestVoucher.getMaGiamGia(),
                            "tenGiamGia", bestVoucher.getTenGiamGia(),
                            "tienGiam", tienGiam
                    ));
                    result.put("voucherChanged", true);
                    result.put("message", "Voucher không hợp lệ! Đã áp dụng voucher thay thế: " + bestVoucher.getTenGiamGia());

                } else {
                    result.put("hasReplacement", false);
                    result.put("voucherChanged", false);
                    result.put("message", "Voucher không hợp lệ! Không có voucher thay thế.");
                }

                // Cộng phí ship nếu Online
                if ("Online".equalsIgnoreCase(hoaDon.getLoaiBan())) {
                    BigDecimal ship = hoaDon.getTienShip() != null ? hoaDon.getTienShip() : BigDecimal.ZERO;
                    tongTienMoi = tongTienMoi.add(ship);
                }

                result.put("tongTienMoi", tongTienMoi);

                return ResponseEntity.ok(result);
            }

            // Voucher hợp lệ
            BigDecimal tienGiam = tinhMucGiamVoucher(currentVoucher, tongTien);
            BigDecimal tongTienMoi = tongTien.subtract(tienGiam);

            if ("Online".equalsIgnoreCase(hoaDon.getLoaiBan())) {
                BigDecimal ship = hoaDon.getTienShip() != null ? hoaDon.getTienShip() : BigDecimal.ZERO;
                tongTienMoi = tongTienMoi.add(ship);
            }

            result.put("isValid", true);
            result.put("voucherInfo", Map.of(
                    "maGiamGia", currentVoucher.getMaGiamGia(),
                    "tenGiamGia", currentVoucher.getTenGiamGia(),
                    "tienGiam", tienGiam
            ));
            result.put("tongTienMoi", tongTienMoi);
            result.put("message", "Voucher hợp lệ");

            // ⭐ KIỂM TRA VOUCHER TỐT HƠN
            GiamGia bestVoucher = timVoucherTotNhatChoHoaDon(hoaDon, tongTien);
            if (bestVoucher != null && !bestVoucher.getMaGiamGia().equals(currentVoucher.getMaGiamGia())) {
                BigDecimal bestDiscount = tinhMucGiamVoucher(bestVoucher, tongTien);
                if (bestDiscount.compareTo(tienGiam) > 0) {
                    result.put("hasBetterVoucher", true);
                    result.put("betterVoucher", Map.of(
                            "maGiamGia", bestVoucher.getMaGiamGia(),
                            "tenGiamGia", bestVoucher.getTenGiamGia(),
                            "tienGiamHienTai", tienGiam,
                            "tienGiamMoi", bestDiscount,
                            "chenhLech", bestDiscount.subtract(tienGiam)
                    ));
                    result.put("message", "Có voucher tốt hơn: " + bestVoucher.getTenGiamGia());
                }
            }

            System.out.println("=== END KIỂM TRA VOUCHER ===");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi kiểm tra voucher: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/apdungvouchertotnhat-ajax")
    @ResponseBody
    public ResponseEntity<?> apDungVoucherTotNhatAjax(@RequestParam("mahd") String mahd) {
        try {
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy hóa đơn!"
                ));
            }

            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            BigDecimal tongTien = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            GiamGia bestVoucher = timVoucherTotNhatChoHoaDon(hoaDon, tongTien);

            if (bestVoucher == null) {
                // Không có voucher nào hợp lệ
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Không có voucher nào để áp dụng!"
                ));
            }

            // ⭐ KIỂM TRA NẾU ĐÃ ÁP DỤNG VOUCHER NÀY RỒI
            if (hoaDon.getMaGiamGia() != null &&
                    hoaDon.getMaGiamGia().getMaGiamGia().equals(bestVoucher.getMaGiamGia())) {
                BigDecimal tienGiam = tinhMucGiamVoucher(bestVoucher, tongTien);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "✅ Đã áp dụng voucher tốt nhất: " + bestVoucher.getTenGiamGia(),
                        "voucher", Map.of(
                                "maGiamGia", bestVoucher.getMaGiamGia(),
                                "tenGiamGia", bestVoucher.getTenGiamGia(),
                                "tienGiam", tienGiam
                        ),
                        "tongTienMoi", tongTien.subtract(tienGiam),
                        "alreadyApplied", true // ⭐ Đánh dấu đã áp dụng
                ));
            }

            // Áp dụng voucher mới
            hoaDon.setMaGiamGia(bestVoucher);
            hoaDonService.save(hoaDon);

            BigDecimal tienGiam = tinhMucGiamVoucher(bestVoucher, tongTien);
            BigDecimal tongTienMoi = tongTien.subtract(tienGiam);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã áp dụng voucher: " + bestVoucher.getTenGiamGia(),
                    "voucher", Map.of(
                            "maGiamGia", bestVoucher.getMaGiamGia(),
                            "tenGiamGia", bestVoucher.getTenGiamGia(),
                            "tienGiam", tienGiam
                    ),
                    "tongTienMoi", tongTienMoi,
                    "alreadyApplied", false
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/apdungvouchertotnhat")
    public String apDungVoucherTotNhat(@RequestParam("mahd") String mahd,
                                       RedirectAttributes redirectAttributes) {
        try {
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                redirectAttributes.addFlashAttribute("mess", "Không tìm thấy hóa đơn!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/banhang/index";
            }

            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            if (listhdct == null || listhdct.isEmpty()) {
                redirectAttributes.addFlashAttribute("mess", "Hóa đơn chưa có sản phẩm!");
                redirectAttributes.addFlashAttribute("messageType", "warning");
                return "redirect:/banhang/index?mahd=" + mahd;
            }

            BigDecimal tongTien = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Tìm voucher tốt nhất (KHÔNG bỏ qua voucher hiện tại)
            GiamGia voucherTotNhat = timVoucherTotNhatChoHoaDon(hoaDon, tongTien);

            if (voucherTotNhat == null) {
                // Không có voucher nào hợp lệ
                if (hoaDon.getMaGiamGia() != null) {
                    hoaDon.setMaGiamGia(null);
                    hoaDonService.save(hoaDon);
                    redirectAttributes.addFlashAttribute("mess", "Đã bỏ mã giảm giá. Không có voucher nào phù hợp!");
                } else {
                    redirectAttributes.addFlashAttribute("mess", "Không có voucher nào phù hợp với hóa đơn!");
                }
                redirectAttributes.addFlashAttribute("messageType", "warning");
                return "redirect:/banhang/index?mahd=" + mahd;
            }

            // ⭐ KIỂM TRA NẾU VOUCHER TỐT NHẤT CHÍNH LÀ VOUCHER HIỆN TẠI
            if (hoaDon.getMaGiamGia() != null &&
                    hoaDon.getMaGiamGia().getMaGiamGia().equals(voucherTotNhat.getMaGiamGia())) {
                // Vẫn giữ nguyên voucher hiện tại, chỉ thông báo
                BigDecimal tienGiam = tinhMucGiamVoucher(voucherTotNhat, tongTien);
                redirectAttributes.addFlashAttribute("mess",
                        "✅ Đã áp dụng voucher tốt nhất: " + voucherTotNhat.getTenGiamGia() +
                                " (Giảm " + formatCurrency(tienGiam) + ")");
                redirectAttributes.addFlashAttribute("messageType", "success");
                return "redirect:/banhang/index?mahd=" + mahd;
            }

            // Áp dụng voucher mới (khác với voucher hiện tại)
            hoaDon.setMaGiamGia(voucherTotNhat);
            hoaDonService.save(hoaDon);

            BigDecimal tienGiam = tinhMucGiamVoucher(voucherTotNhat, tongTien);
            redirectAttributes.addFlashAttribute("mess",
                    "✅ Đã áp dụng voucher tốt nhất: " + voucherTotNhat.getTenGiamGia() +
                            " (Giảm " + formatCurrency(tienGiam) + ")");
            redirectAttributes.addFlashAttribute("messageType", "success");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("mess", "Lỗi áp dụng voucher: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/banhang/index?mahd=" + mahd;
    }

    private Map<String, Object> kiemTraVoucherHienTai(GiamGia voucher, BigDecimal tongTien, HoaDon hoaDon) {
        if (voucher == null) return null;

        Map<String, Object> result = new HashMap<>();

        // 1. Kiểm tra trạng thái
        String trangThai = giamGiaService.tinhToanTrangThai(voucher);
        if (!"Hoạt động".equals(trangThai)) {
            result.put("type", "VOUCHER_STOPPED");
            result.put("message", "Voucher đã bị ngừng hoạt động!");
            result.put("voucherName", voucher.getTenGiamGia());
            result.put("voucherCode", voucher.getMaGiamGia());
            return result;
        }

        // 2. Kiểm tra số lượng
        if (voucher.getIsVoHan() == null || !voucher.getIsVoHan()) {
            if (voucher.getSoLuong() == null || voucher.getSoLuong() <= 0) {
                result.put("type", "VOUCHER_OUT_OF_STOCK");
                result.put("message", "Voucher đã hết số lượng!");
                result.put("voucherName", voucher.getTenGiamGia());
                result.put("voucherCode", voucher.getMaGiamGia());
                return result;
            }
        }

        // 3. Kiểm tra đơn tối thiểu
        if (voucher.getDonToiThieu() != null &&
                voucher.getDonToiThieu().compareTo(BigDecimal.ZERO) > 0 &&
                tongTien.compareTo(voucher.getDonToiThieu()) < 0) {
            BigDecimal needMore = voucher.getDonToiThieu().subtract(tongTien);
            result.put("type", "VOUCHER_MIN_ORDER");
            result.put("message", "Đơn hàng cần thêm " + formatCurrency(needMore));
            result.put("voucherName", voucher.getTenGiamGia());
            result.put("voucherCode", voucher.getMaGiamGia());
            result.put("needMore", needMore);
            return result;
        }

        // 4. Kiểm tra ngày
        if (voucher.getNgayKetThuc() != null &&
                voucher.getNgayKetThuc().isBefore(LocalDateTime.now())) {
            result.put("type", "VOUCHER_EXPIRED");
            result.put("message", "Voucher đã hết hạn!");
            result.put("voucherName", voucher.getTenGiamGia());
            result.put("voucherCode", voucher.getMaGiamGia());
            return result;
        }

        if (voucher.getNgayBatDau() != null &&
                voucher.getNgayBatDau().isAfter(LocalDateTime.now())) {
            result.put("type", "VOUCHER_NOT_STARTED");
            result.put("message", "Voucher chưa đến ngày áp dụng!");
            result.put("voucherName", voucher.getTenGiamGia());
            result.put("voucherCode", voucher.getMaGiamGia());
            return result;
        }

        // 5. ⭐ QUAN TRỌNG: KIỂM TRA VOUCHER CÁ NHÂN - ĐÃ SỬ DỤNG
        if (voucher.getLoaiApDung() != null && voucher.getLoaiApDung() == 2) {
            KhachHang khachHang = hoaDon.getMaKhachHang();
            if (khachHang == null) {
                result.put("type", "VOUCHER_CUSTOMER_ONLY");
                result.put("message", "Voucher chỉ dành cho khách hàng cụ thể!");
                result.put("voucherName", voucher.getTenGiamGia());
                result.put("voucherCode", voucher.getMaGiamGia());
                return result;
            }

            // ⭐ KIỂM TRA TRONG BẢNG KHACHHANG_VOUCHER
            GiamGiaChiTiet ct = giamGiaChiTietRepository
                    .findByKhachHang_MaKHAndGiamGia_MaGiamGia(
                            khachHang.getMaKH(),
                            voucher.getMaGiamGia()
                    ).orElse(null);

            if (ct == null) {
                result.put("type", "VOUCHER_NOT_FOR_CUSTOMER");
                result.put("message", "Khách hàng không có voucher này!");
                result.put("voucherName", voucher.getTenGiamGia());
                result.put("voucherCode", voucher.getMaGiamGia());
                return result;
            }
            if (ct.getTrangThaiSuDung() != null && ct.getTrangThaiSuDung() == 1) {
                result.put("type", "VOUCHER_ALREADY_USED");
                result.put("message", "Voucher này đã được sử dụng!");
                result.put("voucherName", voucher.getTenGiamGia());
                result.put("voucherCode", voucher.getMaGiamGia());
                System.out.println("⚠️ Voucher đã được sử dụng: " + voucher.getMaGiamGia());
                return result;
            }

            System.out.println("✅ Voucher cá nhân hợp lệ, chưa sử dụng");
        }

        return null; // Voucher hợp lệ
    }

    @GetMapping("/kiem-tra-san-pham-ngung")
    @ResponseBody
    public ResponseEntity<?> kiemTraVaXoaSanPhamNgung(@RequestParam String mahd) {
        try {
            HoaDon hoaDon = hoaDonRepo.findById(mahd).orElse(null);

            if (hoaDon == null || "Đã thanh toán".equals(hoaDon.getTrangThai())) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "hasChanges", false,
                        "message", "Hóa đơn đã thanh toán hoặc không tồn tại"
                ));
            }

            List<HoaDonChiTiet> chiTiets = hoaDonChiTietRepository.findByMaHoaDon_MaHoaDon(mahd);
            List<Map<String, Object>> sanPhamBiXoa = new ArrayList<>();
            boolean hasChanges = false;
            BigDecimal tongTienHang = BigDecimal.ZERO;

            // Danh sách sản phẩm còn lại
            List<Map<String, Object>> danhSachSanPhamConLai = new ArrayList<>();

            Iterator<HoaDonChiTiet> iterator = chiTiets.iterator();
            while (iterator.hasNext()) {
                HoaDonChiTiet chiTiet = iterator.next();
                SanPhamChiTiet spct = chiTiet.getSanPhamChiTiet();

                boolean isNgungBan = false;
                String lyDo = "";

                if (spct == null) {
                    isNgungBan = true;
                    lyDo = "Sản phẩm không tồn tại";
                } else {
                    String trangThai = spct.getTrangThai();
                    if ("Ngừng bán".equals(trangThai)) {
                        isNgungBan = true;
                        lyDo = "Đã ngừng bán";
                    }
                }

                if (isNgungBan) {
                    // ⭐ Lưu thông tin sản phẩm bị xóa - SỬA LẠI
                    Map<String, Object> info = new HashMap<>();

                    // ⭐ QUAN TRỌNG: Lấy mã sản phẩm chi tiết là STRING
                    String maSpct = chiTiet.getSanPhamChiTiet() != null ?
                            chiTiet.getSanPhamChiTiet().getMaSanPhamChiTiet() : null;
                    info.put("maSanPhamChiTiet", maSpct);

                    if (spct != null && spct.getSanPham() != null) {
                        info.put("tenSanPham", spct.getSanPham().getTenSanPham());
                        info.put("mauSac", spct.getMauSac() != null ? spct.getMauSac().getTenMauSac() : "N/A");
                        info.put("kichThuoc", spct.getKichThuoc() != null ? spct.getKichThuoc().getTenKichThuoc() : "N/A");
                    } else {
                        info.put("tenSanPham", "Sản phẩm không xác định");
                        info.put("mauSac", "N/A");
                        info.put("kichThuoc", "N/A");
                    }

                    info.put("soLuong", chiTiet.getSoLuong());
                    info.put("donGia", chiTiet.getDonGia());
                    info.put("lyDo", lyDo);
                    sanPhamBiXoa.add(info);

                    // Trả lại tồn kho
                    if (spct != null) {
                        int soLuongTraLai = chiTiet.getSoLuong();
                        int tonKhoHienTai = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
                        spct.setSoLuongTon(tonKhoHienTai + soLuongTraLai);
                        sanPhamChiTietRepository.save(spct);
                    }

                    hoaDonChiTietRepository.delete(chiTiet);
                    iterator.remove();
                    hasChanges = true;
                } else {
                    // ⭐ Lưu sản phẩm còn lại - SỬA LẠI
                    if (spct != null && spct.getSanPham() != null) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("maSanPhamChiTiet", spct.getMaSanPhamChiTiet()); // String
                        item.put("tenSanPham", spct.getSanPham().getTenSanPham());
                        item.put("mauSac", spct.getMauSac() != null ? spct.getMauSac().getTenMauSac() : "N/A");
                        item.put("kichThuoc", spct.getKichThuoc() != null ? spct.getKichThuoc().getTenKichThuoc() : "N/A");
                        item.put("soLuong", chiTiet.getSoLuong());
                        item.put("donGia", chiTiet.getDonGia());
                        item.put("thanhTien", chiTiet.getThanhTien());
                        danhSachSanPhamConLai.add(item);
                    }
                    tongTienHang = tongTienHang.add(chiTiet.getThanhTien());
                }
            }

            // Cập nhật hóa đơn
            Map<String, Object> voucherInfo = new HashMap<>();
            BigDecimal tienGiam = BigDecimal.ZERO;

            if (hasChanges) {
                // Tính lại tiền giảm voucher
                if (hoaDon.getMaGiamGia() != null) {
                    tienGiam = tinhTienGiamVoucher(hoaDon, tongTienHang);
                    voucherInfo.put("maGiamGia", hoaDon.getMaGiamGia().getMaGiamGia());
                    voucherInfo.put("tenGiamGia", hoaDon.getMaGiamGia().getTenGiamGia());
                    voucherInfo.put("tienGiam", tienGiam);
                }

                BigDecimal tongTienMoi = tongTienHang.subtract(tienGiam);
                hoaDon.setTongTien(tongTienMoi);
                hoaDonRepo.save(hoaDon);
            }

            // TRẢ VỀ DỮ LIỆU ĐẦY ĐỦ
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hasChanges", hasChanges);
            response.put("sanPhamBiXoa", sanPhamBiXoa);
            response.put("danhSachSanPham", danhSachSanPhamConLai);
            response.put("tongTienHang", tongTienHang);
            response.put("tongTienMoi", tongTienHang.subtract(tienGiam));
            response.put("voucherInfo", voucherInfo);

            // ⭐ LOG để debug
            System.out.println("=== KIEM TRA SAN PHAM NGUNG ===");
            System.out.println("hasChanges: " + hasChanges);
            System.out.println("sanPhamBiXoa: " + sanPhamBiXoa);
            System.out.println("danhSachSanPhamConLai: " + danhSachSanPhamConLai);
            System.out.println("==================================");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/get-gio-hang")
    @ResponseBody
    public ResponseEntity<?> getGioHang(@RequestParam String mahd) {
        try {
            System.out.println("========================================");
            System.out.println("🔄 [API] getGioHang - mahd: " + mahd);

            HoaDon hoaDon = hoaDonRepo.findById(mahd).orElse(null);
            if (hoaDon == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Không tìm thấy hóa đơn"
                ));
            }

            List<HoaDonChiTiet> chiTiets = hoaDonChiTietRepository.findByMaHoaDon_MaHoaDon(mahd);
            if (chiTiets == null) chiTiets = new ArrayList<>();

            List<Map<String, Object>> danhSachSanPham = new ArrayList<>();
            BigDecimal tongTienHang = BigDecimal.ZERO;

            System.out.println("📦 [PRODUCTS] Danh sách sản phẩm:");

            for (HoaDonChiTiet ct : chiTiets) {
                SanPhamChiTiet spct = ct.getSanPhamChiTiet();
                Map<String, Object> item = new HashMap<>();

                // ⭐ LẤY GIÁ BÁN MỚI NHẤT TỪ SanPhamChiTiet
                BigDecimal giaBanMoi = BigDecimal.ZERO;
                if (spct != null && spct.getGiaBan() != null) {
                    giaBanMoi = spct.getGiaBan();
                } else {
                    giaBanMoi = ct.getDonGia(); // Fallback
                }

                int soLuong = ct.getSoLuong() != null ? ct.getSoLuong() : 1;
                BigDecimal thanhTienMoi = giaBanMoi.multiply(BigDecimal.valueOf(soLuong));

                System.out.println("  - SP: " + (spct != null ? spct.getMaSanPhamChiTiet() : "null"));
                System.out.println("    Giá cũ (HoaDonChiTiet): " + ct.getDonGia());
                System.out.println("    Giá mới (SanPhamChiTiet): " + giaBanMoi);
                System.out.println("    Số lượng: " + soLuong);
                System.out.println("    Thành tiền mới: " + thanhTienMoi);

                if (spct != null && spct.getSanPham() != null) {
                    String tenSanPham = spct.getSanPham().getTenSanPham()
                            + " [" + spct.getMauSac().getTenMauSac()
                            + " - " + spct.getKichThuoc().getTenKichThuoc() + "]";
                    item.put("tenSanPham", tenSanPham);
                    item.put("mauSac", spct.getMauSac() != null ? spct.getMauSac().getTenMauSac() : "N/A");
                    item.put("kichThuoc", spct.getKichThuoc() != null ? spct.getKichThuoc().getTenKichThuoc() : "N/A");
                } else {
                    item.put("tenSanPham", "Sản phẩm không xác định");
                    item.put("mauSac", "N/A");
                    item.put("kichThuoc", "N/A");
                }

                item.put("maSanPhamChiTiet", spct != null ? spct.getMaSanPhamChiTiet() : null);
                item.put("soLuong", soLuong);
                item.put("donGia", giaBanMoi); // ⭐ GIÁ MỚI
                item.put("thanhTien", thanhTienMoi); // ⭐ THÀNH TIỀN MỚI
                item.put("tonKho", spct != null && spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0);

                danhSachSanPham.add(item);
                tongTienHang = tongTienHang.add(thanhTienMoi);
            }

            System.out.println("💰 [CALC] TongTienHang (tính từ giá mới): " + tongTienHang);

            // ⭐ TÍNH TIỀN GIẢM VOUCHER
            BigDecimal tienGiamVoucher = BigDecimal.ZERO;
            GiamGia voucher = hoaDon.getMaGiamGia();

            if (voucher != null) {
                Map<String, Object> checkResult = kiemTraVoucherHienTai(voucher, tongTienHang, hoaDon);
                if (checkResult == null) {
                    tienGiamVoucher = tinhMucGiamVoucher(voucher, tongTienHang);
                    System.out.println("  - Voucher hợp lệ, giảm: " + tienGiamVoucher);
                } else {
                    System.out.println("  - Voucher KHÔNG hợp lệ: " + checkResult);
                    hoaDon.setMaGiamGia(null);
                    hoaDonRepo.save(hoaDon);
                    voucher = null;
                }
            }

            // ⭐ TÍNH TỔNG TIỀN
            BigDecimal tongTien = tongTienHang.subtract(tienGiamVoucher);

            // ⭐ CỘNG PHÍ SHIP
            if ("Online".equalsIgnoreCase(hoaDon.getLoaiBan())) {
                BigDecimal ship = hoaDon.getTienShip() != null ? hoaDon.getTienShip() : BigDecimal.ZERO;
                tongTien = tongTien.add(ship);
            }

            // ⭐ CẬP NHẬT LẠI TỔNG TIỀN VÀO HÓA ĐƠN (CHỈ LƯU TONG_TIEN)
            hoaDon.setTongTien(tongTien);
            hoaDonRepo.save(hoaDon);
            hoaDonRepo.flush();

            // ⭐ TRẢ VỀ RESPONSE
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("danhSachSanPham", danhSachSanPham);
            response.put("tongTienHang", tongTienHang);
            response.put("tienGiamVoucher", tienGiamVoucher);
            response.put("tongTien", tongTien);
            response.put("tongTienMoi", tongTien);
            response.put("maHoaDon", mahd);

            if (voucher != null) {
                Map<String, Object> voucherInfo = new HashMap<>();
                voucherInfo.put("maGiamGia", voucher.getMaGiamGia());
                voucherInfo.put("tenGiamGia", voucher.getTenGiamGia());
                voucherInfo.put("tienGiam", tienGiamVoucher);
                response.put("voucher", voucherInfo);
            }

            System.out.println("📤 [RESPONSE] Trả về: tongTien=" + tongTien + ", tongTienHang=" + tongTienHang);
            System.out.println("========================================");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    @GetMapping("/get-invoice-totals")
    @ResponseBody
    public ResponseEntity<?> getInvoiceTotals(@RequestParam String mahd) {
        try {
            System.out.println("=== GET INVOICE TOTALS ===");
            System.out.println("📋 mahd: " + mahd);

            // 1. Lấy hóa đơn
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Không tìm thấy hóa đơn"));
            }

            // 2. Lấy chi tiết hóa đơn
            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            if (listhdct == null) listhdct = new ArrayList<>();

            // 3. Tính tổng tiền hàng
            BigDecimal tongTienHang = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            System.out.println("💰 Tổng tiền hàng: " + tongTienHang);

            // 4. Xử lý voucher - CHỈ KIỂM TRA, KHÔNG XÓA
            BigDecimal tienGiamVoucher = BigDecimal.ZERO;
            GiamGia voucher = hoaDon.getMaGiamGia();
            boolean voucherHopLe = false;
            String voucherMessage = "";

            if (voucher != null) {
                String maKhachHang = hoaDon.getMaKhachHang() != null ?
                        hoaDon.getMaKhachHang().getMaKH() : null;

                System.out.println("🔍 Kiểm tra voucher: " + voucher.getMaGiamGia() + " - " + voucher.getTenGiamGia());
                System.out.println("👤 Mã KH: " + maKhachHang);

                // ⭐ CHỈ KIỂM TRA, KHÔNG XÓA
                boolean isValid = giamGiaService.kiemTraVoucherHopLeChoThanhToan(
                        voucher.getMaGiamGia(), maKhachHang);

                if (isValid) {
                    tienGiamVoucher = giamGiaService.tinhSoTienGiam(voucher, tongTienHang);
                    voucherHopLe = true;
                    voucherMessage = "Voucher hợp lệ";
                    System.out.println("✅ Voucher hợp lệ, tiền giảm: " + tienGiamVoucher);
                } else {
                    voucherHopLe = false;
                    voucherMessage = "Voucher không hợp lệ! (Sẽ được xử lý khi thanh toán hoặc áp dụng lại)";
                    System.out.println("⚠️ Voucher không hợp lệ: " + voucher.getMaGiamGia());
                    // ⭐ KHÔNG XÓA VOUCHER TẠI ĐÂY
                    // Chỉ đánh dấu là không hợp lệ, không set null
                }
            } else {
                System.out.println("ℹ️ Không có voucher đang áp dụng");
            }

            // 5. Tính tổng tiền sau voucher
            BigDecimal tongTien = tongTienHang.subtract(tienGiamVoucher);

            // 6. Xử lý phí ship (KHÔNG SET CỨNG 30k)
            BigDecimal tienShip = hoaDon.getTienShip() != null ? hoaDon.getTienShip() : BigDecimal.ZERO;
            System.out.println("🚚 Tiền ship trong DB: " + tienShip);

            if ("Online".equalsIgnoreCase(hoaDon.getLoaiBan()) && tienShip.compareTo(BigDecimal.ZERO) > 0) {
                tongTien = tongTien.add(tienShip);
                System.out.println("📦 Đã cộng phí ship: " + tienShip);
            } else if ("Online".equalsIgnoreCase(hoaDon.getLoaiBan())) {
                System.out.println("📦 Chưa có phí ship (FREE SHIP hoặc chưa tính)");
            }

            // 7. Đảm bảo không âm
            if (tongTien.compareTo(BigDecimal.ZERO) < 0) {
                tongTien = BigDecimal.ZERO;
            }

            // 8. Cập nhật tổng tiền vào hóa đơn
            hoaDon.setTongTien(tongTien);
            hoaDonService.save(hoaDon);

            System.out.println("💰 Tổng tiền sau cùng: " + tongTien);

            // 9. Trả về response
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("loaiBan", hoaDon.getLoaiBan());
            result.put("tongTienHang", tongTienHang);
            result.put("tienGiamVoucher", tienGiamVoucher);
            result.put("tongTien", tongTien);
            result.put("tienShip", tienShip);
            result.put("voucherHopLe", voucherHopLe);
            result.put("voucherMessage", voucherMessage);

            // Thông tin người nhận
            result.put("tenNguoiNhan", hoaDon.getTenNguoiNhan() != null ? hoaDon.getTenNguoiNhan() : "");
            result.put("sdtNguoiNhan", hoaDon.getSdtNguoiNhan() != null ? hoaDon.getSdtNguoiNhan() : "");
            result.put("diaChiGiaoHang", hoaDon.getDiaChiGiaoHang() != null ? hoaDon.getDiaChiGiaoHang() : "");

            // Thông tin voucher (nếu có)
            if (voucher != null) {
                Map<String, Object> voucherInfo = new HashMap<>();
                voucherInfo.put("maGiamGia", voucher.getMaGiamGia());
                voucherInfo.put("tenGiamGia", voucher.getTenGiamGia());
                voucherInfo.put("tienGiam", tienGiamVoucher);
                voucherInfo.put("hopLe", voucherHopLe);
                result.put("voucher", voucherInfo);
            }

            System.out.println("=== END GET INVOICE TOTALS ===");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Lỗi: " + e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/get-voucher-discount")
    @ResponseBody
    public ResponseEntity<?> getVoucherDiscount(@RequestParam String mahd) {
        try {
            System.out.println("=== GET VOUCHER DISCOUNT ===");
            System.out.println("mahd: " + mahd);

            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Không tìm thấy hóa đơn"
                ));
            }

            // Lấy chi tiết hóa đơn để tính tổng tiền hàng
            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            if (listhdct == null) listhdct = new ArrayList<>();

            // TÍNH TỔNG TIỀN HÀNG (chưa trừ voucher)
            BigDecimal tongTienHang = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            System.out.println("💰 Tổng tiền hàng: " + tongTienHang);

            // Lấy voucher đang áp dụng
            GiamGia voucher = hoaDon.getMaGiamGia();
            BigDecimal tienGiam = BigDecimal.ZERO;
            boolean voucherHopLe = false;
            String voucherMessage = "";

            if (voucher != null) {
                System.out.println("📌 Voucher đang áp dụng: " + voucher.getMaGiamGia() + " - " + voucher.getTenGiamGia());

                // ⭐ KIỂM TRA VOUCHER CÒN HỢP LỆ KHÔNG
                Map<String, Object> checkResult = kiemTraVoucherHienTai(voucher, tongTienHang, hoaDon);

                if (checkResult == null) {
                    // Voucher hợp lệ -> tính tiền giảm
                    tienGiam = tinhMucGiamVoucher(voucher, tongTienHang);
                    voucherHopLe = true;
                    voucherMessage = "Voucher hợp lệ";
                    System.out.println("✅ Voucher hợp lệ, tiền giảm: " + tienGiam);
                } else {
                    // Voucher không hợp lệ -> tự động bỏ
                    String lyDo = (String) checkResult.get("message");
                    System.out.println("⚠️ Voucher không hợp lệ: " + lyDo);

                    // Tự động bỏ voucher
                    hoaDon.setMaGiamGia(null);
                    hoaDonService.save(hoaDon);
                    voucher = null;
                    voucherMessage = "Voucher không còn hợp lệ: " + lyDo;
                }
            } else {
                System.out.println("ℹ️ Không có voucher đang áp dụng");
            }

            // ⭐ TÍNH TỔNG TIỀN SAU VOUCHER
            BigDecimal tongTien = tongTienHang.subtract(tienGiam);

            // ⭐ CỘNG PHÍ SHIP NẾU LÀ ĐƠN ONLINE
            if ("Online".equalsIgnoreCase(hoaDon.getLoaiBan())) {
                BigDecimal ship = hoaDon.getTienShip() != null ? hoaDon.getTienShip() : BigDecimal.ZERO;
                if (ship.compareTo(BigDecimal.ZERO) == 0) {
                    ship = tinhPhiShipGHN(hoaDon);
                    if (ship == null || ship.compareTo(BigDecimal.ZERO) <= 0) {
                        ship = PHI_SHIP_MAC_DINH;
                    }
                    hoaDon.setTienShip(ship);
                }
                tongTien = tongTien.add(ship);
                System.out.println("🚚 Phí ship: " + ship);
            }

            // ⭐ ĐẢM BẢO KHÔNG ÂM
            if (tongTien.compareTo(BigDecimal.ZERO) < 0) {
                tongTien = BigDecimal.ZERO;
            }

            // ⭐ CẬP NHẬT TỔNG TIỀN VÀO HÓA ĐƠN
            hoaDon.setTongTien(tongTien);
            hoaDonService.save(hoaDon);

            // ⭐ TRẢ VỀ RESPONSE
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("tongTienHang", tongTienHang);
            result.put("tienGiam", tienGiam);
            result.put("tongTien", tongTien);
            result.put("tienShip", hoaDon.getTienShip() != null ? hoaDon.getTienShip() : BigDecimal.ZERO);
            result.put("voucherHopLe", voucherHopLe);
            result.put("message", voucherMessage);

            if (voucher != null && voucherHopLe) {
                Map<String, Object> voucherInfo = new HashMap<>();
                voucherInfo.put("maGiamGia", voucher.getMaGiamGia());
                voucherInfo.put("tenGiamGia", voucher.getTenGiamGia());
                voucherInfo.put("loaiGiamGia", voucher.getLoaiGiamGia());
                voucherInfo.put("giaTriGiam", voucher.getGiaTriGiam());
                voucherInfo.put("tienGiam", tienGiam);
                result.put("voucher", voucherInfo);
            }

            System.out.println("=== END GET VOUCHER DISCOUNT ===");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/check-voucher-valid")
    @ResponseBody
    public ResponseEntity<?> checkVoucherValid(
            @RequestParam String mahd,
            @RequestParam String magg) {
        try {
            System.out.println("=== CHECK VOUCHER VALID ===");
            System.out.println("mahd: " + mahd);
            System.out.println("magg: " + magg);

            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Không tìm thấy hóa đơn"
                ));
            }

            GiamGia voucher = giamGiaService.getGiamGiaById(magg).orElse(null);
            if (voucher == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Voucher không tồn tại"
                ));
            }

            // Lấy tổng tiền hàng
            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            if (listhdct == null) listhdct = new ArrayList<>();

            BigDecimal tongTienHang = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            System.out.println("💰 Tổng tiền hàng: " + tongTienHang);
            System.out.println("📌 Voucher: " + voucher.getMaGiamGia() + " - " + voucher.getTenGiamGia());

            // ⭐ KIỂM TRA VOUCHER HỢP LỆ
            Map<String, Object> checkResult = kiemTraVoucherHienTai(voucher, tongTienHang, hoaDon);
            boolean isValid = (checkResult == null);

            // ⭐ NẾU VOUCHER KHÔNG HỢP LỆ VÀ ĐANG ĐƯỢC ÁP DỤNG -> TỰ ĐỘNG BỎ
            boolean wasApplied = false;
            String actionMessage = "";

            if (!isValid && hoaDon.getMaGiamGia() != null &&
                    hoaDon.getMaGiamGia().getMaGiamGia().equals(magg)) {

                System.out.println("⚠️ Voucher không hợp lệ, tự động bỏ khỏi hóa đơn");
                hoaDon.setMaGiamGia(null);
                hoaDonService.save(hoaDon);
                wasApplied = true;
                actionMessage = "Voucher không hợp lệ, đã tự động bỏ!";

                // ⭐ TÌM VOUCHER THAY THẾ
                GiamGia bestVoucher = timVoucherTotNhatChoHoaDon(hoaDon, tongTienHang);
                if (bestVoucher != null && !bestVoucher.getMaGiamGia().equals(magg)) {
                    hoaDon.setMaGiamGia(bestVoucher);
                    hoaDonService.save(hoaDon);

                    BigDecimal tienGiamMoi = tinhMucGiamVoucher(bestVoucher, tongTienHang);
                    actionMessage += " Đã áp dụng voucher thay thế: " + bestVoucher.getTenGiamGia() +
                            " (Giảm " + formatCurrency(tienGiamMoi) + ")";
                    System.out.println("✅ Đã áp dụng voucher thay thế: " + bestVoucher.getMaGiamGia());
                }
            }

            // ⭐ TÍNH LẠI TỔNG TIỀN
            BigDecimal tienGiam = BigDecimal.ZERO;
            if (isValid && hoaDon.getMaGiamGia() != null) {
                tienGiam = tinhMucGiamVoucher(voucher, tongTienHang);
            }

            BigDecimal tongTien = tongTienHang.subtract(tienGiam);

            // Cộng phí ship nếu Online
            if ("Online".equalsIgnoreCase(hoaDon.getLoaiBan())) {
                BigDecimal ship = hoaDon.getTienShip() != null ? hoaDon.getTienShip() : BigDecimal.ZERO;
                tongTien = tongTien.add(ship);
            }

            if (tongTien.compareTo(BigDecimal.ZERO) < 0) {
                tongTien = BigDecimal.ZERO;
            }

            hoaDon.setTongTien(tongTien);
            hoaDonService.save(hoaDon);

            // ⭐ TRẢ VỀ RESPONSE
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("isValid", isValid);
            result.put("message", isValid ? "Voucher hợp lệ" :
                    (actionMessage.isEmpty() ? "Voucher không còn hợp lệ" : actionMessage));
            result.put("tongTienHang", tongTienHang);
            result.put("tienGiam", tienGiam);
            result.put("tongTien", tongTien);

            if (isValid) {
                result.put("voucher", Map.of(
                        "maGiamGia", voucher.getMaGiamGia(),
                        "tenGiamGia", voucher.getTenGiamGia(),
                        "loaiGiamGia", voucher.getLoaiGiamGia(),
                        "giaTriGiam", voucher.getGiaTriGiam(),
                        "tienGiam", tienGiam
                ));
            }

            // ⭐ THÔNG TIN VOUCHER HIỆN TẠI TRÊN HÓA ĐƠN
            if (hoaDon.getMaGiamGia() != null) {
                result.put("currentVoucher", Map.of(
                        "maGiamGia", hoaDon.getMaGiamGia().getMaGiamGia(),
                        "tenGiamGia", hoaDon.getMaGiamGia().getTenGiamGia()
                ));
            }

            result.put("wasApplied", wasApplied);

            System.out.println("=== END CHECK VOUCHER VALID ===");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/update-voucher-on-total-change")
    @ResponseBody
    public ResponseEntity<?> updateVoucherOnTotalChange(@RequestParam String mahd) {
        try {
            HoaDon hoaDon = hoaDonService.findById(mahd);
            if (hoaDon == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Không tìm thấy hóa đơn"));
            }

            // Lấy tổng tiền hàng
            List<HoaDonChiTiet> listhdct = hoaDonChiTietService.findById(mahd);
            if (listhdct == null) listhdct = new ArrayList<>();

            BigDecimal tongTienHang = listhdct.stream()
                    .map(HoaDonChiTiet::getThanhTien)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            GiamGia currentVoucher = hoaDon.getMaGiamGia();
            boolean voucherChanged = false;
            String message = "";

            // Kiểm tra voucher hiện tại
            if (currentVoucher != null) {
                Map<String, Object> checkResult = kiemTraVoucherHienTai(currentVoucher, tongTienHang, hoaDon);
                if (checkResult != null) {
                    // Voucher không hợp lệ -> tìm voucher thay thế
                    GiamGia bestVoucher = timVoucherTotNhatChoHoaDon(hoaDon, tongTienHang);
                    if (bestVoucher != null && !bestVoucher.getMaGiamGia().equals(currentVoucher.getMaGiamGia())) {
                        hoaDon.setMaGiamGia(bestVoucher);
                        voucherChanged = true;
                        message = "Đã chuyển sang voucher: " + bestVoucher.getTenGiamGia();
                    } else {
                        hoaDon.setMaGiamGia(null);
                        voucherChanged = true;
                        message = "Đã bỏ voucher không hợp lệ!";
                    }
                    hoaDonService.save(hoaDon);
                }
            } else {
                // Không có voucher -> tìm voucher tốt nhất
                GiamGia bestVoucher = timVoucherTotNhatChoHoaDon(hoaDon, tongTienHang);
                if (bestVoucher != null) {
                    hoaDon.setMaGiamGia(bestVoucher);
                    hoaDonService.save(hoaDon);
                    voucherChanged = true;
                    message = "Đã tự động áp dụng voucher: " + bestVoucher.getTenGiamGia();
                }
            }

            // Tính lại tổng tiền
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("voucherChanged", voucherChanged);
            result.put("message", message);

            // Lấy thông tin mới
            Map<String, Object> invoiceInfo = (Map<String, Object>) getInvoiceTotals(mahd).getBody();
            result.put("invoiceInfo", invoiceInfo);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/get-san-pham-list")
    @ResponseBody
    public ResponseEntity<?> getSanPhamList() {
        try {
            System.out.println("📦 [API] getSanPhamList called");
            List<SanPhamChiTiet> sanPhamList = sanPhamChiTietService.getallll();

            if (sanPhamList == null) {
                sanPhamList = new ArrayList<>();
            }

            System.out.println("📦 [API] Found " + sanPhamList.size() + " products");

            List<Map<String, Object>> result = new ArrayList<>();
            for (SanPhamChiTiet sp : sanPhamList) {
                Map<String, Object> item = new HashMap<>();
                item.put("maSanPhamChiTiet", sp.getMaSanPhamChiTiet());
                item.put("tenSanPham", sp.getSanPham() != null ? sp.getSanPham().getTenSanPham() : "Không xác định");
                item.put("mauSac", sp.getMauSac() != null ? sp.getMauSac().getTenMauSac() : "");
                item.put("kichThuoc", sp.getKichThuoc() != null ? sp.getKichThuoc().getTenKichThuoc() : "");
                item.put("giaBan", sp.getGiaBan() != null ? sp.getGiaBan() : BigDecimal.ZERO);
                item.put("soLuongTon", sp.getSoLuongTon() != null ? sp.getSoLuongTon() : 0);
                item.put("trangThai", sp.getTrangThai());
                result.add(item);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sanPhamList", result,
                    "total", result.size()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ [API] Error: " + e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Lỗi lấy danh sách sản phẩm: " + e.getMessage()
            ));
        }
    }

}
