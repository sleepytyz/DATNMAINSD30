package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.KichThuoc;
import com.example.th06876_java202.Repository.KichThuocRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class KichThuocService {

    private final KichThuocRepository kichThuocRepository;
    private final Random random = new Random();

    public KichThuocService(KichThuocRepository kichThuocRepository) {
        this.kichThuocRepository = kichThuocRepository;
    }

    public List<KichThuoc> getall() {
        return kichThuocRepository.findAllByOrderByTenKichThuocAsc();
    }

    public List<KichThuoc> getAllKichThuoc() {
        return kichThuocRepository.findAll();
    }

    public KichThuoc add(KichThuoc kichThuoc) {
        return kichThuocRepository.save(kichThuoc);
    }

    public Optional<KichThuoc> getKichThuocById(String id) {
        return kichThuocRepository.findById(id);
    }

    public String generateMaKichThuoc() {
        String code;
        boolean exists;
        do {
            int randomNumber = 1000 + random.nextInt(9000);
            code = "KT" + randomNumber;
            exists = kichThuocRepository.existsById(code);
        } while (exists);
        return code;
    }

    public String normalizeTenKichThuoc(String ten) {
        if (ten == null) return "";
        ten = ten.trim();
        ten = ten.replaceAll("\\s+", " ");
        return ten;
    }

    private String normalizeForCompare(String ten) {
        if (ten == null) return "";
        ten = ten.trim();
        ten = ten.replaceAll("\\s+", "");
        return ten;
    }

    public boolean existsByTenKichThuoc(String tenKichThuoc) {
        if (tenKichThuoc == null) return false;

        String normalizedInput = normalizeForCompare(tenKichThuoc);

        List<KichThuoc> all = kichThuocRepository.findAll();
        for (KichThuoc kt : all) {
            String existingName = normalizeForCompare(kt.getTenKichThuoc());
            if (existingName.equalsIgnoreCase(normalizedInput)) {
                return true;
            }
        }
        return false;
    }

    public Page<KichThuoc> getallpage(Pageable pageable) {
        return kichThuocRepository.findAllByOrderByNgayTaoDesc(pageable);
    }

    public KichThuoc doiTrangThai(String id) {
        Optional<KichThuoc> optional = kichThuocRepository.findById(id);
        if (optional.isPresent()) {
            KichThuoc kt = optional.get();
            kt.setTrangThai(!kt.isTrangThai());
            return kichThuocRepository.save(kt);
        }
        return null;
    }

    // ===== THÊM CÁC METHOD MỚI =====

    /**
     * Tìm kiếm và lọc kết hợp từ khóa và trạng thái
     */
    public Page<KichThuoc> searchAndFilter(String keyword, Boolean trangThai, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty() && trangThai != null) {
            return kichThuocRepository.findByMaKichThuocContainingOrTenKichThuocContainingAndTrangThai(
                    keyword, keyword, trangThai, pageable
            );
        } else if (keyword != null && !keyword.isEmpty()) {
            return kichThuocRepository.findByMaKichThuocContainingOrTenKichThuocContaining(
                    keyword, keyword, pageable
            );
        } else if (trangThai != null) {
            return kichThuocRepository.findByTrangThai(trangThai, pageable);
        } else {
            return kichThuocRepository.findAllByOrderByNgayTaoDesc(pageable);
        }
    }

    /**
     * Gợi ý tìm kiếm - lấy tối đa 20 kết quả
     */
    public List<KichThuoc> searchSuggestions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String searchTerm = keyword.trim();
        return kichThuocRepository.findTop20ByMaKichThuocContainingOrTenKichThuocContainingOrderByTenKichThuocAsc(
                searchTerm, searchTerm
        );
    }

    /**
     * Đếm số lượng theo trạng thái
     */
    public long countByTrangThai(boolean trangThai) {
        return kichThuocRepository.countByTrangThai(trangThai);
    }

    /**
     * Tìm tất cả theo từ khóa và trạng thái (không phân trang) - Dùng cho export Excel
     */
    public List<KichThuoc> searchAll(String keyword, Boolean trangThai) {
        if (keyword != null && !keyword.isEmpty() && trangThai != null) {
            return kichThuocRepository.findByMaKichThuocContainingOrTenKichThuocContainingAndTrangThai(
                    keyword, keyword, trangThai
            );
        } else if (keyword != null && !keyword.isEmpty()) {
            return kichThuocRepository.findByMaKichThuocContainingOrTenKichThuocContaining(
                    keyword, keyword
            );
        } else if (trangThai != null) {
            return kichThuocRepository.findByTrangThai(trangThai);
        } else {
            return kichThuocRepository.findAll();
        }
    }
}