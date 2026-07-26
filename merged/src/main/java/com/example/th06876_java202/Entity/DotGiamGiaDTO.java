package com.example.th06876_java202.Entity;

import jakarta.validation.Valid;
import lombok.Data;
import java.util.List;

@Data
public class DotGiamGiaDTO {

        @Valid
        private DotGiamGia dotGiamGia = new DotGiamGia();

        private List<String > listMaSanPham;

        private List<String> listMaSanPhamChiTiet;
}