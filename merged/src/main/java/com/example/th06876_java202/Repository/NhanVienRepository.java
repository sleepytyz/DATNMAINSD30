package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.NhanVien;
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
public interface NhanVienRepository extends JpaRepository<NhanVien, String> {
    Optional<NhanVien> findByEmail(String email);

    @Modifying
    @Transactional
    @Query(value = "Update NhanVien set TrangThai = N'Đã nghỉ việc' where MaNhanVien = ?", nativeQuery = true)
    int updateTrangThai(String maNhanVien);

    @Query(value = "SELECT * FROM NhanVien WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR HoTen LIKE %:keyword% OR SoDienThoai LIKE %:keyword%) AND " +
            "(:role IS NULL OR :role = '' OR ChucVu = :role) AND " +
            "(:status IS NULL OR :status = '' OR CAST(TrangThai AS VARCHAR(5)) = :status)",
            countQuery = "SELECT count(*) FROM NhanVien WHERE " +
                    "(:keyword IS NULL OR :keyword = '' OR HoTen LIKE %:keyword% OR SoDienThoai LIKE %:keyword%) AND " +
                    "(:role IS NULL OR :role = '' OR ChucVu = :role) AND " +
                    "(:status IS NULL OR :status = '' OR CAST(TrangThai AS VARCHAR(5)) = :status)",
            nativeQuery = true)
    Page<NhanVien> filter(@Param("keyword") String keyword,
                          @Param("role") String role,
                          @Param("status") String status,
                          Pageable pageable);

    boolean existsByEmailAndMaNhanVienNot(String email, String maNhanVien);

    boolean existsBySoDienThoaiAndMaNhanVienNot(String soDienThoai, String maNhanVien);

    boolean existsById(String id);
    boolean existsByEmail(String email);
    boolean existsBySoDienThoai(String soDienThoai);

    @Query("SELECT COUNT(nv) > 0 FROM NhanVien nv WHERE nv.email = :email AND nv.maNhanVien != :maNhanVien")
    boolean existsByEmailAndNotMaNhanVien(@Param("email") String email, @Param("maNhanVien") String maNhanVien);

    @Query("SELECT COUNT(nv) > 0 FROM NhanVien nv WHERE nv.soDienThoai = :soDienThoai AND nv.maNhanVien != :maNhanVien")
    boolean existsBySoDienThoaiAndNotMaNhanVien(@Param("soDienThoai") String soDienThoai, @Param("maNhanVien") String maNhanVien);

    List<NhanVien> findByHoTenContaining(String hoTen);

    NhanVien findByTaiKhoan_TenDangNhap(String tenDangNhap);
    @Query("SELECT nv FROM NhanVien nv WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR nv.hoTen LIKE %:keyword% OR nv.soDienThoai LIKE %:keyword%) " +
            "AND (:role IS NULL OR :role = '' OR nv.chucVu = :role) " +
            "AND (:status IS NULL OR :status = '' OR nv.trangThai = CAST(:status AS boolean))")
    List<NhanVien> findAllByFilter(@Param("keyword") String keyword,
                                   @Param("role") String role,
                                   @Param("status") String status);

    @Query("SELECT n FROM NhanVien n WHERE " +
            "LOWER(n.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(n.maNhanVien) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(n.soDienThoai) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<NhanVien> findTop10ByHoTenContainingOrMaNhanVienContaining(
            @Param("keyword") String keyword
    );
}