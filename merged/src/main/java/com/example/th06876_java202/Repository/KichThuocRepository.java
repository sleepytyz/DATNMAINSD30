package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.KichThuoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KichThuocRepository extends JpaRepository<KichThuoc, String> {

    boolean existsByTenKichThuoc(String tenKichThuoc);

    Page<KichThuoc> findAllByOrderByNgayTaoDesc(Pageable pageable);

    List<KichThuoc> findAllByOrderByTenKichThuocAsc();

    @Query("SELECT COUNT(k) > 0 FROM KichThuoc k WHERE LOWER(REPLACE(k.tenKichThuoc, ' ', '')) = LOWER(REPLACE(:ten, ' ', ''))")
    boolean existsByTenKichThuocNormalized(@Param("ten") String ten);

    // ===== THÊM CÁC METHOD MỚI =====

    /**
     * Tìm theo từ khóa (mã hoặc tên) - có phân trang
     */
    Page<KichThuoc> findByMaKichThuocContainingOrTenKichThuocContaining(
            String maKichThuoc,
            String tenKichThuoc,
            Pageable pageable
    );

    /**
     * Tìm theo từ khóa và trạng thái - có phân trang
     */
    Page<KichThuoc> findByMaKichThuocContainingOrTenKichThuocContainingAndTrangThai(
            String maKichThuoc,
            String tenKichThuoc,
            Boolean trangThai,
            Pageable pageable
    );

    /**
     * Lọc theo trạng thái - có phân trang
     */
    Page<KichThuoc> findByTrangThai(Boolean trangThai, Pageable pageable);

    /**
     * Gợi ý tìm kiếm - lấy top 20 kết quả
     */
    List<KichThuoc> findTop20ByMaKichThuocContainingOrTenKichThuocContainingOrderByTenKichThuocAsc(
            String maKichThuoc,
            String tenKichThuoc
    );

    /**
     * Đếm số lượng theo trạng thái
     */
    long countByTrangThai(boolean trangThai);

    // ===== THÊM METHOD CHO EXPORT EXCEL =====

    /**
     * Tìm tất cả theo từ khóa (không phân trang)
     */
    List<KichThuoc> findByMaKichThuocContainingOrTenKichThuocContaining(
            String maKichThuoc,
            String tenKichThuoc
    );

    /**
     * Tìm tất cả theo từ khóa và trạng thái (không phân trang)
     */
    List<KichThuoc> findByMaKichThuocContainingOrTenKichThuocContainingAndTrangThai(
            String maKichThuoc,
            String tenKichThuoc,
            Boolean trangThai
    );

    /**
     * Tìm tất cả theo trạng thái (không phân trang)
     */
    List<KichThuoc> findByTrangThai(Boolean trangThai);
}