package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.SanPham;
import com.example.th06876_java202.Entity.SanPhamChiTiet;
import com.example.th06876_java202.Repository.SanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class SanPhamService {

    @Autowired
    SanPhamChiTietService sanPhamChiTietService;

    private final SanPhamRepository sanPhamRepository;

    public SanPhamService(SanPhamRepository sanPhamRepository) {
        this.sanPhamRepository = sanPhamRepository;
    }

    public List<SanPham> getAll() {
        return sanPhamRepository.findAll();
    }

    public Optional<SanPham> findById(String id) {
        return sanPhamRepository.findById(id);
    }

    public SanPham findByTenSanPham(String tenSanPham) {
        return sanPhamRepository.findByTenSanPham(tenSanPham).orElse(null);
    }

    public Page<SanPham> getallpage(Pageable pageable) {
        return sanPhamRepository.findAllByOrderByNgayTaoDesc(pageable);
    }

    public Page<SanPham> searchSanPham(String maDanhMuc, Boolean tt, String maTH, String maKG, String t, Pageable pageable) {
        String keyword = (t == null || t.trim().isEmpty()) ? null : t.trim();
        return sanPhamRepository.searchSanPham(maDanhMuc, tt, maTH, maKG, keyword, pageable);
    }

    public List<SanPham> findAllWithFilters(String maDanhMuc, Boolean tt, String maTH, String maKG, String t) {
        System.out.println("=== findAllWithFilters ===");
        System.out.println("maDanhMuc: '" + maDanhMuc + "'");
        System.out.println("tt: " + tt);
        System.out.println("maTH: '" + maTH + "'");
        System.out.println("maKG: '" + maKG + "'");
        System.out.println("t: '" + t + "'");

        String keyword = (t == null || t.trim().isEmpty()) ? null : t.trim();
        System.out.println("keyword sau khi xử lý: " + keyword);

        List<SanPham> result = sanPhamRepository.findAllWithFilters(
                maDanhMuc,
                tt,
                maTH,
                maKG,
                keyword
        );

        System.out.println("Kết quả tìm thấy: " + (result != null ? result.size() : 0));

        if (result != null && !result.isEmpty()) {
            for (SanPham sp : result) {
                System.out.println("  - " + sp.getMaSanPham() + ": " + sp.getTenSanPham());
            }
        } else {
            System.out.println("⚠️ Không tìm thấy sản phẩm nào!");
            System.out.println("Thử lấy tất cả sản phẩm...");
            List<SanPham> all = sanPhamRepository.findAll();
            System.out.println("Tổng số sản phẩm trong DB: " + (all != null ? all.size() : 0));
            if (all != null && !all.isEmpty()) {
                for (SanPham sp : all) {
                    System.out.println("  - " + sp.getMaSanPham() + ": " + sp.getTenSanPham());
                }
            }
        }

        return result;
    }

    public SanPham save(SanPham sanPham) {
        return sanPhamRepository.save(sanPham);
    }

    public void delete(String maSanPham) {
        sanPhamRepository.deleteById(maSanPham);
    }

    public void updateTrangThai(String maSanPham, boolean trangThai) {
        sanPhamRepository.updateTrangThai(maSanPham, trangThai);
    }

    public boolean isTenSanPhamDuplicate(String ten) {
        if (ten == null) return false;
        String normalizedName = ten.trim().replaceAll("\\s+", " ");
        return sanPhamRepository.existsByTenSanPhamIgnoreCase(normalizedName);
    }

    public long countByTrangThai(boolean trangThai) {
        return sanPhamRepository.countByTrangThai(trangThai);
    }

    public List<SanPham> findTop10ByTenSanPhamContainingOrMaSanPhamContaining(String keyword) {
        return sanPhamRepository.findTop10ByTenSanPhamContainingOrMaSanPhamContaining(keyword);
    }

    // Thêm vào SanPhamService.java

    public void updateGiaTrungBinh(String maSanPham) {
        try {
            SanPham sp = sanPhamRepository.findById(maSanPham).orElse(null);
            if (sp == null) return;

            List<SanPhamChiTiet> variants = sanPhamChiTietService.getallsp(maSanPham);
            if (variants == null || variants.isEmpty()) {
                sp.setGiaBanTrungBinh(BigDecimal.ZERO);
                sanPhamRepository.save(sp);
                return;
            }

            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            for (SanPhamChiTiet variant : variants) {
                if (variant.getGiaBan() != null) {
                    sum = sum.add(variant.getGiaBan());
                    count++;
                }
            }

            if (count > 0) {
                BigDecimal avg = sum.divide(new BigDecimal(count), 0, RoundingMode.HALF_UP);
                sp.setGiaBanTrungBinh(avg);
                sanPhamRepository.save(sp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}