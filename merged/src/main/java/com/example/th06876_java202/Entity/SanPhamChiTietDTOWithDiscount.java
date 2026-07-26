package com.example.th06876_java202.Entity;

import lombok.Data;

import java.math.BigDecimal;

@Data

public class SanPhamChiTietDTOWithDiscount {
    private SanPhamChiTiet sanPhamChiTiet;
    private BigDecimal maxDiscount;
    private BigDecimal priceAfterDiscount;
    private boolean hasDiscount;


}