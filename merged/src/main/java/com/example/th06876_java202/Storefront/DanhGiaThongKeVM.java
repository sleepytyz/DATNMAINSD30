package com.example.th06876_java202.Storefront;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Thống kê đánh giá của 1 sản phẩm: điểm trung bình + phân bố 1..5 sao (cho trang chi tiết). */
@Data
@NoArgsConstructor
public class DanhGiaThongKeVM {
    private double diemTrungBinh;
    private long tongLuot;
    /** soLuot[i] = số lượt đánh giá (i+1) sao, i = 0..4. */
    private long[] soLuot = new long[5];
    /** phanTram[i] = % lượt (i+1) sao trên tổng, i = 0..4. */
    private int[] phanTram = new int[5];

    public long getSao1() { return soLuot[0]; }
    public long getSao2() { return soLuot[1]; }
    public long getSao3() { return soLuot[2]; }
    public long getSao4() { return soLuot[3]; }
    public long getSao5() { return soLuot[4]; }
    public int getPt1() { return phanTram[0]; }
    public int getPt2() { return phanTram[1]; }
    public int getPt3() { return phanTram[2]; }
    public int getPt4() { return phanTram[3]; }
    public int getPt5() { return phanTram[4]; }
}
