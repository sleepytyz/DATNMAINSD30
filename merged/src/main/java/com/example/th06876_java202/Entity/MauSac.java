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
@Table(name = "MauSac")
public class MauSac {

    @Id
    @Column(name = "MaMauSac")
    private String maMauSac;

    @NotBlank(message = "Không bỏ trống màu sắc")
    @Pattern(
            regexp = "^[\\p{L}\\s]+$",
            message = "Màu sắc chỉ chứa chữ cái, khoảng trắng "
    )
    @Column(name = "TenMauSac")
    private String tenMauSac;

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