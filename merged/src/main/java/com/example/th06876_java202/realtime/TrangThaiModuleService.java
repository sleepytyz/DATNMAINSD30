package com.example.th06876_java202.realtime;

import com.example.th06876_java202.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Theo dõi TRẠNG THÁI HOẠT ĐỘNG của từng module trong hệ thống và phát sang
 * màn quản lý theo thời gian thực (kèm API cho lần tải trang đầu tiên).
 *
 * Trạng thái mỗi module: HOAT_DONG (xanh) | CANH_BAO (vàng) | SU_CO (đỏ),
 * kèm số liệu nghiệp vụ để nhân viên nắm nhanh tình hình.
 */
@Service
@RequiredArgsConstructor
public class TrangThaiModuleService {

    public static final String HOAT_DONG = "HOAT_DONG";
    public static final String CANH_BAO  = "CANH_BAO";
    public static final String SU_CO     = "SU_CO";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    private final HoaDonRepo hoaDonRepo;
    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final GiamGiaRepository giamGiaRepository;
    private final DotGiamGiaRepo dotGiamGiaRepo;
    private final DanhGiaRepository danhGiaRepository;
    private final KhachHangRepository khachHangRepository;
    private final ThongBaoRealtimeService thongBaoRealtimeService;

    /** Kết quả lần kiểm tra gần nhất (trả nhanh cho API khi tải trang). */
    private volatile Map<String, Object> ketQuaGanNhat = new HashMap<>();

    /**
     * Tính trạng thái tất cả module. Được gọi định kỳ và khi có sự kiện quan trọng
     * (đơn mới, đổi trạng thái đơn) để bảng trạng thái luôn "sống".
     */
    public synchronized Map<String, Object> tinhTrangThaiTatCa() {
        List<Map<String, Object>> dsModule = new ArrayList<>();
        boolean dbLoi = false;

        // ---------- Module: Cơ sở dữ liệu ----------
        long tongDon = 0;
        try {
            tongDon = hoaDonRepo.count();
            dsModule.add(module("csdl", "Cơ sở dữ liệu", "bi-database-check", HOAT_DONG,
                    "Kết nối SQL Server bình thường", Map.of("tongHoaDon", tongDon)));
        } catch (Exception e) {
            dbLoi = true;
            dsModule.add(module("csdl", "Cơ sở dữ liệu", "bi-database-x", SU_CO,
                    "Không truy vấn được CSDL: " + gon(e.getMessage()), Map.of()));
        }

        // ---------- Module: Website bán hàng (Storefront) ----------
        try {
            long spDangBan = sanPhamRepository.countByTrangThai(true);
            String tt = spDangBan > 0 ? HOAT_DONG : CANH_BAO;
            dsModule.add(module("storefront", "Website bán hàng", "bi-shop", tt,
                    spDangBan > 0 ? ("Đang mở bán " + spDangBan + " sản phẩm")
                                  : "Không có sản phẩm nào đang mở bán",
                    Map.of("sanPhamDangBan", spDangBan)));
        } catch (Exception e) {
            dsModule.add(module("storefront", "Website bán hàng", "bi-shop", SU_CO,
                    "Lỗi truy vấn sản phẩm: " + gon(e.getMessage()), Map.of()));
        }

        // ---------- Module: Đơn hàng online ----------
        try {
            long choXacNhan = hoaDonRepo.countByTrangThai("Chờ xác nhận");
            long dangGiao = hoaDonRepo.countByTrangThai("Đang giao");
            String tt = choXacNhan >= 10 ? CANH_BAO : HOAT_DONG;
            String moTa = choXacNhan > 0
                    ? (choXacNhan + " đơn đang chờ xác nhận" + (choXacNhan >= 10 ? " — cần xử lý gấp" : ""))
                    : "Không có đơn nào chờ xử lý";
            Map<String, Object> soLieu = new HashMap<>();
            soLieu.put("choXacNhan", choXacNhan);
            soLieu.put("dangGiao", dangGiao);
            dsModule.add(module("donhang", "Đơn hàng online", "bi-receipt", tt, moTa, soLieu));
        } catch (Exception e) {
            dsModule.add(module("donhang", "Đơn hàng online", "bi-receipt", SU_CO,
                    "Lỗi truy vấn đơn hàng: " + gon(e.getMessage()), Map.of()));
        }

        // ---------- Module: Kho hàng ----------
        try {
            long sapHet = sanPhamChiTietRepository.demBienTheSapHet(ThongBaoRealtimeService.NGUONG_SAP_HET);
            long hetHang = sanPhamChiTietRepository.demBienTheHetHang();
            String tt = hetHang > 0 ? CANH_BAO : (sapHet > 0 ? CANH_BAO : HOAT_DONG);
            String moTa;
            if (hetHang > 0) moTa = hetHang + " biến thể HẾT HÀNG, " + sapHet + " biến thể sắp hết";
            else if (sapHet > 0) moTa = sapHet + " biến thể sắp hết hàng (≤ " + ThongBaoRealtimeService.NGUONG_SAP_HET + ")";
            else moTa = "Tồn kho ổn định";
            Map<String, Object> soLieu = new HashMap<>();
            soLieu.put("sapHet", sapHet);
            soLieu.put("hetHang", hetHang);
            dsModule.add(module("khohang", "Kho hàng", "bi-box-seam", tt, moTa, soLieu));
        } catch (Exception e) {
            dsModule.add(module("khohang", "Kho hàng", "bi-box-seam", SU_CO,
                    "Lỗi truy vấn tồn kho: " + gon(e.getMessage()), Map.of()));
        }

        // ---------- Module: Khuyến mãi & Voucher ----------
        try {
            long voucherHoatDong = giamGiaRepository.findAll().stream()
                    .filter(g -> "Hoạt động".equals(g.getTrangThai())).count();
            long dotGiamGia = dotGiamGiaRepo.findAll().stream()
                    .filter(d -> "Hoạt động".equals(d.getTrangThai()) || "Đang hoạt động".equals(d.getTrangThai()))
                    .count();
            Map<String, Object> soLieu = new HashMap<>();
            soLieu.put("voucherHoatDong", voucherHoatDong);
            soLieu.put("dotGiamGiaHoatDong", dotGiamGia);
            dsModule.add(module("khuyenmai", "Khuyến mãi & Voucher", "bi-ticket-perforated", HOAT_DONG,
                    voucherHoatDong + " voucher, " + dotGiamGia + " đợt giảm giá đang chạy", soLieu));
        } catch (Exception e) {
            dsModule.add(module("khuyenmai", "Khuyến mãi & Voucher", "bi-ticket-perforated", SU_CO,
                    "Lỗi truy vấn khuyến mãi: " + gon(e.getMessage()), Map.of()));
        }

        // ---------- Module: Đánh giá sản phẩm ----------
        try {
            long tongDanhGia = danhGiaRepository.count();
            dsModule.add(module("danhgia", "Đánh giá sản phẩm", "bi-star-half", HOAT_DONG,
                    "Đã ghi nhận " + tongDanhGia + " đánh giá từ khách hàng", Map.of("tongDanhGia", tongDanhGia)));
        } catch (Exception e) {
            dsModule.add(module("danhgia", "Đánh giá sản phẩm", "bi-star-half", SU_CO,
                    "Lỗi truy vấn đánh giá: " + gon(e.getMessage()), Map.of()));
        }

        // ---------- Module: Khách hàng & Tài khoản ----------
        try {
            long tongKhach = khachHangRepository.count();
            dsModule.add(module("khachhang", "Khách hàng & Tài khoản", "bi-people", HOAT_DONG,
                    tongKhach + " khách hàng trong hệ thống", Map.of("tongKhachHang", tongKhach)));
        } catch (Exception e) {
            dsModule.add(module("khachhang", "Khách hàng & Tài khoản", "bi-people", SU_CO,
                    "Lỗi truy vấn khách hàng: " + gon(e.getMessage()), Map.of()));
        }

        // ---------- Module: Kết nối thời gian thực ----------
        dsModule.add(module("realtime", "Kết nối thời gian thực", "bi-broadcast", HOAT_DONG,
                "Kênh WebSocket đang phát (bạn nhận được bảng này nghĩa là kênh hoạt động)", Map.of()));

        // ---------- Tổng hợp ----------
        long soSuCo = dsModule.stream().filter(m -> SU_CO.equals(m.get("trangThai"))).count();
        long soCanhBao = dsModule.stream().filter(m -> CANH_BAO.equals(m.get("trangThai"))).count();
        String tongThe = dbLoi || soSuCo > 0 ? SU_CO : (soCanhBao > 0 ? CANH_BAO : HOAT_DONG);

        Map<String, Object> ketQua = new LinkedHashMap<>();
        ketQua.put("loai", "TRANG_THAI_MODULE");
        ketQua.put("tongThe", tongThe);
        ketQua.put("soSuCo", soSuCo);
        ketQua.put("soCanhBao", soCanhBao);
        ketQua.put("dsModule", dsModule);
        ketQua.put("thoiGianKiemTra", LocalDateTime.now().format(FMT));

        this.ketQuaGanNhat = ketQua;
        return ketQua;
    }

    /** Kết quả gần nhất (không truy vấn lại) — dùng cho API tải trang. */
    public Map<String, Object> layKetQuaGanNhat() {
        if (ketQuaGanNhat == null || ketQuaGanNhat.isEmpty()) {
            return tinhTrangThaiTatCa();
        }
        return ketQuaGanNhat;
    }

    /** Kiểm tra và phát trạng thái module định kỳ 30 giây / lần lên kênh realtime. */
    @Scheduled(fixedDelay = 30_000, initialDelay = 15_000)
    public void phatDinhKy() {
        try {
            Map<String, Object> kq = tinhTrangThaiTatCa();
            thongBaoRealtimeService.phatTrangThaiModule(kq);
        } catch (Exception e) {
            System.err.println("[Realtime] Lỗi phát trạng thái module: " + e.getMessage());
        }
    }

    /** Phát ngay lập tức (gọi sau các sự kiện lớn: đơn mới, đổi trạng thái đơn). */
    /**
     * Phát trạng thái module cho bảng giám sát.
     *
     * QUAN TRỌNG — vì sao phải hoãn tới sau khi giao dịch commit:
     * hàm này chạy một loạt truy vấn thống kê (đếm hoá đơn, sản phẩm, đánh giá...) và
     * mỗi truy vấn được bọc try-catch để bảng giám sát vẫn hiện dù CSDL trục trặc.
     * Nếu gọi nó GIỮA một giao dịch nghiệp vụ (đặt hàng, huỷ đơn, xác nhận...) mà một
     * truy vấn thống kê ném lỗi, Spring đã đánh dấu giao dịch là "rollback-only" trước
     * khi try-catch kịp nuốt lỗi; nghiệp vụ vẫn chạy tiếp bình thường nhưng đến lúc
     * commit thì bung UnexpectedRollbackException ("Transaction silently rolled back")
     * và người dùng nhận trang lỗi 500 dù thao tác đã hoàn tất.
     *
     * Vì đây chỉ là số liệu GIÁM SÁT, hoãn tới sau commit vừa an toàn tuyệt đối cho
     * nghiệp vụ, vừa cho số liệu chính xác hơn (phản ánh dữ liệu đã lưu thật).
     */
    public void phatNgay() {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            phatNgayThucSu();   // giao dịch nghiệp vụ đã commit an toàn
                        }
                    });
            return;
        }
        phatNgayThucSu();
    }

    /** Tính và phát số liệu — luôn chạy NGOÀI giao dịch nghiệp vụ. */
    private void phatNgayThucSu() {
        try {
            Map<String, Object> kq = tinhTrangThaiTatCa();
            thongBaoRealtimeService.phatTrangThaiModule(kq);
        } catch (Exception e) {
            System.err.println("[Realtime] Lỗi phát trạng thái module: " + e.getMessage());
        }
    }

    private Map<String, Object> module(String ma, String ten, String icon, String trangThai,
                                        String moTa, Map<String, ?> soLieu) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ma", ma);
        m.put("ten", ten);
        m.put("icon", icon);
        m.put("trangThai", trangThai);
        m.put("moTa", moTa);
        m.put("soLieu", soLieu);
        return m;
    }

    private String gon(String s) {
        if (s == null) return "";
        return s.length() > 120 ? s.substring(0, 120) + "…" : s;
    }
}
