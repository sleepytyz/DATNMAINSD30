package com.example.th06876_java202.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
// Trong VoucherDTO.java - Thêm trường

public class VoucherDTO {
    private String maGiamGia;
    private String tenGiamGia;
    private String loaiGiamGia;
    private BigDecimal giaTriGiam;
    private BigDecimal donToiThieu;
    private BigDecimal giamToiDa;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ngayBatDau;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ngayKetThuc;
    private Integer loaiApDung;
    private Integer trangThaiSuDung;
    private Boolean daSuDung;
}