package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.GiamGia;
import com.example.th06876_java202.Entity.GiamGiaChiTiet;
import com.example.th06876_java202.Entity.GiamGiaChiTietId;
import com.example.th06876_java202.Repository.GiamGiaChiTietRepo;
import com.example.th06876_java202.Repository.GiamGiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class GiamGiaService {

    /**
     * Voucher/đợt giảm giá có đang hiệu lực không — KHÔNG so khớp cứng một chuỗi,
     * vì DB có thể lưu "Đang hoạt động" hoặc "Hoạt động". Coi là ngừng khi trạng thái
     * chứa "ngừng/khoá/huỷ"; các trạng thái còn lại (kể cả rỗng) xem như đang chạy.
     */
    /** Định dạng tiền kiểu Việt Nam: 500000 -> "500.000₫". */
    private static String tien(java.math.BigDecimal v) {
        if (v == null) return "0₫";
        java.text.DecimalFormatSymbols kyHieu = new java.text.DecimalFormatSymbols(java.util.Locale.US);
        kyHieu.setGroupingSeparator('.');
        return new java.text.DecimalFormat("#,###", kyHieu).format(v) + "\u20AB";
    }

    /** Định dạng ngày giờ ngắn gọn: "08:30 25/07/2026". */
    private static String ngay(java.time.LocalDateTime t) {
        return t == null ? "" : t.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }

    public static boolean dangHoatDong(String trangThai) {
        if (trangThai == null) return true;
        String t = trangThai.trim().toLowerCase();
        return !(t.contains("ngừng") || t.contains("ngưng")
                || t.contains("khoá") || t.contains("khóa")
                || t.contains("huỷ") || t.contains("hủy")
                || t.contains("hết"));
    }

    @Autowired
    GiamGiaRepository giamGiaRepository;

    @Autowired
    GiamGiaChiTietRepo giamGiaChiTietRepo;

    private final Random random = new Random();

    @Transactional
    public void markVoucherAsUsed(String maKhachHang, String maGiamGia) {
        GiamGiaChiTietId id = new GiamGiaChiTietId(maKhachHang, maGiamGia);
        GiamGiaChiTiet ct = giamGiaChiTietRepo.findById(id).orElse(null);

        if (ct != null) {
            ct.setTrangThaiSuDung(1);
            giamGiaChiTietRepo.save(ct);
        }
    }
    public String generateMaGiamGia() {
        String code;
        boolean exists;
        int attempts = 0;
        int maxAttempts = 100;

        do {
            int randomNumber = 1000 + random.nextInt(9000);
            code = "GG" + randomNumber;
            exists = giamGiaRepository.existsById(code);
            attempts++;

            if (attempts > maxAttempts) {
                code = "GG" + System.currentTimeMillis();
                break;
            }
        } while (exists);

        return code;
    }

    public GiamGia save(GiamGia giamGia) {
        if (giamGia.getNgayTao() == null) {
            giamGia.setNgayTao(LocalDateTime.now());
        }
        return giamGiaRepository.save(giamGia);
    }

    public List<GiamGia> getGiamGia3() {
        return giamGiaRepository.findDanhSachCanCapNhat();
    }

    public List<GiamGia> getGiamGia1() {
        List<GiamGia> result = giamGiaRepository.findSoLuongVoucher();
        if (result != null) {
            for (GiamGia gg : result) {
                System.out.println("  - " + gg.getMaGiamGia() +
                        " | " + gg.getTenGiamGia() +
                        " | Trạng thái: " + gg.getTrangThai() +
                        " | SL: " + gg.getSoLuong() +
                        " | Vô hạn: " + gg.getIsVoHan());
            }
        }
        return result;
    }

    public List<GiamGia> getAllVouchers() {
        return giamGiaRepository.findAllVouchers();
    }

    public void giamSoLuongVoucher(String id){
        giamGiaRepository.giamSoLuongVoucher(id);
    }

    public Optional<GiamGia> getGiamGiaById(String id) {
        return giamGiaRepository.findById(id);
    }

    public List<GiamGia> timkiem(String keyword) {
        return giamGiaRepository.timkiem(keyword);
    }

    public List<GiamGia> loclg(String keyword) {
        return giamGiaRepository.getGiamGia(keyword);
    }

    public List<GiamGia> loctt(String keyword) {
        return giamGiaRepository.loctt(keyword);
    }

    public List<GiamGia> locng(LocalDateTime date, LocalDateTime time) {
        return giamGiaRepository.timkiemngay(date,time);
    }

    @Transactional
    public void activateVoucher(String id) {
        giamGiaRepository.activateVoucher(id);
    }

    public Page<GiamGia> getFilteredGiamGia(String kw, String tt, String lg, Integer loaiApDung,
                                            LocalDateTime start, LocalDateTime end, int page) {
        Pageable pageable = PageRequest.of(page, 5);
        return giamGiaRepository.filterAll(kw, tt, lg, loaiApDung, start, end, pageable);
    }

    public List<GiamGia> findAllFiltered(String kw, String tt, String lg, Integer loaiApDung,
                                         LocalDateTime start, LocalDateTime end) {
        return giamGiaRepository.findAllFiltered(kw, tt, lg, loaiApDung, start, end);
    }

    public String tinhToanTrangThai(GiamGia gg) {
        if (gg == null) return "Ngừng hoạt động";

        LocalDateTime now = LocalDateTime.now();

        if (!dangHoatDong(gg.getTrangThai())) {
            return gg.getTrangThai();
        }

        if (gg.getNgayBatDau() == null || gg.getNgayKetThuc() == null) {
            return "Ngừng hoạt động";
        }
        if (gg.getIsVoHan() != null && gg.getIsVoHan()) {
            if (now.isBefore(gg.getNgayBatDau())) {
                return "Sắp hoạt động";
            } else if (now.isAfter(gg.getNgayKetThuc())) {
                return "Ngừng hoạt động";
            } else {
                return "Hoạt động";
            }
        }
        if (gg.getSoLuong() == null || gg.getSoLuong() <= 0) {
            return "Hết lượt";
        }

        if (now.isBefore(gg.getNgayBatDau())) {
            return "Sắp hoạt động";
        } else if (now.isAfter(gg.getNgayKetThuc())) {
            return "Ngừng hoạt động";
        } else {
            return "Hoạt động";
        }
    }

    @Transactional
    public void capNhatTrangThaiChoScheduler(String trangThai, String id) {
        giamGiaRepository.updateTrangThai(trangThai, id);
    }

    @Transactional
    public void updateTrangThaiToStop(String id) {
        giamGiaRepository.updateTrangThaiToStop(id);
    }

    public Optional<GiamGia> findByTen(String tenGiamGia) {
        if (tenGiamGia == null || tenGiamGia.isBlank()) {
            return Optional.empty();
        }
        return giamGiaRepository.findActiveByTenGiamGiaIgnoreCase(tenGiamGia.trim());
    }

    /** Khớp GẦN ĐÚNG theo mã hoặc tên (phương án cuối khi gõ thiếu/thừa ký tự). */
    public Optional<GiamGia> timGanDung(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) return Optional.empty();
        return giamGiaRepository
                .findTop10ByMaGiamGiaContainingOrTenGiamGiaContaining(
                        tuKhoa.trim(), org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().findFirst();
    }

    public Optional<GiamGia> findByMa(String maGiamGia) {
        if (maGiamGia == null || maGiamGia.isBlank()) {
            return Optional.empty();
        }
        String ma = maGiamGia.trim();
        Optional<GiamGia> gg = giamGiaRepository.findByMaGiamGia(ma);
        if (gg.isPresent()) return gg;
        // Thử khớp không phân biệt hoa/thường (khách gõ gg001 / GG001)
        return giamGiaRepository.findByMaGiamGiaIgnoreCase(ma);
    }

    public String kiemTraVoucherHopLe(GiamGia gg, String maKhachHang, BigDecimal tongTienHang) {
        if (gg == null) return "Mã giảm giá không tồn tại.";
        // Voucher khả dụng nếu KHÔNG bị ngừng/khoá và ĐANG trong thời gian hiệu lực —
        // không so khớp cứng chuỗi trạng thái (DB có thể lưu "Đang hoạt động"/"Hoạt động").
        String tt = gg.getTrangThai() != null ? gg.getTrangThai().trim().toLowerCase() : "";
        if (tt.contains("ngừng") || tt.contains("khoá") || tt.contains("khóa") || tt.contains("huỷ") || tt.contains("hủy")) {
            return "Mã giảm giá hiện không khả dụng.";
        }
        java.time.LocalDateTime bayGio = java.time.LocalDateTime.now();
        if (gg.getNgayBatDau() != null && bayGio.isBefore(gg.getNgayBatDau())) {
            return "Mã này bắt đầu áp dụng từ " + ngay(gg.getNgayBatDau()) + " — bạn vui lòng quay lại sau.";
        }
        if (gg.getNgayKetThuc() != null && bayGio.isAfter(gg.getNgayKetThuc())) {
            return "Mã này đã hết hạn lúc " + ngay(gg.getNgayKetThuc()) + ".";
        }

        if ((gg.getIsVoHan() == null || !gg.getIsVoHan()) &&
                (gg.getSoLuong() == null || gg.getSoLuong() <= 0)) {
            return "Mã này đã hết lượt sử dụng.";
        }

        if (gg.getDonToiThieu() != null && tongTienHang.compareTo(gg.getDonToiThieu()) < 0) {
            java.math.BigDecimal conThieu = gg.getDonToiThieu().subtract(tongTienHang);
            return "Mã này chỉ áp dụng cho đơn từ " + tien(gg.getDonToiThieu())
                    + ". Đơn của bạn đang " + tien(tongTienHang)
                    + " — cần mua thêm " + tien(conThieu) + " nữa.";
        }
        if (gg.getLoaiApDung() != null && gg.getLoaiApDung() == 2) {
            if (maKhachHang == null) return "Bạn cần đăng nhập để sử dụng mã giảm giá này.";
            GiamGiaChiTiet ct = giamGiaChiTietRepo
                    .findByKhachHang_MaKHAndGiamGia_MaGiamGia(maKhachHang, gg.getMaGiamGia())
                    .orElse(null);
            if (ct == null) return "Mã này chỉ dành riêng cho một số khách hàng — tài khoản của bạn chưa được tặng mã.";
            if (ct.getTrangThaiSuDung() != null && ct.getTrangThaiSuDung() == 1) return "Bạn đã sử dụng mã giảm giá này.";
        }
        return null;
    }

    public BigDecimal tinhSoTienGiam(GiamGia gg, BigDecimal tongTienHang) {
        if (gg == null) return BigDecimal.ZERO;
        BigDecimal soTienGiam;
        if ("PhanTram".equals(gg.getLoaiGiamGia())) {
            soTienGiam = tongTienHang.multiply(gg.getGiaTriGiam()).divide(BigDecimal.valueOf(100));
        } else {
            soTienGiam = gg.getGiaTriGiam();
        }
        if (gg.getGiamToiDa() != null && gg.getGiamToiDa().compareTo(BigDecimal.ZERO) > 0
                && soTienGiam.compareTo(gg.getGiamToiDa()) > 0) {
            soTienGiam = gg.getGiamToiDa();
        }
        if (soTienGiam.compareTo(tongTienHang) > 0) soTienGiam = tongTienHang;
        return soTienGiam;
    }

    public List<GiamGia> getVoucherCongKhai() {
        List<GiamGia> ketQua = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (GiamGia gg : giamGiaRepository.findAll()) {
            if (!dangHoatDong(gg.getTrangThai())) continue;

            if (gg.getIsVoHan() == null || !gg.getIsVoHan()) {
                if (gg.getSoLuong() == null || gg.getSoLuong() <= 0) continue;
            }
            if (gg.getNgayBatDau() != null && now.isBefore(gg.getNgayBatDau())) continue;
            if (gg.getNgayKetThuc() != null && now.isAfter(gg.getNgayKetThuc())) continue;

            Integer loaiApDung = gg.getLoaiApDung();
            if (loaiApDung == null || loaiApDung == 1) {
                ketQua.add(gg);
            }
        }

        System.out.println("✅ Voucher công khai: " + ketQua.size());
        return ketQua;
    }

    public List<GiamGia> getVoucherKhaDungChoKhachHang(String maKhachHang) {
        List<GiamGia> ketQua = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (GiamGia gg : giamGiaRepository.findAll()) {
            if (!dangHoatDong(gg.getTrangThai())) continue;
            if (gg.getIsVoHan() == null || !gg.getIsVoHan()) {
                if (gg.getSoLuong() == null || gg.getSoLuong() <= 0) continue;
            }
            if (gg.getNgayBatDau() != null && now.isBefore(gg.getNgayBatDau())) continue;
            if (gg.getNgayKetThuc() != null && now.isAfter(gg.getNgayKetThuc())) continue;
            Integer loaiApDung = gg.getLoaiApDung();
            if (loaiApDung != null && loaiApDung == 1) {
                ketQua.add(gg);
                continue;
            }
            if (loaiApDung != null && loaiApDung == 2) {
                if (maKhachHang != null) {
                    GiamGiaChiTiet ct = giamGiaChiTietRepo
                            .findByKhachHang_MaKHAndGiamGia_MaGiamGia(maKhachHang, gg.getMaGiamGia())
                            .orElse(null);
                    if (ct != null && (ct.getTrangThaiSuDung() == null || ct.getTrangThaiSuDung() == 0)) {
                        ketQua.add(gg);
                    }
                }
                continue;
            }
            if (loaiApDung == null) {
                ketQua.add(gg);
            }
        }

        System.out.println("✅ Tìm thấy " + ketQua.size() + " voucher khả dụng");
        return ketQua;
    }
    @Transactional
    public void danhDauDaSuDungChoKhachHang(String maKhachHang, String maGiamGia) {
        if (maKhachHang == null) return;
        giamGiaChiTietRepo.findByKhachHang_MaKHAndGiamGia_MaGiamGia(maKhachHang, maGiamGia)
                .ifPresent(ct -> {
                    ct.setTrangThaiSuDung(1);
                    giamGiaChiTietRepo.save(ct);
                });
    }

    @Transactional
    public void hoanLaiVoucher(String maGiamGia, String maKhachHang) {
        giamGiaRepository.findById(maGiamGia).ifPresent(gg -> {
            gg.setSoLuong(gg.getSoLuong() == null ? 1 : gg.getSoLuong() + 1);
            giamGiaRepository.save(gg);
        });
        if (maKhachHang != null) {
            giamGiaChiTietRepo.findByKhachHang_MaKHAndGiamGia_MaGiamGia(maKhachHang, maGiamGia)
                    .ifPresent(ct -> {
                        ct.setTrangThaiSuDung(0);
                        giamGiaChiTietRepo.save(ct);
                    });
        }
    }
    public GiamGia findById(String maGiamGia) {
        return giamGiaRepository.findById(maGiamGia).orElse(null);
    }

    public boolean kiemTraVoucherHopLeChoThanhToan(String maGiamGia, String maKhachHang) {
        GiamGia voucher = giamGiaRepository.findById(maGiamGia).orElse(null);
        if (voucher == null) {
            System.out.println("❌ Voucher không tồn tại: " + maGiamGia);
            return false;
        }

        if (!dangHoatDong(voucher.getTrangThai())) {
            System.out.println("❌ Voucher không hoạt động: " + voucher.getTrangThai());
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (voucher.getNgayBatDau() != null && now.isBefore(voucher.getNgayBatDau())) {
            System.out.println("❌ Voucher chưa đến ngày bắt đầu");
            return false;
        }
        if (voucher.getNgayKetThuc() != null && now.isAfter(voucher.getNgayKetThuc())) {
            System.out.println("❌ Voucher đã hết hạn");
            return false;
        }

        Integer loaiApDung = voucher.getLoaiApDung();

        if (loaiApDung != null && loaiApDung == 2) {
            if (maKhachHang == null || maKhachHang.isEmpty()) {
                System.out.println("❌ Voucher cá nhân cần mã khách hàng");
                return false;
            }

            // ⭐ KIỂM TRA CHI TIẾT VOUCHER CÁ NHÂN
            GiamGiaChiTiet ct = giamGiaChiTietRepo
                    .findByKhachHang_MaKHAndGiamGia_MaGiamGia(maKhachHang, maGiamGia)
                    .orElse(null);

            if (ct == null) {
                System.out.println("❌ Khách hàng " + maKhachHang + " không có voucher này");
                return false;
            }

            if (ct.getTrangThaiSuDung() != null && ct.getTrangThaiSuDung() == 1) {
                System.out.println("❌ Voucher đã được sử dụng!");
                return false;
            }

            System.out.println("✅ Voucher cá nhân hợp lệ - KH: " + maKhachHang);
            return true;
        }

        if (loaiApDung != null && loaiApDung == 1) {
            if (voucher.getIsVoHan() == null || !voucher.getIsVoHan()) {
                if (voucher.getSoLuong() == null || voucher.getSoLuong() <= 0) {
                    System.out.println("❌ Voucher công khai đã hết số lượng");
                    return false;
                }
            }
            System.out.println("✅ Voucher công khai hợp lệ");
            return true;
        }

        System.out.println("⚠️ Loại áp dụng không xác định: " + loaiApDung);
        return false;
    }

    @Transactional
    public void giamSoLuongVoucherChoThanhToan(String maGiamGia, String maKhachHang) {
        GiamGia voucher = giamGiaRepository.findById(maGiamGia).orElse(null);
        if (voucher == null) {
            System.out.println("⚠️ Không tìm thấy voucher: " + maGiamGia);
            return;
        }

        Integer loaiApDung = voucher.getLoaiApDung();
        if (loaiApDung != null && loaiApDung == 2) {
            if (maKhachHang == null || maKhachHang.isEmpty()) {
                System.out.println("❌ Không có mã khách hàng để giảm voucher cá nhân");
                return;
            }

            int updated = giamGiaChiTietRepo.updateTrangThaiDaSuDung(maGiamGia, maKhachHang);
            if (updated > 0) {
                System.out.println("✅ Đã đánh dấu voucher cá nhân đã dùng: " + maGiamGia + " - KH: " + maKhachHang);
            } else {
                System.out.println("⚠️ Không tìm thấy voucher cá nhân để đánh dấu: " + maGiamGia);
            }
            return;
        }

        if (loaiApDung != null && loaiApDung == 1) {
            if (voucher.getIsVoHan() == null || !voucher.getIsVoHan()) {
                if (voucher.getSoLuong() != null && voucher.getSoLuong() > 0) {
                    voucher.setSoLuong(voucher.getSoLuong() - 1);
                    giamGiaRepository.save(voucher);
                    System.out.println("✅ Đã giảm số lượng voucher công khai: " + maGiamGia + ", còn: " + voucher.getSoLuong());
                } else {
                    System.out.println("⚠️ Voucher công khai đã hết số lượng: " + maGiamGia);
                }
            } else {
                System.out.println("✅ Voucher vô hạn, không cần giảm số lượng: " + maGiamGia);
            }
        }
    }





}