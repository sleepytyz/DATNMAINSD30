package com.example.th06876_java202.Entity;

import com.example.th06876_java202.Entity.KhachHang;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "DiaChi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiaChi implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDiaChi")
    private Integer maDiaChi;

    @NotBlank(message = "Vui lòng chọn Quận/Huyện")
    @Column(name = "QuanHuyen")
    private String quanHuyen;

    @NotBlank(message = "Vui lòng chọn Phường/Xã")
    @Column(name = "PhuongXa")
    private String phuongXa;

    @Column(name = "DiaChiMacDinh", columnDefinition = "BIT")
    private Boolean diaChiMacDinh;

    @ManyToOne
    @JoinColumn(name = "MaKhachHang")
    private KhachHang khachHang;

    @NotBlank(message = "Tên người nhận không được để trống")
    @Column(name = "TenNguoiNhan")
    private String tenNguoiNhan;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    @Pattern(
            regexp = "^(0(3|5|7|8|9))[0-9]{8}$",
            message = "Số điện thoại không hợp lệ"
    )
    @Column(name = "SoDienThoaiNguoiNhan")
    private String soDienThoaiNguoiNhan;

    @NotBlank(message = "Vui lòng chọn Tỉnh/Thành")
    @Column(name = "TinhThanh")
    private String tinhThanh;

    @NotBlank(message = "Địa chỉ cụ thể không được để trống")
    @Column(name = "DiaChiCuThe")
    private String diaChiCuThe;

    /** Mã quận/huyện theo hệ thống Giao Hàng Nhanh — dùng để tính cước tự động. */
    @Column(name = "MaQuanHuyenGHN")
    private Integer maQuanHuyenGHN;

    /** Mã phường/xã (WardCode) theo hệ thống Giao Hàng Nhanh. */
    @Column(name = "MaPhuongXaGHN")
    private String maPhuongXaGHN;
}