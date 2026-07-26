package com.example.th06876_java202.Storefront;

/** Lỗi nghiệp vụ khi đặt hàng online (hết hàng, giỏ trống, địa chỉ thiếu...). */
public class DatHangException extends RuntimeException {
    public DatHangException(String message) {
        super(message);
    }
}