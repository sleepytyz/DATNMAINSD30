package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.KhachHang;
import com.example.th06876_java202.Repository.KhachHangRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class KhachHangService {
    private final KhachHangRepository khachHangRepo;
    private final Random random = new Random();

    public String generateMaKH() {
        String code;
        boolean exists;
        int attempts = 0;
        int maxAttempts = 100;

        do {
            int randomNumber = 1000 + random.nextInt(9000);
            code = "KH" + randomNumber;
            exists = khachHangRepo.existsById(code);
            attempts++;

            if (attempts > maxAttempts) {
                code = "KH" + System.currentTimeMillis();
                break;
            }
        } while (exists);

        return code;
    }

    public Page<KhachHang> searchByKeyword(String keyword, Pageable pageable) {
        return khachHangRepo.searchByKeyword(keyword, pageable);
    }

    public boolean existsById(String maKH) {
        return khachHangRepo.existsById(maKH);
    }

    public boolean existsBySdt(String sdt) {
        return khachHangRepo.existsBySdt(sdt);
    }

    public boolean existsByEmail(String email) {
        return khachHangRepo.existsByEmail(email);
    }

    public boolean existsBySdtAndNotMaKH(String sdt, String maKH) {
        return khachHangRepo.existsBySdtAndMaKHNot(sdt, maKH);
    }

    public boolean existsByEmailAndNotMaKH(String email, String maKH) {
        return khachHangRepo.existsByEmailAndMaKHNot(email, maKH);
    }

    public List<KhachHang> getkh(){
      return   khachHangRepo.findban();
    }

    public KhachHang findByMaKH(String maKH) {
        return   khachHangRepo.findByMaKH(maKH);
    }

    public List<KhachHang> getAllKhachHang() {
        return khachHangRepo.findAll();
    }

    public Page<KhachHang> getAllKhachHangPagin(Pageable pageable) {
        return khachHangRepo.findAll(pageable);
    }

    public KhachHang getKhachHangById(String maKH) {
        return khachHangRepo.findById(maKH).orElse(null);
    }

    public KhachHang findByTenDangNhap(String tenDangNhap) {
        return khachHangRepo.findByTaiKhoan_TenDangNhap(tenDangNhap).orElse(null);
    }

    public Page<KhachHang> findBySdt(String sdt, Pageable pageable) {
        return khachHangRepo.findBySdtt(sdt, pageable);
    }

    public List<KhachHang> findAllBySdt(String sdt) {
        return khachHangRepo.findAllBySdt(sdt);
    }

    public Page<KhachHang> searchByPhone(String sdt, Pageable pageable) {
        if (sdt == null || sdt.trim().isEmpty()) {
            return khachHangRepo.findAll(pageable);
        }
        return khachHangRepo.findBySdtContaining(sdt, pageable);
    }

    public Page<KhachHang> searchByPhone(String sdt, int page) {
        Pageable pageable = PageRequest.of(page, 5);
        return searchByPhone(sdt, pageable);
    }

    public Page<KhachHang> findByTrangThai(boolean trangThai, Pageable pageable) {
        return khachHangRepo.findByTrangThai(trangThai, pageable);
    }

    public KhachHang save(KhachHang khachHang) {
        return khachHangRepo.save(khachHang);
    }

    public void delete(String id) {
        khachHangRepo.deleteById(id);
    }

    public void lock(String maKH) {
        KhachHang kh = khachHangRepo.findById(maKH).orElse(null);
        if (kh != null) {
            kh.setTrangThai(false);
            khachHangRepo.save(kh);
        }
    }

    public void unlock(String maKH) {
        KhachHang kh = khachHangRepo.findById(maKH).orElse(null);
        if (kh != null) {
            kh.setTrangThai(true);
            khachHangRepo.save(kh);
        }
    }

    public void validateKhachHang(KhachHang kh, BindingResult result) {
        if (kh.getSdt() != null && !kh.getSdt().trim().isEmpty()) {
            String sdtClean = kh.getSdt().trim();
            boolean sdtExists = (kh.getMaKH() == null || kh.getMaKH().isEmpty())
                    ? khachHangRepo.existsBySdt(sdtClean)
                    : khachHangRepo.existsBySdtAndMaKHNot(sdtClean, kh.getMaKH());

            if (sdtExists) {
                result.rejectValue("sdt", "error.kh", "Số điện thoại đã tồn tại!");
            }
        }

        if (kh.getEmail() != null && !kh.getEmail().trim().isEmpty()) {
            String emailClean = kh.getEmail().trim();
            boolean emailExists = (kh.getMaKH() == null || kh.getMaKH().isEmpty())
                    ? khachHangRepo.existsByEmail(emailClean)
                    : khachHangRepo.existsByEmailAndMaKHNot(emailClean, kh.getMaKH());

            if (emailExists) {
                result.rejectValue("email", "error.kh", "Email này đã được sử dụng!");
            }
        }
    }

    @PostConstruct
    public void initKhachLe() {
        try {
            List<KhachHang> existing = khachHangRepo.findAllBySdt("0000000000");
            if (existing == null || existing.isEmpty()) {
                KhachHang khachLe = new KhachHang();
                khachLe.setMaKH("KH000000");
                khachLe.setHoTen("Khách lẻ");
                khachLe.setSdt("0000000000");
                khachLe.setEmail("khachle@fsshop.com");
                khachLe.setNgayDangKy(LocalDate.now());
                khachLe.setTrangThai(true);
                khachLe.setGioiTinh(true);
                khachHangRepo.save(khachLe);
                System.out.println("✅ Đã tạo khách hàng 'Khách lẻ' mặc định!");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Lỗi tạo khách hàng mặc định: " + e.getMessage());
            e.printStackTrace();
        }
    }
}