package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.HoaDon;
import com.example.th06876_java202.Entity.HoaDonChiTiet;
import com.example.th06876_java202.Entity.SanPhamBanChayDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Integer> {

    @Query("SELECT hdct FROM HoaDonChiTiet hdct " +
            "WHERE hdct.maHoaDon.maHoaDon = :maHoaDon " +
            "AND hdct.sanPhamChiTiet.maSanPhamChiTiet = :maSPCT")
    HoaDonChiTiet getallsphd(
            @Param("maHoaDon") String maHoaDon,
            @Param("maSPCT") String maSanPhamChiTiet
    );

    @Query("SELECT hdct FROM HoaDonChiTiet hdct " +
            "WHERE hdct.maHoaDon.maHoaDon = :maHoaDon")
    List<HoaDonChiTiet> getallsphd(@Param("maHoaDon") String maHoaDon);

    @Query("SELECT hdct FROM HoaDonChiTiet hdct " +
            "WHERE hdct.maHoaDon = :hoaDon")
    List<HoaDonChiTiet> findByMaHoaDon(@Param("hoaDon") HoaDon hoaDon);

    List<HoaDonChiTiet> findByMaHoaDon_MaHoaDon(String maHoaDon);

    @Query("SELECT hdct FROM HoaDonChiTiet hdct " +
            "WHERE hdct.maHoaDon.maHoaDon = :maHoaDon " +
            "AND hdct.sanPhamChiTiet.maSanPhamChiTiet = :maSPCT")
    HoaDonChiTiet findByMaHoaDonAndMaSanPhamChiTiet(
            @Param("maHoaDon") String maHoaDon,
            @Param("maSPCT") String maSPCT
    );

    @Query("SELECT SUM(hct.soLuong) FROM HoaDonChiTiet hct " +
            "WHERE hct.sanPhamChiTiet.maSanPhamChiTiet = :maSpct")
    Integer tongSoLuongDaBan(@Param("maSpct") String maSpct);

    // Lấy sản phẩm với giá mới nhất từ database
    @Query("SELECT hdct.id, hdct.maHoaDon.maHoaDon, " +
            "hdct.sanPhamChiTiet.maSanPhamChiTiet, " +
            "hdct.soLuong, hdct.donGia, " +
            "spct.giaBan, spct.soLuongTon " +
            "FROM HoaDonChiTiet hdct " +
            "JOIN hdct.sanPhamChiTiet spct " +
            "WHERE hdct.maHoaDon.maHoaDon = :maHoaDon")
    List<Object[]> findCartItemsWithLatestPrice(@Param("maHoaDon") String maHoaDon);

    // Method này đã có thể dùng
    HoaDonChiTiet findByMaHoaDon_MaHoaDonAndSanPhamChiTiet_MaSanPhamChiTiet(
            String maHoaDon,
            String maSanPhamChiTiet
    );

    // ================= Website bán hàng online =================

    /**
     * Thống kê số lượng ĐÃ BÁN theo sản phẩm (chỉ tính đơn đã hoàn tất:
     * "Đã giao" cho online và "Đã thanh toán" cho bán tại quầy).
     * Trả về [maSanPham, tongSoLuongDaBan] — dùng cho mục "Bán chạy" & sắp xếp.
     */
    @Query("SELECT ct.sanPhamChiTiet.sanPham.maSanPham, SUM(ct.soLuong) " +
            "FROM HoaDonChiTiet ct " +
            "WHERE ct.maHoaDon.trangThai IN ('Đã giao', 'Đã thanh toán') " +
            "GROUP BY ct.sanPhamChiTiet.sanPham.maSanPham")
    List<Object[]> thongKeSoLuongDaBanTheoSanPham();



    // [THÊM - MERGE thống kê] Top sản phẩm bán chạy trong khoảng thời gian, chỉ tính đơn đã
    // thu tiền thật (Đã thanh toán tại quầy / Đã giao online) - dùng cho dashboard trang chủ Admin.
    @Query("SELECT new com.example.th06876_java202.Entity.SanPhamBanChayDTO(sp.tenSanPham, SUM(hdct.soLuong)) " +
            "FROM HoaDonChiTiet hdct " +
            "JOIN hdct.sanPhamChiTiet spct " +
            "JOIN spct.sanPham sp " +
            "JOIN hdct.maHoaDon hd " +
            "WHERE hd.trangThai IN ('Đã thanh toán', 'Đã giao') " +
            "AND hd.ngayTao BETWEEN :tuNgay AND :denNgay " +
            "GROUP BY sp.maSanPham, sp.tenSanPham " +
            "ORDER BY SUM(hdct.soLuong) DESC")
    List<SanPhamBanChayDTO> topSanPhamBanChay(@Param("tuNgay") java.time.LocalDateTime tuNgay,
                                              @Param("denNgay") java.time.LocalDateTime denNgay,
                                              org.springframework.data.domain.Pageable pageable);


}