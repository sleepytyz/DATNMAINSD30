package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Một tin nhắn trong kênh Chatbot & Hỗ trợ trực tuyến.
 * MaPhien: 1 phiên = 1 cuộc hội thoại — "KH-<mã KH>" với khách đã đăng nhập,
 * "GUEST-xxxxxxxx" với khách vãng lai (gắn theo HttpSession).
 * NguoiGui: KHACH | BOT | NHANVIEN.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "HoTroTinNhan")
public class HoTroTinNhan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTinNhan")
    private Integer maTinNhan;

    @Column(name = "MaPhien", length = 50, nullable = false)
    private String maPhien;

    @Column(name = "MaKhachHang", length = 20)
    private String maKhachHang;

    @Column(name = "TenHienThi", length = 100)
    private String tenHienThi;

    @Column(name = "NguoiGui", length = 10, nullable = false)
    private String nguoiGui;

    @Column(name = "NoiDung", length = 2000, nullable = false)
    private String noiDung;

    @Column(name = "ThoiGian", nullable = false)
    private LocalDateTime thoiGian;

    @Column(name = "DaXem", nullable = false)
    private Boolean daXem = false;
}
