package com.example.th06876_java202.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Gộp số liệu bán hàng (doanh thu, số đơn) + chấm công (giờ công, số ngày công,
 * số lần trễ, số lần vắng) của MỘT nhân viên trong 1 khoảng thời gian.
 * Dùng cho bảng "Hiệu suất nhân viên" (ADMIN) và trang "Thống kê của tôi" (STAFF).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NhanVienHieuSuatDTO {
    private String maNhanVien;
    private String hoTen;

    // Bán hàng
    private Integer soDonHang = 0;
    private BigDecimal doanhThu = BigDecimal.ZERO;

    // Chấm công
    private Integer soNgayCong = 0;
    private BigDecimal tongGioLam = BigDecimal.ZERO;
    private Integer soLanTre = 0;
    private Integer soLanVangMat = 0;
}
