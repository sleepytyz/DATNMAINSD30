package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.CaLamViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface CaLamViecRepository extends JpaRepository<CaLamViec, Integer> {
    CaLamViec findByMaCa(int maCa);

    List<CaLamViec> findByTenCaContaining(String tenCa);

    @Query("SELECT c FROM CaLamViec c WHERE c.gioBatDau BETWEEN :gioBatDau AND :gioKetThuc")
    List<CaLamViec> findByGio(LocalTime gioBatDau, LocalTime gioKetThuc);
}