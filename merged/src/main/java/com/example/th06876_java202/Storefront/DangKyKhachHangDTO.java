package com.example.th06876_java202.Storefront;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Dữ liệu form đăng ký tài khoản khách hàng.
 * Đăng ký chỉ yêu cầu: tên đăng nhập, mật khẩu, xác nhận mật khẩu.
 * Các thông tin cá nhân khác (họ tên, SĐT, email...) khách hàng sẽ bổ sung sau
 * trong trang "Tài khoản của tôi" sau khi đăng nhập.
 */
@Data
public class DangKyKhachHangDTO {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 4, max = 50, message = "Tên đăng nhập phải từ 4 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "Tên đăng nhập chỉ gồm chữ, số, gạch dưới, không dấu cách")
    private String tenDangNhap;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 6, max = 100, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String matKhau;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String xnMatKhau;
}
