package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Đánh giá sản phẩm của khách hàng.
 * Mỗi đánh giá gắn với 1 sản phẩm (SanPham), 1 khách hàng (KhachHang) và (tuỳ chọn) hoá đơn đã mua.
 * Khách chỉ được đánh giá sản phẩm đã mua và đơn đã ở trạng thái "Đã giao".
 */
@Entity
@Table(name = "DanhGia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DanhGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDanhGia")
    private Integer maDanhGia;

    @ManyToOne
    @JoinColumn(name = "MaSanPham")
    private SanPham sanPham;

    @ManyToOne
    @JoinColumn(name = "MaKhachHang")
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "MaHoaDon")
    private HoaDon hoaDon;

    /** Số sao 1..5 */
    @Column(name = "SoSao")
    private Integer soSao;

    @Column(name = "NoiDung", length = 2000)
    private String noiDung;

    @Column(name = "NgayDanhGia")
    private LocalDateTime ngayDanhGia;

    /** Ẩn/hiện đánh giá (admin có thể ẩn đánh giá vi phạm). */
    @Column(name = "TrangThai")
    private Boolean trangThai = true;
}
