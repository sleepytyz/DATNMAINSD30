package com.example.th06876_java202.Service;

import com.example.th06876_java202.config.GioVN;
import com.example.th06876_java202.Entity.CaLamViec;
import com.example.th06876_java202.Entity.ChamCong;
import com.example.th06876_java202.Entity.HoaDon;
import com.example.th06876_java202.Entity.NhanVien;
import com.example.th06876_java202.Entity.PhienBanHang;
import com.example.th06876_java202.Repository.ChamCongRepository;
import com.example.th06876_java202.Repository.HoaDonRepo;
import com.example.th06876_java202.Repository.PhienBanHangRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Nghiệp vụ GIAO CA / QUỸ TIỀN tại quầy.
 *
 * Nguyên tắc cốt lõi của dòng tiền:
 *      Tiền đầu ca + Doanh thu tiền mặt trong ca = Tiền cuối ca (dự kiến)
 *      Chênh lệch = Tiền cuối ca THỰC ĐẾM - Tiền cuối ca dự kiến
 */
@Service
@RequiredArgsConstructor
public class PhienBanHangService {

    private final PhienBanHangRepository phienRepo;
    private final HoaDonRepo hoaDonRepo;
    private final ChamCongRepository chamCongRepository;

    /** Chuỗi phương thức thanh toán được coi là TIỀN MẶT (nằm trong két) */
    private static final String TIEN_MAT = "Tiền mặt";

    // ==================================================================
    // TRUY VẤN TRẠNG THÁI
    // ==================================================================

    /** Phiên đang mở của nhân viên (null nếu chưa mở ca) */
    public PhienBanHang layPhienDangMo(String maNhanVien) {
        if (maNhanVien == null) return null;
        List<PhienBanHang> list = phienRepo.timPhienDangMo(maNhanVien);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Nhân viên đã mở ca chưa? -> điều kiện được phép bán hàng */
    public boolean dangTrongCa(String maNhanVien) {
        return layPhienDangMo(maNhanVien) != null;
    }

    /** Toàn bộ phiên đang mở (Admin theo dõi) */
    public List<PhienBanHang> layTatCaPhienDangMo() {
        return phienRepo.timTatCaPhienDangMo();
    }

    /** Lịch sử phiên của nhân viên */
    public List<PhienBanHang> lichSu(String maNhanVien) {
        return phienRepo.lichSuTheoNhanVien(maNhanVien);
    }

    /** Lịch sử phiên theo khoảng (Admin đối soát) */
    public List<PhienBanHang> lichSuTheoKhoang(LocalDateTime tuNgay, LocalDateTime denNgay) {
        return phienRepo.lichSuTheoKhoang(tuNgay, denNgay);
    }

    /**
     * Ca làm việc HÔM NAY mà nhân viên được xếp lịch và CHƯA kết thúc (chưa check-out).
     * Đây là điều kiện để mở ca: chỉ cần CÓ LỊCH hôm nay.
     *
     * [SỬA] Trước đây bắt buộc phải check-in TRƯỚC rồi mới cho mở ca -> nhân viên phải
     * thao tác 2 lần cho cùng một việc "bắt đầu làm". Nay MỞ CA = TỰ ĐỘNG CHẤM CÔNG,
     * nên chỉ cần có lịch là mở được.
     *
     * Trả về null nếu hôm nay không có ca nào (hoặc mọi ca đã check-out xong).
     */
    public ChamCong layCaHopLeDeMoCa(String maNhanVien) {
        LocalDate homNay = GioVN.ngayHomNay();
        List<ChamCong> dsHomNay = chamCongRepository
                .findByNhanVien_MaNhanVienAndNgayChamCongBetween(maNhanVien, homNay, homNay);
        if (dsHomNay == null || dsHomNay.isEmpty()) return null;

        // Ưu tiên ca đang làm dở (đã check-in, chưa check-out)
        for (ChamCong cc : dsHomNay) {
            if (cc.getGioVao() != null && cc.getGioRa() == null) {
                return cc;
            }
        }
        // Nếu không có ca dở, lấy ca chưa chấm công (sẽ được tự check-in khi mở ca)
        for (ChamCong cc : dsHomNay) {
            if (cc.getGioVao() == null) {
                return cc;
            }
        }
        return null;
    }

    // ==================================================================
    // MỞ CA
    // ==================================================================

    /**
     * Mở ca: nhân viên đếm tiền trong két rồi nhập số tiền đầu ca.
     *
     * [SỬA] MỞ CA = TỰ ĐỘNG CHẤM CÔNG (check-in). Nhân viên không phải bấm check-in
     * riêng ở trang Chấm công nữa. Hệ thống vẫn ghi nhận đầy đủ giờ vào + trạng thái
     * (Đúng giờ / Đến muộn) như bình thường.
     *
     * Điều kiện:
     *   - Chưa có phiên nào đang mở (không mở 2 ca cùng lúc).
     *   - Có lịch làm việc hôm nay.
     *   - Tiền đầu ca >= 0.
     */
    @Transactional(rollbackFor = Exception.class)
    public PhienBanHang moCa(NhanVien nhanVien, BigDecimal tienDauCa, String ghiChu) {
        if (nhanVien == null) {
            throw new IllegalArgumentException("Không xác định được nhân viên.");
        }
        if (tienDauCa == null || tienDauCa.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tiền đầu ca không hợp lệ (phải >= 0).");
        }
        if (dangTrongCa(nhanVien.getMaNhanVien())) {
            throw new IllegalStateException("Bạn đang có một ca chưa đóng. Vui lòng đóng ca cũ trước.");
        }

        ChamCong caHopLe = layCaHopLeDeMoCa(nhanVien.getMaNhanVien());
        if (caHopLe == null) {
            throw new IllegalStateException(
                    "Hôm nay bạn không có lịch làm việc nào (hoặc đã kết thúc ca). "
                            + "Vui lòng liên hệ quản lý để được xếp ca.");
        }

        // === TỰ ĐỘNG CHẤM CÔNG (check-in) nếu chưa check-in ===
        if (caHopLe.getGioVao() == null) {
            caHopLe.setGioVao(GioVN.gioHienTai());
            caHopLe.setTrangThai(false); // false = đã qua/đang làm (theo quy ước sẵn có)
            String moTa = "Tự động chấm công khi mở ca lúc " + GioVN.gioHienTai();
            String cu = caHopLe.getGhiChu();
            caHopLe.setGhiChu((cu == null || cu.isBlank()) ? moTa : (cu + " | " + moTa));
            chamCongRepository.save(caHopLe);
        }

        PhienBanHang phien = new PhienBanHang();
        phien.setNhanVien(nhanVien);
        phien.setCaLamViec(caHopLe.getCaLamViec());
        phien.setMaChamCong(caHopLe.getMaChamCong()); // để đóng ca biết check-out bản ghi nào
        phien.setThoiGianMoCa(GioVN.bayGio());
        phien.setTienDauCa(tienDauCa);
        phien.setDoanhThuTienMat(BigDecimal.ZERO);
        phien.setDoanhThuChuyenKhoan(BigDecimal.ZERO);
        phien.setSoHoaDon(0);
        phien.setTrangThai(PhienBanHang.DANG_MO);
        phien.setGhiChu(ghiChu);

        return phienRepo.save(phien);
    }

    // ==================================================================
    // TỔNG KẾT TRONG CA (tính realtime, chưa lưu)
    // ==================================================================

    /**
     * Tính doanh thu phát sinh của phiên tính đến THỜI ĐIỂM HIỆN TẠI.
     * Dùng cho màn hình "Đóng ca" để nhân viên thấy trước con số đối soát.
     * KHÔNG lưu DB - chỉ set vào object để hiển thị.
     */
    public PhienBanHang tinhDoanhThuHienTai(PhienBanHang phien) {
        if (phien == null) return null;

        String maNV = phien.getNhanVien().getMaNhanVien();
        LocalDateTime tuLuc = phien.getThoiGianMoCa();
        LocalDateTime denLuc = (phien.getThoiGianDongCa() != null)
                ? phien.getThoiGianDongCa()
                : GioVN.bayGio();

        BigDecimal tienMat = nz(hoaDonRepo.tongTienTheoPhuongThucTrongCa(maNV, tuLuc, denLuc, TIEN_MAT));
        BigDecimal khongTienMat = nz(hoaDonRepo.tongTienKhongTienMatTrongCa(maNV, tuLuc, denLuc));
        long soHD = hoaDonRepo.demHoaDonTrongCa(maNV, tuLuc, denLuc);

        phien.setDoanhThuTienMat(tienMat);
        phien.setDoanhThuChuyenKhoan(khongTienMat);
        phien.setSoHoaDon((int) soHD);
        // Tiền dự kiến trong két = tiền đầu ca + doanh thu TIỀN MẶT
        phien.setTienDuKien(nz(phien.getTienDauCa()).add(tienMat));

        return phien;
    }

    /** Danh sách hoá đơn phát sinh trong phiên (xem chi tiết khi đóng ca) */
    public List<HoaDon> hoaDonTrongPhien(PhienBanHang phien) {
        if (phien == null) return List.of();
        LocalDateTime denLuc = (phien.getThoiGianDongCa() != null)
                ? phien.getThoiGianDongCa()
                : GioVN.bayGio();
        return hoaDonRepo.danhSachHoaDonTrongCa(
                phien.getNhanVien().getMaNhanVien(), phien.getThoiGianMoCa(), denLuc);
    }

    // ==================================================================
    // ĐÓNG CA + ĐỐI SOÁT
    // ==================================================================

    /**
     * Đóng ca: nhân viên đếm lại tiền trong két và nhập số thực tế.
     * Hệ thống chốt doanh thu và tính chênh lệch:
     *      TienDuKien = TienDauCa + DoanhThuTienMat
     *      ChenhLech  = TienCuoiCaThucTe - TienDuKien
     */
    /**
     * [GIỮ TƯƠNG THÍCH] Chữ ký cũ - không truyền lý do thiếu quỹ.
     * Nội bộ gọi lại bản đầy đủ với lyDoThieuQuy = null.
     */
    @Transactional(rollbackFor = Exception.class)
    public PhienBanHang dongCa(String maNhanVien, BigDecimal tienCuoiCaThucTe, String ghiChu) {
        return dongCa(maNhanVien, tienCuoiCaThucTe, ghiChu, null);
    }

    /**
     * Đóng ca (bản đầy đủ).
     *
     * Bổ sung 2 ràng buộc nghiệp vụ:
     *  (1) CHẶN ĐÓNG CA QUÁ SỚM: phải làm tối thiểu {@link PhienBanHang#PHUT_TOI_THIEU_TRUOC_KHI_DONG}
     *      phút kể từ lúc mở ca (chống mở xong đóng luôn).
     *  (2) THIẾU QUỸ VƯỢT NGƯỠNG phải có LÝ DO + đánh dấu chờ admin duyệt
     *      (nhân viên không được tự lấy tiền -> mọi khoản thiếu phải giải trình).
     *
     * @param lyDoThieuQuy lý do do nhân viên khai khi thiếu quỹ vượt ngưỡng (bắt buộc trong trường hợp đó)
     */
    @Transactional(rollbackFor = Exception.class)
    public PhienBanHang dongCa(String maNhanVien, BigDecimal tienCuoiCaThucTe,
                               String ghiChu, String lyDoThieuQuy) {
        PhienBanHang phien = layPhienDangMo(maNhanVien);
        if (phien == null) {
            throw new IllegalStateException("Bạn không có ca nào đang mở.");
        }
        if (tienCuoiCaThucTe == null || tienCuoiCaThucTe.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tiền cuối ca không hợp lệ (phải >= 0).");
        }

        LocalDateTime now = GioVN.bayGio();

        // (1) CHẶN ĐÓNG CA QUÁ SỚM
        if (phien.getThoiGianMoCa() != null) {
            long soPhutDaLam = java.time.Duration.between(phien.getThoiGianMoCa(), now).toMinutes();
            long toiThieu = PhienBanHang.PHUT_TOI_THIEU_TRUOC_KHI_DONG;
            if (soPhutDaLam < toiThieu) {
                long conThieu = toiThieu - soPhutDaLam;
                throw new IllegalStateException(
                        "Chưa thể đóng ca. Cần làm tối thiểu " + toiThieu + " phút mỗi ca "
                        + "(mới làm được " + soPhutDaLam + " phút, còn " + conThieu + " phút nữa).");
            }
        }

        // Chốt doanh thu trước để biết tiền dự kiến -> tính chênh lệch
        tinhDoanhThuHienTai(phien);
        BigDecimal duKien = nz(phien.getTienDuKien());
        BigDecimal chenhLech = tienCuoiCaThucTe.subtract(duKien);

        // (2) THIẾU QUỸ VƯỢT NGƯỠNG -> bắt buộc lý do + chờ duyệt
        //     chenhLech âm nghĩa là thiếu; so sánh phần thiếu với ngưỡng.
        BigDecimal phanThieu = chenhLech.signum() < 0 ? chenhLech.abs() : BigDecimal.ZERO;
        boolean thieuVuotNguong =
                phanThieu.compareTo(PhienBanHang.NGUONG_THIEU_QUY_PHAI_DUYET) > 0;

        if (thieuVuotNguong) {
            if (lyDoThieuQuy == null || lyDoThieuQuy.isBlank()) {
                throw new IllegalStateException(
                        "Quỹ đang thiếu " + phanThieu.toPlainString() + "đ (vượt ngưỡng "
                        + PhienBanHang.NGUONG_THIEU_QUY_PHAI_DUYET.toPlainString() + "đ). "
                        + "Bạn phải nhập LÝ DO thiếu quỹ để quản lý xem xét. "
                        + "Lưu ý: nhân viên không được tự lấy tiền khỏi két.");
            }
            phien.setCanDuyet(true);
            phien.setDaDuyet(false);
            phien.setLyDoThieuQuy(lyDoThieuQuy.trim());
        } else {
            phien.setCanDuyet(false);
            phien.setDaDuyet(false);
            if (lyDoThieuQuy != null && !lyDoThieuQuy.isBlank()) {
                phien.setLyDoThieuQuy(lyDoThieuQuy.trim());
            }
        }

        phien.setThoiGianDongCa(now);

        // === TỰ ĐỘNG CHẤM CÔNG RA (check-out) bản ghi chấm công gắn với phiên ===
        if (phien.getMaChamCong() != null) {
            chamCongRepository.findById(phien.getMaChamCong()).ifPresent(cc -> {
                if (cc.getGioVao() != null && cc.getGioRa() == null) {
                    LocalTime gioRa = GioVN.gioHienTai();
                    cc.setGioRa(gioRa);
                    long phut = java.time.Duration.between(cc.getGioVao(), gioRa).toMinutes();
                    if (phut > 0) {
                        cc.setSoGioLam(BigDecimal.valueOf(phut / 60.0)
                                .setScale(2, java.math.RoundingMode.HALF_UP));
                    }
                    String moTa = "Tự động chấm công ra khi đóng ca lúc " + gioRa.withNano(0);
                    String cu = cc.getGhiChu();
                    cc.setGhiChu((cu == null || cu.isBlank()) ? moTa : (cu + " | " + moTa));
                    chamCongRepository.save(cc);
                }
            });
        }

        // (doanh thu & tiền dự kiến đã được chốt ở trên bằng tinhDoanhThuHienTai)
        phien.setTienCuoiCaThucTe(tienCuoiCaThucTe);
        phien.setChenhLech(chenhLech);
        phien.setTrangThai(PhienBanHang.DA_DONG);

        if (ghiChu != null && !ghiChu.isBlank()) {
            String cu = phien.getGhiChu();
            phien.setGhiChu((cu == null || cu.isBlank()) ? ghiChu : (cu + " | " + ghiChu));
        }

        return phienRepo.save(phien);
    }

    /** Diễn giải chênh lệch cho người dùng dễ đọc */
    public String moTaChenhLech(BigDecimal chenhLech) {
        if (chenhLech == null) return "";
        int cmp = chenhLech.compareTo(BigDecimal.ZERO);
        if (cmp == 0) return "Khớp quỹ";
        if (cmp > 0) return "Thừa quỹ";
        return "Thiếu quỹ";
    }

    /**
     * Tiền mặt cuối ca của PHIÊN GẦN NHẤT đã đóng (của cùng nhân viên).
     * Dùng để gợi ý "bàn giao từ ca trước" khi mở ca mới.
     * Trả về 0 nếu chưa từng có ca nào.
     */
    public BigDecimal tienBanGiaoTuCaTruoc(String maNhanVien) {
        List<PhienBanHang> ls = phienRepo.lichSuTheoNhanVien(maNhanVien);
        if (ls == null) return BigDecimal.ZERO;
        for (PhienBanHang p : ls) {
            if (PhienBanHang.DA_DONG.equals(p.getTrangThai()) && p.getTienCuoiCaThucTe() != null) {
                return p.getTienCuoiCaThucTe();
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Lịch làm việc của nhân viên trong khoảng ngày (dùng cho trang "Lịch của tôi").
     * Sắp xếp: ngày mới nhất trước.
     */
    public List<ChamCong> lichLamViecCuaToi(String maNhanVien, LocalDate tuNgay, LocalDate denNgay) {
        List<ChamCong> ds = chamCongRepository
                .findByNhanVien_MaNhanVienAndNgayChamCongBetween(maNhanVien, tuNgay, denNgay);
        if (ds == null) return List.of();
        return ds.stream()
                .sorted((a, b) -> {
                    if (a.getNgayChamCong() == null) return 1;
                    if (b.getNgayChamCong() == null) return -1;
                    return b.getNgayChamCong().compareTo(a.getNgayChamCong());
                })
                .toList();
    }

    /** Tìm phiên theo mã (cho admin xem chi tiết) */
    public PhienBanHang timTheoMa(Integer maPhien) {
        if (maPhien == null) return null;
        return phienRepo.findById(maPhien).orElse(null);
    }

    // ==================================================================
    // NGHIỆP VỤ ADMIN: DUYỆT THIẾU QUỸ & CHỐT / THU TIỀN KÉT
    //   -> Chỉ admin mới được xử lý tiền. Nhân viên không có nghiệp vụ lấy tiền.
    // ==================================================================

    /** Danh sách các ca đang chờ admin duyệt (thiếu quỹ vượt ngưỡng) */
    public List<PhienBanHang> layCacCaChoDuyet() {
        return phienRepo.findAll().stream()
                .filter(PhienBanHang::isChoDuyet)
                .sorted((a, b) -> {
                    LocalDateTime x = a.getThoiGianDongCa();
                    LocalDateTime y = b.getThoiGianDongCa();
                    if (x == null) return 1;
                    if (y == null) return -1;
                    return y.compareTo(x);
                })
                .toList();
    }

    /**
     * Admin duyệt (xác nhận đã xử lý) khoản thiếu quỹ của một ca.
     * Ghi lại ai duyệt để truy vết.
     */
    @Transactional(rollbackFor = Exception.class)
    public PhienBanHang adminDuyetThieuQuy(Integer maPhien, String tenAdmin, String ghiChuAdmin) {
        PhienBanHang phien = phienRepo.findById(maPhien)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca #" + maPhien));
        if (!Boolean.TRUE.equals(phien.getCanDuyet())) {
            throw new IllegalStateException("Ca này không có khoản thiếu quỹ cần duyệt.");
        }
        phien.setDaDuyet(true);
        String moTa = "Admin " + (tenAdmin == null ? "?" : tenAdmin) + " đã duyệt thiếu quỹ lúc "
                + GioVN.bayGio();
        if (ghiChuAdmin != null && !ghiChuAdmin.isBlank()) {
            moTa += " - " + ghiChuAdmin.trim();
        }
        String cu = phien.getGhiChu();
        phien.setGhiChu((cu == null || cu.isBlank()) ? moTa : (cu + " | " + moTa));
        return phienRepo.save(phien);
    }

    /**
     * TỒN QUỸ HIỆN TẠI trong két (mô hình A - tiền cuốn chiếu):
     *   = tiền cuối ca của phiên ĐÃ ĐÓNG gần nhất  -  số admin đã thu ở phiên đó.
     * Đây là số tiền admin có thể thu về.
     */
    public BigDecimal layTonQuyHienTai() {
        return phienRepo.findAll().stream()
                .filter(p -> PhienBanHang.DA_DONG.equals(p.getTrangThai()))
                .filter(p -> p.getThoiGianDongCa() != null)
                .max((a, b) -> a.getThoiGianDongCa().compareTo(b.getThoiGianDongCa()))
                .map(p -> nz(p.getTienCuoiCaThucTe()).subtract(nz(p.getTienDaThu())))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Admin CHỐT/THU tiền khỏi két. Ghi nhận đầy đủ ai thu, bao nhiêu, khi nào
     * lên phiên đã đóng gần nhất. Sau khi thu, tồn quỹ giảm tương ứng và ca kế tiếp
     * sẽ kế thừa phần còn lại làm tiền đầu ca.
     *
     * @param soTienThu số tiền admin lấy ra khỏi két (> 0, <= tồn quỹ hiện tại)
     */
    @Transactional(rollbackFor = Exception.class)
    public PhienBanHang adminChotThuTienKet(BigDecimal soTienThu, String tenAdmin, String ghiChuAdmin) {
        if (soTienThu == null || soTienThu.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thu phải lớn hơn 0.");
        }
        BigDecimal ton = layTonQuyHienTai();
        if (soTienThu.compareTo(ton) > 0) {
            throw new IllegalStateException("Số tiền thu (" + soTienThu.toPlainString()
                    + "đ) vượt quá tồn quỹ hiện tại (" + ton.toPlainString() + "đ).");
        }

        PhienBanHang phienMoiNhat = phienRepo.findAll().stream()
                .filter(p -> PhienBanHang.DA_DONG.equals(p.getTrangThai()))
                .filter(p -> p.getThoiGianDongCa() != null)
                .max((a, b) -> a.getThoiGianDongCa().compareTo(b.getThoiGianDongCa()))
                .orElseThrow(() -> new IllegalStateException("Chưa có ca nào đóng để thu tiền."));

        BigDecimal daThuTruoc = nz(phienMoiNhat.getTienDaThu());
        phienMoiNhat.setTienDaThu(daThuTruoc.add(soTienThu));
        phienMoiNhat.setNguoiThuTien(tenAdmin);
        phienMoiNhat.setThoiGianThuTien(GioVN.bayGio());

        String moTa = "Admin " + (tenAdmin == null ? "?" : tenAdmin) + " thu "
                + soTienThu.toPlainString() + "đ khỏi két lúc " + GioVN.bayGio();
        if (ghiChuAdmin != null && !ghiChuAdmin.isBlank()) {
            moTa += " - " + ghiChuAdmin.trim();
        }
        String cu = phienMoiNhat.getGhiChu();
        phienMoiNhat.setGhiChu((cu == null || cu.isBlank()) ? moTa : (cu + " | " + moTa));

        return phienRepo.save(phienMoiNhat);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
