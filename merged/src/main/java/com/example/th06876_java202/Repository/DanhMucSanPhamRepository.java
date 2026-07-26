package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.DanhMucSanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DanhMucSanPhamRepository extends JpaRepository<DanhMucSanPham, String> {

    @Modifying
    @Transactional
    @Query(value = "update DanhMucSanPham set TrangThai = 0 where MaDanhMuc = ?", nativeQuery = true)
    int updateTrangThai(String maDanhMuc);

    // SỬA: Bỏ OrderBy ở đây
    Page<DanhMucSanPham> findAll(Pageable pageable);

    Page<DanhMucSanPham> findByMaDanhMucContainingOrTenDanhMucContaining(
            String maDanhMuc,
            String tenDanhMuc,
            Pageable pageable
    );

    Page<DanhMucSanPham> findByMaDanhMucContainingOrTenDanhMucContainingAndTrangThai(
            String maDanhMuc,
            String tenDanhMuc,
            Boolean trangThai,
            Pageable pageable
    );

    Page<DanhMucSanPham> findByTrangThai(Boolean trangThai, Pageable pageable);

    List<DanhMucSanPham> findTop20ByMaDanhMucContainingOrTenDanhMucContainingOrderByTenDanhMucAsc(
            String maDanhMuc,
            String tenDanhMuc
    );

    long countByTrangThai(boolean trangThai);

    List<DanhMucSanPham> findByMaDanhMucContainingOrTenDanhMucContaining(
            String maDanhMuc,
            String tenDanhMuc
    );

    List<DanhMucSanPham> findByMaDanhMucContainingOrTenDanhMucContainingAndTrangThai(
            String maDanhMuc,
            String tenDanhMuc,
            Boolean trangThai
    );

    List<DanhMucSanPham> findByTrangThai(Boolean trangThai);

    List<DanhMucSanPham> findAllByOrderByNgayTaoDesc();
}