package com.example.th06876_java202.Entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SanPhamDTO {
    // ===== THÔNG TIN CƠ BẢN =====
    private String maSanPham;
    private String tenSanPham;
    private String moTa;
    private Boolean trangThai;
    private LocalDateTime ngayTao;
    private String ngayTaoDisplay;  // Định dạng ngày tháng

    // ===== THÔNG TIN DANH MỤC =====
    private String maDanhMuc;
    private String tenDanhMuc;

    // ===== THÔNG TIN THƯƠNG HIỆU =====
    private String maThuongHieu;
    private String tenThuongHieu;

    // ===== THÔNG TIN KIỂU GIÀY =====
    private String maKieuGiay;
    private String tenKieuGiay;

    // ===== THÔNG TIN CHẤT LIỆU =====
    private String maChatLieu;
    private String tenChatLieu;

    // ===== THÔNG TIN GIÁ VÀ TỒN KHO =====
    private BigDecimal giaMin;
    private BigDecimal giaMax;
    private BigDecimal giaBanTrungBinh;
    private String giaBanDisplay;
    private Integer tongTon;

    // ===== THÔNG TIN BỔ SUNG =====
    private Integer soLuongBienThe;  // Số lượng biến thể
    private String trangThaiDisplay;  // Hiển thị trạng thái dạng text
}