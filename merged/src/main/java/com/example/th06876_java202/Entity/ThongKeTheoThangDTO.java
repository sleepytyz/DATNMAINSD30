package com.example.th06876_java202.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThongKeTheoThangDTO {
    private Integer nam;
    private Integer thang;
    private Integer soDonHang;
    private BigDecimal doanhThu;
}
