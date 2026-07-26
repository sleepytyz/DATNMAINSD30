package com.example.th06876_java202.Storefront;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Thông tin 1 biến thể (màu + size) trên trang chi tiết sản phẩm. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BienTheVM {
    private String maSanPhamChiTiet;
    private String maMauSac;
    private String tenMauSac;
    private String maKichThuoc;
    private String tenKichThuoc;
    private BigDecimal giaGoc;
    private BigDecimal giaSauGiam;
    private Integer phanTramGiam;
    private Integer soLuongTon;
    private String anh;                              // ảnh đại diện của biến thể
    private List<String> danhSachAnh = new ArrayList<>(); // bộ sưu tập ảnh của biến thể
}