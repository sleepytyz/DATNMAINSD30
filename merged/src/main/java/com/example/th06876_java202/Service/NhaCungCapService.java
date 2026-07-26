package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.NhaCungCap;
import com.example.th06876_java202.Repository.NhaCungCapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NhaCungCapService {
    private final NhaCungCapRepository nhaCungCapRepository;

    public List<NhaCungCap> findAll() {
        return nhaCungCapRepository.findAll();
    }

    public NhaCungCap save(NhaCungCap nhaCungCap) {
        return nhaCungCapRepository.save(nhaCungCap);
    }

    public void delete(NhaCungCap nhaCungCap) {
        nhaCungCapRepository.delete(nhaCungCap);
    }

    public NhaCungCap findById(int id) {
        return nhaCungCapRepository.findById(id).orElse(null);
    }
}
