package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PHIÊN BÁN HÀNG (ca quỹ) - quản lý dòng tiền tại quầy theo từng ca.
 *
 * Luồng nghiệp vụ:
 *   1. MỞ CA : nhân viên đếm tiền trong két -> nhập TienDauCa -> phiên chuyển sang DANG_MO,
 *              lúc này mới được phép bán hàng.
 *   2. TRONG CA: mọi hoá đơn thanh toán được gắn vào phiên đang mở của nhân viên đó.
 *   3. ĐÓNG CA: nhân viên đếm lại tiền trong két -> nhập TienCuoiCaThucTe.
 *              Hệ thống đối soát:
 *                 TienDuKien = TienDauCa + DoanhThuTienMat
 *                 ChenhLech  = TienCuoiCaThucTe - TienDuKien
 *              (ChenhLech > 0: thừa quỹ | < 0: thiếu quỹ | = 0: khớp)
 *
 * Bảng GiaoCa cũ vẫn giữ nguyên (dùng cho biên bản bàn giao giữa 2 nhân viên),
 * bảng này bổ sung riêng cho luồng mở/đóng ca + đối soát quỹ.
 */
@Entity
@Table(name = "PhienBanHang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhienBanHang {

    /** Trạng thái phiên */
    public static final String DANG_MO = "DANG_MO";
    public static final String DA_DONG = "DA_DONG";

    // ================== CẤU HÌNH NGHIỆP VỤ (dễ chỉnh) ==================
    /**
     * Thời gian tối thiểu (phút) phải làm trước khi được ĐÓNG CA.
     * Chống việc "mở ca xong đóng luôn" (tùy tiện). Đặt 30 phút.
     */
    public static final long PHUT_TOI_THIEU_TRUOC_KHI_DONG = 30;

    /**
     * Ngưỡng THIẾU QUỸ (đồng). Nếu đếm ra thiếu hơn số này so với dự kiến,
     * bắt buộc nhân viên phải nhập lý do và ca sẽ bị đánh dấu CHỜ ADMIN DUYỆT.
     * Nhân viên không có nghiệp vụ tự lấy tiền -> mọi khoản thiếu đều phải giải trình.
     */
    public static final BigDecimal NGUONG_THIEU_QUY_PHAI_DUYET = new BigDecimal("20000");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaPhien")
    private Integer maPhien;

    /** Nhân viên phụ trách phiên (người mở ca) */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "MaNhanVien", nullable = false)
    private NhanVien nhanVien;

    /** Ca làm việc tương ứng (Sáng/Chiều/Tối) - lấy từ lịch chấm công hôm đó */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "MaCa")
    private CaLamViec caLamViec;

    /**
     * Bản ghi chấm công gắn với phiên này.
     * Mở ca -> tự check-in bản ghi này; Đóng ca -> tự check-out nó.
     */
    @Column(name = "MaChamCong")
    private Integer maChamCong;

    @Column(name = "ThoiGianMoCa", nullable = false)
    private LocalDateTime thoiGianMoCa;

    @Column(name = "ThoiGianDongCa")
    private LocalDateTime thoiGianDongCa;

    /** Tiền mặt thực tế trong két lúc BẮT ĐẦU ca (nhân viên tự đếm & nhập) */
    @Column(name = "TienDauCa", nullable = false, precision = 18, scale = 2)
    private BigDecimal tienDauCa = BigDecimal.ZERO;

    /** Tiền mặt thực tế trong két lúc KẾT THÚC ca (nhân viên tự đếm & nhập) */
    @Column(name = "TienCuoiCaThucTe", precision = 18, scale = 2)
    private BigDecimal tienCuoiCaThucTe;

    /** Doanh thu TIỀN MẶT phát sinh trong ca (hệ thống tự tính từ hoá đơn) */
    @Column(name = "DoanhThuTienMat", precision = 18, scale = 2)
    private BigDecimal doanhThuTienMat = BigDecimal.ZERO;

    /** Doanh thu CHUYỂN KHOẢN/thẻ/QR trong ca (không nằm trong két) */
    @Column(name = "DoanhThuChuyenKhoan", precision = 18, scale = 2)
    private BigDecimal doanhThuChuyenKhoan = BigDecimal.ZERO;

    /** Số hoá đơn đã bán trong ca */
    @Column(name = "SoHoaDon")
    private Integer soHoaDon = 0;

    /** Tiền dự kiến phải có trong két = TienDauCa + DoanhThuTienMat */
    @Column(name = "TienDuKien", precision = 18, scale = 2)
    private BigDecimal tienDuKien;

    /** Chênh lệch = TienCuoiCaThucTe - TienDuKien (âm = thiếu, dương = thừa) */
    @Column(name = "ChenhLech", precision = 18, scale = 2)
    private BigDecimal chenhLech;

    /** DANG_MO | DA_DONG */
    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = DANG_MO;

    @Column(name = "GhiChu", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    // ================== KIỂM SOÁT THIẾU QUỸ / DUYỆT ==================
    /**
     * Ca này có bị thiếu quỹ vượt ngưỡng khi đóng không.
     * true = cần admin xem xét (nhân viên đã phải nhập lý do khi đóng ca).
     */
    @Column(name = "CanDuyet")
    private Boolean canDuyet = false;

    /**
     * Admin đã duyệt/xử lý khoản thiếu quỹ chưa.
     * null hoặc false = chưa duyệt; true = admin đã xác nhận xử lý.
     */
    @Column(name = "DaDuyet")
    private Boolean daDuyet = false;

    /** Lý do thiếu quỹ do nhân viên khai khi đóng ca (bắt buộc nếu thiếu > ngưỡng) */
    @Column(name = "LyDoThieuQuy", columnDefinition = "NVARCHAR(MAX)")
    private String lyDoThieuQuy;

    // ================== CHỐT / THU TIỀN KÉT (chỉ Admin) ==================
    /**
     * Số tiền admin đã THU khỏi két tại thời điểm chốt ca này.
     * Chỉ admin mới được rút tiền khỏi két -> mỗi lần thu đều để lại vết ở đây.
     */
    @Column(name = "TienDaThu", precision = 18, scale = 2)
    private BigDecimal tienDaThu;

    @Column(name = "NguoiThuTien", length = 50)
    private String nguoiThuTien;

    @Column(name = "ThoiGianThuTien")
    private LocalDateTime thoiGianThuTien;

    /** Tiện ích: phiên đang mở hay không */
    @Transient
    public boolean isDangMo() {
        return DANG_MO.equals(this.trangThai);
    }

    /** Tiện ích: ca này đang chờ admin duyệt khoản thiếu quỹ */
    @Transient
    public boolean isChoDuyet() {
        return Boolean.TRUE.equals(canDuyet) && !Boolean.TRUE.equals(daDuyet);
    }
}
