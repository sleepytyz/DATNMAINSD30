package com.example.th06876_java202.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThongKeTongQuanDTO {
    private Integer tongDonHang = 0;
    private BigDecimal tongDoanhThu = BigDecimal.ZERO;
    private BigDecimal trungBinhDon = BigDecimal.ZERO;
    private LocalDate ngayDau;
    private LocalDate ngayCuoi;
}