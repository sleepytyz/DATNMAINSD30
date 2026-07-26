package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.DotGiamGia;
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

@Repository
public interface DotGiamGiaRepo extends JpaRepository<DotGiamGia, String> {

    // ===== SỬA: So sánh với chuỗi 'Hoạt động' (nvarchar) =====
    @Query(value = """
        SELECT dgg.* FROM DotGiamGia dgg 
        INNER JOIN ChiTietDotGiamGia ctdgg ON dgg.MaGiamGia = ctdgg.MaGiamGia 
        WHERE ctdgg.MaSanPham = ?1 AND dgg.TrangThai = N'Hoạt động'
        """, nativeQuery = true)
    List<DotGiamGia> findBySanPham(String maSanPham);

    @Modifying
    @Transactional
    @Query(value = "UPDATE DotGiamGia SET TrangThai = N'Ngừng hoạt động' WHERE MaGiamGia = ?1", nativeQuery = true)
    void updateTrangThai(String maSanPham);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE DotGiamGia SET TrangThai = N'Đã huỷ' WHERE MaGiamGia = :id AND TrangThai = N'Sắp hoạt động'", nativeQuery = true)
    int cancelVoucher(@Param("id") String id);

    // ===== FILTER PAGING - SỬA LỖI ORDER BY TRÙNG =====
    @Query(value = """
    SELECT * FROM DotGiamGia
    WHERE
    (:keyword = '' OR
     TenGiamGia LIKE CONCAT('%', :keyword, '%')
     OR MaGiamGia LIKE CONCAT('%', :keyword, '%'))
    AND (:trangThai = '' OR TrangThai = :trangThai)
    AND (:tuNgay IS NULL OR NgayBatDau >= :tuNgay)
    AND (:denNgay IS NULL OR NgayKetThuc <= :denNgay)
    """,
            countQuery = """
    SELECT COUNT(*) FROM DotGiamGia
    WHERE
    (:keyword = '' OR
     TenGiamGia LIKE CONCAT('%', :keyword, '%')
     OR MaGiamGia LIKE CONCAT('%', :keyword, '%'))
    AND (:trangThai = '' OR TrangThai = :trangThai)
    AND (:tuNgay IS NULL OR NgayBatDau >= :tuNgay)
    AND (:denNgay IS NULL OR NgayKetThuc <= :denNgay)
    """,
            nativeQuery = true)
    Page<DotGiamGia> filterPaging(
            @Param("keyword") String keyword,
            @Param("trangThai") String trangThai,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay,
            Pageable pageable
    );

    Page<DotGiamGia> findAllByOrderByMaGiamGiaDesc(Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "UPDATE DotGiamGia SET NgayBatDau = GETDATE(), TrangThai = N'Hoạt động' WHERE MaGiamGia = :id", nativeQuery = true)
    void activateVoucher(@Param("id") String id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE DotGiamGia SET TrangThai = N'Ngừng hoạt động' WHERE MaGiamGia = :id", nativeQuery = true)
    void updateTrangThaiToStop(@Param("id") String id);

    @org.springframework.data.jpa.repository.Query(
            "SELECT d FROM DotGiamGia d " +
                    "WHERE (d.trangThai IS NULL OR d.trangThai NOT IN ('Ngừng hoạt động', 'Đã huỷ')) " +
                    "AND :today BETWEEN d.ngayBatDau AND d.ngayKetThuc " +
                    "ORDER BY d.giaTriGiam DESC")
    java.util.List<DotGiamGia> dangChayHomNay(
            @org.springframework.data.repository.query.Param("today") java.time.LocalDate today);
}