package com.example.th06876_java202.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamBanChayDTO {
    private String tenSanPham;
    private Long soLuongBan;
}
