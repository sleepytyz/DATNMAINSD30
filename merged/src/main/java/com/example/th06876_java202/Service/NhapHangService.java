package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.ChiTietNhapHangDTO;
import com.example.th06876_java202.Entity.PhieuNhapHang;
import com.example.th06876_java202.Entity.PhieuNhapHangDTO;
import com.example.th06876_java202.Entity.SanPhamDetailDTO;
import com.example.th06876_java202.Repository.ChiTietNhapHangRepository;
import com.example.th06876_java202.Repository.PhieuNhapHangRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NhapHangService {
    private final PhieuNhapHangRepository phieuNhapHangRepository;
    private final ChiTietNhapHangRepository chiTietNhapHangRepository;

    // Thêm method mới - lấy có phân trang
    public Page<PhieuNhapHangDTO> getAllPhieuNhapPhanTrang(Pageable pageable) {
        Page<PhieuNhapHang> phieuNhapPage = phieuNhapHangRepository.findAll(pageable);

        return phieuNhapPage.map(entity -> {
            PhieuNhapHangDTO dto = new PhieuNhapHangDTO();
            dto.setMaPhieuNhap(entity.getMaPhieuNhap());
            dto.setTenNhaCungCap(entity.getNhaCungCap().getTenNhaCungCap());
            dto.setNgayNhap(entity.getNgayNhap());
            dto.setTrangThai(entity.getTrangThai());
            return dto;
        });
    }

    public ChiTietNhapHangDTO getChiTietPhieu(Integer maPhieuNhap) {
        PhieuNhapHang entity = phieuNhapHangRepository.findById(maPhieuNhap)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu"));

        ChiTietNhapHangDTO dto = new ChiTietNhapHangDTO();
        dto.setMaPhieuNhap(entity.getMaPhieuNhap());
        dto.setNgayTao(entity.getNgayNhap());
        dto.setTenNhanVien(entity.getNhanVien().getHoTen());
        dto.setTenNhaCungCap(entity.getNhaCungCap().getTenNhaCungCap());
        String trangThai = entity.getTrangThai();
        dto.setTrangThai(trangThai);

        // Map class CSS cho trạng thái
        switch (trangThai) {
            case "Đã nhận":
                dto.setTrangThai("Đã nhận");
                break;
            case "Đang vận chuyển":
                dto.setTrangThai("Đang vận chuyển");
                break;
            case "Đã hủy":
                dto.setTrangThai("Đã hủy");
                break;
            default:
                dto.setTrangThai("Ko có tt");
        }


        List<SanPhamDetailDTO> sanPhams = entity.getChiTietNhapHangList().stream().map(ct -> {
            SanPhamDetailDTO sp = new SanPhamDetailDTO();
            sp.setTenSanPham(ct.getSanPhamChiTiet().getSanPham().getTenSanPham());
            sp.setSize(ct.getSanPhamChiTiet().getKichThuoc().getTenKichThuoc());
            sp.setSoLuong(ct.getSoLuongNhap());
            sp.setDonGia(ct.getDonGiaNhap());
            sp.setThanhTien(ct.getThanhTien());
            return sp;
        }).collect(Collectors.toList());

        dto.setSanPhamDetails(sanPhams);

        BigDecimal tongTien = sanPhams.stream()
                .map(SanPhamDetailDTO::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTongTien(tongTien);

        return dto;
    }

    public List<PhieuNhapHangDTO> getPhieuNhapByStatus(String status) {
        List<PhieuNhapHang> phieuNhapHangs = phieuNhapHangRepository.findByTrangThai(status);

        return phieuNhapHangs.stream().map(entity -> {
            PhieuNhapHangDTO dto = new PhieuNhapHangDTO();
            dto.setMaPhieuNhap(entity.getMaPhieuNhap());
            dto.setTenNhaCungCap(entity.getNhaCungCap().getTenNhaCungCap());
            dto.setNgayNhap(entity.getNgayNhap());
            dto.setTrangThai(entity.getTrangThai());
            return dto;
        }).collect(Collectors.toList());
    }
}