package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "KieuGiay")
public class KieuGiay {

    @Id
    @Column(name = "MaKieuGiay")
    private String maKieuGiay;

    @NotBlank(message = "Không bỏ trống kiểu giày")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Kiểu giày chỉ chứa chữ cái, khoảng trắng ")
    @Column(name = "TenKieuGiay")
    private String tenKieuGiay;

    @Column(name = "TrangThai")
    private boolean trangThai;

    @Column(name = "NgayTao", updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
    }
}