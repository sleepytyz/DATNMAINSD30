package com.example.th06876_java202.config;

import com.example.th06876_java202.Entity.DanhMucSanPham;
import com.example.th06876_java202.Service.DanhMucSanPhamService;
import com.example.th06876_java202.Storefront.GioHang;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GioHangGlobalAdvice {

    private final GioHang gioHang;
    private final DanhMucSanPhamService danhMucSanPhamService;

    @ModelAttribute("gioHangSoLuong")
    public int gioHangSoLuong() {
        try {
            return gioHang.tongSoLuong();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Danh mục đang hoạt động dùng cho menu "Danh mục" ở đầu trang (mọi trang). */
    @ModelAttribute("danhMucMenu")
    public List<DanhMucSanPham> danhMucMenu() {
        try {
            return danhMucSanPhamService.getAll().stream()
                    .filter(DanhMucSanPham::isTrangThai)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }
}
