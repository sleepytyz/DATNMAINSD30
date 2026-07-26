package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.PhienBanHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhienBanHangRepository extends JpaRepository<PhienBanHang, Integer> {

    /**
     * Phiên ĐANG MỞ của một nhân viên (mỗi nhân viên chỉ được mở 1 phiên tại một thời điểm).
     * Dùng để: kiểm tra đã mở ca chưa, gắn hoá đơn vào ca, và đóng ca.
     */
    @Query("SELECT p FROM PhienBanHang p " +
            "WHERE p.nhanVien.maNhanVien = :maNhanVien " +
            "AND p.trangThai = 'DANG_MO' " +
            "ORDER BY p.thoiGianMoCa DESC")
    List<PhienBanHang> timPhienDangMo(@Param("maNhanVien") String maNhanVien);

    /** Toàn bộ phiên đang mở (cho Admin theo dõi ai đang trong ca) */
    @Query("SELECT p FROM PhienBanHang p WHERE p.trangThai = 'DANG_MO' ORDER BY p.thoiGianMoCa DESC")
    List<PhienBanHang> timTatCaPhienDangMo();

    /** Lịch sử phiên của 1 nhân viên, mới nhất trước */
    @Query("SELECT p FROM PhienBanHang p " +
            "WHERE p.nhanVien.maNhanVien = :maNhanVien " +
            "ORDER BY p.thoiGianMoCa DESC")
    List<PhienBanHang> lichSuTheoNhanVien(@Param("maNhanVien") String maNhanVien);

    /** Lịch sử phiên trong khoảng thời gian (cho Admin đối soát) */
    @Query("SELECT p FROM PhienBanHang p " +
            "WHERE p.thoiGianMoCa BETWEEN :tuNgay AND :denNgay " +
            "ORDER BY p.thoiGianMoCa DESC")
    List<PhienBanHang> lichSuTheoKhoang(@Param("tuNgay") LocalDateTime tuNgay,
                                        @Param("denNgay") LocalDateTime denNgay);
}
