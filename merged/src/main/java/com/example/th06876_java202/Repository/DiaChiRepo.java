package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.DiaChi;
import com.example.th06876_java202.Entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository địa chỉ khách hàng.
 * (Đã dọn dẹp: loại bỏ toàn bộ khai báo method bị TRÙNG LẶP ở bản gốc —
 *  interface Java không cho phép hai method cùng chữ ký.)
 */
@Repository
public interface DiaChiRepo extends JpaRepository<DiaChi, Integer> {

    // ============ TÌM THEO MÃ KHÁCH HÀNG (String) ============
    List<DiaChi> findByKhachHang_MaKH(String maKH);

    // ============ TÌM THEO ĐỐI TƯỢNG KHÁCH HÀNG ============
    List<DiaChi> findByKhachHang(KhachHang khachHang);

    // ============ TÌM ĐỊA CHỈ MẶC ĐỊNH ============
    @Query("SELECT d FROM DiaChi d WHERE d.khachHang.maKH = :maKH AND d.diaChiMacDinh = true")
    DiaChi findDefaultByMaKH(@Param("maKH") String maKH);

    Optional<DiaChi> findByKhachHang_MaKHAndDiaChiMacDinhTrue(String maKH);

    DiaChi findByKhachHangAndDiaChiMacDinh(KhachHang khachHang, Boolean macDinh);

    // ============ ĐẾM ĐỊA CHỈ MẶC ĐỊNH ============
    @Query("SELECT COUNT(d) FROM DiaChi d WHERE d.khachHang.maKH = :maKH AND d.diaChiMacDinh = true")
    int countDefaultAddressByKhachHang(@Param("maKH") String maKH);

    // ============ ĐẾM SỐ LƯỢNG ĐỊA CHỈ CỦA KHÁCH HÀNG ============
    long countByKhachHang_MaKH(String maKH);

    // ============ RESET MẶC ĐỊNH (gỡ tất cả) ============
    @Modifying
    @Transactional
    @Query("UPDATE DiaChi d SET d.diaChiMacDinh = false WHERE d.khachHang.maKH = :maKH")
    void resetDiaChiMacDinhByKhachHang(@Param("maKH") String maKH);

    @Modifying
    @Transactional
    @Query("UPDATE DiaChi d SET d.diaChiMacDinh = false WHERE d.khachHang.maKH = :maKH")
    void resetAllDefault(@Param("maKH") String maKH);

    // ============ SET MẶC ĐỊNH TRỰC TIẾP ============
    @Modifying
    @Transactional
    @Query("UPDATE DiaChi d SET d.diaChiMacDinh = true WHERE d.khachHang.maKH = :maKH AND d.maDiaChi = :maDiaChi")
    void setDefaultAddressDirectly(@Param("maKH") String maKH, @Param("maDiaChi") Integer maDiaChi);

    // ============ TÌM THEO SỐ ĐIỆN THOẠI / TÊN (LIKE) ============
    List<DiaChi> findBySoDienThoaiNguoiNhanContaining(String soDienThoai);

    List<DiaChi> findByTenNguoiNhanContaining(String tenNguoiNhan);

    // ============ XÓA TẤT CẢ ĐỊA CHỈ CỦA KHÁCH HÀNG ============
    @Modifying
    @Transactional
    @Query("DELETE FROM DiaChi d WHERE d.khachHang.maKH = :maKH")
    void deleteByKhachHang_MaKH(@Param("maKH") String maKH);

    // ============ TÌM THEO TỈNH/THÀNH & QUẬN/HUYỆN ============
    List<DiaChi> findByTinhThanh(String tinhThanh);

    List<DiaChi> findByQuanHuyen(String quanHuyen);

    // ============ TÌM THEO ID + MÃ KHÁCH HÀNG ============
    Optional<DiaChi> findByMaDiaChiAndKhachHang_MaKH(Integer maDiaChi, String maKH);

    // ============ LẤY DANH SÁCH / ĐỊA CHỈ MẶC ĐỊNH TRỰC TIẾP ============
    @Query("SELECT d FROM DiaChi d WHERE d.khachHang.maKH = :maKH")
    List<DiaChi> findAddressesByMaKH(@Param("maKH") String maKH);

    @Query("SELECT d FROM DiaChi d WHERE d.khachHang.maKH = :maKH AND d.diaChiMacDinh = true")
    DiaChi findDefaultAddressDirectly(@Param("maKH") String maKH);
}
