package com.example.th06876_java202.config;

import com.example.th06876_java202.Entity.KhachHang;
import com.example.th06876_java202.Service.KhachHangService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Cung cấp tên hiển thị của khách hàng đang đăng nhập cho mọi trang (dùng ở header).
 * Nếu khách hàng chưa cập nhật họ tên thì hiển thị tạm tên đăng nhập.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class TaiKhoanGlobalAdvice {

    private final KhachHangService khachHangService;

    @ModelAttribute("tenHienThiHeader")
    public String tenHienThiHeader(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            KhachHang khachHang = khachHangService.findByTenDangNhap(authentication.getName());
            if (khachHang != null && khachHang.getHoTen() != null && !khachHang.getHoTen().isBlank()) {
                return khachHang.getHoTen();
            }
            return authentication.getName();
        } catch (Exception e) {
            return null;
        }
    }
}