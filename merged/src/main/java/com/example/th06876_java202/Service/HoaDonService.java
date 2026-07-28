package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.HoaDon;
import com.example.th06876_java202.Repository.HoaDonRepo;
import com.example.th06876_java202.realtime.ThongBaoRealtimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HoaDonService {

    @Autowired
    private HoaDonRepo repo;

    @Autowired
    private ThongBaoRealtimeService thongBaoRealtimeService;

    @org.springframework.context.annotation.Lazy
    @Autowired
    private com.example.th06876_java202.Storefront.DonHangOnlineService donHangOnlineService;

    public Page<HoaDon> findByTrangThaiIn(List<String> trangThaiList, Pageable pageable) {
        return repo.findByTrangThaiIn(trangThaiList, pageable);
    }

    public List<HoaDon> findByTrangThaiInList(List<String> trangThaiList) {
        return repo.findByTrangThaiIn(trangThaiList);
    }

    public Page<HoaDon> searchByNgayTaodhAndStatus(LocalDateTime ngay, LocalDateTime ngay2,
                                                   List<String> allowedStatuses, Pageable pageable) {
        if (ngay != null && ngay2 != null) {
            return repo.findByNgayTaoBetweenAndTrangThaiIn(ngay, ngay2, allowedStatuses, pageable);
        } else if (ngay != null) {
            return repo.findByNgayTaoAfterAndTrangThaiIn(ngay, allowedStatuses, pageable);
        } else if (ngay2 != null) {
            return repo.findByNgayTaoBeforeAndTrangThaiIn(ngay2, allowedStatuses, pageable);
        }
        return repo.findByTrangThaiIn(allowedStatuses, pageable);
    }

    public List<HoaDon> searchByNgayTaodhAndStatusList(LocalDateTime ngay, LocalDateTime ngay2,
                                                       List<String> allowedStatuses) {
        if (ngay != null && ngay2 != null) {
            return repo.findByNgayTaoBetweenAndTrangThaiIn(ngay, ngay2, allowedStatuses);
        } else if (ngay != null) {
            return repo.findByNgayTaoAfterAndTrangThaiIn(ngay, allowedStatuses);
        } else if (ngay2 != null) {
            return repo.findByNgayTaoBeforeAndTrangThaiIn(ngay2, allowedStatuses);
        }
        return repo.findByTrangThaiIn(allowedStatuses);
    }

    public Page<HoaDon> searchByMaAndStatus(String maHoaDon, List<String> allowedStatuses, Pageable pageable) {
        return repo.findByMaHoaDonAndTrangThaiIn(maHoaDon, allowedStatuses, pageable);
    }

    public List<HoaDon> findByTrangThai(String trangThai) {
        return repo.findByTrangThai(trangThai);
    }

    public List<HoaDon> getAllDH() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "ngayTao"));
    }

    public HoaDon findById(String id) {
        return repo.findById(id).orElse(null);
    }

    public Page<HoaDon> findByTrangThai(String trangThai, Pageable pageable) {
        return repo.findByTrangThai(trangThai, pageable);
    }

    public List<HoaDon> findAllByTrangThai(String trangThai) {
        return repo.findByTrangThai(trangThai);
    }

    public List<HoaDon> getAll() {
        return repo.findAll();
    }

    public long countByTrangThai(String trangThai) {
        return repo.countByTrangThai(trangThai);
    }

    public void suatt(String mahd) {
        HoaDon hd = repo.findById(mahd).orElse(null);
        if (hd != null && "Chờ xác nhận".equals(hd.getTrangThai())) {
            // xacNhanDonTruTonKho đã LÀM TRỌN VẸN: trừ tồn kho, đổi trạng thái sang
            // "Đã xác nhận", lưu đơn VÀ phát thông báo realtime + email cho khách.
            // Trước đây ở đây đổi trạng thái + phát thông báo LẦN NỮA -> quản lý nhận
            // thông báo đúp và khách nhận 2 email. Nay chỉ gọi đúng một lần.
            donHangOnlineService.xacNhanDonTruTonKho(hd);
        }
    }

    public HoaDon save(HoaDon hoaDon) {
        return repo.save(hoaDon);
    }

    public void suattdg(String mahd) {
        HoaDon hd = repo.findById(mahd).orElse(null);
        if (hd != null && "Đã xác nhận".equals(hd.getTrangThai())) {
            String cu = hd.getTrangThai();
            hd.setTrangThai("Đang giao");
            repo.save(hd);
            thongBaoRealtimeService.trangThaiDonThayDoi(hd, cu, "Quản lý bán hàng");
        }
    }

    public void suattdgg(String mahd) {
        HoaDon hd = repo.findById(mahd).orElse(null);
        if (hd != null && "Đang giao".equals(hd.getTrangThai())) {
            String cu = hd.getTrangThai();
            hd.setTrangThai("Đã giao");
            // Giao thành công = hoàn tất thanh toán (COD nhận tiền khi giao)
            if (hd.getNgayThanhToan() == null) {
                hd.setNgayThanhToan(LocalDateTime.now());
            }
            repo.save(hd);
            thongBaoRealtimeService.trangThaiDonThayDoi(hd, cu, "Quản lý bán hàng");
        }
    }

    public Page<HoaDon> findByKhachHang(String maKH, Pageable pageable) {
        return repo.findByMaKhachHang_MaKHOrderByMaHoaDonDesc(maKH, pageable);
    }
}
