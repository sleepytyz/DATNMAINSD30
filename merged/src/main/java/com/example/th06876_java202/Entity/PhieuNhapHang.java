package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "PhieuNhapHang")
public class PhieuNhapHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaPhieuNhap")
    private Integer maPhieuNhap;

    @ManyToOne
    @JoinColumn(name = "MaNhaCungCap", nullable = false)
    private NhaCungCap nhaCungCap;

    @ManyToOne
    @JoinColumn(name = "MaNhanVien", nullable = false)
    private NhanVien nhanVien;

    @Column(name = "NgayNhap")
    private LocalDateTime ngayNhap;

    @Column(name = "TongTienNhap", precision = 18, scale = 2)
    private BigDecimal tongTienNhap;

    @Column(name = "TrangThai", length = 50)
    private String trangThai;

    @Column(name = "GhiChu", length = 500)
    private String ghiChu;

    @OneToMany(mappedBy = "phieuNhap")
    private List<ChiTietNhapHang> chiTietNhapHangList;
}
