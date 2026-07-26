package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.*;
import com.example.th06876_java202.Repository.SanPhamChiTietRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class SanPhamChiTietService {

    @Autowired
    private MauSacService mauSacService;

    @Autowired
    private KichThuocService kichThuocService;

    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SanPhamChiTietService(SanPhamChiTietRepository sanPhamChiTietRepository) {
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
    }

    public Page<SanPhamChiTiet> getall(Pageable pageable) {
        return sanPhamChiTietRepository.findAll(pageable);
    }

    public List<SanPhamChiTiet> getalll() {
        return sanPhamChiTietRepository.findAll();
    }

    public List<SanPhamChiTiet> getallll() {
        return sanPhamChiTietRepository.findAllOrderByNgayTaoDesc();
    }

    public SanPhamChiTiet them(SanPhamChiTiet sanPhamChiTiet) {
        if (sanPhamChiTiet.getNgayTao() == null) {
            sanPhamChiTiet.setNgayTao(LocalDateTime.now());
        }

        return sanPhamChiTietRepository.save(sanPhamChiTiet);
    }

    public Optional<SanPhamChiTiet> findbyIid(String id) {
        return sanPhamChiTietRepository.findByIdWithSanPham(id);
    }

    public Optional<SanPhamChiTiet> findbyId(String id) {
        return sanPhamChiTietRepository.findById(id);
    }

    public Double gia() {
        return sanPhamChiTietRepository.findMaxGiaBan();
    }

    public int suaSanPham2(String maSanPham) {
        return sanPhamChiTietRepository.updateTrangThai(maSanPham);
    }

    public int updateTrangThai(String id, String trangThai) {
        return sanPhamChiTietRepository.updateTrangThaii(id, trangThai);
    }

    public int suaSanPham3(String maSanPham) {
        return sanPhamChiTietRepository.updateTrangThaiii(maSanPham);
    }

    public List<String> getSize() {
        return sanPhamChiTietRepository.findAllSize();
    }

    public List<String> getMsac() {
        return sanPhamChiTietRepository.findAllMauSac();
    }

    public List<SanPhamChiTiet> getallsp(String maSanPham) {
        return sanPhamChiTietRepository.findByMaSanPham(maSanPham);
    }

    public Page<SanPhamChiTiet> findAllWithFilters(
            String size,
            String msac,
            String tt,
            BigDecimal gia,
            BigDecimal gia2,
            String tonKho,
            Pageable pageable) {

        return sanPhamChiTietRepository.findAllWithFilters(
                size, msac, tt, gia, gia2, tonKho, pageable);
    }

    public List<SanPhamChiTiet> findAllWithFilters(String size, String msac, String tt,
                                                   BigDecimal gia, BigDecimal gia2, String tonKho) {
        return sanPhamChiTietRepository.findAllWithFiltersList(size, msac, tt, gia, gia2, tonKho);
    }

    public void capNhatTrangThaii(SanPhamChiTiet spct) {
        String hienTai = spct.getTrangThai();
        if ("Ngừng bán".equals(hienTai) || "Ngừng kinh doanh".equals(hienTai)) {
            return;
        }
        Integer soLuong = spct.getSoLuongTon();

        if (soLuong == null || soLuong <= 0) {
            spct.setTrangThai("Hết hàng");
        } else if (soLuong < 10) {
            spct.setTrangThai("Sắp hết");
        } else {
            spct.setTrangThai("Còn hàng");
        }
    }

    public BigDecimal getGiaMin(String maSanPham) {
        List<SanPhamChiTiet> list = sanPhamChiTietRepository.findByMaSanPham(maSanPham);
        if (list == null || list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return list.stream()
                .map(SanPhamChiTiet::getGiaBan)
                .filter(g -> g != null && g.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getGiaMax(String maSanPham) {
        List<SanPhamChiTiet> list = sanPhamChiTietRepository.findByMaSanPham(maSanPham);
        if (list == null || list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return list.stream()
                .map(SanPhamChiTiet::getGiaBan)
                .filter(g -> g != null && g.compareTo(BigDecimal.ZERO) > 0)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public List<SanPhamChiTiet> findsp(List<String> listMaSanPham) {
        if (listMaSanPham == null || listMaSanPham.isEmpty()) {
            return new ArrayList<>();
        }
        return sanPhamChiTietRepository.findBySanPham_MaSanPhamIn(listMaSanPham);
    }

    public SanPhamChiTiet getById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return sanPhamChiTietRepository.findById(id).orElse(null);
    }

    @Transactional
    public SanPhamChiTiet kiemTraVaCapNhatTrangThaiSauKhiBan(String maSpct) {
        Optional<SanPhamChiTiet> optional = sanPhamChiTietRepository.findById(maSpct);
        if (optional.isEmpty()) {
            System.out.println(" Không tìm thấy sản phẩm: " + maSpct);
            return null;
        }

        SanPhamChiTiet spct = optional.get();
        Integer tonKhoHienTai = spct.getSoLuongTon();
        String trangThaiHienTai = spct.getTrangThai();
        if ("Ngừng bán".equals(trangThaiHienTai) || "Ngừng kinh doanh".equals(trangThaiHienTai)) {
            System.out.println("Sản phẩm " + maSpct + " đang ngừng bán, không cập nhật trạng thái");
            return spct;
        }

        String trangThaiMoi;
        if (tonKhoHienTai == null || tonKhoHienTai <= 0) {
            trangThaiMoi = "Hết hàng";
            System.out.println(" Sản phẩm " + maSpct + " ĐÃ HẾT HÀNG!");
        } else if (tonKhoHienTai <= 10) {
            trangThaiMoi = "Sắp hết";
            System.out.println(" Sản phẩm " + maSpct + " sắp hết, còn " + tonKhoHienTai);
        } else {
            trangThaiMoi = "Còn hàng";
        }
        if (!trangThaiMoi.equals(trangThaiHienTai)) {
            spct.setTrangThai(trangThaiMoi);
            sanPhamChiTietRepository.save(spct);
            System.out.println(" Cập nhật trạng thái sản phẩm " + maSpct +
                    ": " + trangThaiHienTai + " -> " + trangThaiMoi);
        }

        return spct;
    }

    @Transactional
    public List<SanPhamChiTiet> kiemTraVaCapNhatTrangThaiSauKhiBanNhieu(List<String> danhSachMaSpct) {
        List<SanPhamChiTiet> result = new ArrayList<>();
        for (String maSpct : danhSachMaSpct) {
            SanPhamChiTiet spct = kiemTraVaCapNhatTrangThaiSauKhiBan(maSpct);
            if (spct != null) {
                result.add(spct);
            }
        }
        return result;
    }


}