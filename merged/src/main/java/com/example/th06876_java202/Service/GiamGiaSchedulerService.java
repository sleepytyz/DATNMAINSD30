package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.DotGiamGia;
import com.example.th06876_java202.Entity.GiamGia;
import com.example.th06876_java202.Repository.DotGiamGiaRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GiamGiaSchedulerService {

    @Autowired
    private GiamGiaService giamGiaService;

    @Autowired
    private DotGiamGiaService dotGiamGiaService;

    @Autowired
    private DotGiamGiaRepo dotGiamGiaRepo;

    // Chạy mỗi 30 giây
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void tuDongCapNhatTrangThai() {
        System.out.println("=== SCHEDULER START ===");
        System.out.println("Scheduler đang chạy kiểm tra trạng thái lúc: " + LocalDateTime.now());

        // Cập nhật cho GiamGia
        capNhatTrangThaiGiamGia();

        // Cập nhật cho DotGiamGia
        capNhatTrangThaiDotGiamGia();

        System.out.println("=== SCHEDULER END ===");
    }

    // ===== CẬP NHẬT TRẠNG THÁI CHO GIAM GIA =====
    private void capNhatTrangThaiGiamGia() {
        try {
            List<GiamGia> danhSach = giamGiaService.getGiamGia3();
            LocalDateTime now = LocalDateTime.now();
            int updatedCount = 0;

            for (GiamGia gg : danhSach) {
                // Bỏ qua các chương trình đã huỷ
                if ("Đã huỷ".equals(gg.getTrangThai())) {
                    System.out.println("Bỏ qua chương trình đã huỷ: " + gg.getMaGiamGia());
                    continue;
                }

                // Bỏ qua các chương trình đã ngừng hoạt động thủ công
                if ("Ngừng hoạt động".equals(gg.getTrangThai())) {
                    System.out.println("Bỏ qua chương trình đã ngừng hoạt động thủ công: " + gg.getMaGiamGia());
                    continue;
                }

                String trangThaiMoi = tinhToanTrangThaiGiamGia(gg, now);

                // Chỉ cập nhật khi trạng thái thay đổi
                if (!trangThaiMoi.equals(gg.getTrangThai())) {
                    giamGiaService.capNhatTrangThaiChoScheduler(trangThaiMoi, gg.getMaGiamGia());
                    updatedCount++;
                    System.out.println("Đã cập nhật GiamGia " + gg.getMaGiamGia() +
                            " từ '" + gg.getTrangThai() + "' sang '" + trangThaiMoi + "'");
                }
            }

            if (updatedCount > 0) {
                System.out.println("Đã cập nhật " + updatedCount + " chương trình giảm giá");
            }

        } catch (Exception e) {
            System.out.println("Lỗi khi cập nhật GiamGia: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== CẬP NHẬT TRẠNG THÁI CHO ĐỢT GIẢM GIÁ =====
    private void capNhatTrangThaiDotGiamGia() {
        try {
            List<DotGiamGia> danhSach = dotGiamGiaService.getAll();
            LocalDateTime now = LocalDateTime.now();
            int updatedCount = 0;

            for (DotGiamGia dgg : danhSach) {
                // Bỏ qua các đợt đã huỷ
                if ("Đã huỷ".equals(dgg.getTrangThai())) {
                    System.out.println("Bỏ qua đợt giảm giá đã huỷ: " + dgg.getMaGiamGia());
                    continue;
                }

                // Bỏ qua các đợt đã ngừng hoạt động thủ công
                if ("Ngừng hoạt động".equals(dgg.getTrangThai())) {
                    System.out.println("Bỏ qua đợt giảm giá đã ngừng hoạt động thủ công: " + dgg.getMaGiamGia());
                    continue;
                }

                String trangThaiMoi = tinhToanTrangThaiDotGiamGia(dgg, now);

                // Chỉ cập nhật khi trạng thái thay đổi
                if (!trangThaiMoi.equals(dgg.getTrangThai())) {
                    dgg.setTrangThai(trangThaiMoi);
                    dotGiamGiaService.save(dgg);
                    updatedCount++;
                    System.out.println("Đã cập nhật DotGiamGia " + dgg.getMaGiamGia() +
                            " từ '" + dgg.getTrangThai() + "' sang '" + trangThaiMoi + "'");
                }
            }

            if (updatedCount > 0) {
                System.out.println("Đã cập nhật " + updatedCount + " đợt giảm giá");
            }

        } catch (Exception e) {
            System.out.println("Lỗi khi cập nhật DotGiamGia: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== TÍNH TOÁN TRẠNG THÁI CHO GIAM GIA =====
    private String tinhToanTrangThaiGiamGia(GiamGia gg, LocalDateTime now) {
        // Giữ nguyên trạng thái đã huỷ
        if ("Đã huỷ".equals(gg.getTrangThai())) {
            return "Đã huỷ";
        }

        // Giữ nguyên trạng thái ngừng hoạt động (không tự động đổi lại)
        if ("Ngừng hoạt động".equals(gg.getTrangThai())) {
            return "Ngừng hoạt động";
        }

        // Kiểm tra null
        if (gg.getNgayBatDau() == null || gg.getNgayKetThuc() == null) {
            return "Ngừng hoạt động";
        }

        // Tính toán trạng thái dựa trên ngày tháng
        if (now.isBefore(gg.getNgayBatDau())) {
            return "Sắp hoạt động";
        }
        if (now.isAfter(gg.getNgayKetThuc())) {
            return "Ngừng hoạt động";
        }
        return "Hoạt động";
    }

    // ===== TÍNH TOÁN TRẠNG THÁI CHO ĐỢT GIẢM GIÁ =====
    private String tinhToanTrangThaiDotGiamGia(DotGiamGia dgg, LocalDateTime now) {
        // Giữ nguyên trạng thái đã huỷ
        if ("Đã huỷ".equals(dgg.getTrangThai())) {
            return "Đã huỷ";
        }

        // Giữ nguyên trạng thái ngừng hoạt động (không tự động đổi lại)
        if ("Ngừng hoạt động".equals(dgg.getTrangThai())) {
            return "Ngừng hoạt động";
        }

        // Kiểm tra null (NgayBatDau và NgayKetThuc là LocalDate trong DotGiamGia)
        if (dgg.getNgayBatDau() == null || dgg.getNgayKetThuc() == null) {
            return "Ngừng hoạt động";
        }

        // Chuyển LocalDate sang LocalDateTime để so sánh
        LocalDateTime ngayBatDau = dgg.getNgayBatDau().atStartOfDay();
        LocalDateTime ngayKetThuc = dgg.getNgayKetThuc().atTime(23, 59, 59);

        // Tính toán trạng thái dựa trên ngày tháng
        if (now.isBefore(ngayBatDau)) {
            return "Sắp hoạt động";
        }
        if (now.isAfter(ngayKetThuc)) {
            return "Ngừng hoạt động";
        }
        return "Hoạt động";
    }

    // ===== METHOD ĐỂ CHẠY THỦ CÔNG (TÙY CHỌN) =====
    public void runManualUpdate() {
        tuDongCapNhatTrangThai();
    }
}