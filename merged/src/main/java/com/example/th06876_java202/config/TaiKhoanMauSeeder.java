package com.example.th06876_java202.config;

import com.example.th06876_java202.Entity.KhachHang;
import com.example.th06876_java202.Entity.NhanVien;
import com.example.th06876_java202.Entity.TaiKhoan;
import com.example.th06876_java202.Repository.KhachHangRepository;
import com.example.th06876_java202.Repository.NhanVienRepository;
import com.example.th06876_java202.Repository.TaiKhoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TẠO SẴN 3 TÀI KHOẢN MẪU KHI KHỞI ĐỘNG (ADMIN / STAFF / USER).
 *
 * ==================== VÌ SAO CẦN CLASS NÀY ====================
 * Trước đây mật khẩu BCrypt được viết CỨNG trong file .sql. Mỗi lần tạo DB mới,
 * chuỗi hash trong SQL thường không khớp với BCrypt của ứng dụng (do copy thiếu ký tự,
 * do hash được sinh từ máy khác, do encoding...) -> KHÔNG ĐĂNG NHẬP ĐƯỢC, cả nhóm phải
 * tự đăng ký tài khoản mới rất mất công.
 *
 * Class này để CHÍNH JAVA mã hoá mật khẩu rồi lưu xuống DB, nên hash LUÔN KHỚP.
 * Tạo DB mới -> chạy app -> có ngay 3 tài khoản đăng nhập được.
 *
 * ==================== TÀI KHOẢN MẪU ====================
 *      admin / 123456   (ROLE_ADMIN - toàn quyền)
 *      staff / 123456   (ROLE_STAFF - nhân viên bán hàng)
 *      user  / 123456   (ROLE_USER  - khách hàng)
 *
 * LƯU Ý: mật khẩu vẫn được MÃ HOÁ BCrypt trong DB (đúng chuẩn bảo mật, không lưu
 * plaintext). Chỉ là bạn luôn BIẾT mật khẩu gốc là 123456 để đăng nhập/test.
 *
 * An toàn khi chạy nhiều lần: đã tồn tại thì bỏ qua, KHÔNG ghi đè, KHÔNG tạo trùng.
 */
@Component
@RequiredArgsConstructor
public class TaiKhoanMauSeeder implements CommandLineRunner {

    private final TaiKhoanRepository taiKhoanRepository;
    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;
    private final PasswordEncoder passwordEncoder;

    /** Mật khẩu gốc dùng chung cho cả 3 tài khoản mẫu - đổi ở đây nếu muốn */
    private static final String MAT_KHAU_MAU = "123456";

    @Override
    @Transactional
    public void run(String... args) {
        try {
            taoAdmin();
            taoStaff();
            taoUser();
        } catch (Exception e) {
            // Không để lỗi seed làm sập app
            System.out.println("⚠️ Lỗi khi tạo tài khoản mẫu: " + e.getMessage());
        }
    }

    // ==================================================================
    // ADMIN
    // ==================================================================
    private void taoAdmin() {
        // Nếu tài khoản đã tồn tại (vd 'admin' được tạo sẵn bởi file .sql với hash cứng
        // không đăng nhập được) -> ĐẶT LẠI mật khẩu về MAT_KHAU_MAU cho chắc chắn login được.
        var tkCu = taiKhoanRepository.findByTenDangNhap("admin");
        if (tkCu.isPresent()) {
            datLaiMatKhau(tkCu.get(), "ADMIN");
            return;
        }

        TaiKhoan tk = new TaiKhoan();
        tk.setTenDangNhap("admin");
        tk.setMatKhau(passwordEncoder.encode(MAT_KHAU_MAU)); // Java tự mã hoá -> luôn khớp
        tk.setVaiTro("ADMIN");
        tk.setTrangThai(true);
        tk = taiKhoanRepository.save(tk);

        // Hồ sơ nhân viên đi kèm (cần có để dùng chấm công / mở ca / thống kê cá nhân)
        NhanVien nv = new NhanVien();
        nv.setMaNhanVien("NV000001");
        nv.setHoTen("Quản trị viên");
        nv.setEmail("admin@fsshop.com");
        nv.setSoDienThoai("0900000001");
        nv.setChucVu("Quản lý");
        nv.setLuongCoBan(new BigDecimal("15000000"));
        nv.setNgaySinh(LocalDate.of(1995, 1, 1));
        nv.setGioiTinh(true);
        nv.setDiaChi("FS Shop");
        nv.setNgayVaoLam(LocalDateTime.now());
        nv.setTrangThai(true);
        nv.setTaiKhoan(tk);
        nhanVienRepository.save(nv);

        System.out.println("✅ Đã tạo tài khoản mẫu: admin / " + MAT_KHAU_MAU + " (ADMIN)");
    }

    /**
     * Đặt lại mật khẩu của tài khoản đã tồn tại về MAT_KHAU_MAU.
     * Giải quyết trường hợp file .sql đã tạo sẵn tài khoản với hash BCrypt hỏng.
     */
    private void datLaiMatKhau(TaiKhoan tk, String vaiTroMongMuon) {
        tk.setMatKhau(passwordEncoder.encode(MAT_KHAU_MAU));
        tk.setTrangThai(true);
        if (tk.getVaiTro() == null || tk.getVaiTro().isBlank()) {
            tk.setVaiTro(vaiTroMongMuon);
        }
        taiKhoanRepository.save(tk);
        System.out.println("🔑 Đã đặt lại mật khẩu: " + tk.getTenDangNhap()
                + " / " + MAT_KHAU_MAU + " (" + tk.getVaiTro() + ")");
    }

    // ==================================================================
    // STAFF
    // ==================================================================
    private void taoStaff() {
        var tkCu = taiKhoanRepository.findByTenDangNhap("staff");
        if (tkCu.isPresent()) {
            datLaiMatKhau(tkCu.get(), "STAFF");
            return;
        }

        TaiKhoan tk = new TaiKhoan();
        tk.setTenDangNhap("staff");
        tk.setMatKhau(passwordEncoder.encode(MAT_KHAU_MAU));
        tk.setVaiTro("STAFF");
        tk.setTrangThai(true);
        tk = taiKhoanRepository.save(tk);

        NhanVien nv = new NhanVien();
        nv.setMaNhanVien("NV000002");
        nv.setHoTen("Nhân viên bán hàng");
        nv.setEmail("staff@fsshop.com");
        nv.setSoDienThoai("0900000002");
        nv.setChucVu("Nhân viên");
        nv.setLuongCoBan(new BigDecimal("8000000"));
        nv.setNgaySinh(LocalDate.of(2000, 1, 1));
        nv.setGioiTinh(true);
        nv.setDiaChi("FS Shop");
        nv.setNgayVaoLam(LocalDateTime.now());
        nv.setTrangThai(true);
        nv.setTaiKhoan(tk);
        nhanVienRepository.save(nv);

        System.out.println("✅ Đã tạo tài khoản mẫu: staff / " + MAT_KHAU_MAU + " (STAFF)");
    }

    // ==================================================================
    // USER (khách hàng)
    // ==================================================================
    private void taoUser() {
        var tkCu = taiKhoanRepository.findByTenDangNhap("user");
        if (tkCu.isPresent()) {
            datLaiMatKhau(tkCu.get(), "USER");
            return;
        }

        TaiKhoan tk = new TaiKhoan();
        tk.setTenDangNhap("user");
        tk.setMatKhau(passwordEncoder.encode(MAT_KHAU_MAU));
        tk.setVaiTro("USER");
        tk.setTrangThai(true);
        tk = taiKhoanRepository.save(tk);

        // Hồ sơ khách hàng đi kèm (để mua hàng, xem đơn...)
        KhachHang kh = new KhachHang();
        kh.setMaKH("KH000001");
        kh.setHoTen("Khách hàng demo");
        kh.setEmail("user@fsshop.com");
        kh.setSdt("0900000003");
        kh.setNgaySinh(LocalDate.of(2000, 6, 15));
        kh.setGioiTinh(true);
        kh.setNgayDangKy(LocalDate.now());
        kh.setTrangThai(true);
        kh.setTaiKhoan(tk);
        khachHangRepository.save(kh);

        System.out.println("✅ Đã tạo tài khoản mẫu: user / " + MAT_KHAU_MAU + " (USER)");
    }
}
