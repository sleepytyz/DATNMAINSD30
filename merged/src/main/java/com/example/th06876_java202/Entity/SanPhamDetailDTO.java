package com.example.th06876_java202.Entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SanPhamDetailDTO {
    private String tenSanPham;
    private String size;
    private Integer soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;
}
