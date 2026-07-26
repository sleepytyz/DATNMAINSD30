package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "GiaoCa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GiaoCa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaGiaoCa")
    private Integer maGiaoCa;

    @Column(name = "NgayGiao")
    private LocalDateTime ngayGiao;

    @ManyToOne
    @JoinColumn(name = "MaNhanVienBanGiao")
    private NhanVien nhanVienBanGiao;

    @ManyToOne
    @JoinColumn(name = "MaNhanVienNhanGiao")
    private NhanVien nhanVienNhanGiao;

    @Column(name = "TienMatBanGiao")
    private BigDecimal tienMatBanGiao;

    @Column(name = "SoHoaDonTrongCa")
    private Integer soHoaDonTrongCa;

    @Column(name = "DoanhThuTrongCa")
    private BigDecimal doanhThuTrongCa;

    @Column(name = "GhiChu")
    private String ghiChu;

    @Column(name = "XacNhan")
    private Boolean xacNhan;
}