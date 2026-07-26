package com.example.th06876_java202.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamChiTietDTO {

    private String maBienThe;

    private String maMauSac;
    private String maKichThuoc;
    private BigDecimal giaBan;
    private Integer soLuongTon;
    private String duongDanAnh;

    private List<String> danhSachAnh = new ArrayList<>();

    public String getAnhDaiDien() {
        if (duongDanAnh != null && !duongDanAnh.isEmpty()) {
            return duongDanAnh;
        }
        if (danhSachAnh != null && !danhSachAnh.isEmpty()) {
            return danhSachAnh.get(0);
        }
        return null;
    }

    public boolean hasMinimumImages(int min) {
        return danhSachAnh != null && danhSachAnh.size() >= min;
    }

    public int getImageCount() {
        return danhSachAnh != null ? danhSachAnh.size() : 0;
    }
}