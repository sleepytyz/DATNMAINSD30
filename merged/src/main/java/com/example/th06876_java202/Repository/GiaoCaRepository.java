package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.GiaoCa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GiaoCaRepository extends JpaRepository<GiaoCa, Integer> {
    GiaoCa findByMaGiaoCa(int maGiaoCa);

}