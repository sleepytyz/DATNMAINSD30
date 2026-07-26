package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ChatLieu")
public class ChatLieu {

    @Id
    @Column(name = "MaChatLieu")
    private String maChatLieu;

    @NotBlank(message = "Không bỏ trống tên chất liệu")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Chất liệu chỉ chứa chữ cái và khoảng trắng")
    @Column(name = "TenChatLieu")
    private String tenChatLieu;

    @Column(name = "TrangThai")
    private boolean trangThai;

    @Column(name = "NgayTao", updatable = false)
    private LocalDateTime ngayTao;

    // Tự động set ngày tạo trước khi lưu
    @PrePersist
    protected void onCreate() {
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
    }
}