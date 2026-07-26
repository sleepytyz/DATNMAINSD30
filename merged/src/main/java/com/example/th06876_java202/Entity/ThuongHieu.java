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
@Table(name = "ThuongHieu")
public class ThuongHieu {

    @Id
    @Column(name = "MaThuongHieu")
    private String maThuongHieu;

    @NotBlank(message = "Không bỏ trống thương hiệu")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Thương hiệu chỉ chứa chữ cái, khoảng trắng ")
    @Column(name = "TenThuongHieu")
    private String tenThuongHieu;

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