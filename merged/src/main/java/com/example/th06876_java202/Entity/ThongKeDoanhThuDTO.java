package com.example.th06876_java202.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThongKeDoanhThuDTO {
    private LocalDate ngay;
    private Integer soDonHang;
    private BigDecimal doanhThu;
    private BigDecimal trungBinhDon;
}
