package com.example.th06876_java202.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhieuNhapHangDTO {
    private Integer maPhieuNhap;
    private String tenNhaCungCap;
    private LocalDateTime ngayNhap;
    private String trangThai;
}
