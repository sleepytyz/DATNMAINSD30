package com.example.th06876_java202.Entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherEmailDTO {
    private String maGiamGia;
    private String tenGiamGia;
    private String loaiGiamGia;
    private BigDecimal giaTri;
    private BigDecimal donToiThieu;
    private BigDecimal giamToiDa;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private String loaiApDung;
}
