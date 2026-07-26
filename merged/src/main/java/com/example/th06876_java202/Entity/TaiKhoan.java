package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TaiKhoan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaiKhoan implements java.io.Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTaiKhoan")
    private Integer maTaiKhoan;

    @Column(name = "TenDangNhap")
    private String tenDangNhap;

    @Column(name = "MatKhau")
    private String matKhau;

    @Column(name = "VaiTro")
    private String vaiTro;

    @Column(name = "TrangThai")
    private Boolean trangThai;

    @OneToOne(mappedBy = "taiKhoan", fetch = FetchType.EAGER)
    private NhanVien nhanVien;

    @OneToOne(mappedBy = "taiKhoan", fetch = FetchType.EAGER)
    private KhachHang khachHang;
}