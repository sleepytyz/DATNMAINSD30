package com.example.th06876_java202.Storefront;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Một dòng trong giỏ hàng (lưu trong session của khách).
 * Chỉ lưu mã biến thể + số lượng; giá / tồn kho / khuyến mãi LUÔN được đọc lại
 * từ CSDL tại thời điểm hiển thị và đặt hàng để bảo đảm chính xác.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GioHangItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String maSanPhamChiTiet;
    private Integer soLuong;
}