package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.GiamGia;
import com.example.th06876_java202.Entity.KhachHang;
import com.example.th06876_java202.Service.GiamGiaService;
import com.example.th06876_java202.Service.KhachHangService;
import com.example.th06876_java202.Storefront.CartLineVM;
import com.example.th06876_java202.Storefront.GioHang;
import com.example.th06876_java202.Storefront.GioHangService;
import com.example.th06876_java202.Storefront.GioHangView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Giỏ hàng: trang xem giỏ + bộ API JSON (AJAX) để thêm / đổi số lượng / xoá /
 * áp mã giảm giá mà KHÔNG cần tải lại trang — kèm tổng tiền tính lại tức thì.
 */
@Controller
@RequestMapping("/gio-hang")
@RequiredArgsConstructor
public class GioHangController {

    private final GioHang gioHang;
    private final GioHangService gioHangService;
    private final KhachHangService khachHangService;
    private final GiamGiaService giamGiaService;

    private String maKhachHangHienTai(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        KhachHang kh = khachHangService.findByTenDangNhap(authentication.getName());
        return kh != null ? kh.getMaKH() : null;
    }

    // =====================================================================
    // TRANG GIỎ HÀNG
    // =====================================================================

    @GetMapping
    public String trangGioHang(Model model, Authentication authentication) {
        String maKH = maKhachHangHienTai(authentication);
        GioHangView view = gioHangService.xemGioHang(gioHang, maKH);
        model.addAttribute("gio", view);

        // Voucher gợi ý cho khách đã đăng nhập (công khai + voucher cá nhân được tặng)
        List<GiamGia> voucherGoiY = List.of();
        try {
            voucherGoiY = giamGiaService.getVoucherKhaDungChoKhachHang(maKH);
        } catch (Exception ignored) { }
        model.addAttribute("voucherGoiY", voucherGoiY);
        model.addAttribute("daDangNhap", maKH != null);
        return "giohang/index";
    }

    // =====================================================================
    // API JSON (AJAX)
    // =====================================================================

    /** Thêm vào giỏ (nút "Thêm vào giỏ" ở mọi nơi). */
    @PostMapping("/them")
    @ResponseBody
    public Map<String, Object> them(@RequestParam String maSanPhamChiTiet,
                                    @RequestParam(defaultValue = "1") int soLuong,
                                    Authentication authentication) {
        String loi = gioHangService.themVaoGio(gioHang, maSanPhamChiTiet, soLuong);
        Map<String, Object> kq = tomTat(authentication);
        kq.put("ok", loi == null || loi.startsWith("Chỉ còn"));
        kq.put("thongBao", loi == null ? "Đã thêm sản phẩm vào giỏ hàng." : loi);
        return kq;
    }

    /** Đổi số lượng 1 dòng (nút +/- trong trang giỏ). */
    @PostMapping("/cap-nhat")
    @ResponseBody
    public Map<String, Object> capNhat(@RequestParam String maSanPhamChiTiet,
                                       @RequestParam int soLuong,
                                       Authentication authentication) {
        String canhBao = gioHangService.capNhatSoLuong(gioHang, maSanPhamChiTiet, soLuong);
        Map<String, Object> kq = tomTat(authentication);
        kq.put("ok", true);
        if (canhBao != null) kq.put("thongBao", canhBao);
        return kq;
    }

    /** Xoá 1 dòng khỏi giỏ. */
    @PostMapping("/xoa")
    @ResponseBody
    public Map<String, Object> xoa(@RequestParam String maSanPhamChiTiet,
                                   Authentication authentication) {
        gioHang.xoaSanPham(maSanPhamChiTiet);
        Map<String, Object> kq = tomTat(authentication);
        kq.put("ok", true);
        kq.put("thongBao", "Đã xoá sản phẩm khỏi giỏ hàng.");
        return kq;
    }
    @PostMapping("/ap-dung-voucher")
    @ResponseBody
    public Map<String, Object> apDungVoucher(@RequestParam String maVoucher,
                                             Authentication authentication) {

        System.out.println("=== ÁP DỤNG VOUCHER ===");
        System.out.println("📝 maVoucher nhận được: '" + maVoucher + "'");
        System.out.println("🔐 authentication: " + (authentication != null ? "đã đăng nhập" : "chưa đăng nhập"));

        String maKhachHang = maKhachHangHienTai(authentication);
        System.out.println("👤 maKhachHang: " + maKhachHang);
        System.out.println("🛒 gioHang: " + (gioHang != null ? "không null" : "NULL!"));
        if (gioHang != null) {
            System.out.println("   - Số dòng: " + gioHang.getDanhSach().size());
            System.out.println("   - Voucher đang áp dụng: " + gioHang.getMaGiamGiaApDung());
        }

        String loi = gioHangService.apDungVoucher(gioHang, maKhachHang, maVoucher);

        System.out.println("📤 Kết quả: " + (loi == null ? "THÀNH CÔNG" : "LỖI: " + loi));

        Map<String, Object> kq = tomTat(authentication);
        kq.put("ok", loi == null);
        kq.put("thongBao", loi == null ? "Áp dụng mã giảm giá thành công!" : loi);

        System.out.println("📨 Response: " + kq);
        return kq;
    }
    @PostMapping("/bo-voucher")
    @ResponseBody
    public Map<String, Object> boVoucher(Authentication authentication) {
        gioHangService.boVoucher(gioHang);
        Map<String, Object> kq = tomTat(authentication);
        kq.put("ok", true);
        kq.put("thongBao", "Đã bỏ mã giảm giá.");
        return kq;
    }

    @GetMapping("/mini")
    @ResponseBody
    public Map<String, Object> mini(Authentication authentication) {
        Map<String, Object> kq = tomTat(authentication);
        kq.put("ok", true);
        return kq;
    }

    private Map<String, Object> tomTat(Authentication authentication) {
        GioHangView view = gioHangService.xemGioHang(gioHang, maKhachHangHienTai(authentication));
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("tongSoLuong", view.getTongSoLuong());
        kq.put("tongTienHang", view.getTongTienHang());
        kq.put("tietKiemKhuyenMai", view.getTietKiemKhuyenMai());
        kq.put("soTienGiamVoucher", view.getSoTienGiamVoucher());
        kq.put("tenVoucher", view.getVoucherApDung() != null ? view.getVoucherApDung().getTenGiamGia() : null);
        kq.put("tienShip", view.getTienShip());
        kq.put("conThieuDeFreeship", view.getConThieuDeFreeship());
        kq.put("tongThanhToan", view.getTongThanhToan());
        kq.put("canhBao", view.getCanhBao());

        List<Map<String, Object>> dong = new ArrayList<>();
        for (CartLineVM l : view.getDongHang()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ma", l.getMaSanPhamChiTiet());
            m.put("soLuong", l.getSoLuong());
            m.put("soLuongTon", l.getSoLuongTon());
            m.put("donGia", l.getDonGia());
            m.put("thanhTien", l.getThanhTien());
            m.put("conHopLe", l.isConHopLe());
            dong.add(m);
        }
        kq.put("dong", dong);
        return kq;
    }
}
