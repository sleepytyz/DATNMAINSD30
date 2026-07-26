package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.*;
import com.example.th06876_java202.Service.*;
import com.example.th06876_java202.Storefront.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Cửa hàng: danh sách sản phẩm (bộ lọc danh mục / thương hiệu / kiểu giày / giá /
 * màu / size / khuyến mãi / còn hàng + sắp xếp + phân trang), trang chi tiết
 * (biến thể, thư viện ảnh, đánh giá), sản phẩm yêu thích và gợi ý tìm kiếm nhanh.
 */
@Controller
@RequiredArgsConstructor
public class CuaHangController {

    private final SanPhamService sanPhamService;
    private final SanPhamHienThiService sanPhamHienThiService;
    private final DanhMucSanPhamService danhMucSanPhamService;
    private final ThuongHieuService thuongHieuService;
    private final KieuGiayService kieuGiayService;
    private final DanhGiaService danhGiaService;
    private final KhachHangService khachHangService;
    private final YeuThich yeuThich;

    private String maKhachHangHienTai(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        KhachHang kh = khachHangService.findByTenDangNhap(authentication.getName());
        return kh != null ? kh.getMaKH() : null;
    }

    // =====================================================================
    // DANH SÁCH SẢN PHẨM
    // =====================================================================

    @GetMapping("/cua-hang/san-pham")
    public String danhSach(@RequestParam(required = false) String danhMuc,
                           @RequestParam(required = false) String thuongHieu,
                           @RequestParam(required = false) String kieuGiay,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) BigDecimal giaTu,
                           @RequestParam(required = false) BigDecimal giaDen,
                           @RequestParam(required = false) String mauSac,
                           @RequestParam(required = false) String kichThuoc,
                           @RequestParam(defaultValue = "false") boolean khuyenMai,
                           @RequestParam(defaultValue = "false") boolean conHang,
                           @RequestParam(defaultValue = "moi-nhat") String sapXep,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {

        // Bước 1: lọc thô ở tầng CSDL (danh mục / thương hiệu / kiểu giày / từ khoá)
        List<SanPham> ds = sanPhamService.findAllWithFilters(
                lam(danhMuc), true, lam(thuongHieu), lam(kieuGiay), lam(q));
        List<SanPhamCardVM> cards = sanPhamHienThiService.taoDanhSachCard(ds);

        // Tuỳ chọn màu / size cho bộ lọc (lấy từ chính tập kết quả hiện tại)
        LinkedHashSet<String> mauCoSan = new LinkedHashSet<>();
        LinkedHashSet<String> sizeCoSan = new LinkedHashSet<>();
        for (SanPhamCardVM c : cards) {
            if (c.getTenMauSacs() != null) mauCoSan.addAll(c.getTenMauSacs());
            if (c.getTenKichThuocs() != null) sizeCoSan.addAll(c.getTenKichThuocs());
        }
        List<String> dsSize = new ArrayList<>(sizeCoSan);
        dsSize.sort(Comparator.comparingInt(CuaHangController::thuTuSize));

        // Bước 2: lọc tinh + sắp xếp + phân trang trong bộ nhớ
        KetQuaTrangVM kq = sanPhamHienThiService.locSapXepPhanTrang(
                cards, giaTu, giaDen, lam(mauSac), lam(kichThuoc), khuyenMai, conHang, sapXep, page, 12);

        model.addAttribute("kq", kq);
        model.addAttribute("danhMucs", danhMucSanPhamService.getAll().stream()
                .filter(DanhMucSanPham::isTrangThai).toList());
        model.addAttribute("thuongHieus", thuongHieuService.findAll().stream()
                .filter(ThuongHieu::isTrangThai).toList());
        model.addAttribute("kieuGiays", kieuGiayService.findAll());
        model.addAttribute("mauCoSan", mauCoSan);
        model.addAttribute("sizeCoSan", dsSize);
        model.addAttribute("yeuThichSet", yeuThich.getMaSanPhams());

        // Giữ lại lựa chọn đang áp dụng
        model.addAttribute("fDanhMuc", lam(danhMuc));
        model.addAttribute("fThuongHieu", lam(thuongHieu));
        model.addAttribute("fKieuGiay", lam(kieuGiay));
        model.addAttribute("fQ", lam(q));
        model.addAttribute("fGiaTu", giaTu);
        model.addAttribute("fGiaDen", giaDen);
        model.addAttribute("fMauSac", lam(mauSac));
        model.addAttribute("fKichThuoc", lam(kichThuoc));
        model.addAttribute("fKhuyenMai", khuyenMai);
        model.addAttribute("fConHang", conHang);
        model.addAttribute("fSapXep", sapXep);
        return "cuahang/danh-sach";
    }

    // =====================================================================
    // CHI TIẾT SẢN PHẨM
    // =====================================================================

    @GetMapping("/cua-hang/san-pham/{maSanPham}")
    public String chiTiet(@PathVariable String maSanPham,
                          @RequestParam(name = "bt", required = false) String btChon,
                          Authentication authentication,
                          Model model) {
        SanPham sp = sanPhamService.findById(maSanPham).orElse(null);
        if (sp == null || !Boolean.TRUE.equals(sp.getTrangThai())) {
            return "redirect:/cua-hang/san-pham";
        }

        SanPhamCardVM card = sanPhamHienThiService.taoCard(sp);
        List<BienTheVM> bienThe = sanPhamHienThiService.taoDanhSachBienThe(maSanPham);
        DanhGiaThongKeVM thongKeDanhGia = sanPhamHienThiService.thongKeDanhGiaSanPham(maSanPham);
        List<DanhGia> danhGias = danhGiaService.danhSachTheoSanPham(maSanPham);

        String maKH = maKhachHangHienTai(authentication);
        boolean coTheDanhGia = maKH != null && danhGiaService.khachCoTheDanhGia(maKH, maSanPham);

        // Sản phẩm liên quan: cùng danh mục, bỏ chính nó
        String maDanhMuc = sp.getDanhMucSanPham() != null ? sp.getDanhMucSanPham().getMaDanhMuc() : null;
        List<SanPhamCardVM> lienQuan = new ArrayList<>();
        if (maDanhMuc != null) {
            List<SanPham> cungDanhMuc = sanPhamService.findAllWithFilters(maDanhMuc, true, null, null, null);
            lienQuan = sanPhamHienThiService.taoDanhSachCard(cungDanhMuc).stream()
                    .filter(c -> !maSanPham.equals(c.getMaSanPham()))
                    .limit(4)
                    .toList();
        }

        model.addAttribute("sp", sp);
        model.addAttribute("card", card);
        model.addAttribute("bienThe", bienThe);
        model.addAttribute("btChon", btChon);
        model.addAttribute("thongKeDanhGia", thongKeDanhGia);
        model.addAttribute("danhGias", danhGias);
        model.addAttribute("coTheDanhGia", coTheDanhGia);
        model.addAttribute("daYeuThich", yeuThich.chua(maSanPham));
        model.addAttribute("lienQuan", lienQuan);
        return "cuahang/chi-tiet";
    }

    // =====================================================================
    // YÊU THÍCH (wishlist theo phiên)
    // =====================================================================

    @PostMapping("/cua-hang/yeu-thich/{maSanPham}")
    @ResponseBody
    public Map<String, Object> daoYeuThich(@PathVariable String maSanPham) {
        boolean dangThich = yeuThich.daoTrangThai(maSanPham);
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("ok", true);
        kq.put("yeuThich", dangThich);
        kq.put("soLuong", yeuThich.soLuong());
        kq.put("thongBao", dangThich ? "Đã thêm vào danh sách yêu thích." : "Đã bỏ khỏi danh sách yêu thích.");
        return kq;
    }

    @GetMapping("/cua-hang/yeu-thich")
    public String trangYeuThich(Model model) {
        List<SanPhamCardVM> cards = new ArrayList<>();
        List<SanPham> sps = new ArrayList<>();
        for (String ma : yeuThich.getMaSanPhams()) {
            sanPhamService.findById(ma)
                    .filter(sp -> Boolean.TRUE.equals(sp.getTrangThai()))
                    .ifPresent(sps::add);
        }
        if (!sps.isEmpty()) {
            cards = sanPhamHienThiService.taoDanhSachCard(sps);
        }
        model.addAttribute("cards", cards);
        model.addAttribute("yeuThichSet", yeuThich.getMaSanPhams());
        return "cuahang/yeu-thich";
    }

    // =====================================================================
    // GỢI Ý TÌM KIẾM NHANH (AJAX)
    // =====================================================================

    @GetMapping("/api/cua-hang/tim-kiem")
    @ResponseBody
    public List<Map<String, Object>> goiYTimKiem(@RequestParam(required = false) String q) {
        List<Map<String, Object>> kq = new ArrayList<>();
        String tuKhoa = lam(q);
        if (tuKhoa == null || tuKhoa.length() < 2) return kq;

        List<SanPham> ds = sanPhamService.findAllWithFilters(null, true, null, null, tuKhoa);
        List<SanPhamCardVM> cards = sanPhamHienThiService.taoDanhSachCard(
                ds.stream().limit(6).toList());
        for (SanPhamCardVM c : cards) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ma", c.getMaSanPham());
            m.put("ten", c.getTenSanPham());
            m.put("anh", c.getAnh());
            m.put("gia", c.getGiaSauGiam());
            m.put("giaGoc", c.getGiaGoc());
            m.put("phanTramGiam", c.getPhanTramGiam());
            m.put("conHang", c.isConHang());
            kq.add(m);
        }
        return kq;
    }


    private static String lam(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static int thuTuSize(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 1000 + (s != null ? s.hashCode() % 100 : 0);
        }
    }
}