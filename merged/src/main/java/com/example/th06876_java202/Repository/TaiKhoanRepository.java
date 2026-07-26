package com.example.th06876_java202.Repository;


import com.example.th06876_java202.Entity.TaiKhoan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {
    @EntityGraph(attributePaths = {"nhanVien", "khachHang"})
    Optional<TaiKhoan> findByTenDangNhap(String name);

    boolean existsByTenDangNhap(String name);

}