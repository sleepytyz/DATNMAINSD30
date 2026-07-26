package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.PhieuNhapHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhieuNhapHangRepository extends JpaRepository<PhieuNhapHang, Integer> {
    List<PhieuNhapHang> findByTrangThai(String trangThai);
}
