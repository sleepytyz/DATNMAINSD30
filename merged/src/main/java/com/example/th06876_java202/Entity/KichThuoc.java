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
@Table(name = "KichThuoc")
public class KichThuoc {

    @Id
    @Column(name = "MaKichThuoc")
    private String maKichThuoc;

    @NotBlank(message = "Không bỏ trống kích thước")
    @Pattern(
            regexp = "^\\d+(\\.\\d+)?$",
            message = "Kích thước chỉ chứa số hoặc số thập phân"
    )
    @Column(name = "TenKichThuoc")
    private String tenKichThuoc;

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