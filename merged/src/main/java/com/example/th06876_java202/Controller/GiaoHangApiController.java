package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.KhachHang;
import com.example.th06876_java202.Service.KhachHangService;
import com.example.th06876_java202.Storefront.GiaoHangNhanhService;
import com.example.th06876_java202.Storefront.GioHang;
import com.example.th06876_java202.Storefront.GioHangService;
import com.example.th06876_java202.Storefront.GioHangView;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * API phục vụ trang thanh toán: đổ danh sách địa giới của Giao Hàng Nhanh và
 * TÍNH LẠI CƯỚC ngay khi khách chọn xong Tỉnh / Quận-Huyện / Phường-Xã
 * (không cần tải lại trang).
 */
@RestController
@RequestMapping("/api/giao-hang")
@RequiredArgsConstructor
public class GiaoHangApiController {

    private static final DecimalFormat TIEN =
            new DecimalFormat("#,###", new DecimalFormatSymbols(Locale.US));

    private final GiaoHangNhanhService giaoHangNhanhService;
    private final GioHangService gioHangService;
    private final GioHang gioHang;
    private final KhachHangService khachHangService;

    /** Hệ thống có đang dùng cước thật của GHN không? */
    @GetMapping("/trang-thai")
    public Map<String, Object> trangThai() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bat", giaoHangNhanhService.daCauHinh());
        return m;
    }

    @GetMapping("/tinh-thanh")
    public List<Map<String, Object>> tinhThanh() {
        return giaoHangNhanhService.layTinhThanh();
    }

    @GetMapping("/quan-huyen")
    public List<Map<String, Object>> quanHuyen(@RequestParam("tinh") int maTinh) {
        return giaoHangNhanhService.layQuanHuyen(maTinh);
    }

    @GetMapping("/phuong-xa")
    public List<Map<String, Object>> phuongXa(@RequestParam("huyen") int maQuanHuyen) {
        return giaoHangNhanhService.layPhuongXa(maQuanHuyen);
    }

    /**
     * CHẨN ĐOÁN cấu hình GHN — mở bằng trình duyệt:
     *   /api/giao-hang/kiem-tra                      (kiểm tra token + master data)
     *   /api/giao-hang/kiem-tra?huyen=1442&xa=21012  (thử tính cước một tuyến cụ thể)
     */
    @GetMapping("/kiem-tra")
    public Map<String, Object> kiemTra(@RequestParam(value = "huyen", required = false) Integer huyen,
                                       @RequestParam(value = "xa", required = false) String xa) {
        return giaoHangNhanhService.kiemTra(huyen, xa);
    }

    /**
     * Giỏ dùng để tính cước: PHẢI trùng với giỏ trang thanh toán đang hiển thị.
     * Nếu khách đang ở chế độ "Mua ngay" (giỏ tạm trong session) mà API lại tính trên
     * giỏ hàng thật thì con số nhảy sai ngay khi đổi địa chỉ — nên dựng cùng một giỏ.
     */
    private GioHang gioDangThanhToan(HttpSession session) {
        Object ma = session.getAttribute(ThanhToanController.MN_SPCT);
        if (ma == null) return gioHang;
        GioHang tam = new GioHang();
        Object sl = session.getAttribute(ThanhToanController.MN_SL);
        tam.themSanPham((String) ma, sl instanceof Integer ? (Integer) sl : 1);
        Object v = session.getAttribute(ThanhToanController.MN_VOUCHER);
        if (v != null) tam.setMaGiamGiaApDung((String) v);
        return tam;
    }

    /** Tính lại cước + tổng thanh toán theo địa chỉ khách vừa chọn. */
    @GetMapping("/phi-ship")
    public Map<String, Object> phiShip(@RequestParam(value = "huyen", required = false) Integer maQuanHuyen,
                                       @RequestParam(value = "xa", required = false) String maPhuongXa,
                                       HttpSession session,
                                       Authentication authentication) {
        String maKH = maKhachHangHienTai(authentication);
        GioHangView view = gioHangService.xemGioHang(gioDangThanhToan(session), maKH, maQuanHuyen, maPhuongXa);

        BigDecimal phi = view.getTienShip() != null ? view.getTienShip() : BigDecimal.ZERO;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nguon", view.getNguonPhiShip());
        m.put("mienPhi", phi.signum() <= 0);
        m.put("phiShip", phi);
        m.put("phiShipText", phi.signum() > 0 ? TIEN.format(phi) + "\u20AB" : "Miễn phí");
        m.put("tongThanhToan", view.getTongThanhToan());
        m.put("tongThanhToanText", TIEN.format(view.getTongThanhToan()) + "\u20AB");
        BigDecimal goc = view.getPhiShipGoc() != null ? view.getPhiShipGoc() : BigDecimal.ZERO;
        m.put("phiShipGoc", goc);
        m.put("phiShipGocText", goc.signum() > 0 ? TIEN.format(goc) + "\u20AB" : "");
        m.put("ghiChu", ghiChuNguon(view.getNguonPhiShip())
                + ("FREESHIP".equals(view.getNguonPhiShip()) && goc.signum() > 0
                ? " Cước GHN cho tuyến này là " + TIEN.format(goc) + "\u20AB — cửa hàng tài trợ toàn bộ." : ""));
        if ("CO_DINH".equals(view.getNguonPhiShip()) && giaoHangNhanhService.daCauHinh()) {
            m.put("lyDo", giaoHangNhanhService.getLoiCuoi());   // vì sao chưa lấy được cước GHN
        }
        return m;
    }

    private String maKhachHangHienTai(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        KhachHang kh = khachHangService.findByTenDangNhap(authentication.getName());
        return kh != null ? kh.getMaKH() : null;
    }

    private String ghiChuNguon(String nguon) {
        if ("GHN".equals(nguon)) return "Cước tính theo Giao Hàng Nhanh cho tuyến bạn chọn.";
        if ("FREESHIP".equals(nguon)) return "Đơn đạt mốc miễn phí vận chuyển.";
        return "Đang áp dụng biểu phí mặc định (chưa xác định được tuyến giao).";
    }
}