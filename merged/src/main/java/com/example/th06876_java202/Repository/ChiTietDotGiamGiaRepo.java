package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.ChiTietDotGiamGia;
import com.example.th06876_java202.Entity.SanPhamChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChiTietDotGiamGiaRepo extends JpaRepository<ChiTietDotGiamGia, Integer> {

    boolean existsByDotGiamGia_MaGiamGiaAndSanPham_MaSanPham(
            String maGiamGia,
            String maSanPham
    );

    @Query(value = "SELECT * FROM ChiTietDotGiamGia WHERE " +
            "(:maGiamGia IS NULL OR :maGiamGia = '' OR MaGiamGia = :maGiamGia)",
            nativeQuery = true)
    List<ChiTietDotGiamGia> filterByDotGiamGia(@Param("maGiamGia") String maGiamGia);


    @Query("SELECT DISTINCT ct.sanPham.maSanPham FROM ChiTietDotGiamGia ct WHERE ct.dotGiamGia.maGiamGia = :id")
    List<String> findSanPhamByDot(@Param("id") String id);

    @Query("SELECT ct.sanPhamChiTiet.maSanPhamChiTiet FROM ChiTietDotGiamGia ct WHERE ct.dotGiamGia.maGiamGia = :id")
    List<String> findSanPhamChiTietByDot(@Param("id") String id);

    @Modifying
    @Transactional
    @Query("DELETE FROM ChiTietDotGiamGia c WHERE c.dotGiamGia.maGiamGia = :maGiamGia")
    void deleteByDotGiamGia_MaGiamGia(@Param("maGiamGia") String maGiamGia);


    boolean existsByDotGiamGia_MaGiamGiaAndSanPham_MaSanPhamAndSanPhamChiTiet_MaSanPhamChiTiet(
            String maGiamGia, String maSanPham, String maSanPhamChiTiet
    );

    List<ChiTietDotGiamGia> findBySanPhamChiTiet_MaSanPhamChiTiet(String maSanPhamChiTiet);

    @Query("SELECT d.giaTriGiam FROM ChiTietDotGiamGia c JOIN c.dotGiamGia d " +
            "LEFT JOIN c.sanPham sp LEFT JOIN c.sanPhamChiTiet spct LEFT JOIN spct.sanPham sp2 " +
            "WHERE (sp.maSanPham = :maSanPham OR sp2.maSanPham = :maSanPham) " +
            "AND (d.trangThai IS NULL OR d.trangThai NOT IN ('Ngừng hoạt động', 'Đã huỷ')) " +
            "AND :today BETWEEN d.ngayBatDau AND d.ngayKetThuc " +
            "ORDER BY d.giaTriGiam DESC")
    List<java.math.BigDecimal> findActiveDiscountPercentBySanPham(@Param("maSanPham") String maSanPham, @Param("today") java.time.LocalDate today);

    @Query("SELECT d.giaTriGiam FROM ChiTietDotGiamGia c JOIN c.dotGiamGia d " +
            "LEFT JOIN c.sanPham sp LEFT JOIN c.sanPhamChiTiet spct " +
            "WHERE (spct.maSanPhamChiTiet = :maSPCT " +
            "       OR (spct IS NULL AND sp.maSanPham = :maSanPham)) " +
            "AND (d.trangThai IS NULL OR d.trangThai NOT IN ('Ngừng hoạt động', 'Đã huỷ')) " +
            "AND :today BETWEEN d.ngayBatDau AND d.ngayKetThuc " +
            "ORDER BY d.giaTriGiam DESC")
    List<java.math.BigDecimal> findActiveDiscountPercentChoBienThe(@Param("maSPCT") String maSPCT,
                                                                   @Param("maSanPham") String maSanPham,
                                                                   @Param("today") java.time.LocalDate today);

    @Query("SELECT d.giaTriGiam FROM ChiTietDotGiamGia c JOIN c.dotGiamGia d " +
            "WHERE c.sanPhamChiTiet.maSanPhamChiTiet = :maSPCT " +
            "AND (d.trangThai IS NULL OR d.trangThai NOT IN ('Ngừng hoạt động', 'Đã huỷ')) " +
            "AND :today BETWEEN d.ngayBatDau AND d.ngayKetThuc " +
            "ORDER BY d.giaTriGiam DESC")
    List<java.math.BigDecimal> findActiveDiscountPercentBySanPhamChiTiet(@Param("maSPCT") String maSPCT, @Param("today") java.time.LocalDate today);

    @Query("SELECT DISTINCT COALESCE(sp.maSanPham, sp2.maSanPham) " +
            "FROM ChiTietDotGiamGia c " +
            "LEFT JOIN c.sanPham sp LEFT JOIN c.sanPhamChiTiet spct LEFT JOIN spct.sanPham sp2 " +
            "JOIN c.dotGiamGia d WHERE d.maGiamGia = :maDot")
    List<String> maSanPhamTrongDot(@Param("maDot") String maDot);

}