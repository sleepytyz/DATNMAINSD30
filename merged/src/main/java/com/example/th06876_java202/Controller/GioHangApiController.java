package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.HoaDonChiTiet;
import com.example.th06876_java202.Entity.SanPhamChiTiet;
import com.example.th06876_java202.Repository.HoaDonChiTietRepository;
import com.example.th06876_java202.Repository.SanPhamChiTietRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// GioHangApiController.java (Tạo mới)
@RestController
@RequestMapping("/api/giohang")
public class GioHangApiController {

    @Autowired
    private HoaDonChiTietRepository hdctRepository;

    @Autowired
    private SanPhamChiTietRepository spctRepository;

    @GetMapping("/{maHoaDon}")
    public ResponseEntity<?> getCartItems(@PathVariable String maHoaDon) {
        List<HoaDonChiTiet> items = hdctRepository.findByMaHoaDon_MaHoaDon(maHoaDon);

        List<Map<String, Object>> result = items.stream().map(item -> {
            String maSPCT = item.getSanPhamChiTiet().getMaSanPhamChiTiet();
            SanPhamChiTiet sp = spctRepository.findById(maSPCT).orElse(null);

            Map<String, Object> map = new HashMap<>();
            map.put("maSPCT", maSPCT);
            map.put("soLuong", item.getSoLuong());
            map.put("giaCu", item.getDonGia()); // Giá cũ trong hóa đơn
            map.put("giaMoi", sp != null ? sp.getGiaBan() : item.getDonGia()); // Giá mới
            map.put("tonKho", sp != null ? sp.getSoLuongTon() : 0);
            map.put("tenSanPham", sp != null ? sp.getSanPham().getTenSanPham() : "");
            map.put("mauSac", sp != null ? sp.getMauSac().getTenMauSac() : "");
            map.put("kichThuoc", sp != null ? sp.getKichThuoc().getTenKichThuoc() : "");
            map.put("daThayDoiGia", sp != null && !sp.getGiaBan().equals(item.getDonGia()));

            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}