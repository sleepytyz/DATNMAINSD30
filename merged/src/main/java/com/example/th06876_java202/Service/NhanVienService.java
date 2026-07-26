package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.NhanVien;
import com.example.th06876_java202.Repository.NhanVienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class NhanVienService {
    private final NhanVienRepository nhanVienRepository;
    private final Random random = new Random();

    // ===== TẠO MÃ NHÂN VIÊN RANDOM =====
    public String generateMaNhanVien() {
        String code;
        boolean exists;
        int attempts = 0;
        int maxAttempts = 100;

        do {
            int randomNumber = 1000 + random.nextInt(9000);
            code = "NV" + randomNumber;
            exists = nhanVienRepository.existsById(code);
            attempts++;

            if (attempts > maxAttempts) {
                code = "NV" + System.currentTimeMillis();
                break;
            }
        } while (exists);

        return code;
    }

    // ===== LẤY TẤT CẢ NHÂN VIÊN =====
    public List<NhanVien> getAllNhanVien() {
        return nhanVienRepository.findAll();
    }

    // ===== TÌM NHÂN VIÊN THEO ID =====
    public NhanVien getNhanVienById(String maNV) {
        return nhanVienRepository.findById(maNV).orElse(null);
    }

    // ===== TÌM THEO TÊN ĐĂNG NHẬP =====
    public NhanVien findByUsername(String username) {
        return nhanVienRepository.findByTaiKhoan_TenDangNhap(username);
    }

    // ===== TÌM KIẾM NHÂN VIÊN (CÓ LỌC) =====
    public List<NhanVien> search(String keyword, String role, Boolean status) {
        List<NhanVien> list = nhanVienRepository.findAll();

        return list.stream()
                .filter(nv -> keyword == null || keyword.isBlank()
                        || nv.getHoTen().toLowerCase().contains(keyword.toLowerCase())
                        || nv.getSoDienThoai().contains(keyword))
                .filter(nv -> role == null || role.isBlank()
                        || nv.getChucVu().equals(role))
                .filter(nv -> status == null
                        || nv.getTrangThai().equals(status))
                .toList();
    }

    // ===== THAO TÁC CRUD =====
    public NhanVien save(NhanVien nhanVien) {
        return nhanVienRepository.save(nhanVien);
    }

    public void delete(String maNV) {
        nhanVienRepository.deleteById(maNV);
    }

    // ===== KHÓA/MỞ KHÓA NHÂN VIÊN =====
    public void lock(String maNV) {
        NhanVien nv = nhanVienRepository.findById(maNV).orElse(null);
        if (nv != null) {
            nv.setTrangThai(false);
            nhanVienRepository.save(nv);
        }
    }

    public void unlock(String maNV) {
        NhanVien nv = nhanVienRepository.findById(maNV).orElse(null);
        if (nv != null) {
            nv.setTrangThai(true);
            nhanVienRepository.save(nv);
        }
    }

    // ===== KIỂM TRA TỒN TẠI =====
    public boolean existsById(String maNV) {
        return nhanVienRepository.existsById(maNV);
    }

    public boolean existsBySoDienThoai(String soDienThoai) {
        return nhanVienRepository.existsBySoDienThoai(soDienThoai);
    }

    public boolean existsByEmail(String email) {
        return nhanVienRepository.existsByEmail(email);
    }

    public boolean existsBySoDienThoaiAndNotMaNV(String soDienThoai, String maNV) {
        return nhanVienRepository.existsBySoDienThoaiAndMaNhanVienNot(soDienThoai, maNV);
    }

    public boolean existsByEmailAndNotMaNV(String email, String maNV) {
        return nhanVienRepository.existsByEmailAndMaNhanVienNot(email, maNV);
    }
}