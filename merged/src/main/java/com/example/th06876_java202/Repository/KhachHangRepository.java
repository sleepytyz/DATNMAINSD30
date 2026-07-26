package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.KhachHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface KhachHangRepository extends JpaRepository<KhachHang, String> {

    @Query("select k from KhachHang k where k.sdt like concat('%', :sdt, '%')")
    Page<KhachHang> findBySdtt(@Param("sdt") String sdt, Pageable pageable);

    List<KhachHang> findTop10BySdtContaining(String sdt);

    KhachHang findByMaKH(String maKH);

    @Query("SELECT k FROM KhachHang k WHERE k.sdt LIKE CONCAT('%', :sdt, '%')")
    Page<KhachHang> findBySdtContaining(@Param("sdt") String sdt, Pageable pageable);

    @Query("SELECT k FROM KhachHang k WHERE (:sdt IS NULL OR :sdt = '' OR k.sdt LIKE CONCAT('%', :sdt, '%'))")
    List<KhachHang> findAllBySdt(@Param("sdt") String sdt);


    @Query(value = "select * from KhachHang order by NgayDangKy  desc", nativeQuery = true)
    List<KhachHang> findban();

    @Modifying
    @Transactional
    @Query("update KhachHang k set k.trangThai = false where k.maKH = :maKH")
    void updateTrangThai(@Param("maKH") String maKH);

    Optional<KhachHang> findByEmail(String email);

    Optional<KhachHang> findByTaiKhoan_TenDangNhap(String tenDangNhap);

    boolean existsBySdt(String sdt);
    boolean existsByEmail(String email);

    boolean existsBySdtAndMaKHNot(String sdt, String maKH);
    boolean existsByEmailAndMaKHNot(String email, String maKH);

    @Query("SELECT COUNT(dc) > 0 FROM DiaChi dc WHERE dc.soDienThoaiNguoiNhan = :sdt AND dc.khachHang.maKH != :maKH")
    boolean existsBySdtInDiaChi(@Param("sdt") String sdt, @Param("maKH") String maKH);

    @Query("SELECT k FROM KhachHang k WHERE k.trangThai = :trangThai")
    Page<KhachHang> findByTrangThai(@Param("trangThai") boolean trangThai, Pageable pageable);

    List<KhachHang> findByTrangThai(Boolean trangThai);

    @Query("SELECT kh FROM KhachHang kh WHERE " +
            "LOWER(kh.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(kh.maKH) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(kh.sdt) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(kh.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<KhachHang> findTop10ByHoTenContainingOrMaKHContaining(
            @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT kh FROM KhachHang kh WHERE " +
            "LOWER(kh.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(kh.maKH) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(kh.sdt) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(kh.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<KhachHang> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}