package com.example.th06876_java202.Entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChiTietNhapHangDTO {
    //Thông tin nhà cung cấp
    private String tenNhaCungCap;
    private String sdt;
    private String diaChi;

    //Thông tin nhập hàng
    private Integer maPhieuNhap;
    private LocalDateTime ngayTao;
    private String tenNhanVien; // Lấy từ đối tượng NhanVien
    private String trangThai;

    //Thông tin sản phẩm
    List<SanPhamDetailDTO> sanPhamDetails;

    //Tổng tiền
    private BigDecimal tongTien;
}
