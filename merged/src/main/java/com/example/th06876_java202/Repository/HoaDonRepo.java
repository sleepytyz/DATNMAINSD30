package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.HoaDon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Repository
public interface HoaDonRepo extends JpaRepository<HoaDon, String> {

    Page<HoaDon> findByTrangThaiIn(List<String> trangThaiList, Pageable pageable);

    List<HoaDon> findByTrangThaiIn(List<String> trangThaiList);

    Page<HoaDon> findByNgayTaoBetweenAndTrangThaiIn(LocalDateTime tuNgay, LocalDateTime denNgay,
                                                    List<String> trangThaiList, Pageable pageable);

    List<HoaDon> findByNgayTaoBetweenAndTrangThaiIn(LocalDateTime tuNgay, LocalDateTime denNgay,
                                                    List<String> trangThaiList);

    Page<HoaDon> findByNgayTaoAfterAndTrangThaiIn(LocalDateTime ngay, List<String> trangThaiList, Pageable pageable);

    List<HoaDon> findByNgayTaoAfterAndTrangThaiIn(LocalDateTime ngay, List<String> trangThaiList);

    Page<HoaDon> findByNgayTaoBeforeAndTrangThaiIn(LocalDateTime ngay, List<String> trangThaiList, Pageable pageable);

    List<HoaDon> findByNgayTaoBeforeAndTrangThaiIn(LocalDateTime ngay, List<String> trangThaiList);

    Page<HoaDon> findByMaHoaDonAndTrangThaiIn(String maHoaDon, List<String> trangThaiList, Pageable pageable);

    Page<HoaDon> findByTrangThai(String trangThai, Pageable pageable);

    List<HoaDon> findByTrangThai(String trangThai);

    long countByTrangThai(String trangThai);


    Page<HoaDon> findAll(Pageable pageable);

    @Query(value = "select * from HoaDon where TrangThai = N'Yêu cầu huỷ'", nativeQuery = true)
    List<HoaDon> findByTrangThai();

    @Query(value = """
            SELECT 
                CAST(h.NgayTao AS DATE) as ngay,
                COUNT(h.MaHoaDon) as soDonHang,
                ISNULL(SUM(h.TongTien), 0) as doanhThu,
                ISNULL(AVG(h.TongTien), 0) as trungBinhDon
            FROM HoaDon h
            WHERE h.TrangThai IN (N'Đã thanh toán', N'Đã giao')
                AND h.NgayTao BETWEEN ?1 AND ?2
            GROUP BY CAST(h.NgayTao AS DATE)
            ORDER BY CAST(h.NgayTao AS DATE) DESC
            """, nativeQuery = true)
    List<Object[]> thongKeDoanhThuTheoNgay(LocalDateTime startDate, LocalDateTime endDate);

    @Query(value = """
            SELECT 
                YEAR(h.NgayTao) as nam,
                MONTH(h.NgayTao) as thang,
                COUNT(h.MaHoaDon) as soDonHang,
                ISNULL(SUM(h.TongTien), 0) as doanhThu
            FROM HoaDon h
            WHERE h.TrangThai IN (N'Đã thanh toán', N'Đã giao')
                AND h.NgayTao BETWEEN ?1 AND ?2
            GROUP BY YEAR(h.NgayTao), MONTH(h.NgayTao)
            ORDER BY YEAR(h.NgayTao) DESC, MONTH(h.NgayTao) DESC
            """, nativeQuery = true)
    List<Object[]> thongKeDoanhThuTheoThang(LocalDateTime startDate, LocalDateTime endDate);

    @Modifying
    @Transactional
    @Query(value = "update HoaDon set TrangThai = N'Đã huỷ' where MaHoaDon = ?", nativeQuery = true)
    int huy(String mahd);


    Page<HoaDon> findByMaKhachHang_MaKHOrderByMaHoaDonDesc(String maKH, Pageable pageable);

    @Query(value = """
            SELECT 
                ISNULL(COUNT(h.MaHoaDon), 0) as tongDonHang,
                ISNULL(SUM(h.TongTien), 0) as tongDoanhThu,
                ISNULL(AVG(h.TongTien), 0) as trungBinhDon,
                MIN(h.NgayTao) as ngayDau,
                MAX(h.NgayTao) as ngayCuoi
            FROM HoaDon h
            WHERE h.TrangThai IN (N'Đã thanh toán', N'Đã giao')
                AND h.NgayTao BETWEEN ?1 AND ?2
            """, nativeQuery = true)
    List<Object[]> thongKeTongQuan(LocalDateTime startDate, LocalDateTime endDate);

    @Query(value = """
            SELECT 
                h.MaNhanVien as maNhanVien,
                nv.HoTen as hoTen,
                COUNT(h.MaHoaDon) as soDonHang,
                ISNULL(SUM(h.TongTien), 0) as doanhThu
            FROM HoaDon h
            JOIN NhanVien nv ON nv.MaNhanVien = h.MaNhanVien
            WHERE h.TrangThai IN (N'Đã thanh toán', N'Đã giao')
                AND h.NgayTao BETWEEN ?1 AND ?2
                AND h.MaNhanVien IS NOT NULL
            GROUP BY h.MaNhanVien, nv.HoTen
            ORDER BY doanhThu DESC
            """, nativeQuery = true)
    List<Object[]> thongKeHieuSuatBanHangTheoNhanVien(LocalDateTime startDate, LocalDateTime endDate);

    @Query(value = """
            SELECT 
                CAST(h.NgayTao AS DATE) as ngay,
                COUNT(h.MaHoaDon) as soDonHang,
                ISNULL(SUM(h.TongTien), 0) as doanhThu,
                ISNULL(AVG(h.TongTien), 0) as trungBinhDon
            FROM HoaDon h
            WHERE h.TrangThai IN (N'Đã thanh toán', N'Đã giao')
                AND h.MaNhanVien = ?1
                AND h.NgayTao BETWEEN ?2 AND ?3
            GROUP BY CAST(h.NgayTao AS DATE)
            ORDER BY CAST(h.NgayTao AS DATE) DESC
            """, nativeQuery = true)
    List<Object[]> thongKeDoanhThuCaNhanTheoNgay(String maNhanVien, LocalDateTime startDate, LocalDateTime endDate);

    @Query(value = """
            SELECT 
                ISNULL(COUNT(h.MaHoaDon), 0) as tongDonHang,
                ISNULL(SUM(h.TongTien), 0) as tongDoanhThu,
                ISNULL(AVG(h.TongTien), 0) as trungBinhDon,
                MIN(h.NgayTao) as ngayDau,
                MAX(h.NgayTao) as ngayCuoi
            FROM HoaDon h
            WHERE h.TrangThai IN (N'Đã thanh toán', N'Đã giao')
                AND h.MaNhanVien = ?1
                AND h.NgayTao BETWEEN ?2 AND ?3
            """, nativeQuery = true)
    List<Object[]> thongKeTongQuanCaNhan(String maNhanVien, LocalDateTime startDate, LocalDateTime endDate);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT h FROM HoaDon h WHERE h.maHoaDon = :ma")
    java.util.Optional<HoaDon> khoaDonDeXuLy(
            @org.springframework.data.repository.query.Param("ma") String ma);
    // ==================== [MERGE - GIAO CA / QUY TIEN] ====================
    @Query("SELECT COALESCE(SUM(h.tongTien), 0) FROM HoaDon h " +
            "WHERE h.maNhanVien.maNhanVien = :maNhanVien " +
            "AND h.ngayTao BETWEEN :tuLuc AND :denLuc " +
            "AND h.trangThai IN ('Đã thanh toán', 'Đã giao') " +
            "AND h.phuongThucThanhToan = :phuongThuc")
    BigDecimal tongTienTheoPhuongThucTrongCa(@Param("maNhanVien") String maNhanVien,
                                             @Param("tuLuc") LocalDateTime tuLuc,
                                             @Param("denLuc") LocalDateTime denLuc,
                                             @Param("phuongThuc") String phuongThuc);

    @Query("SELECT COALESCE(SUM(h.tongTien), 0) FROM HoaDon h " +
            "WHERE h.maNhanVien.maNhanVien = :maNhanVien " +
            "AND h.ngayTao BETWEEN :tuLuc AND :denLuc " +
            "AND h.trangThai IN ('Đã thanh toán', 'Đã giao') " +
            "AND (h.phuongThucThanhToan IS NULL OR h.phuongThucThanhToan <> 'Tiền mặt')")
    BigDecimal tongTienKhongTienMatTrongCa(@Param("maNhanVien") String maNhanVien,
                                           @Param("tuLuc") LocalDateTime tuLuc,
                                           @Param("denLuc") LocalDateTime denLuc);

    @Query("SELECT COUNT(h) FROM HoaDon h " +
            "WHERE h.maNhanVien.maNhanVien = :maNhanVien " +
            "AND h.ngayTao BETWEEN :tuLuc AND :denLuc " +
            "AND h.trangThai IN ('Đã thanh toán', 'Đã giao')")
    long demHoaDonTrongCa(@Param("maNhanVien") String maNhanVien,
                          @Param("tuLuc") LocalDateTime tuLuc,
                          @Param("denLuc") LocalDateTime denLuc);

    @Query("SELECT h FROM HoaDon h " +
            "WHERE h.maNhanVien.maNhanVien = :maNhanVien " +
            "AND h.ngayTao BETWEEN :tuLuc AND :denLuc " +
            "AND h.trangThai IN ('Đã thanh toán', 'Đã giao') " +
            "ORDER BY h.ngayTao DESC")
    List<HoaDon> danhSachHoaDonTrongCa(@Param("maNhanVien") String maNhanVien,
                                       @Param("tuLuc") LocalDateTime tuLuc,
                                       @Param("denLuc") LocalDateTime denLuc);

    // ==================== [MERGE - THONG KE] ====================
    @Query("SELECT COALESCE(SUM(h.tongTien), 0) FROM HoaDon h " +
            "WHERE h.ngayTao BETWEEN :tuNgay AND :denNgay " +
            "AND h.trangThai IN ('Đã thanh toán', 'Đã giao')")
    BigDecimal doanhThuThucTe(@Param("tuNgay") LocalDateTime tuNgay,
                              @Param("denNgay") LocalDateTime denNgay);

    @Query("SELECT COALESCE(SUM(h.tongTien), 0) FROM HoaDon h " +
            "WHERE h.ngayTao BETWEEN :tuNgay AND :denNgay " +
            "AND h.trangThai IN ('Chờ xác nhận', 'Đã xác nhận', 'Đang giao', 'Đang xử lý')")
    BigDecimal doanhThuDuKien(@Param("tuNgay") LocalDateTime tuNgay,
                              @Param("denNgay") LocalDateTime denNgay);

    @Query("SELECT h.trangThai, COUNT(h), COALESCE(SUM(h.tongTien), 0) FROM HoaDon h " +
            "WHERE h.ngayTao BETWEEN :tuNgay AND :denNgay " +
            "GROUP BY h.trangThai ORDER BY COUNT(h) DESC")
    List<Object[]> phanBoTrangThaiDonHang(@Param("tuNgay") LocalDateTime tuNgay,
                                          @Param("denNgay") LocalDateTime denNgay);
}
