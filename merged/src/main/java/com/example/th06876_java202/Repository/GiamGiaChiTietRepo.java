package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.GiamGiaChiTiet;
import com.example.th06876_java202.Entity.GiamGiaChiTietId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface GiamGiaChiTietRepo extends JpaRepository<GiamGiaChiTiet, GiamGiaChiTietId> {

    @Query("SELECT ct.id.maKhachHang FROM GiamGiaChiTiet ct WHERE ct.id.maGiamGia = :maGiamGia")
    List<String> findMaKhachHangByMaGiamGia(@Param("maGiamGia") String maGiamGia);

    @Modifying
    @Query("UPDATE GiamGiaChiTiet ct SET ct.trangThaiSuDung = :trangThai WHERE ct.id.maGiamGia = :maGiamGia")
    void updateTrangThaiSuDungByMaGiamGia(@Param("maGiamGia") String maGiamGia, @Param("trangThai") int trangThai);

    long countByGiamGia_MaGiamGia(String maGiamGia);

    boolean existsById(GiamGiaChiTietId id);

    Optional<GiamGiaChiTiet> findByKhachHang_MaKHAndGiamGia_MaGiamGia(String maKH, String maGiamGia);

    @Query("SELECT ct FROM GiamGiaChiTiet ct WHERE ct.khachHang.maKH = :maKH")
    List<GiamGiaChiTiet> findByKhachHang_MaKH(@Param("maKH") String maKH);

    boolean existsById_MaGiamGiaAndId_MaKhachHang(String maGiamGia, String maKhachHang);

    @Query(value =
            "SELECT kv.* FROM KHACHHANG_VOUCHER kv " +
                    "INNER JOIN GiamGia gg ON kv.MaGiamGia = gg.MaGiamGia " +
                    "WHERE kv.MaKhachHang = :maKhachHang " +
                    "AND gg.TrangThai = N'Hoạt động' " +
                    "AND (gg.NgayKetThuc IS NULL OR gg.NgayKetThuc >= GETDATE())",
            nativeQuery = true)
    List<GiamGiaChiTiet> findValidVouchersByKhachHangNative(@Param("maKhachHang") String maKhachHang);

    @Transactional
    @Query("UPDATE GiamGiaChiTiet gct SET gct.trangThaiSuDung = 1 " +
            "WHERE gct.id.maGiamGia = :maGiamGia " +
            "AND gct.id.maKhachHang = :maKhachHang " +
            "AND gct.trangThaiSuDung = 0")
    int updateTrangThaiDaSuDung(
            @Param("maGiamGia") String maGiamGia,
            @Param("maKhachHang") String maKhachHang);

    @Query("SELECT COUNT(gct) FROM GiamGiaChiTiet gct " +
            "WHERE gct.id.maGiamGia = :maGiamGia " +
            "AND gct.id.maKhachHang = :maKhachHang " +
            "AND gct.trangThaiSuDung = 0")
    int countVoucherChuaSuDung(
            @Param("maGiamGia") String maGiamGia,
            @Param("maKhachHang") String maKhachHang);

}