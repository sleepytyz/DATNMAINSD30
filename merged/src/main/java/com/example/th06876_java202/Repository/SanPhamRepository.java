package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, String> {
    boolean existsByTenSanPham(String TenSanPham);

    boolean existsByMaSanPham(String maSanPham);

    Optional<SanPham> findByTenSanPham(String tenSanPham);

    @Modifying
    @Transactional
    @Query(value = "UPDATE SanPham SET TrangThai = ?2 WHERE MaSanPham = ?1", nativeQuery = true)
    int updateTrangThai(String maSanPham, boolean trangThai);

    @Query(value = "Select * from SanPham where MaDanhMuc = ?", nativeQuery = true)
    List<SanPham> getallbymaDanhMuc(String maDanhMuc);

    @Query(value = "select * from SanPham where TenSanPham like CONCAT('%', :keyword, '%') or CAST(MaSanPham AS VARCHAR(20)) LIKE CONCAT('%', :keyword, '%')",
            nativeQuery = true)
    List<SanPham> timkiem(@Param("keyword") String keyword);

    @Query(value = "select * from SanPham where TrangThai = ?", nativeQuery = true)
    List<SanPham> getallbyTrangThai(String tt);

    boolean existsByTenSanPhamIgnoreCase(String tenSanPham);

    Page<SanPham> findAllByOrderByNgayTaoDesc(Pageable pageable);

    List<SanPham> findTop8ByTrangThaiTrueOrderByMaSanPhamDesc();

    @Query("SELECT s FROM SanPham s " +
            "LEFT JOIN s.danhMucSanPham dm " +
            "LEFT JOIN s.thuongHieu th " +
            "LEFT JOIN s.kieuGiay kg " +
            "WHERE (:maDanhMuc IS NULL OR dm.maDanhMuc = :maDanhMuc) AND " +
            "(:tt IS NULL OR s.trangThai = :tt) AND " +
            "(:maTH IS NULL OR th.maThuongHieu = :maTH) AND " +
            "(:maKG IS NULL OR kg.maKieuGiay = :maKG) AND " +
            "(:keyword IS NULL OR s.tenSanPham LIKE %:keyword% OR CAST(s.maSanPham AS string) LIKE %:keyword%) " +
            "ORDER BY s.ngayTao DESC")
    Page<SanPham> searchSanPham(@Param("maDanhMuc") String maDanhMuc,
                                @Param("tt") Boolean tt,
                                @Param("maTH") String maTH,
                                @Param("maKG") String maKG,
                                @Param("keyword") String keyword,
                                Pageable pageable);

    long countByTrangThai(boolean trangThai);


    @Query("SELECT DISTINCT sp FROM SanPham sp " +
            "LEFT JOIN sp.danhMucSanPham dm " +
            "LEFT JOIN sp.thuongHieu th " +
            "LEFT JOIN sp.kieuGiay kg " +
            "WHERE (:maDanhMuc IS NULL OR dm.maDanhMuc = :maDanhMuc) " +
            "AND (:tt IS NULL OR sp.trangThai = :tt) " +
            "AND (:maTH IS NULL OR th.maThuongHieu = :maTH) " +
            "AND (:maKG IS NULL OR kg.maKieuGiay = :maKG) " +
            "AND (:t IS NULL OR " +
            "LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :t, '%')) OR " +
            "LOWER(sp.maSanPham) LIKE LOWER(CONCAT('%', :t, '%'))) " +
            "ORDER BY sp.ngayTao DESC")
    List<SanPham> findAllWithFilters(@Param("maDanhMuc") String maDanhMuc,
                                     @Param("tt") Boolean tt,
                                     @Param("maTH") String maTH,
                                     @Param("maKG") String maKG,
                                     @Param("t") String t);

    // Trong SanPhamRepository
    @Query("SELECT sp FROM SanPham sp " +
            "LEFT JOIN FETCH sp.thuongHieu " +
            "WHERE LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(sp.maSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY sp.ngayTao DESC")
    List<SanPham> findTop10ByTenSanPhamContainingOrMaSanPhamContaining(
            @Param("keyword") String keyword
    );
}