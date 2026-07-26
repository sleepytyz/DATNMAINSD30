package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.HoTroTinNhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HoTroTinNhanRepository extends JpaRepository<HoTroTinNhan, Integer> {

    /** Toàn bộ tin nhắn của 1 phiên, theo thứ tự thời gian. */
    List<HoTroTinNhan> findByMaPhienOrderByMaTinNhanAsc(String maPhien);

    /** Tin NHÂN VIÊN gần nhất trong phiên (để bot biết đang có người thật hỗ trợ). */
    HoTroTinNhan findTopByMaPhienAndNguoiGuiOrderByMaTinNhanDesc(String maPhien, String nguoiGui);

    /** Mỗi phiên lấy mã tin nhắn CUỐI CÙNG — dùng dựng danh sách hội thoại. */
    @Query("SELECT t.maPhien, MAX(t.maTinNhan) FROM HoTroTinNhan t GROUP BY t.maPhien")
    List<Object[]> tinCuoiMoiPhien();

    /** Số tin của KHÁCH chưa được nhân viên đọc, theo từng phiên. */
    @Query("SELECT t.maPhien, COUNT(t) FROM HoTroTinNhan t " +
            "WHERE t.nguoiGui = 'KHACH' AND t.daXem = false GROUP BY t.maPhien")
    List<Object[]> demChuaDocMoiPhien();

    /** Tổng số tin của khách chưa đọc (badge sidebar quản lý). */
    @Query("SELECT COUNT(t) FROM HoTroTinNhan t WHERE t.nguoiGui = 'KHACH' AND t.daXem = false")
    long tongChuaDoc();

    /** Nhân viên mở phiên → đánh dấu đã đọc mọi tin khách trong phiên. */
    @Modifying
    @Query("UPDATE HoTroTinNhan t SET t.daXem = true WHERE t.maPhien = :maPhien AND t.nguoiGui = 'KHACH'")
    void danhDauDaXem(@Param("maPhien") String maPhien);
}
