package com.example.th06876_java202.config;

import com.example.th06876_java202.Entity.TaiKhoan;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.Collections;

public class CustomUserDetails extends User {

    private final TaiKhoan taiKhoan;
    private final String hoTenNhanVien;

    public CustomUserDetails(TaiKhoan taiKhoan) {
        super(taiKhoan.getTenDangNhap(),
                taiKhoan.getMatKhau(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + taiKhoan.getVaiTro())));

        this.taiKhoan = taiKhoan;
        this.hoTenNhanVien = (taiKhoan.getNhanVien() != null) ? taiKhoan.getNhanVien().getHoTen() : "Chưa gán NV";
    }

    public TaiKhoan getTaiKhoan() {
        return this.taiKhoan;
    }

    public String getHoTenNhanVien() {
        return hoTenNhanVien;
    }
}