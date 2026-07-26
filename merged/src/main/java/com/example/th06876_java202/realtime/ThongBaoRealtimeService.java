package com.example.th06876_java202.realtime;

import com.example.th06876_java202.Entity.HoaDon;
import com.example.th06876_java202.Entity.HoaDonChiTiet;
import com.example.th06876_java202.Entity.SanPhamChiTiet;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trung tâm phát THÔNG BÁO THỜI GIAN THỰC (WebSocket/STOMP) kết nối
 * Website bán hàng online ⇄ Website quản lý bán hàng.
 *
 * Nguyên tắc: mọi thông điệp chỉ được gửi SAU KHI giao dịch CSDL commit thành công
 * (nếu đang trong transaction) để phía nhận không bao giờ thấy dữ liệu "chưa tồn tại".
 */
@Service
@RequiredArgsConstructor
public class ThongBaoRealtimeService {

    public static final String TOPIC_QUANLY_DON_HANG = "/topic/quanly/don-hang";
    public static final String TOPIC_QUANLY_TON_KHO  = "/topic/quanly/ton-kho";
    public static final String TOPIC_QUANLY_MODULE   = "/topic/quanly/module";
    public static final String TOPIC_DON_HANG_PREFIX = "/topic/don-hang/";

    /** Ngưỡng tồn kho coi là "sắp hết" để cảnh báo bên quản lý. */
    public static final int NGUONG_SAP_HET = 5;

    private static final DateTimeFormatter FMT_GIO = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    private final SimpMessagingTemplate messagingTemplate;

    // =====================================================================
    // 1) ĐƠN HÀNG ONLINE MỚI  →  bên Quản lý (hoá đơn chờ xác nhận)
    // =====================================================================

    /**
     * Khách vừa đặt hàng online thành công: đẩy "hoá đơn chờ xác nhận" sang màn quản lý
     * kèm số lượng sản phẩm, tổng tiền để nhân viên xử lý ngay.
     */
    public void donHangOnlineMoi(HoaDon hoaDon, List<HoaDonChiTiet> chiTiet) {
        if (hoaDon == null) return;

        int tongSoLuong = 0;
        int soDong = 0;
        if (chiTiet != null) {
            soDong = chiTiet.size();
            for (HoaDonChiTiet ct : chiTiet) {
                tongSoLuong += ct.getSoLuong() != null ? ct.getSoLuong() : 0;
            }
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("loai", "DON_HANG_MOI");
        msg.put("maHoaDon", hoaDon.getMaHoaDon());
        msg.put("tenKhachHang", hoaDon.getMaKhachHang() != null ? hoaDon.getMaKhachHang().getHoTen() : "Khách lẻ");
        msg.put("soDienThoai", hoaDon.getMaKhachHang() != null ? hoaDon.getMaKhachHang().getSdt() : "");
        msg.put("tongTien", giaTri(hoaDon.getTongTien()));
        msg.put("tienShip", giaTri(hoaDon.getTienShip()));
        msg.put("soLuongSanPham", tongSoLuong);
        msg.put("soDongHang", soDong);
        msg.put("phuongThucThanhToan", hoaDon.getPhuongThucThanhToan());
        msg.put("trangThai", hoaDon.getTrangThai());
        msg.put("loaiBan", hoaDon.getLoaiBan());
        msg.put("diaChiGiaoHang", hoaDon.getDiaChiGiaoHang());
        msg.put("thoiGian", thoiGianHienTai());

        guiSauCommit(TOPIC_QUANLY_DON_HANG, msg);
    }

    // =====================================================================
    // 2) ĐỔI TRẠNG THÁI ĐƠN  →  cả 2 phía cùng cập nhật
    // =====================================================================

    /**
     * Khi trạng thái đơn thay đổi (xác nhận / giao hàng / hoàn tất / huỷ — từ bất kỳ phía nào):
     *  - Bên quản lý: cập nhật bảng đơn hàng + các thẻ đếm trạng thái.
     *  - Bên khách: trang "Theo dõi đơn hàng" & "Đơn hàng của tôi" tự nhảy trạng thái, không cần F5.
     */
    public void trangThaiDonThayDoi(HoaDon hoaDon, String trangThaiCu, String nguoiThucHien) {
        if (hoaDon == null) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("loai", "DOI_TRANG_THAI");
        msg.put("maHoaDon", hoaDon.getMaHoaDon());
        msg.put("trangThaiCu", trangThaiCu);
        msg.put("trangThaiMoi", hoaDon.getTrangThai());
        msg.put("nguoiThucHien", nguoiThucHien);
        msg.put("loaiBan", hoaDon.getLoaiBan());
        msg.put("tongTien", giaTri(hoaDon.getTongTien()));
        msg.put("thoiGian", thoiGianHienTai());

        guiSauCommit(TOPIC_QUANLY_DON_HANG, msg);
        guiSauCommit(TOPIC_DON_HANG_PREFIX + hoaDon.getMaHoaDon(), msg);
    }

    // =====================================================================
    // 3) CẢNH BÁO TỒN KHO  →  bên Quản lý
    // =====================================================================

    /** Sau khi trừ kho vì đơn online, nếu biến thể sắp hết / hết hàng thì báo cho quản lý. */
    public void kiemTraVaCanhBaoTonKho(SanPhamChiTiet spct) {
        if (spct == null) return;
        int ton = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
        if (ton > NGUONG_SAP_HET) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("loai", ton <= 0 ? "HET_HANG" : "SAP_HET_HANG");
        msg.put("maSanPhamChiTiet", spct.getMaSanPhamChiTiet());
        msg.put("tenSanPham", spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : spct.getMaSanPhamChiTiet());
        msg.put("mauSac", spct.getMauSac() != null ? spct.getMauSac().getTenMauSac() : "");
        msg.put("kichThuoc", spct.getKichThuoc() != null ? spct.getKichThuoc().getTenKichThuoc() : "");
        msg.put("soLuongTon", ton);
        msg.put("thoiGian", thoiGianHienTai());

        guiSauCommit(TOPIC_QUANLY_TON_KHO, msg);
    }

    // =====================================================================
    // 4) TRẠNG THÁI MODULE  →  bên Quản lý (bảng "sức khoẻ hệ thống")
    // =====================================================================

    /** Phát trạng thái hoạt động của từng module (được TrangThaiModuleService gọi định kỳ / khi có biến động). */
    public void phatTrangThaiModule(Map<String, Object> trangThai) {
        if (trangThai == null) return;
        guiSauCommit(TOPIC_QUANLY_MODULE, trangThai);
    }

    // =====================================================================
    // Hạ tầng gửi
    // =====================================================================

    /**
     * Gửi thông điệp SAU KHI transaction hiện tại commit; nếu không có transaction thì gửi ngay.
     * Nhờ đó phía quản lý bấm vào thông báo là chắc chắn thấy đơn trong CSDL.
     */
    private void guiSauCommit(String dich, Object payload) {
        Runnable gui = () -> {
            try {
                messagingTemplate.convertAndSend(dich, payload);
            } catch (Exception e) {
                // Realtime là kênh phụ trợ: lỗi gửi không được phép ảnh hưởng nghiệp vụ chính
                System.err.println("[Realtime] Không gửi được thông báo tới " + dich + ": " + e.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    gui.run();
                }
            });
        } else {
            gui.run();
        }
    }

    private String thoiGianHienTai() {
        return LocalDateTime.now().format(FMT_GIO);
    }

    private BigDecimal giaTri(BigDecimal b) {
        return b != null ? b : BigDecimal.ZERO;
    }
}
