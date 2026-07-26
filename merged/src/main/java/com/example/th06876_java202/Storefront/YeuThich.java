package com.example.th06876_java202.Storefront;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Danh sách SẢN PHẨM YÊU THÍCH theo phiên — tính năng bổ sung của website bán hàng.
 * Chỉ lưu mã sản phẩm; thông tin hiển thị được nạp lại từ CSDL.
 */
@Component
@SessionScope
public class YeuThich implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Set<String> maSanPhams = new LinkedHashSet<>();

    /** Bật/tắt yêu thích. Trả về true nếu sau thao tác sản phẩm ĐANG được yêu thích. */
    public synchronized boolean daoTrangThai(String maSanPham) {
        if (maSanPham == null || maSanPham.isBlank()) return false;
        if (maSanPhams.contains(maSanPham)) {
            maSanPhams.remove(maSanPham);
            return false;
        }
        maSanPhams.add(maSanPham);
        return true;
    }

    public boolean chua(String maSanPham) {
        return maSanPham != null && maSanPhams.contains(maSanPham);
    }

    public Set<String> getMaSanPhams() {
        return maSanPhams;
    }

    public int soLuong() {
        return maSanPhams.size();
    }
}