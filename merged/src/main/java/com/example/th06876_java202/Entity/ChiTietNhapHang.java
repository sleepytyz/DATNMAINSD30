package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ChiTietNhapHang")
public class ChiTietNhapHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTietNhap")
    private Integer maChiTietNhap;

    @ManyToOne
    @JoinColumn(name = "MaPhieuNhap")
    private PhieuNhapHang phieuNhap;

    @ManyToOne
    @JoinColumn(name = "MaSanPhamChiTiet")
    private SanPhamChiTiet sanPhamChiTiet;

    @Column(name = "SoLuongNhap")
    private Integer soLuongNhap;

    @Column(name = "DonGiaNhap", precision = 18, scale = 2)
    private BigDecimal donGiaNhap;

    @Column(name = "ThanhTien", precision = 18, scale = 2)
    private BigDecimal thanhTien;
}
