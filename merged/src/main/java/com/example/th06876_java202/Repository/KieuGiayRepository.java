package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.KieuGiay;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KieuGiayRepository extends JpaRepository<KieuGiay, String> {

    Page<KieuGiay> findAll(Pageable pageable);

    Page<KieuGiay> findByMaKieuGiayContainingOrTenKieuGiayContaining(
            String maKieuGiay,
            String tenKieuGiay,
            Pageable pageable
    );

    Page<KieuGiay> findByMaKieuGiayContainingOrTenKieuGiayContainingAndTrangThai(
            String maKieuGiay,
            String tenKieuGiay,
            Boolean trangThai,
            Pageable pageable
    );

    Page<KieuGiay> findByTrangThai(Boolean trangThai, Pageable pageable);

    List<KieuGiay> findTop20ByMaKieuGiayContainingOrTenKieuGiayContainingOrderByTenKieuGiayAsc(
            String maKieuGiay,
            String tenKieuGiay
    );

    long countByTrangThai(boolean trangThai);

    List<KieuGiay> findByMaKieuGiayContainingOrTenKieuGiayContaining(
            String maKieuGiay,
            String tenKieuGiay
    );

    List<KieuGiay> findByMaKieuGiayContainingOrTenKieuGiayContainingAndTrangThai(
            String maKieuGiay,
            String tenKieuGiay,
            Boolean trangThai
    );

    List<KieuGiay> findByTrangThai(Boolean trangThai);
}