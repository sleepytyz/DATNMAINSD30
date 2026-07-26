package com.example.th06876_java202.Storefront;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


@Component
@SessionScope
public class GioHang implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Map<String, GioHangItem> danhSach = new LinkedHashMap<>();
    private String maGiamGiaApDung;

    public synchronized void themSanPham(String maSPCT, int soLuong) {
        if (maSPCT == null || soLuong <= 0) return;
        GioHangItem item = danhSach.get(maSPCT);
        if (item == null) {
            danhSach.put(maSPCT, new GioHangItem(maSPCT, soLuong));
        } else {
            item.setSoLuong(item.getSoLuong() + soLuong);
        }
    }

    public synchronized void capNhatSoLuong(String maSPCT, int soLuong) {
        if (maSPCT == null) return;
        if (soLuong <= 0) {
            danhSach.remove(maSPCT);
            return;
        }
        GioHangItem item = danhSach.get(maSPCT);
        if (item != null) {
            item.setSoLuong(soLuong);
        }
    }

    public synchronized void xoaSanPham(String maSPCT) {
        danhSach.remove(maSPCT);
    }

    public synchronized void xoaTatCa() {
        danhSach.clear();
        maGiamGiaApDung = null;
    }

    public Map<String, GioHangItem> getDanhSach() {
        return danhSach;
    }

    public int tongSoLuong() {
        return danhSach.values().stream().mapToInt(GioHangItem::getSoLuong).sum();
    }

    public boolean isEmpty() {
        return danhSach.isEmpty();
    }

    public String getMaGiamGiaApDung() {
        return maGiamGiaApDung;
    }

    public void setMaGiamGiaApDung(String maGiamGiaApDung) {
        this.maGiamGiaApDung = maGiamGiaApDung;
    }
}