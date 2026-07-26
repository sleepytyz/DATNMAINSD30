package com.example.th06876_java202.Repository;

import com.example.th06876_java202.Entity.ChatLieu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatLieuRepository extends JpaRepository<ChatLieu, String> {

    boolean existsByTenChatLieu(String tenChatLieu);

    Page<ChatLieu> findAllByOrderByNgayTaoDesc(Pageable pageable);

    // ===== THÊM CÁC METHOD MỚI =====

    /**
     * Tìm theo từ khóa (mã hoặc tên) - có phân trang
     */
    Page<ChatLieu> findByMaChatLieuContainingOrTenChatLieuContaining(
            String maChatLieu,
            String tenChatLieu,
            Pageable pageable
    );

    /**
     * Tìm theo từ khóa và trạng thái - có phân trang
     */
    Page<ChatLieu> findByMaChatLieuContainingOrTenChatLieuContainingAndTrangThai(
            String maChatLieu,
            String tenChatLieu,
            Boolean trangThai,
            Pageable pageable
    );

    /**
     * Lọc theo trạng thái - có phân trang
     */
    Page<ChatLieu> findByTrangThai(Boolean trangThai, Pageable pageable);

    /**
     * Gợi ý tìm kiếm - lấy top 20 kết quả
     */
    List<ChatLieu> findTop20ByMaChatLieuContainingOrTenChatLieuContainingOrderByTenChatLieuAsc(
            String maChatLieu,
            String tenChatLieu
    );

    /**
     * Đếm số lượng theo trạng thái
     */
    long countByTrangThai(boolean trangThai);

    // ===== THÊM METHOD CHO EXPORT EXCEL =====

    /**
     * Tìm tất cả theo từ khóa (không phân trang)
     */
    List<ChatLieu> findByMaChatLieuContainingOrTenChatLieuContaining(
            String maChatLieu,
            String tenChatLieu
    );

    /**
     * Tìm tất cả theo từ khóa và trạng thái (không phân trang)
     */
    List<ChatLieu> findByMaChatLieuContainingOrTenChatLieuContainingAndTrangThai(
            String maChatLieu,
            String tenChatLieu,
            Boolean trangThai
    );

    /**
     * Tìm tất cả theo trạng thái (không phân trang)
     */
    List<ChatLieu> findByTrangThai(Boolean trangThai);
}