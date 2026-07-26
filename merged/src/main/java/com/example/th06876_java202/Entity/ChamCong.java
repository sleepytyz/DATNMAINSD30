package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "ChamCong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChamCong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChamCong")
    private Integer maChamCong;

    @ManyToOne
    @JoinColumn(name = "MaNhanVien")
    private NhanVien nhanVien;

    @ManyToOne
    @JoinColumn(name = "MaCa")
    private CaLamViec caLamViec;

    @Column(name = "NgayChamCong")
    private LocalDate ngayChamCong;

    @Column(name = "GioVao")
    private LocalTime gioVao;

    @Column(name = "GioRa")
    private LocalTime gioRa;

    @Column(name = "TrangThai")
    private Boolean trangThai; // false: đã qua (đã chấm công), true: sắp tới (lịch đã xếp)

    @Column(name = "SoGioLam")
    private BigDecimal soGioLam;

    @Column(name = "GhiChu")
    private String ghiChu;

    // [SỬA] Cờ tạm (KHÔNG map vào DB) đánh dấu bản ghi này có ngày = hôm nay hay không.
    // Tính sẵn ở controller để template chỉ cần đọc chamCong.laHomNay, tránh dùng
    // Set.contains() trong Thymeleaf (dễ sai do lệch kiểu Integer/Long).
    @Transient
    private boolean laHomNay;

    // ==================================================================
    // TRẠNG THÁI CHẤM CÔNG TỰ ĐỘNG
    // Hệ thống tự đối chiếu giờ check-in thực tế với giờ bắt đầu ca quy định:
    //   - CHUA_DEN_GIO : ca trong tương lai, chưa tới lượt
    //   - DUNG_GIO     : có mặt, check-in trong khoảng cho phép (<= 15 phút trễ)
    //   - DEN_MUON     : check-in sau giờ vào ca quá 15 phút
    //   - VANG_MAT     : ca đã qua (ngày cũ) mà không hề check-in
    //   - CHUA_CHAM    : hôm nay, đã tới ca nhưng chưa check-in
    // ==================================================================

    /** Số phút trễ được chấp nhận vẫn tính là đúng giờ */
    public static final int PHUT_TRE_CHO_PHEP = 15;

    @Transient
    public String getTrangThaiChamCong() {
        java.time.LocalDate homNay = com.example.th06876_java202.config.GioVN.ngayHomNay();

        // Ca ở tương lai
        if (ngayChamCong != null && ngayChamCong.isAfter(homNay)) {
            return "CHUA_DEN_GIO";
        }

        // Đã check-in nhưng chưa check-out, VÀ ca là của ngày trước hôm nay
        // -> không còn là "đang làm" nữa, mà là quên chấm công ra.
        if (gioVao != null && gioRa == null
                && ngayChamCong != null && ngayChamCong.isBefore(homNay)) {
            return "QUEN_CHECKOUT";
        }

        // Đã check-in -> xét đúng giờ hay muộn
        if (gioVao != null) {
            if (caLamViec == null || caLamViec.getGioBatDau() == null) {
                return "DUNG_GIO";
            }
            java.time.LocalTime gioQuyDinh = caLamViec.getGioBatDau();
            java.time.LocalTime hanChot = gioQuyDinh.plusMinutes(PHUT_TRE_CHO_PHEP);
            return gioVao.isAfter(hanChot) ? "DEN_MUON" : "DUNG_GIO";
        }

        // Chưa check-in: ngày cũ -> vắng mặt; hôm nay -> chưa chấm
        if (ngayChamCong != null && ngayChamCong.isBefore(homNay)) {
            return "VANG_MAT";
        }
        return "CHUA_CHAM";
    }

    /** Nhãn tiếng Việt để hiển thị */
    @Transient
    public String getNhanTrangThai() {
        switch (getTrangThaiChamCong()) {
            case "DUNG_GIO":       return "Đúng giờ";
            case "DEN_MUON":       return "Đến muộn";
            case "VANG_MAT":       return "Vắng mặt";
            case "CHUA_DEN_GIO":   return "Sắp tới";
            case "QUEN_CHECKOUT":  return "Quên chấm công ra";
            default:               return "Chưa chấm công";
        }
    }

    /** Số phút đi muộn (0 nếu không muộn) - để hiển thị "muộn 20 phút" */
    @Transient
    public long getSoPhutMuon() {
        if (gioVao == null || caLamViec == null || caLamViec.getGioBatDau() == null) return 0;
        long phut = java.time.Duration.between(caLamViec.getGioBatDau(), gioVao).toMinutes();
        return Math.max(phut, 0);
    }
}