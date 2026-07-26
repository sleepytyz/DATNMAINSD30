package com.example.th06876_java202.Service;



import com.example.th06876_java202.Entity.KhachHang;
import com.example.th06876_java202.Entity.NhanVien;
import com.example.th06876_java202.Entity.TaiKhoan;
import com.example.th06876_java202.Repository.KhachHangRepository;
import com.example.th06876_java202.Repository.NhanVienRepository;
import com.example.th06876_java202.Repository.TaiKhoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaiKhoanService {

//    private final Repo_Account repo;
//
//    public AccountServiceImpl(Repo_Account repo) {
//        this.repo = repo;
//    }
//
//    public List<TaiKhoan> getAll() {
//        return repo.findAll();
//    }
//
//    public List<TaiKhoan> search(String keyword) {
//        if(keyword == null || keyword.trim().isEmpty()){
//            return repo.findAll();
//        }
//        return repo.findByTenDangNhapContaining(keyword);
//    }
//
//    public TaiKhoan save(TaiKhoan account) {
//        return repo.save(account);
//    }
//
//    public void lock(Integer id) {
//        TaiKhoan account = repo.findById(id).orElse(null);
//        if(account != null){
//            account.setTrangThai(false);
//            repo.save(account);
//        }
//    }
//
//    public void unlock(Integer id) {
//        TaiKhoan account = repo.findById(id).orElse(null);
//        if(account != null){
//            account.setTrangThai(true);
//            repo.save(account);
//        }
//    }
//
//    public void delete(Integer id){
//        TaiKhoan account = repo.findById(id).orElse(null);
//        if(account != null){
//            repo.delete(account);
//        }
//    }
//
//    public TaiKhoan dangnhap(String tenDangNhap, String matkhau) {
//        return repo.findTenDangNhap(tenDangNhap,matkhau);
//    }
//
//    public TaiKhoan findByTenDangNhap(String tendn) {
//        return repo.findByTenDangNhap(tendn);
//    }
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;

    public List<TaiKhoan> fetchUsers() {
        List<TaiKhoan> tkList = taiKhoanRepository.findAll();
        return tkList;
    }

    public void createUser(TaiKhoan taiKhoan) {
        taiKhoan.setMatKhau(passwordEncoder.encode(taiKhoan.getMatKhau()));
        taiKhoan.setVaiTro("USER");
        taiKhoan.setTrangThai(true);
        taiKhoanRepository.save(taiKhoan);
    }

    public void save(TaiKhoan taiKhoan) {
        taiKhoanRepository.save(taiKhoan);
    }

    public TaiKhoan findUserById(int id) {
        Optional<TaiKhoan> tk = taiKhoanRepository.findById(id);
        return tk.orElse(null);
    }

    public void updateUser(TaiKhoan taiKhoan) {
        taiKhoan.setMatKhau(passwordEncoder.encode(taiKhoan.getMatKhau()));
        taiKhoanRepository.save(taiKhoan);
    }

    public void deleteUserById(int id) {
        taiKhoanRepository.deleteById(id);
    }

    public TaiKhoan findUserByTenDangNhap(String name) {
        Optional<TaiKhoan> userOpt = taiKhoanRepository.findByTenDangNhap(name);
        return userOpt.orElse(null);
    }

    public boolean isTenDangNhapExist(String name) {
        return taiKhoanRepository.existsByTenDangNhap(name);
    }

    public void handleRegister(TaiKhoan taiKhoan) {
        taiKhoan.setMatKhau(passwordEncoder.encode(taiKhoan.getMatKhau()));
        taiKhoan.setVaiTro("USER");

        taiKhoanRepository.save(taiKhoan);
    }

    public TaiKhoan findByEmail(String email) {
        Optional<KhachHang> khachHang = khachHangRepository.findByEmail(email);
        if (khachHang.isPresent() && khachHang.get().getTaiKhoan() != null) {
            return khachHang.get().getTaiKhoan();
        }

        Optional<NhanVien> nhanVien = nhanVienRepository.findByEmail(email);
        if (nhanVien.isPresent() && nhanVien.get().getTaiKhoan() != null) {
            return nhanVien.get().getTaiKhoan();
        }

        return null;
    }

    /**
     * Đăng ký tài khoản khách hàng đầy đủ: tạo TaiKhoan (vai trò USER) và hồ sơ KhachHang liên kết.
     */
    @org.springframework.transaction.annotation.Transactional
    public KhachHang registerCustomer(TaiKhoan taiKhoan, KhachHang khachHang) {
        taiKhoan.setMatKhau(passwordEncoder.encode(taiKhoan.getMatKhau()));
        taiKhoan.setVaiTro("USER");
        taiKhoan.setTrangThai(true);
        TaiKhoan savedTk = taiKhoanRepository.save(taiKhoan);

        if (khachHang.getMaKH() == null || khachHang.getMaKH().isBlank()) {
            khachHang.setMaKH(taoMaKhachHang());
        }
        khachHang.setTaiKhoan(savedTk);
        khachHang.setTrangThai(true);
        khachHang.setNgayDangKy(java.time.LocalDate.now());
        return khachHangRepository.save(khachHang);
    }

    /** Sinh mã khách hàng duy nhất dạng KHxxxx (khớp cột MaKhachHang VARCHAR(20)). */
    private String taoMaKhachHang() {
        java.util.Random random = new java.util.Random();
        String code;
        int attempts = 0;
        do {
            code = "KH" + (1000 + random.nextInt(9000));
            attempts++;
            if (attempts > 100) {
                code = "KH" + System.currentTimeMillis();
                break;
            }
        } while (khachHangRepository.existsById(code));
        return code;
    }

    /**
     * Đổi mật khẩu cho tài khoản đang đăng nhập, có kiểm tra mật khẩu cũ.
     * Trả về null nếu thành công, hoặc thông báo lỗi nếu thất bại.
     */
    @org.springframework.transaction.annotation.Transactional
    public String doiMatKhau(Integer maTaiKhoan, String matKhauCu, String matKhauMoi) {
        TaiKhoan tk = taiKhoanRepository.findById(maTaiKhoan).orElse(null);
        if (tk == null) return "Không tìm thấy tài khoản.";
        if (!passwordEncoder.matches(matKhauCu, tk.getMatKhau())) {
            return "Mật khẩu hiện tại không đúng.";
        }
        tk.setMatKhau(passwordEncoder.encode(matKhauMoi));
        taiKhoanRepository.save(tk);
        return null;
    }
}