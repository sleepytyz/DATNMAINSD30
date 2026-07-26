package com.example.th06876_java202.Storefront;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Form đổi mật khẩu trong trang tài khoản cá nhân. */
@Data
public class DoiMatKhauDTO {

    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
    private String matKhauCu;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Size(min = 6, max = 100, message = "Mật khẩu mới tối thiểu 6 ký tự")
    private String matKhauMoi;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu mới")
    private String xnMatKhauMoi;
}
