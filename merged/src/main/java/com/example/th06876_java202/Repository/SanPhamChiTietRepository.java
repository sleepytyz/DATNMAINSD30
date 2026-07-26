package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.SanPhamChiTiet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, String> {

    @Modifying
    @Transactional
    @Query(value = "update SanPhamChiTiet set TrangThai = N'Ngừng bán' where MaSanPham = ?", nativeQuery = true)
    int updateTrangThai(String maSanPham);

    @Modifying
    @Transactional
    @Query(value = "update SanPhamChiTiet set TrangThai = :trangThai where MaSanPhamChiTiet = :id", nativeQuery = true)
    int updateTrangThaii(@Param("id") String id, @Param("trangThai") String trangThai);

    @Modifying
    @Transactional
    @Query(value = "update SanPhamChiTiet set TrangThai = case when SoLuongTon = 0 then N'Hết hàng' when SoLuongTon < 10 then N'Sắp hết' else N'Còn hàng' end where MaSanPham = ?", nativeQuery = true)
    int updateTrangThaiii(String maSanPham);

    List<SanPhamChiTiet> findBySanPham_MaSanPhamAndMauSac_TenMauSac(
            String maSP,
            String tenMauSac);

    @Query("SELECT s FROM SanPhamChiTiet s ORDER BY s.ngayTao DESC")
    Page<SanPhamChiTiet> findAll(Pageable pageable);

    @Query("SELECT s FROM SanPhamChiTiet s WHERE s.mauSac.maMauSac = :maMauSac ORDER BY s.ngayTao DESC")
    Page<SanPhamChiTiet> findByMauSac_MaMauSac(@Param("maMauSac") String maMauSac, Pageable pageable);

    @Query("SELECT s FROM SanPhamChiTiet s WHERE s.kichThuoc.maKichThuoc = :maKichThuoc ORDER BY s.ngayTao DESC")
    Page<SanPhamChiTiet> findByKichThuoc_MaKichThuoc(@Param("maKichThuoc") String maKichThuoc, Pageable pageable);

    @Query(value = "select distinct ms.TenMauSac from SanPhamChiTiet spct inner join MauSac ms on spct.MaMauSac = ms.MaMauSac order by ms.TenMauSac", nativeQuery = true)
    List<String> findAllMauSac();

    @Query(value = "select distinct kt.TenKichThuoc from SanPhamChiTiet spct inner join KichThuoc kt on spct.MaKichThuoc = kt.MaKichThuoc order by kt.TenKichThuoc", nativeQuery = true)
    List<String> findAllSize();

    @Query("SELECT s FROM SanPhamChiTiet s WHERE s.trangThai = :tt ORDER BY s.ngayTao DESC")
    Page<SanPhamChiTiet> locTheoTrangThaiHienThi(@Param("tt") String tt, Pageable pageable);

    @Query(value = "SELECT * FROM SanPhamChiTiet WHERE GiaBan >= ?1 AND GiaBan <= ?2 ORDER BY NgayTao DESC",
            countQuery = "SELECT COUNT(*) FROM SanPhamChiTiet WHERE GiaBan >= ?1 AND GiaBan <= ?2",
            nativeQuery = true)
    Page<SanPhamChiTiet> findByGiaBanAndGiaBan(BigDecimal gt, BigDecimal gb, Pageable pageable);

    @Query("SELECT MAX(s.giaBan) FROM SanPhamChiTiet s")
    Double findMaxGiaBan();

    @Query(value = " SELECT Sum(SoLuongTon) FROM SanPhamChiTiet ", nativeQuery = true)
    Integer sluong();

    @Query("SELECT s FROM SanPhamChiTiet s WHERE " +
            "(:tonKho = '0' AND s.soLuongTon = 0) OR " +
            "(:tonKho = '1-10' AND s.soLuongTon BETWEEN 1 AND 10) OR " +
            "(:tonKho = '11-50' AND s.soLuongTon BETWEEN 11 AND 50) OR " +
            "(:tonKho = '51-100' AND s.soLuongTon BETWEEN 51 AND 100) OR " +
            "(:tonKho = '101-500' AND s.soLuongTon BETWEEN 101 AND 500) OR " +
            "(:tonKho = '501-1000' AND s.soLuongTon BETWEEN 501 AND 1000) OR " +
            "(:tonKho = '1000+' AND s.soLuongTon > 1000) " +
            "ORDER BY s.ngayTao DESC")
    Page<SanPhamChiTiet> findByTonKho(@Param("tonKho") String tonKho, Pageable pageable);
    @Query(value = "SELECT * FROM SanPhamChiTiet WHERE MaMauSac = ? ORDER BY NgayTao DESC", nativeQuery = true)
    List<SanPhamChiTiet> findByMauSac(String mauSac);

    @Query(value = "SELECT * FROM SanPhamChiTiet WHERE MaKichThuoc = ? ORDER BY NgayTao DESC", nativeQuery = true)
    List<SanPhamChiTiet> findBySize(String size);
    @Query(value = "SELECT * FROM SanPhamChiTiet WHERE TrangThai = ? ORDER BY NgayTao DESC", nativeQuery = true)
    List<SanPhamChiTiet> findByTT(String tt);

    @Query(value = "SELECT * FROM SanPhamChiTiet WHERE MaSanPham = ? ORDER BY NgayTao DESC", nativeQuery = true)
    List<SanPhamChiTiet> findByMaSanPham(String maSanPham);

    @Query(value = "SELECT * FROM SanPhamChiTiet WHERE MaSanPham IN (:listMaSanPham) ORDER BY NgayTao DESC", nativeQuery = true)
    List<SanPhamChiTiet> findByidmasp(@Param("listMaSanPham") List<String> listMaSanPham);

    @Query("SELECT s FROM SanPhamChiTiet s " +
            "WHERE (:size IS NULL OR :size = '' OR s.kichThuoc.maKichThuoc = :size) " +
            "AND (:msac IS NULL OR :msac = '' OR s.mauSac.maMauSac = :msac) " +
            "AND (:tt IS NULL OR :tt = '' OR s.trangThai = :tt) " +
            "AND (:gia IS NULL OR s.giaBan >= :gia) " +
            "AND (:gia2 IS NULL OR s.giaBan <= :gia2) " +
            "AND (" +
            "   (:tonKho IS NULL OR :tonKho = '') OR " +
            "   (:tonKho = '0' AND s.soLuongTon = 0) OR " +
            "   (:tonKho = '1-10' AND s.soLuongTon BETWEEN 1 AND 10) OR " +
            "   (:tonKho = '11-50' AND s.soLuongTon BETWEEN 11 AND 50) OR " +
            "   (:tonKho = '51-100' AND s.soLuongTon BETWEEN 51 AND 100) OR " +
            "   (:tonKho = '101-500' AND s.soLuongTon BETWEEN 101 AND 500) OR " +
            "   (:tonKho = '501-1000' AND s.soLuongTon BETWEEN 501 AND 1000) OR " +
            "   (:tonKho = '1000+' AND s.soLuongTon > 1000)" +
            ") " +
            "ORDER BY s.ngayTao DESC")
    List<SanPhamChiTiet> findAllWithFiltersList(
            @Param("size") String size,
            @Param("msac") String msac,
            @Param("tt") String tt,
            @Param("gia") BigDecimal gia,
            @Param("gia2") BigDecimal gia2,
            @Param("tonKho") String tonKho);

    @Query("SELECT s FROM SanPhamChiTiet s " +
            "WHERE (:size IS NULL OR :size = '' OR s.kichThuoc.maKichThuoc = :size) " +
            "AND (:msac IS NULL OR :msac = '' OR s.mauSac.maMauSac = :msac) " +
            "AND (:tt IS NULL OR :tt = '' OR s.trangThai = :tt) " +
            "AND (:gia IS NULL OR s.giaBan >= :gia) " +
            "AND (:gia2 IS NULL OR s.giaBan <= :gia2) " +
            "AND (" +
            "   (:tonKho IS NULL OR :tonKho = '') OR " +
            "   (:tonKho = '0' AND s.soLuongTon = 0) OR " +
            "   (:tonKho = '1-10' AND s.soLuongTon BETWEEN 1 AND 10) OR " +
            "   (:tonKho = '11-50' AND s.soLuongTon BETWEEN 11 AND 50) OR " +
            "   (:tonKho = '51-100' AND s.soLuongTon BETWEEN 51 AND 100) OR " +
            "   (:tonKho = '101-500' AND s.soLuongTon BETWEEN 101 AND 500) OR " +
            "   (:tonKho = '501-1000' AND s.soLuongTon BETWEEN 501 AND 1000) OR " +
            "   (:tonKho = '1000+' AND s.soLuongTon > 1000)" +
            ") " +
            "ORDER BY s.ngayTao DESC")
    Page<SanPhamChiTiet> findAllWithFilters(
            @Param("size") String size,
            @Param("msac") String msac,
            @Param("tt") String tt,
            @Param("gia") BigDecimal gia,
            @Param("gia2") BigDecimal gia2,
            @Param("tonKho") String tonKho,
            Pageable pageable);

    @Query("SELECT s FROM SanPhamChiTiet s WHERE s.sanPham.maSanPham IN :maSanPhamList")
    List<SanPhamChiTiet> findBySanPham_MaSanPhamIn(@Param("maSanPhamList") List<String> maSanPhamList);

    @Query("SELECT spct FROM SanPhamChiTiet spct WHERE spct.maSanPhamChiTiet = :maSPCT")
    Optional<SanPhamChiTiet> findByMaSanPhamChiTiet(@Param("maSPCT") String maSPCT);


    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SanPhamChiTiet s WHERE s.maSanPhamChiTiet = :ma")
    Optional<SanPhamChiTiet> khoaBienTheDeDatHang(@Param("ma") String ma);

    @Query("SELECT COUNT(s) FROM SanPhamChiTiet s WHERE s.soLuongTon > 0 AND s.soLuongTon <= :nguong "
            + "AND (s.trangThai IS NULL OR (s.trangThai <> 'Ngừng bán' AND s.trangThai <> 'Ngừng kinh doanh'))")
    long demBienTheSapHet(@Param("nguong") int nguong);

    @Query("SELECT COUNT(s) FROM SanPhamChiTiet s WHERE (s.soLuongTon IS NULL OR s.soLuongTon = 0) "
            + "AND (s.trangThai IS NULL OR (s.trangThai <> 'Ngừng bán' AND s.trangThai <> 'Ngừng kinh doanh'))")
    long demBienTheHetHang();

    @Query(value = "SELECT * FROM SanPhamChiTiet " +
            "WHERE TrangThai IN (N'Còn hàng', N'Sắp hết') " +
            "ORDER BY NgayTao DESC", nativeQuery = true)
    List<SanPhamChiTiet> findAllOrderByNgayTaoDesc();



    @Query("SELECT spct FROM SanPhamChiTiet spct " +
            "LEFT JOIN FETCH spct.sanPham " +
            "LEFT JOIN FETCH spct.mauSac " +
            "LEFT JOIN FETCH spct.kichThuoc " +
            "WHERE spct.maSanPhamChiTiet = :id")
    Optional<SanPhamChiTiet> findByIdWithSanPham(@Param("id") String id);

    // [MERGE - THONG KE] San pham sap het hang (ton <= nguong), bo hang ngung ban
    @Query("SELECT spct FROM SanPhamChiTiet spct " +
            "JOIN FETCH spct.sanPham sp " +
            "LEFT JOIN FETCH spct.kichThuoc " +
            "LEFT JOIN FETCH spct.mauSac " +
            "WHERE spct.soLuongTon IS NOT NULL " +
            "AND spct.soLuongTon <= :nguong " +
            "AND (spct.trangThai IS NULL " +
            "     OR (spct.trangThai <> 'Ngừng bán' AND spct.trangThai <> 'Ngừng kinh doanh')) " +
            "ORDER BY spct.soLuongTon ASC")
    List<SanPhamChiTiet> sanPhamSapHetHang(@Param("nguong") Integer nguong);
}