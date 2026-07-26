package com.example.th06876_java202.Entity;

    import jakarta.persistence.*;
    import jakarta.validation.constraints.*; // Thêm import này
    import lombok.AllArgsConstructor;
    import lombok.Getter;
    import lombok.NoArgsConstructor;
    import lombok.Setter;
    import org.springframework.format.annotation.DateTimeFormat;

    import java.time.LocalDate;
    import java.util.ArrayList;
    import java.util.List;

    @Entity
    @Table(name = "KhachHang")
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public class KhachHang implements java.io.Serializable {
        @Id
        @Column(name = "MaKhachHang")
        private String maKH;

        @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
        @Pattern(
                regexp = "^[\\p{L} ]*$",
                message = "Họ tên chỉ được chứa chữ cái và khoảng trắng"
        )

        @Column(name = "HoTen", nullable = true)
        private String hoTen;

        @Column(name = "SoDienThoai", length = 15, nullable = true, unique = false)
        @Pattern(regexp = "^$|^(0[0-9]{9,10}|[0-9]{10})$",
                message = "Số điện thoại phải có 10-11 số, bắt đầu bằng 0")
        private String sdt;

        @Column(name = "Email", length = 100, nullable = true)
        @Pattern(regexp = "^$|^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$",
                message = "Email không đúng định dạng")
        private String email;

        @OneToMany(mappedBy = "khachHang",
                cascade = CascadeType.ALL,
                orphanRemoval = true)
        private List<DiaChi> danhSachDiaChi = new ArrayList<>();

        @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @Column(name = "NgaySinh")
        private LocalDate ngaySinh;

        @Column(name = "GioiTinh")
        private Boolean gioiTinh;

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @Column(name = "NgayDangKy")
        private LocalDate ngayDangKy = LocalDate.now();

        @Column(name = "GhiChu")
        private String ghiChu;

        @Column(name = "TrangThai")
        private boolean trangThai = true;

        @OneToOne
        @JoinColumn(name = "MaTaiKhoan", referencedColumnName = "MaTaiKhoan")
        private TaiKhoan taiKhoan;
    }