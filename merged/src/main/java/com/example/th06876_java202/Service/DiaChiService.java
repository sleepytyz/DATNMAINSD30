package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.DiaChi;
import com.example.th06876_java202.Entity.KhachHang;
import com.example.th06876_java202.Repository.DiaChiRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service quản lý địa chỉ khách hàng.
 *
 * Phiên bản này được HỢP NHẤT (merge) & dọn dẹp từ hai nhánh:
 *  - Bản "fixed": chuẩn hoá xử lý địa chỉ mặc định, thêm findByKhachHang(String)/delete(...).
 *  - Bản team: bổ sung các method banhang cần (save trả về DiaChi, findByKhachHang_MaKH,
 *    resetDiaChiMacDinh, deleteById, countDefaultAddressByKhachHang, findDefaultByMaKH...).
 * Các định nghĩa trùng lặp ở nhánh team đã được loại bỏ; mỗi method chỉ còn 1 bản.
 */
@Service
public class DiaChiService {

    private static final Logger logger = LoggerFactory.getLogger(DiaChiService.class);

    @Autowired
    private GHNLocationService ghnLocationService;

    @Autowired
    private DiaChiRepo diaChiRepo;

    // ============ RESET ĐỊA CHỈ MẶC ĐỊNH ============
    @Transactional
    public void resetDiaChiMacDinh(String maKH) {
        List<DiaChi> diaChiList = diaChiRepo.findByKhachHang_MaKH(maKH);
        for (DiaChi dc : diaChiList) {
            if (Boolean.TRUE.equals(dc.getDiaChiMacDinh())) {
                dc.setDiaChiMacDinh(false);
                diaChiRepo.save(dc);
            }
        }
    }

    // ============ LƯU ĐỊA CHỈ (THÊM MỚI HOẶC CẬP NHẬT) — trả về DiaChi ============
    @Transactional(rollbackFor = Exception.class)
    public DiaChi save(DiaChi diaChi) {
        if (diaChi == null) {
            throw new IllegalArgumentException("Địa chỉ không được null!");
        }
        if (diaChi.getKhachHang() == null || diaChi.getKhachHang().getMaKH() == null) {
            throw new IllegalArgumentException("Khách hàng không hợp lệ!");
        }
        // Nếu đặt làm mặc định -> gỡ mặc định các địa chỉ khác của cùng khách hàng
        if (Boolean.TRUE.equals(diaChi.getDiaChiMacDinh())) {
            resetDiaChiMacDinh(diaChi.getKhachHang().getMaKH());
        }
        DiaChi saved = diaChiRepo.save(diaChi);
        logger.info("Đã lưu địa chỉ ID: {}, Mặc định: {}", saved.getMaDiaChi(), saved.getDiaChiMacDinh());
        return saved;
    }

    // ============ LẤY DANH SÁCH THEO MÃ KHÁCH HÀNG (String) ============
    public List<DiaChi> findByKhachHang_MaKH(String maKH) {
        if (maKH == null || maKH.isEmpty()) return new ArrayList<>();
        return diaChiRepo.findByKhachHang_MaKH(maKH);
    }

    // Alias tiện dụng dùng ở nhiều controller (nhận String maKH)
    public List<DiaChi> findByKhachHang(String maKH) {
        return findByKhachHang_MaKH(maKH);
    }

    // ============ LẤY DANH SÁCH THEO ĐỐI TƯỢNG KHÁCH HÀNG ============
    public List<DiaChi> findByKhachHang(KhachHang khachHang) {
        return diaChiRepo.findByKhachHang(khachHang);
    }

    // ============ TÌM THEO ID ============
    public Optional<DiaChi> findById(Integer id) {
        return diaChiRepo.findById(id);
    }

    // ============ ĐẾM SỐ ĐỊA CHỈ MẶC ĐỊNH ============
    public int countDefaultAddressByKhachHang(String maKH) {
        return diaChiRepo.countDefaultAddressByKhachHang(maKH);
    }

    // ============ ĐỊA CHỈ MẶC ĐỊNH THEO KHÁCH HÀNG ============
    public DiaChi findDefaultByKhachHang(KhachHang khachHang) {
        return diaChiRepo.findByKhachHangAndDiaChiMacDinh(khachHang, true);
    }

    public DiaChi findByKhachHangAndDiaChiMacDinh(KhachHang khachHang, Boolean macDinh) {
        return diaChiRepo.findByKhachHangAndDiaChiMacDinh(khachHang, macDinh);
    }

    // Tìm địa chỉ mặc định theo mã KH (String)
    public DiaChi findDefaultByMaKH(String maKH) {
        List<DiaChi> list = diaChiRepo.findByKhachHang_MaKH(maKH);
        for (DiaChi dc : list) {
            if (Boolean.TRUE.equals(dc.getDiaChiMacDinh())) {
                return dc;
            }
        }
        return null;
    }

    // ============ XOÁ THEO ID (deleteById): tự chuyển mặc định nếu cần ============
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer id) {
        Optional<DiaChi> diaChiOpt = diaChiRepo.findById(id);
        if (diaChiOpt.isPresent()) {
            DiaChi diaChi = diaChiOpt.get();
            if (Boolean.TRUE.equals(diaChi.getDiaChiMacDinh()) && diaChi.getKhachHang() != null) {
                String maKH = diaChi.getKhachHang().getMaKH();
                List<DiaChi> otherAddresses = diaChiRepo.findByKhachHang_MaKH(maKH);
                otherAddresses.removeIf(dc -> dc.getMaDiaChi().equals(id));
                if (!otherAddresses.isEmpty()) {
                    DiaChi newDefault = otherAddresses.get(0);
                    newDefault.setDiaChiMacDinh(true);
                    diaChiRepo.save(newDefault);
                }
            }
            diaChiRepo.deleteById(id);
        }
    }

    // Alias: delete(id) dùng ở TaiKhoanCaNhanController (bản fixed)
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        deleteById(id);
    }

    // ============ XOÁ TẤT CẢ ĐỊA CHỈ CỦA KHÁCH HÀNG ============
    @Transactional(rollbackFor = Exception.class)
    public void deleteAllByKhachHang(String maKH) {
        List<DiaChi> list = diaChiRepo.findByKhachHang_MaKH(maKH);
        if (!list.isEmpty()) {
            diaChiRepo.deleteAll(list);
        }
    }

    // ============ TIỆN ÍCH ============
    public boolean hasAddress(String maKH) {
        List<DiaChi> list = diaChiRepo.findByKhachHang_MaKH(maKH);
        return list != null && !list.isEmpty();
    }

    public long countByKhachHang(String maKH) {
        return diaChiRepo.countByKhachHang_MaKH(maKH);
    }

    // ============ CẬP NHẬT ĐỊA CHỈ ============
    @Transactional(rollbackFor = Exception.class)
    public DiaChi update(Integer maDiaChi, DiaChi diaChiUpdate) {
        Optional<DiaChi> optional = diaChiRepo.findById(maDiaChi);
        if (optional.isPresent()) {
            DiaChi existing = optional.get();
            if (diaChiUpdate.getTenNguoiNhan() != null) existing.setTenNguoiNhan(diaChiUpdate.getTenNguoiNhan());
            if (diaChiUpdate.getSoDienThoaiNguoiNhan() != null) existing.setSoDienThoaiNguoiNhan(diaChiUpdate.getSoDienThoaiNguoiNhan());
            if (diaChiUpdate.getDiaChiCuThe() != null) existing.setDiaChiCuThe(diaChiUpdate.getDiaChiCuThe());
            if (diaChiUpdate.getPhuongXa() != null) existing.setPhuongXa(diaChiUpdate.getPhuongXa());
            if (diaChiUpdate.getQuanHuyen() != null) existing.setQuanHuyen(diaChiUpdate.getQuanHuyen());
            if (diaChiUpdate.getTinhThanh() != null) existing.setTinhThanh(diaChiUpdate.getTinhThanh());

            if (Boolean.TRUE.equals(diaChiUpdate.getDiaChiMacDinh())) {
                resetDiaChiMacDinh(existing.getKhachHang().getMaKH());
                existing.setDiaChiMacDinh(true);
            } else if (diaChiUpdate.getDiaChiMacDinh() != null) {
                existing.setDiaChiMacDinh(false);
            }
            return diaChiRepo.save(existing);
        }
        return null;
    }

    public List<DiaChi> findBySoDienThoai(String soDienThoai) {
        return diaChiRepo.findBySoDienThoaiNguoiNhanContaining(soDienThoai);
    }

    public List<DiaChi> findByTenNguoiNhan(String tenNguoiNhan) {
        return diaChiRepo.findByTenNguoiNhanContaining(tenNguoiNhan);
    }


}
