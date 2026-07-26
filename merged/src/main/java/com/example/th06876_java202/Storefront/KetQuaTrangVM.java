package com.example.th06876_java202.Storefront;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Kết quả phân trang thủ công cho trang danh sách sản phẩm (sau khi lọc/sắp xếp trong bộ nhớ). */
@Data
@NoArgsConstructor
public class KetQuaTrangVM {
    private List<SanPhamCardVM> noiDung = new ArrayList<>();
    private int trangHienTai;   // 0-based
    private int tongTrang;
    private long tongPhanTu;
    private int kichThuoc;

    public boolean isCoTrangTruoc() { return trangHienTai > 0; }
    public boolean isCoTrangSau()   { return trangHienTai < tongTrang - 1; }
    public boolean isRong()         { return noiDung == null || noiDung.isEmpty(); }
    public int getTuPhanTu()        { return tongPhanTu == 0 ? 0 : trangHienTai * kichThuoc + 1; }
    public long getDenPhanTu()      { return Math.min((long) (trangHienTai + 1) * kichThuoc, tongPhanTu); }
}