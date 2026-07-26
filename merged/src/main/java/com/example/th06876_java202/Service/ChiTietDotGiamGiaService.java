package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.ChiTietDotGiamGia;
import com.example.th06876_java202.Entity.DotGiamGia;
import com.example.th06876_java202.Entity.SanPham;
import com.example.th06876_java202.Entity.SanPhamChiTiet;
import com.example.th06876_java202.Repository.ChiTietDotGiamGiaRepo;
import com.example.th06876_java202.Repository.SanPhamChiTietRepository;
import com.example.th06876_java202.Repository.SanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChiTietDotGiamGiaService {

    @Autowired
    private ChiTietDotGiamGiaRepo repo;

    @Autowired
    private SanPhamChiTietService sanPhamChiTietRepo;

    @Autowired
    private SanPhamChiTietRepository sanPhamRepository;

    @Autowired
    private DotGiamGiaService dotGiamGiaRepo;

    @Autowired
    private SanPhamService sanPhamRepo;

    public List<ChiTietDotGiamGia> getAll() {
        return repo.findAll();
    }

    public ChiTietDotGiamGia getById(Integer id) {
        return repo.getById(id);
    }

    public void save(ChiTietDotGiamGia ct) {
        repo.save(ct);
    }

    public void delete(Integer id){
        repo.deleteById(id);
    }

    public boolean exists(String maGiamGia, String maSanPham) {
        return repo.existsByDotGiamGia_MaGiamGiaAndSanPham_MaSanPham(maGiamGia, maSanPham);
    }

    public Page<ChiTietDotGiamGia> getAllPage(Pageable pageable) {
        return repo.findAll(pageable);
    }

    public List<ChiTietDotGiamGia> filterByMaGiamGia(String maGiamGia) {
        return repo.filterByDotGiamGia(maGiamGia);
    }

    @Transactional
    public int saveAllDetails(String maGiamGia, String maSanPham, List<String> listMaSanPhamChiTiet) {
        System.out.println("=== saveAllDetails START ===");
        System.out.println("maGiamGia: " + maGiamGia);
        System.out.println("maSanPham: " + maSanPham);
        System.out.println("listMaSanPhamChiTiet: " + listMaSanPhamChiTiet);

        if (maGiamGia == null || maSanPham == null || listMaSanPhamChiTiet == null) {
            System.err.println("Lỗi: Dữ liệu đầu vào bị null!");
            return 0;
        }

        if (listMaSanPhamChiTiet.isEmpty()) {
            System.out.println("listMaSanPhamChiTiet rỗng, không có gì để lưu");
            return 0;
        }

        var dotGiamGia = dotGiamGiaRepo.findById(maGiamGia).orElse(null);
        var sanPham = sanPhamRepo.findById(maSanPham).orElse(null);

        if (dotGiamGia == null || sanPham == null) {
            System.err.println("Lỗi: Không tìm thấy DotGiamGia hoặc SanPham trong CSDL");
            return 0;
        }

        System.out.println("Đã tìm thấy DotGiamGia: " + dotGiamGia.getMaGiamGia());
        System.out.println("Đã tìm thấy SanPham: " + sanPham.getMaSanPham());

        // ⭐ KHÔNG XÓA Ở ĐÂY - Đã xóa ở Controller

        int savedCount = 0;
        for (String maCT : listMaSanPhamChiTiet) {
            if (maCT == null || maCT.trim().isEmpty()) {
                System.out.println("Bỏ qua maCT null hoặc rỗng");
                continue;
            }

            maCT = maCT.trim();

            // ⭐ NẾU CHỨA DẤU PHẨY, TÁCH RA
            if (maCT.contains(",")) {
                String[] parts = maCT.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        // Gọi đệ quy hoặc thêm vào danh sách
                        List<String> singleList = new ArrayList<>();
                        singleList.add(trimmed);
                        saveAllDetails(maGiamGia, maSanPham, singleList);
                    }
                }
                continue;
            }

            System.out.println("Đang xử lý maCT: " + maCT);

            // Tìm SanPhamChiTiet theo ID
            var chiTietSP = sanPhamChiTietRepo.findbyId(maCT).orElse(null);

            if (chiTietSP != null) {
                // KIỂM TRA: Biến thể này có thuộc về sản phẩm cha không?
                if (chiTietSP.getSanPham() != null && chiTietSP.getSanPham().getMaSanPham().equals(maSanPham)) {

                    // ⭐ KIỂM TRA XEM ĐÃ TỒN TẠI CHƯA
                    boolean exists = repo.existsByDotGiamGia_MaGiamGiaAndSanPham_MaSanPhamAndSanPhamChiTiet_MaSanPhamChiTiet(
                            maGiamGia, maSanPham, maCT
                    );

                    if (!exists) {
                        ChiTietDotGiamGia ct = new ChiTietDotGiamGia();
                        ct.setDotGiamGia(dotGiamGia);
                        ct.setSanPham(sanPham);
                        ct.setSanPhamChiTiet(chiTietSP);
                        repo.save(ct);
                        savedCount++;
                        System.out.println("✅ Đã lưu chi tiết cho maCT: " + maCT);
                    } else {
                        System.out.println("ℹ️ Chi tiết đã tồn tại, bỏ qua: " + maCT);
                    }
                } else {
                    System.err.println("❌ Biến thể " + maCT + " không thuộc về sản phẩm " + maSanPham);
                }
            } else {
                System.err.println("❌ Không tìm thấy SanPhamChiTiet với ID: " + maCT);
            }
        }

        System.out.println("=== saveAllDetails END ===");
        System.out.println("Đã lưu " + savedCount + " chi tiết mới");
        return savedCount;
    }



    public List<String> getSanPhamByDot(String idDot) {
        return repo.findSanPhamByDot(idDot);
    }

    public List<String> getSanPhamChiTietByDot(String idDot) {
        return repo.findSanPhamChiTietByDot(idDot);
    }

    // ===== 3. PHƯƠNG THỨC XOÁ CHI TIẾT THEO MÃ ĐỢT GIẢM GIÁ =====
    @Transactional
    public void deleteByDotId(String maGiamGia) {
        repo.deleteByDotGiamGia_MaGiamGia(maGiamGia);
    }

    // Trong ChiTietDotGiamGiaService.java
    public List<ChiTietDotGiamGia> findBySanPhamChiTiet_MaSanPhamChiTiet(String maSanPhamChiTiet) {
        return repo.findBySanPhamChiTiet_MaSanPhamChiTiet(maSanPhamChiTiet);
    }
}