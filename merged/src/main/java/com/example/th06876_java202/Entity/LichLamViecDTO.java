package com.example.th06876_java202.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LichLamViecDTO {
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private List<LichNhanVienDTO> danhSachNhanVien;
}