package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.MauSac;
import com.example.th06876_java202.Repository.MauSacRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class MauSacService {

    @Autowired
    private MauSacRepository mauSacRepository;

    private final Random random = new Random();

    public List<MauSac> findAll() {
        return mauSacRepository.findAll();
    }

    public MauSac add(MauSac mauSac) {
        return mauSacRepository.save(mauSac);
    }

    public Optional<MauSac> findById(String id) {
        return mauSacRepository.findById(id);
    }

    public String generateMaMauSac() {
        String code;
        boolean exists;
        do {
            int randomNumber = 1000 + random.nextInt(9000);
            code = "MS" + randomNumber;
            exists = mauSacRepository.existsById(code);
        } while (exists);
        return code;
    }

    public String normalizeTenMauSac(String ten) {
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
        return ten;
    }

    public boolean existsByTenMauSac(String tenMauSac) {
        if (tenMauSac == null) return false;

        String normalizedInput = normalizeForCompare(tenMauSac);

        List<MauSac> all = mauSacRepository.findAll();
        for (MauSac ms : all) {
            String existingName = normalizeForCompare(ms.getTenMauSac());
            if (existingName.equalsIgnoreCase(normalizedInput)) {
                return true;
            }
        }
        return false;
    }

    public MauSac doiTrangThai(String id) {
        Optional<MauSac> optional = mauSacRepository.findById(id);
        if (optional.isPresent()) {
            MauSac dm = optional.get();
            dm.setTrangThai(!dm.isTrangThai());
            return mauSacRepository.save(dm);
        }
        return null;
    }

    public Page<MauSac> getallpage(Pageable pageable) {
        return mauSacRepository.findAll(pageable);
    }

    public Page<MauSac> searchAndFilter(String keyword, Boolean trangThai, Pageable pageable) {
        try {
            if (keyword != null && !keyword.isEmpty() && trangThai != null) {
                return mauSacRepository.findByMaMauSacContainingOrTenMauSacContainingAndTrangThai(
                        keyword, keyword, trangThai, pageable
                );
            } else if (keyword != null && !keyword.isEmpty()) {
                return mauSacRepository.findByMaMauSacContainingOrTenMauSacContaining(
                        keyword, keyword, pageable
                );
            } else if (trangThai != null) {
                return mauSacRepository.findByTrangThai(trangThai, pageable);
            } else {
                return mauSacRepository.findAll(pageable);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return mauSacRepository.findAll(pageable);
        }
    }

    public List<MauSac> searchSuggestions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String searchTerm = keyword.trim();
        return mauSacRepository.findTop20ByMaMauSacContainingOrTenMauSacContainingOrderByTenMauSacAsc(
                searchTerm, searchTerm
        );
    }

    public long countByTrangThai(boolean trangThai) {
        return mauSacRepository.countByTrangThai(trangThai);
    }

    public List<MauSac> searchAll(String keyword, Boolean trangThai) {
        if (keyword != null && !keyword.isEmpty() && trangThai != null) {
            return mauSacRepository.findByMaMauSacContainingOrTenMauSacContainingAndTrangThai(
                    keyword, keyword, trangThai
            );
        } else if (keyword != null && !keyword.isEmpty()) {
            return mauSacRepository.findByMaMauSacContainingOrTenMauSacContaining(
                    keyword, keyword
            );
        } else if (trangThai != null) {
            return mauSacRepository.findByTrangThai(trangThai);
        } else {
            return mauSacRepository.findAll();
        }
    }
}