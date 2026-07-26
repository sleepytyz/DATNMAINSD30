package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.DanhGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DanhGiaRepository extends JpaRepository<DanhGia, Integer> {

    /** Đánh giá đang hiển thị của 1 sản phẩm, mới nhất trước. */
    @Query("SELECT d FROM DanhGia d WHERE d.sanPham.maSanPham = :maSP AND d.trangThai = true ORDER BY d.ngayDanhGia DESC")
    List<DanhGia> findHienThiBySanPham(@Param("maSP") String maSP);

    /** Toàn bộ đánh giá của 1 sản phẩm (kể cả ẩn) - dùng cho admin. */
    List<DanhGia> findBySanPham_MaSanPhamOrderByNgayDanhGiaDesc(String maSanPham);

    /** Đánh giá của 1 khách hàng cho 1 sản phẩm trong 1 hoá đơn cụ thể (chống đánh giá trùng). */
    @Query("SELECT d FROM DanhGia d WHERE d.khachHang.maKH = :maKH AND d.sanPham.maSanPham = :maSP AND d.hoaDon.maHoaDon = :maHD")
    DanhGia findByKhachSanPhamHoaDon(@Param("maKH") String maKH,
                                     @Param("maSP") String maSP,
                                     @Param("maHD") String maHoaDon);

    long countBySanPham_MaSanPhamAndTrangThaiTrue(String maSanPham);

    /** Điểm trung bình của sản phẩm (null nếu chưa có đánh giá). */
    @Query("SELECT AVG(d.soSao) FROM DanhGia d WHERE d.sanPham.maSanPham = :maSP AND d.trangThai = true")
    Double diemTrungBinh(@Param("maSP") String maSP);

    List<DanhGia> findByKhachHang_MaKHOrderByNgayDanhGiaDesc(String maKH);

    /**
     * Thống kê đánh giá theo sản phẩm cho website bán hàng:
     * trả về [maSanPham, điểm trung bình, số lượt đánh giá] (chỉ tính đánh giá đang hiển thị).
     */
    @Query("SELECT d.sanPham.maSanPham, AVG(d.soSao), COUNT(d) FROM DanhGia d " +
            "WHERE d.trangThai = true GROUP BY d.sanPham.maSanPham")
    List<Object[]> thongKeDanhGiaTheoSanPham();

    /** Phân bố số sao (1..5) của 1 sản phẩm: trả về [soSao, soLuot]. */
    @Query("SELECT d.soSao, COUNT(d) FROM DanhGia d " +
            "WHERE d.sanPham.maSanPham = :maSP AND d.trangThai = true GROUP BY d.soSao")
    List<Object[]> phanBoSoSao(@Param("maSP") String maSP);

    /** Toàn bộ đánh giá, mới nhất trước — dùng cho module quản lý đánh giá. */
    List<DanhGia> findAllByOrderByNgayDanhGiaDesc();
}
