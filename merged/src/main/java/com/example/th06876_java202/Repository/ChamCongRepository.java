package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.ChamCong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ChamCongRepository extends JpaRepository<ChamCong, Integer> {
    
    ChamCong findByMaChamCong(int maChamCong);

    List<ChamCong> findByNhanVien_HoTenContaining(String hoTen);
    
    List<ChamCong> findByNgayChamCongBetween(LocalDate tuNgay, LocalDate denNgay);
    
    List<ChamCong> findByNhanVien_MaNhanVienAndNgayChamCong(String maNhanVien, LocalDate ngay);
    
    List<ChamCong> findByNhanVien_MaNhanVienAndNgayChamCongBetween(String maNhanVien, LocalDate tuNgay, LocalDate denNgay);
    
    @Query("SELECT DISTINCT cc.nhanVien.maNhanVien FROM ChamCong cc WHERE cc.ngayChamCong BETWEEN :tuNgay AND :denNgay")
    List<Integer> findMaNhanVienCoLichTrongKhoang(@Param("tuNgay") LocalDate tuNgay, @Param("denNgay") LocalDate denNgay);
    
    @Query("SELECT cc FROM ChamCong cc WHERE cc.ngayChamCong BETWEEN :tuNgay AND :denNgay AND cc.trangThai = true")
    List<ChamCong> findLichSapToiTrongKhoang(@Param("tuNgay") LocalDate tuNgay, @Param("denNgay") LocalDate denNgay);
    
    @Query("SELECT cc FROM ChamCong cc WHERE cc.ngayChamCong BETWEEN :tuNgay AND :denNgay AND cc.trangThai = false")
    List<ChamCong> findLichDaQuaTrongKhoang(@Param("tuNgay") LocalDate tuNgay, @Param("denNgay") LocalDate denNgay);

    // [MỚI] Tổng hợp chấm công theo TỪNG nhân viên trong khoảng ngày, dùng cho
    // bảng "Hiệu suất nhân viên" của ADMIN: tổng giờ công, số ngày công, số lần đi trễ
    // (check-in trễ hơn 5 phút so với giờ bắt đầu ca) và số lần vắng mặt không chấm công
    // dù đã được xếp lịch (chỉ tính các ngày đã qua).
    @Query(value = """
            SELECT 
                cc.MaNhanVien as maNhanVien,
                nv.HoTen as hoTen,
                COUNT(CASE WHEN cc.GioVao IS NOT NULL THEN 1 END) as soNgayCong,
                ISNULL(SUM(cc.SoGioLam), 0) as tongGioLam,
                SUM(CASE WHEN cc.GioVao IS NOT NULL AND cc.GioVao > DATEADD(MINUTE, 5, ca.GioBatDau) THEN 1 ELSE 0 END) as soLanTre,
                SUM(CASE WHEN cc.GioVao IS NULL AND cc.NgayChamCong < CAST(GETDATE() AS DATE) THEN 1 ELSE 0 END) as soLanVangMat
            FROM ChamCong cc
            JOIN CaLamViec ca ON ca.MaCa = cc.MaCa
            JOIN NhanVien nv ON nv.MaNhanVien = cc.MaNhanVien
            WHERE cc.NgayChamCong BETWEEN ?1 AND ?2
            GROUP BY cc.MaNhanVien, nv.HoTen
            """, nativeQuery = true)
    List<Object[]> thongKeChamCongTheoNhanVien(LocalDate tuNgay, LocalDate denNgay);

    // [MỚI] Bản thu gọn của truy vấn trên nhưng lọc riêng CHO 1 NHÂN VIÊN
    // (dùng cho trang "Thống kê của tôi" - STAFF)
    @Query(value = """
            SELECT 
                cc.MaNhanVien as maNhanVien,
                nv.HoTen as hoTen,
                COUNT(CASE WHEN cc.GioVao IS NOT NULL THEN 1 END) as soNgayCong,
                ISNULL(SUM(cc.SoGioLam), 0) as tongGioLam,
                SUM(CASE WHEN cc.GioVao IS NOT NULL AND cc.GioVao > DATEADD(MINUTE, 5, ca.GioBatDau) THEN 1 ELSE 0 END) as soLanTre,
                SUM(CASE WHEN cc.GioVao IS NULL AND cc.NgayChamCong < CAST(GETDATE() AS DATE) THEN 1 ELSE 0 END) as soLanVangMat
            FROM ChamCong cc
            JOIN CaLamViec ca ON ca.MaCa = cc.MaCa
            JOIN NhanVien nv ON nv.MaNhanVien = cc.MaNhanVien
            WHERE cc.MaNhanVien = ?1 AND cc.NgayChamCong BETWEEN ?2 AND ?3
            GROUP BY cc.MaNhanVien, nv.HoTen
            """, nativeQuery = true)
    List<Object[]> thongKeChamCongCaNhan(String maNhanVien, LocalDate tuNgay, LocalDate denNgay);
}