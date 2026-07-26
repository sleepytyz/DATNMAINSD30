package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.ChiTietNhapHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietNhapHangRepository extends JpaRepository<ChiTietNhapHang, Integer> {
    List<ChiTietNhapHang> findByPhieuNhap_MaPhieuNhap(Integer maPhieuNhap);
}
