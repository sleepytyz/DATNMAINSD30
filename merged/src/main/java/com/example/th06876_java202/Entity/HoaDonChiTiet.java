package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table( name = "ChiTietHoaDon")
public class HoaDonChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTiet")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "MaHoaDon")
    private HoaDon maHoaDon;

    @ManyToOne
    @JoinColumn( name = "MaSanPhamChiTiet")
    private SanPhamChiTiet sanPhamChiTiet;

    @Column(name = "SoLuong")
    private Integer soLuong;

    @Column( name = "DonGia")
    private BigDecimal donGia;

    @Column( name = "TienGiam")
    private BigDecimal tienGiam;

    @Column( name = "ThanhTien")
    private BigDecimal ThanhTien;
}
