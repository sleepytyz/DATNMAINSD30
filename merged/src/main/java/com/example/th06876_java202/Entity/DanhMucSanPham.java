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
@Table(name = "DanhMucSanPham")
public class DanhMucSanPham {

    @Id
    @Column(name = "MaDanhMuc")
    private String maDanhMuc;

    @NotBlank(message = "Tên danh mục không để trống")
    @Pattern(regexp = "^[\\p{L}\\s]*$", message = "Tên danh mục chỉ được chứa chữ cái và khoảng trắng")
    @Column(name = "TenDanhMuc")
    private String tenDanhMuc;

    @Column(name = "MoTa")
    private String moTa;

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