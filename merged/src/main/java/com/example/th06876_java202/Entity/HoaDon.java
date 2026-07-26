package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "HoaDon")
public class HoaDon {

    @Id
    private String maHoaDon;

    @ManyToOne
    @JoinColumn(name = "MaKhachHang")
    private KhachHang maKhachHang;

    @ManyToOne
    @JoinColumn(name = "MaNhanVien")
    private NhanVien maNhanVien;

    @ManyToOne
    @JoinColumn(name = "MaGiamGia")
    private GiamGia maGiamGia;

    @Column(name = "TongTien")
    private BigDecimal tongTien;

    @Column(name = "TienKhachDua")
    private BigDecimal tienKhachDua;

    @Column(name = "TienThua")
    private BigDecimal tienThua;

    @Column(name = "TienShip")
    private BigDecimal tienShip;

    @Column(name = "PhuongThucThanhToan")
    private String phuongThucThanhToan;

    @Column(name = "TrangThai")
    private String trangThai;

    @Column(name = "GhiChu")
    private String ghiChu;

    @Column(name = "NgayTao")
    private LocalDateTime ngayTao;

    @Column(name = "NgayThanhToan")
    private LocalDateTime ngayThanhToan;

    @Column(name = "LoaiBan")
    private String loaiBan;

    @Column(name = "DiaChiGiaoHang", length = 1000)
    private String diaChiGiaoHang;

    @Column(name = "TenNguoiNhan")
    private String tenNguoiNhan;

    @Column(name = "SdtNguoiNhan")
    private String sdtNguoiNhan;

    @OneToMany(mappedBy = "maHoaDon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HoaDonChiTiet> hoaDonChiTiets = new ArrayList<>();
}