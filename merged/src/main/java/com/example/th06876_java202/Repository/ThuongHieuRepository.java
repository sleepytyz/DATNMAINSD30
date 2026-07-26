package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.ThuongHieu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, String> {

    Page<ThuongHieu> findAll(Pageable pageable);

    Page<ThuongHieu> findByMaThuongHieuContainingOrTenThuongHieuContaining(
            String maThuongHieu,
            String tenThuongHieu,
            Pageable pageable
    );

    Page<ThuongHieu> findByMaThuongHieuContainingOrTenThuongHieuContainingAndTrangThai(
            String maThuongHieu,
            String tenThuongHieu,
            Boolean trangThai,
            Pageable pageable
    );

    Page<ThuongHieu> findByTrangThai(Boolean trangThai, Pageable pageable);

    List<ThuongHieu> findTop20ByMaThuongHieuContainingOrTenThuongHieuContainingOrderByTenThuongHieuAsc(
            String maThuongHieu,
            String tenThuongHieu
    );

    long countByTrangThai(boolean trangThai);

    List<ThuongHieu> findByMaThuongHieuContainingOrTenThuongHieuContaining(
            String maThuongHieu,
            String tenThuongHieu
    );

    List<ThuongHieu> findByMaThuongHieuContainingOrTenThuongHieuContainingAndTrangThai(
            String maThuongHieu,
            String tenThuongHieu,
            Boolean trangThai
    );

    List<ThuongHieu> findByTrangThai(Boolean trangThai);
}