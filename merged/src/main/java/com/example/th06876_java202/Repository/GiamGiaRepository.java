package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.GiamGia;
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
import java.util.Optional;

@Repository
public interface GiamGiaRepository extends JpaRepository<GiamGia, String> {

    @Query(value = """
        SELECT * FROM GiamGia 
        WHERE (:keyword IS NULL OR :keyword = '' OR TenChuongTrinh LIKE %:keyword% OR CAST(MaGiamGia AS VARCHAR(30)) LIKE %:keyword%)
        AND (:tt IS NULL OR :tt = '' OR TrangThai = :tt)
        AND (:lg IS NULL OR :lg = '' OR LoaiGiamGia = :lg)
        AND (:loaiApDung IS NULL OR LoaiApDung = :loaiApDung)
        AND (:tuNgay IS NULL OR NgayBatDau >= :tuNgay)
        AND (:denNgay IS NULL OR NgayKetThuc <= :denNgay)
        ORDER BY NgayTao DESC
    """, nativeQuery = true, countQuery = """
        SELECT count(*) FROM GiamGia 
        WHERE (:keyword IS NULL OR :keyword = '' OR TenChuongTrinh LIKE %:keyword% OR CAST(MaGiamGia AS VARCHAR(30)) LIKE %:keyword%)
        AND (:tt IS NULL OR :tt = '' OR TrangThai = :tt)
        AND (:lg IS NULL OR :lg = '' OR LoaiGiamGia = :lg)
        AND (:loaiApDung IS NULL OR LoaiApDung = :loaiApDung)
        AND (:tuNgay IS NULL OR NgayBatDau >= :tuNgay)
        AND (:denNgay IS NULL OR NgayKetThuc <= :denNgay)
    """)
    Page<GiamGia> filterAll(
            @Param("keyword") String keyword,
            @Param("tt") String tt,
            @Param("lg") String lg,
            @Param("loaiApDung") Integer loaiApDung,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay,
            Pageable pageable
    );

    @Query(value = "SELECT * FROM GiamGia ORDER BY NgayTao DESC", nativeQuery = true)
    List<GiamGia> findAllVouchers();

    @Query(value = """
        SELECT * FROM GiamGia 
        WHERE (TrangThai IS NULL OR TrangThai LIKE N'%hoạt động%') 
        AND (IsVoHan = 1 OR SoLuong > 0)
        AND (NgayBatDau IS NULL OR NgayBatDau <= GETDATE())
        AND (NgayKetThuc IS NULL OR NgayKetThuc >= GETDATE())
        ORDER BY NgayTao DESC
    """, nativeQuery = true)
    List<GiamGia> findVoucherDangHoatDong();

    @Query(value = "SELECT * FROM GiamGia WHERE (TrangThai IS NULL OR TrangThai LIKE N'%hoạt động%') AND (IsVoHan = 1 OR SoLuong > 0) ORDER BY NgayTao DESC", nativeQuery = true)
    List<GiamGia> findSoLuongVoucher();

    @Modifying
    @Transactional
    @Query("UPDATE GiamGia g SET g.soLuong = g.soLuong - 1 WHERE g.maGiamGia = :id AND g.soLuong > 0")
    int giamSoLuongVoucher(@Param("id") String id);

    @Query(value = "select * from GiamGia where LoaiGiamGia = ?", nativeQuery = true)
    List<GiamGia> getGiamGia(String loaiGia);

    @Query(value = "select * from GiamGia where TrangThai = ?", nativeQuery = true)
    List<GiamGia> loctt(String tt);

    @Query(value = "select * from GiamGia where TenChuongTrinh like CONCAT('%', :keyword , '%' ) or CAST(MaGiamGia as Varchar(30)) like CONCAT('%', :keyword, '%' ) ", nativeQuery = true)
    List<GiamGia> timkiem(@Param("keyword") String keyword);

    @Query(value = "select * from GiamGia where NgayBatDau > ? and NgayKetThuc < ?", nativeQuery = true)
    List<GiamGia> timkiemngay(LocalDateTime ngaybd, LocalDateTime ngayketthuc);

    java.util.Optional<GiamGia> findByTenGiamGiaIgnoreCase(String tenGiamGia);

    @Query("SELECT g FROM GiamGia g WHERE g.trangThai != 'Ngừng hoạt động'")
    List<GiamGia> findDanhSachCanCapNhat();

    @Modifying
    @Transactional
    @Query("UPDATE GiamGia g SET g.trangThai = :trangThai WHERE g.maGiamGia = :id")
    void updateTrangThai(@Param("trangThai") String trangThai, @Param("id") String id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE GiamGia SET TrangThai = N'Ngừng hoạt động' WHERE MaGiamGia = :id", nativeQuery = true)
    void updateTrangThaiToStop(@Param("id") String id);

    @Query(value = """
    SELECT * FROM GiamGia 
    WHERE (:keyword IS NULL OR :keyword = '' OR TenChuongTrinh LIKE %:keyword% OR CAST(MaGiamGia AS VARCHAR(30)) LIKE %:keyword%)
    AND (:tt IS NULL OR :tt = '' OR TrangThai = :tt)
    AND (:lg IS NULL OR :lg = '' OR LoaiGiamGia = :lg)
    AND (:loaiApDung IS NULL OR LoaiApDung = :loaiApDung)
    AND (:tuNgay IS NULL OR NgayBatDau >= :tuNgay)
    AND (:denNgay IS NULL OR NgayKetThuc <= :denNgay)
    ORDER BY NgayTao DESC
""", nativeQuery = true)
    List<GiamGia> findAllFiltered(
            @Param("keyword") String keyword,
            @Param("tt") String tt,
            @Param("lg") String lg,
            @Param("loaiApDung") Integer loaiApDung,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay
    );

    @Modifying
    @Transactional
    @Query(value = "UPDATE GiamGia SET NgayBatDau = GETDATE(), TrangThai = N'Hoạt động' WHERE MaGiamGia = :id", nativeQuery = true)
    void activateVoucher(@Param("id") String id);

    @Query("SELECT gg FROM GiamGia gg WHERE " +
            "LOWER(gg.maGiamGia) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(gg.tenGiamGia) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<GiamGia> findTop10ByMaGiamGiaContainingOrTenGiamGiaContaining(
            @Param("keyword") String keyword, Pageable pageable);

    Optional<GiamGia> findByMaGiamGia(String maGiamGia);

    @Query("SELECT g FROM GiamGia g WHERE UPPER(TRIM(g.maGiamGia)) = UPPER(TRIM(:ma))")
    Optional<GiamGia> findByMaGiamGiaIgnoreCase(@Param("ma") String ma);

    // Tìm voucher theo TÊN CHƯƠNG TRÌNH, KHÔNG lọc trạng thái ở đây —
    // trạng thái/hạn/số lượng do kiemTraVoucherHopLe kiểm sau (tránh phụ thuộc chuỗi cứng).
    @Query("SELECT g FROM GiamGia g WHERE UPPER(TRIM(g.tenGiamGia)) = UPPER(TRIM(:ten))")
    Optional<GiamGia> findActiveByTenGiamGiaIgnoreCase(@Param("ten") String tenGiamGia);
}