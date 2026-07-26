package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.*;
import com.example.th06876_java202.Repository.DanhGiaRepository;
import com.example.th06876_java202.Repository.HoaDonChiTietRepository;
import com.example.th06876_java202.Repository.HoaDonRepo;
import com.example.th06876_java202.Repository.SanPhamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Nghiệp vụ đánh giá sản phẩm.
 * Quy tắc: khách chỉ được đánh giá sản phẩm mình ĐÃ MUA và đơn đã ở trạng thái "Đã giao",
 * mỗi sản phẩm trong một hoá đơn chỉ đánh giá được một lần.
 */
@Service
@RequiredArgsConstructor
public class DanhGiaService {

    private final DanhGiaRepository danhGiaRepository;
    private final HoaDonRepo hoaDonRepo;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final SanPhamRepository sanPhamRepository;

    /** Danh sách đánh giá hiển thị của 1 sản phẩm. */
    public List<DanhGia> danhSachTheoSanPham(String maSanPham) {
        return danhGiaRepository.findHienThiBySanPham(maSanPham);
    }

    public long soLuongDanhGia(String maSanPham) {
        return danhGiaRepository.countBySanPham_MaSanPhamAndTrangThaiTrue(maSanPham);
    }

    /** Điểm trung bình (0 nếu chưa có đánh giá). */
    public double diemTrungBinh(String maSanPham) {
        Double d = danhGiaRepository.diemTrungBinh(maSanPham);
        return d != null ? Math.round(d * 10.0) / 10.0 : 0.0;
    }

    /**
     * Kiểm tra khách đã mua sản phẩm này trong 1 đơn đã giao chưa; trả về hoá đơn hợp lệ gần nhất
     * để gắn vào đánh giá, hoặc null nếu không đủ điều kiện.
     */
    @Transactional(readOnly = true)
    public HoaDon timHoaDonDaGiaoChuaDanhGia(String maKhachHang, String maSanPham) {
        if (maKhachHang == null || maSanPham == null) return null;
        List<HoaDon> dsDaGiao = hoaDonRepo.findByTrangThai("Đã giao");
        for (HoaDon hd : dsDaGiao) {
            if (hd.getMaKhachHang() == null || hd.getMaKhachHang().getMaKH() == null) continue;
            if (!hd.getMaKhachHang().getMaKH().equals(maKhachHang)) continue;

            // hoá đơn này có chứa sản phẩm cần đánh giá không?
            List<HoaDonChiTiet> cts = hoaDonChiTietRepository.getallsphd(hd.getMaHoaDon());
            boolean coSanPham = cts.stream().anyMatch(ct ->
                    ct.getSanPhamChiTiet() != null
                            && ct.getSanPhamChiTiet().getSanPham() != null
                            && maSanPham.equals(ct.getSanPhamChiTiet().getSanPham().getMaSanPham()));
            if (!coSanPham) continue;

            // đã đánh giá sản phẩm trong đúng hoá đơn này chưa?
            DanhGia daCo = danhGiaRepository.findByKhachSanPhamHoaDon(maKhachHang, maSanPham, hd.getMaHoaDon());
            if (daCo == null) {
                return hd; // đủ điều kiện đánh giá
            }
        }
        return null;
    }

    public boolean khachCoTheDanhGia(String maKhachHang, String maSanPham) {
        return timHoaDonDaGiaoChuaDanhGia(maKhachHang, maSanPham) != null;
    }

    /**
     * Lưu một đánh giá mới. Trả về null nếu thành công, hoặc thông báo lỗi.
     */
    @Transactional
    public String themDanhGia(KhachHang khachHang, String maSanPham, int soSao, String noiDung) {
        if (khachHang == null) return "Bạn cần đăng nhập để đánh giá.";
        if (soSao < 1 || soSao > 5) return "Vui lòng chọn số sao từ 1 đến 5.";

        SanPham sp = sanPhamRepository.findById(maSanPham).orElse(null);
        if (sp == null) return "Sản phẩm không tồn tại.";

        HoaDon hoaDon = timHoaDonDaGiaoChuaDanhGia(khachHang.getMaKH(), maSanPham);
        if (hoaDon == null) {
            return "Bạn chỉ có thể đánh giá sản phẩm đã mua và đã nhận hàng (hoặc bạn đã đánh giá sản phẩm này rồi).";
        }

        DanhGia dg = new DanhGia();
        dg.setSanPham(sp);
        dg.setKhachHang(khachHang);
        dg.setHoaDon(hoaDon);
        dg.setSoSao(soSao);
        dg.setNoiDung(noiDung != null ? noiDung.trim() : null);
        dg.setNgayDanhGia(LocalDateTime.now());
        dg.setTrangThai(true);
        danhGiaRepository.save(dg);
        return null;
    }

    // ===== Dùng cho admin =====

    public List<DanhGia> tatCaTheoSanPham(String maSanPham) {
        return danhGiaRepository.findBySanPham_MaSanPhamOrderByNgayDanhGiaDesc(maSanPham);
    }

    @Transactional
    public void doiTrangThai(Integer maDanhGia, boolean hien) {
        danhGiaRepository.findById(maDanhGia).ifPresent(d -> {
            d.setTrangThai(hien);
            danhGiaRepository.save(d);
        });
    }
}
