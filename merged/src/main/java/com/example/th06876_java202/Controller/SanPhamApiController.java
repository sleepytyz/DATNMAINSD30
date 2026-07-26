package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.SanPhamChiTiet;
import com.example.th06876_java202.Repository.SanPhamChiTietRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sanpham")
public class SanPhamApiController {

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllProductVariants() {
        List<SanPhamChiTiet> variants = sanPhamChiTietRepository.findAll();

        List<Map<String, Object>> result = variants.stream()
                .map(sp -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("maSanPhamChiTiet", sp.getMaSanPhamChiTiet());
                    map.put("tenSanPham", sp.getSanPham().getTenSanPham());
                    map.put("giaBan", sp.getGiaBan());
                    map.put("soLuongTon", sp.getSoLuongTon());
                    map.put("mauSac", sp.getMauSac().getTenMauSac());
                    map.put("kichThuoc", sp.getKichThuoc().getTenKichThuoc());

                    BigDecimal giaSauGiam = sp.getGiaBan();
                    map.put("giaSauGiam", giaSauGiam);

                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }


    @GetMapping("/detail/{maSPCT}")
    public ResponseEntity<?> getProductDetail(@PathVariable String maSPCT) {
        Optional<SanPhamChiTiet> spOpt = sanPhamChiTietRepository.findById(maSPCT);
        if (spOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SanPhamChiTiet sp = spOpt.get();
        Map<String, Object> map = new HashMap<>();
        map.put("maSanPhamChiTiet", sp.getMaSanPhamChiTiet());
        map.put("tenSanPham", sp.getSanPham().getTenSanPham());
        map.put("giaBan", sp.getGiaBan());
        map.put("soLuongTon", sp.getSoLuongTon());
        map.put("mauSac", sp.getMauSac().getTenMauSac());
        map.put("kichThuoc", sp.getKichThuoc().getTenKichThuoc());
        map.put("giaSauGiam", sp.getGiaBan());

        return ResponseEntity.ok(map);
    }
}
