package com.example.th06876_java202.Storefront;

import com.example.th06876_java202.Entity.SanPham;
import com.example.th06876_java202.Entity.SanPhamChiTiet;
import com.example.th06876_java202.Repository.ChiTietDotGiamGiaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Tính giá khuyến mãi hiện tại cho sản phẩm / biến thể, dựa trên các
 * "Đợt giảm giá" (DotGiamGia / ChiTietDotGiamGia) mà bên Quản lý đã thiết lập
 * và còn hiệu lực theo ngày. Nhờ đó giá trên website LUÔN khớp với chương trình
 * khuyến mãi bên quản lý đang chạy.
 */
@Service
@RequiredArgsConstructor
public class KhuyenMaiService {

    private final ChiTietDotGiamGiaRepo chiTietDotGiamGiaRepo;

    /** % giảm cao nhất đang áp dụng cho 1 biến thể cụ thể, 0 nếu không có. */
    public int phanTramGiamBienThe(String maSanPhamChiTiet) {
        if (maSanPhamChiTiet == null) return 0;
        List<BigDecimal> list = chiTietDotGiamGiaRepo
                .findActiveDiscountPercentBySanPhamChiTiet(maSanPhamChiTiet, LocalDate.now());
        if (list == null || list.isEmpty()) return 0;
        return list.get(0).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /** % giảm cao nhất đang áp dụng cho 1 sản phẩm (bất kỳ biến thể nào), 0 nếu không có. */
    public int phanTramGiamSanPham(String maSanPham) {
        if (maSanPham == null) return 0;
        List<BigDecimal> list = chiTietDotGiamGiaRepo
                .findActiveDiscountPercentBySanPham(maSanPham, LocalDate.now());
        if (list == null || list.isEmpty()) return 0;
        return list.get(0).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * % giảm thực áp cho 1 biến thể: dòng khuyến mãi gán ĐÚNG biến thể này,
     * hoặc dòng gán cho CẢ sản phẩm (không gán biến thể cụ thể).
     * KHÔNG lấy nhầm % của biến thể khác cùng sản phẩm — bảo đảm giá ở giỏ hàng
     * / đặt hàng đúng với thiết lập của quầy quản lý.
     */
    public int phanTramGiamChoBienThe(SanPham sanPham, String maSanPhamChiTiet) {
        String maSP = sanPham != null ? sanPham.getMaSanPham() : null;
        List<BigDecimal> list = chiTietDotGiamGiaRepo
                .findActiveDiscountPercentChoBienThe(maSanPhamChiTiet, maSP, LocalDate.now());
        if (list == null || list.isEmpty()) return 0;
        return list.get(0).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public BigDecimal giaSauGiam(BigDecimal giaGoc, int phanTramGiam) {
        if (giaGoc == null) return BigDecimal.ZERO;
        if (phanTramGiam <= 0) return giaGoc;
        BigDecimal heSo = BigDecimal.valueOf(100 - phanTramGiam).divide(BigDecimal.valueOf(100));
        return giaGoc.multiply(heSo).setScale(0, RoundingMode.HALF_UP);
    }

    /** Giá thực tế (đã áp khuyến mãi nếu có) của 1 biến thể. */
    public BigDecimal giaThucTe(SanPhamChiTiet spct) {
        int pt = phanTramGiamChoBienThe(spct.getSanPham(), spct.getMaSanPhamChiTiet());
        return giaSauGiam(spct.getGiaBan(), pt);
    }
}