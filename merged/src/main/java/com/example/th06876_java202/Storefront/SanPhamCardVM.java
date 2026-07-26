package com.example.th06876_java202.Storefront;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class SanPhamCardVM {
    private String maSanPham;
    private String tenSanPham;
    private String tenThuongHieu;
    private String tenDanhMuc;
    private String anh;
    private BigDecimal giaGoc;
    private BigDecimal giaSauGiam;
    private Integer phanTramGiam;
    private boolean conHang;
    private int tongTon;
    private long daBan;
    private double diemTrungBinh;
    private long soLuotDanhGia;
    private boolean moiVe;
    private String maBienTheHienThi;
    private boolean banChay;
    private List<String> tenMauSacs = new ArrayList<>();
    private List<String> tenKichThuocs = new ArrayList<>();
}
