package com.example.th06876_java202.Service;


import com.example.th06876_java202.Entity.HoaDon;
import com.example.th06876_java202.Entity.HoaDonChiTiet;
import com.example.th06876_java202.Entity.SanPhamChiTiet;
import com.example.th06876_java202.Repository.HoaDonChiTietRepository;
import com.example.th06876_java202.Repository.SanPhamChiTietRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class HoaDonChiTietService {

    @Autowired
    SanPhamChiTietRepository sanPhamChiTietRepository;

    private final HoaDonChiTietRepository hoaDonChiTietRepository;

    public HoaDonChiTietService(HoaDonChiTietRepository hoaDonChiTietRepository) {
        this.hoaDonChiTietRepository = hoaDonChiTietRepository;
    }

    public void xoa(Integer id) {
        hoaDonChiTietRepository.deleteById(id);
    }

    public HoaDonChiTiet findAll(String maHoaDon, String maSanPhamChiTiet) {
        return hoaDonChiTietRepository.findByMaHoaDon_MaHoaDonAndSanPhamChiTiet_MaSanPhamChiTiet(
                maHoaDon, maSanPhamChiTiet
        );
    }

    public HoaDonChiTiet luu(HoaDonChiTiet hoaDonChiTiet) {
        return hoaDonChiTietRepository.save(hoaDonChiTiet);
    }

    public List<HoaDonChiTiet> luuTatCa(List<HoaDonChiTiet> list) {
        return hoaDonChiTietRepository.saveAll(list);
    }

    public List<HoaDonChiTiet> findById(String id) {
        return hoaDonChiTietRepository.getallsphd(id);
    }

    public void xoa(HoaDonChiTiet hdct) {
        hoaDonChiTietRepository.delete(hdct);
    }

    public List<HoaDonChiTiet> findByHoaDOn(HoaDon hoaDon) {
        return hoaDonChiTietRepository.findByMaHoaDon(hoaDon);
    }

    @Transactional
    public void capNhatGiaSanPham(String maHoaDon, String maSPCT) {
        HoaDonChiTiet hdct = hoaDonChiTietRepository.findByMaHoaDonAndMaSanPhamChiTiet(maHoaDon, maSPCT);

        // Cách 2: Nếu method trên chưa có, dùng cách này
        // HoaDonChiTiet hdct = hoaDonChiTietRepository.findByMaHoaDonAndMaSanPhamChiTiet(maHoaDon, maSPCT);

        if (hdct == null) {
            throw new RuntimeException("Không tìm thấy sản phẩm trong hóa đơn!");
        }

        // Tìm sản phẩm chi tiết để lấy giá mới
        SanPhamChiTiet sp = sanPhamChiTietRepository.findById(maSPCT)  // ← DÙNG SANPHAM REPO
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong database!"));

        // Cập nhật giá mới
        BigDecimal giaMoi = sp.getGiaBan();  // Hoặc sp.getGiaSauGiam() nếu có giảm giá
        int soLuong = hdct.getSoLuong();
        BigDecimal thanhTienMoi = giaMoi.multiply(BigDecimal.valueOf(soLuong));  // ← SỬA ĐÚNG

        hdct.setDonGia(giaMoi);
        hdct.setThanhTien(thanhTienMoi);

        // Lưu lại
        hoaDonChiTietRepository.save(hdct);
    }

    @Transactional
    public void capNhatGiaTatCa(String maHoaDon, List<String> maSPCTs) {
        if (maSPCTs == null || maSPCTs.isEmpty()) {
            throw new RuntimeException("Danh sách sản phẩm trống!");
        }

        for (String maSPCT : maSPCTs) {
            capNhatGiaSanPham(maHoaDon, maSPCT);
        }
    }

    /**
     * Lấy danh sách sản phẩm trong hóa đơn với giá mới nhất
     */
    public List<Object[]> getCartItemsWithLatestPrice(String maHoaDon) {
        return hoaDonChiTietRepository.findCartItemsWithLatestPrice(maHoaDon);
    }

}
