package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "CaLamViec")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// [SỬA] CaLamViec được dùng làm KHÓA của HashMap (xem ChamCongController) để ghép
// lịch chấm công theo ca. Không có equals()/hashCode() riêng thì Java dùng so sánh
// mặc định theo địa chỉ bộ nhớ -> 2 đối tượng cùng 1 ca (load từ 2 câu query khác
// nhau, ví dụ findCaAll() và ChamCong.getCaLamViec()) có thể bị coi là KHÁC NHAU,
// khiến việc ghép lịch/hiển thị bị sai một cách khó lường (chỉ tình cờ đúng khi
// Hibernate tái sử dụng đúng 1 instance trong cùng phiên làm việc).
@EqualsAndHashCode(of = "maCa")
public class CaLamViec {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaCa")
    private Integer maCa;

    @NotBlank(message = "Tên ca không được để trống")
    @Column(name = "TenCa")
    private String tenCa;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    @Column(name = "GioBatDau")
    private LocalTime gioBatDau;

    @NotNull(message = "Giờ kết thúc không được để trống")
    @Column(name = "GioKetThuc")
    private LocalTime gioKetThuc;

    @Column(name = "MoTa")
    private String moTa;

    @OneToMany(mappedBy = "caLamViec", fetch = FetchType.LAZY)
    private List<ChamCong> danhSachChamCong;
}