package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.LienHe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LienHeRepository extends JpaRepository<LienHe, Integer> {
    List<LienHe> findAllByOrderByMaLienHeDesc();
    long countByTrangThai(String trangThai);
}
