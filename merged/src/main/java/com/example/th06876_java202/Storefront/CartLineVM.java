package com.example.th06876_java202.Storefront;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Một dòng giỏ hàng để hiển thị (trang giỏ hàng / thanh toán / giỏ mini).
 * Luôn được dựng lại từ dữ liệu mới nhất trong CSDL mỗi lần xem.
 */
@Data
@NoArgsConstructor
public class CartLineVM {
    private String maSanPhamChiTiet;
    private String maSanPham;
    private String tenSanPham;
    private String tenMauSac;
    private String tenKichThuoc;
    private String anh;
    private BigDecimal donGia;      // đơn giá thực tế (đã áp khuyến mãi)
    private BigDecimal donGiaGoc;   // giá gốc (hiển thị gạch ngang nếu có KM)
    private Integer phanTramGiam;
    private Integer soLuong;
    private Integer soLuongTon;     // tồn kho hiện tại (để giới hạn nút tăng)
    private BigDecimal thanhTien;
    private boolean conHopLe;       // false nếu hết hàng / ngừng bán / không tồn tại
}