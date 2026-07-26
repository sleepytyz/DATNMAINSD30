package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.DotGiamGia;
import com.example.th06876_java202.Repository.DotGiamGiaRepo;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DotGiamGiaService {

    @Autowired
    private DotGiamGiaRepo dotGiamGiaRepo;

    @Autowired
    private EntityManager entityManager;

    public void clearCache() {
        entityManager.clear();
    }

    public List<DotGiamGia> getAll() {
        return dotGiamGiaRepo.findAll();
    }

    public DotGiamGia getById(String id) {
        return dotGiamGiaRepo.findById(id).orElse(null);
    }

    @Transactional
    public DotGiamGia save(DotGiamGia dgg) {
        if (dgg.getNgayTao() == null) {
            dgg.setNgayTao(LocalDateTime.now());
        }
        return dotGiamGiaRepo.save(dgg);
    }

    public void delete(String id) {
        dotGiamGiaRepo.deleteById(id);
    }

    public void update(DotGiamGia dotGiamGia) {
        dotGiamGiaRepo.save(dotGiamGia);
    }

    // ===== SỬA: XỬ LÝ NGOẠI LỆ KHI LẤY GIẢM GIÁ THEO SẢN PHẨM =====
    public List<DotGiamGia> getBymasp(String masp) {
        if (masp == null || masp.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<DotGiamGia> result = dotGiamGiaRepo.findBySanPham(masp);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy đợt giảm giá theo mã sản phẩm '" + masp + "': " + e.getMessage());
            // Trả về danh sách rỗng thay vì throw exception
            return new ArrayList<>();
        }
    }

    public void suaa(String id) {
        dotGiamGiaRepo.updateTrangThai(id);
    }

    // Trong DotGiamGiaService.java
    public Page<DotGiamGia> filterPaging(String keyword,
                                         String trangThai,
                                         LocalDateTime tuNgay,
                                         LocalDateTime denNgay,
                                         Pageable pageable) {
        // Xử lý keyword null -> empty
        String searchKeyword = (keyword == null || keyword.trim().isEmpty()) ? "" : keyword.trim();
        String searchTrangThai = (trangThai == null || trangThai.trim().isEmpty()) ? "" : trangThai.trim();

        return dotGiamGiaRepo.filterPaging(searchKeyword, searchTrangThai, tuNgay, denNgay, pageable);
    }

    public Optional<DotGiamGia> findById(String id) {
        return dotGiamGiaRepo.findById(id);
    }

    public Page<DotGiamGia> getAllPaging(Pageable pageable) {
        return dotGiamGiaRepo.findAllByOrderByMaGiamGiaDesc(pageable);
    }

    @Transactional
    public void capNhatTrangThai() {
        List<DotGiamGia> list = dotGiamGiaRepo.findAll();
        LocalDate today = LocalDate.now();

        for (DotGiamGia dgg : list) {
            if ("Đã huỷ".equals(dgg.getTrangThai())) {
                continue;
            }

            if (today.isBefore(dgg.getNgayBatDau())) {
                dgg.setTrangThai("Sắp hoạt động");
            } else if (!today.isAfter(dgg.getNgayKetThuc())) {
                dgg.setTrangThai("Hoạt động");
            } else {
                dgg.setTrangThai("Ngừng hoạt động");
            }

            dotGiamGiaRepo.save(dgg);
        }
    }

    @Transactional
    public boolean cancelVoucher(String id) {
        int updated = dotGiamGiaRepo.cancelVoucher(id);
        return updated > 0;
    }

    @Transactional
    public void activateVoucher(String id) {
        dotGiamGiaRepo.activateVoucher(id);
    }

    @Transactional
    public void updateTrangThaiToStop(String id) {
        dotGiamGiaRepo.updateTrangThaiToStop(id);
    }
}