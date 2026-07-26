package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.KieuGiay;
import com.example.th06876_java202.Repository.KieuGiayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class KieuGiayService {

    @Autowired
    private KieuGiayRepository kieuGiayRepository;

    private final Random random = new Random();

    public List<KieuGiay> findAll() {
        return kieuGiayRepository.findAll();
    }

    public KieuGiay them(KieuGiay kieuGiay) {
        return kieuGiayRepository.save(kieuGiay);
    }

    public Optional<KieuGiay> findById(String id) {
        return kieuGiayRepository.findById(id);
    }

    public String generateMaKieuGiay() {
        String code;
        boolean exists;
        do {
            int randomNumber = 1000 + random.nextInt(9000);
            code = "KG" + randomNumber;
            exists = kieuGiayRepository.existsById(code);
        } while (exists);
        return code;
    }

    public String normalizeTenKieuGiay(String ten) {
        if (ten == null) return "";
        ten = ten.trim();
        ten = ten.replaceAll("\\s+", " ");
        String[] words = ten.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    private String normalizeForCompare(String ten) {
        if (ten == null) return "";
        ten = ten.trim();
        ten = ten.replaceAll("\\s+", " ");
        ten = ten.toLowerCase();
        ten = removeDiacritics(ten);
        return ten;
    }

    private String removeDiacritics(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    public boolean existsByTenKieuGiay(String tenKieuGiay) {
        if (tenKieuGiay == null) return false;

        String normalizedInput = normalizeForCompare(tenKieuGiay);

        List<KieuGiay> all = kieuGiayRepository.findAll();
        for (KieuGiay kg : all) {
            String existingName = normalizeForCompare(kg.getTenKieuGiay());
            if (existingName.equals(normalizedInput)) {
                return true;
            }
        }
        return false;
    }

    public KieuGiay doiTrangThai(String id) {
        Optional<KieuGiay> optional = kieuGiayRepository.findById(id);
        if (optional.isPresent()) {
            KieuGiay dm = optional.get();
            dm.setTrangThai(!dm.isTrangThai());
            return kieuGiayRepository.save(dm);
        }
        return null;
    }

    public Page<KieuGiay> getallpage(Pageable pageable) {
        return kieuGiayRepository.findAll(pageable);
    }

    public Page<KieuGiay> searchAndFilter(String keyword, Boolean trangThai, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty() && trangThai != null) {
            return kieuGiayRepository.findByMaKieuGiayContainingOrTenKieuGiayContainingAndTrangThai(
                    keyword, keyword, trangThai, pageable
            );
        } else if (keyword != null && !keyword.isEmpty()) {
            return kieuGiayRepository.findByMaKieuGiayContainingOrTenKieuGiayContaining(
                    keyword, keyword, pageable
            );
        } else if (trangThai != null) {
            return kieuGiayRepository.findByTrangThai(trangThai, pageable);
        } else {
            return kieuGiayRepository.findAll(pageable);
        }
    }

    public List<KieuGiay> searchSuggestions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String searchTerm = keyword.trim();
        return kieuGiayRepository.findTop20ByMaKieuGiayContainingOrTenKieuGiayContainingOrderByTenKieuGiayAsc(
                searchTerm, searchTerm
        );
    }

    public long countByTrangThai(boolean trangThai) {
        return kieuGiayRepository.countByTrangThai(trangThai);
    }

    public List<KieuGiay> searchAll(String keyword, Boolean trangThai) {
        if (keyword != null && !keyword.isEmpty() && trangThai != null) {
            return kieuGiayRepository.findByMaKieuGiayContainingOrTenKieuGiayContainingAndTrangThai(
                    keyword, keyword, trangThai
            );
        } else if (keyword != null && !keyword.isEmpty()) {
            return kieuGiayRepository.findByMaKieuGiayContainingOrTenKieuGiayContaining(
                    keyword, keyword
            );
        } else if (trangThai != null) {
            return kieuGiayRepository.findByTrangThai(trangThai);
        } else {
            return kieuGiayRepository.findAll();
        }
    }
}