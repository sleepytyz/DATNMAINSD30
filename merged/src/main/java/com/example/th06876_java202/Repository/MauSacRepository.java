package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.MauSac;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MauSacRepository extends JpaRepository<MauSac, String> {

    Page<MauSac> findAll(Pageable pageable);

    Page<MauSac> findByMaMauSacContainingOrTenMauSacContaining(
            String maMauSac,
            String tenMauSac,
            Pageable pageable
    );

    Page<MauSac> findByMaMauSacContainingOrTenMauSacContainingAndTrangThai(
            String maMauSac,
            String tenMauSac,
            Boolean trangThai,
            Pageable pageable
    );

    Page<MauSac> findByTrangThai(Boolean trangThai, Pageable pageable);

    List<MauSac> findTop20ByMaMauSacContainingOrTenMauSacContainingOrderByTenMauSacAsc(
            String maMauSac,
            String tenMauSac
    );

    long countByTrangThai(boolean trangThai);

    List<MauSac> findByMaMauSacContainingOrTenMauSacContaining(
            String maMauSac,
            String tenMauSac
    );

    List<MauSac> findByMaMauSacContainingOrTenMauSacContainingAndTrangThai(
            String maMauSac,
            String tenMauSac,
            Boolean trangThai
    );

    List<MauSac> findByTrangThai(Boolean trangThai);
}