package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Tin nhắn khách gửi từ trang "Liên hệ" của website bán hàng. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "LienHe")
public class LienHe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaLienHe")
    private Integer maLienHe;

    @Column(name = "HoTen", length = 100, nullable = false)
    private String hoTen;

    @Column(name = "Email", length = 150, nullable = false)
    private String email;

    @Column(name = "NoiDung", length = 2000, nullable = false)
    private String noiDung;

    @Column(name = "ThoiGian", nullable = false)
    private LocalDateTime thoiGian;

    /** 'Chưa xử lý' | 'Đã xử lý' */
    @Column(name = "TrangThai", length = 20, nullable = false)
    private String trangThai = "Chưa xử lý";
}
