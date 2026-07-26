package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "KHACHHANG_VOUCHER")
public class GiamGiaChiTiet {

    @EmbeddedId
    private GiamGiaChiTietId id;

    @ManyToOne
    @MapsId("maKhachHang")
    @JoinColumn(name = "MaKhachHang", insertable = false, updatable = false)
    private KhachHang khachHang;

    @ManyToOne
    @MapsId("maGiamGia")
    @JoinColumn(name = "MaGiamGia", insertable = false, updatable = false)
    private GiamGia giamGia;

    @Column(name = "NgayNhan")
    private LocalDateTime ngayNhan;

    @Column(name = "TrangThaiSuDung")
    private Integer trangThaiSuDung;
}