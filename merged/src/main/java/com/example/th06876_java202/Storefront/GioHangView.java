package com.example.th06876_java202.Storefront;

import com.example.th06876_java202.Entity.GiamGia;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Toàn cảnh giỏ hàng đã tính toán: dòng hàng, khuyến mãi, voucher, phí ship, tổng thanh toán. */
@Data
@NoArgsConstructor
public class GioHangView {
    private List<CartLineVM> dongHang = new ArrayList<>();
    private List<String> canhBao = new ArrayList<>();
    private BigDecimal tongTienHang = BigDecimal.ZERO;
    private BigDecimal tietKiemKhuyenMai = BigDecimal.ZERO;
    private GiamGia voucherApDung;
    private String tenVoucher;
    private BigDecimal soTienGiamVoucher = BigDecimal.ZERO;
    private BigDecimal tienShip = BigDecimal.ZERO;
    private BigDecimal conThieuDeFreeship = BigDecimal.ZERO;
    private BigDecimal phiShipGoc = BigDecimal.ZERO;
    private String nguonPhiShip = "CO_DINH";
    private BigDecimal tongThanhToan = BigDecimal.ZERO;
    private int tongSoLuong;

    public String getTenVoucher() {
        if (tenVoucher != null && !tenVoucher.isBlank()) {
            return tenVoucher;
        }
        if (voucherApDung != null) {
            return voucherApDung.getTenGiamGia();
        }
        return null;
    }
}