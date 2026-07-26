package com.example.th06876_java202.config;

import com.example.th06876_java202.Entity.TaiKhoan;
import com.example.th06876_java202.Service.TaiKhoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final TaiKhoanService taiKhoanService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        TaiKhoan taiKhoan = taiKhoanService.findUserByTenDangNhap(username); // Hoặc qua repo tùy code bạn
        if (taiKhoan == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return new CustomUserDetails(taiKhoan);
    }
}
