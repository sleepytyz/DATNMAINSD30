package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.GiamGia;
import com.example.th06876_java202.Entity.GiamGiaChiTiet;
import com.example.th06876_java202.Entity.GiamGiaChiTietId;
import com.example.th06876_java202.Entity.KhachHang;
import com.example.th06876_java202.Repository.GiamGiaChiTietRepo;
import com.example.th06876_java202.Repository.GiamGiaRepository;
import com.example.th06876_java202.Repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GiamGiaChiTietService {

    @Autowired
    GiamGiaRepository giamGiaRepository;

    @Autowired
    KhachHangRepository khachHangRepository;

    @Autowired
    GiamGiaChiTietRepo giamGiaChiTietRepo;

    public boolean existsById(GiamGiaChiTietId id) {
        return giamGiaChiTietRepo.existsById(id);
    }

    @Transactional
    public void updateTrangThaiToNgungHoatDong(String id) {
        GiamGia giamGia = giamGiaRepository.findById(id).orElse(null);
        if (giamGia != null) {
            giamGia.setTrangThai("Ngừng hoạt động");
            giamGiaRepository   .save(giamGia);

            giamGiaChiTietRepo.updateTrangThaiSuDungByMaGiamGia(id, 2);
        }
    }

    @Transactional
    public void ganVoucher(String maKhachHang, String maGiamGia) {
        GiamGiaChiTietId id = new GiamGiaChiTietId(maKhachHang, maGiamGia);

        if (giamGiaChiTietRepo.existsById(id)) return;
        KhachHang kh = khachHangRepository.getReferenceById(maKhachHang);
        GiamGia gg = giamGiaRepository.getReferenceById(maGiamGia);

        GiamGiaChiTiet chiTiet = new GiamGiaChiTiet();
        chiTiet.setId(id);
        chiTiet.setKhachHang(kh);
        chiTiet.setGiamGia(gg);
        chiTiet.setNgayNhan(LocalDateTime.now());
        chiTiet.setTrangThaiSuDung(0);

        giamGiaChiTietRepo.save(chiTiet);
        giamGiaChiTietRepo.flush();
    }
    public List<GiamGiaChiTiet> getVouchersByKhachHang(String maKhachHang) {
        System.out.println("=== GiamGiaChiTietService.getVouchersByKhachHang ===");
        System.out.println("maKhachHang: " + maKhachHang);

        try {
            if (maKhachHang == null || maKhachHang.trim().isEmpty()) {
                return new ArrayList<>();
            }

            // Kiểm tra khách hàng tồn tại
            KhachHang khachHang = khachHangRepository.findById(maKhachHang).orElse(null);
            System.out.println("Khách hàng tìm thấy: " + (khachHang != null ? khachHang.getHoTen() : "NULL"));

            if (khachHang == null) {
                System.out.println("Không tìm thấy khách hàng với mã: " + maKhachHang);
                return new ArrayList<>();
            }

            // Sử dụng method có sẵn
            List<GiamGiaChiTiet> result = giamGiaChiTietRepo.findByKhachHang_MaKH(maKhachHang);
            System.out.println("Kết quả: " + (result != null ? result.size() : 0) + " bản ghi");

            return result != null ? result : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("LỖI trong getVouchersByKhachHang: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<GiamGiaChiTiet> getValidVouchersByKhachHang(String maKhachHang) {
        System.out.println("=== getValidVouchersByKhachHang ===");
        System.out.println("maKhachHang: " + maKhachHang);

        try {
            if (maKhachHang == null || maKhachHang.trim().isEmpty()) {
                return new ArrayList<>();
            }

            // ⭐ GỌI HÀM NATIVE QUERY CÓ JOIN
            List<GiamGiaChiTiet> result = giamGiaChiTietRepo.findValidVouchersByKhachHangNative(maKhachHang);
            System.out.println("Số voucher còn hiệu lực: " + (result != null ? result.size() : 0));

            return result != null ? result : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("Lỗi getValidVouchersByKhachHang: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
