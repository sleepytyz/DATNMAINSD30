package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.ChatLieu;
import com.example.th06876_java202.Repository.ChatLieuRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class ChatLieuService {

    private final ChatLieuRepository chatLieuRepository;
    private final Random random = new Random();

    public ChatLieuService(ChatLieuRepository chatLieuRepository) {
        this.chatLieuRepository = chatLieuRepository;
    }

    public List<ChatLieu> findAll() {
        return chatLieuRepository.findAll();
    }

    public ChatLieu add(ChatLieu chatLieu) {
        return chatLieuRepository.save(chatLieu);
    }

    public Optional<ChatLieu> findById(String id) {
        return chatLieuRepository.findById(id);
    }

    /**
     * Tạo mã chất liệu tự động: CL + 4 số ngẫu nhiên
     */
    public String generateMaChatLieu() {
        String code;
        boolean exists;
        do {
            int randomNumber = 1000 + random.nextInt(9000);
            code = "CL" + randomNumber;
            exists = chatLieuRepository.existsById(code);
        } while (exists);
        return code;
    }

    /**
     * Chuẩn hóa tên: loại bỏ khoảng trắng thừa, viết hoa chữ cái đầu mỗi từ
     */
    public String normalizeTenChatLieu(String ten) {
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

    /**
     * Chuẩn hóa tên để so sánh (không viết hoa, chỉ loại bỏ khoảng trắng thừa)
     */
    private String normalizeForCompare(String ten) {
        if (ten == null) return "";
        ten = ten.trim();
        ten = ten.replaceAll("\\s+", " ");
        return ten;
    }

    public boolean existsByTenChatLieu(String tenChatLieu) {
        if (tenChatLieu == null) return false;

        String normalizedInput = normalizeForCompare(tenChatLieu);
        List<ChatLieu> all = chatLieuRepository.findAll();
        for (ChatLieu cl : all) {
            String existingName = normalizeForCompare(cl.getTenChatLieu());
            if (existingName.equalsIgnoreCase(normalizedInput)) {
                return true;
            }
        }
        return false;
    }

    public ChatLieu doiTrangThai(String id) {
        Optional<ChatLieu> optional = chatLieuRepository.findById(id);
        if (optional.isPresent()) {
            ChatLieu dm = optional.get();
            dm.setTrangThai(!dm.isTrangThai());
            return chatLieuRepository.save(dm);
        }
        return null;
    }

    public Page<ChatLieu> getallpage(Pageable pageable) {
        return chatLieuRepository.findAllByOrderByNgayTaoDesc(pageable);
    }

    // ===== THÊM CÁC METHOD MỚI =====

    /**
     * Tìm kiếm và lọc kết hợp từ khóa và trạng thái
     */
    public Page<ChatLieu> searchAndFilter(String keyword, Boolean trangThai, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty() && trangThai != null) {
            return chatLieuRepository.findByMaChatLieuContainingOrTenChatLieuContainingAndTrangThai(
                    keyword, keyword, trangThai, pageable
            );
        } else if (keyword != null && !keyword.isEmpty()) {
            return chatLieuRepository.findByMaChatLieuContainingOrTenChatLieuContaining(
                    keyword, keyword, pageable
            );
        } else if (trangThai != null) {
            return chatLieuRepository.findByTrangThai(trangThai, pageable);
        } else {
            return chatLieuRepository.findAllByOrderByNgayTaoDesc(pageable);
        }
    }

    /**
     * Gợi ý tìm kiếm - lấy tối đa 20 kết quả
     */
    public List<ChatLieu> searchSuggestions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String searchTerm = keyword.trim();
        return chatLieuRepository.findTop20ByMaChatLieuContainingOrTenChatLieuContainingOrderByTenChatLieuAsc(
                searchTerm, searchTerm
        );
    }

    /**
     * Đếm số lượng theo trạng thái
     */
    public long countByTrangThai(boolean trangThai) {
        return chatLieuRepository.countByTrangThai(trangThai);
    }

    /**
     * Tìm tất cả theo từ khóa và trạng thái (không phân trang) - Dùng cho export Excel
     */
    public List<ChatLieu> searchAll(String keyword, Boolean trangThai) {
        if (keyword != null && !keyword.isEmpty() && trangThai != null) {
            return chatLieuRepository.findByMaChatLieuContainingOrTenChatLieuContainingAndTrangThai(
                    keyword, keyword, trangThai
            );
        } else if (keyword != null && !keyword.isEmpty()) {
            return chatLieuRepository.findByMaChatLieuContainingOrTenChatLieuContaining(
                    keyword, keyword
            );
        } else if (trangThai != null) {
            return chatLieuRepository.findByTrangThai(trangThai);
        } else {
            return chatLieuRepository.findAll();
        }
    }
}