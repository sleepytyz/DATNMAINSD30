package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "NhanVien")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NhanVien implements java.io.Serializable {
    @Id
    @Column(name = "MaNhanVien", length = 20)
    private String maNhanVien;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên không quá 100 ký tự")
    @Pattern(
            regexp = "^[\\p{L}\\s]+$",
            message = "Họ tên chỉ được chứa chữ cái và khoảng trắng"
    )
    @Column(name = "HoTen", nullable = false, length = 100)
    private String hoTen;

    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    @Column(name = "SoDienThoai", unique = true, length = 10)
    private String soDienThoai;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Column(name = "Email", unique = true, length = 100)
    private String email;

    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "NgaySinh")
    private LocalDate ngaySinh;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    @Column(name = "DiaChi", length = 255)
    private String diaChi;

    @Column(name = "GioiTinh")
    private Boolean gioiTinh;

    @Column(name = "ChucVu", length = 50)
    private String chucVu;

    @Column(name = "LuongCoBan", precision = 18, scale = 2)
    private BigDecimal luongCoBan;

    @Column(name = "NgayVaoLam")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime ngayVaoLam;

    @Column(name = "TrangThai")
    private Boolean trangThai;

    @Column(name = "GhiChu", length = 255)
    private String ghiChu;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "MaTaiKhoan", referencedColumnName = "MaTaiKhoan")
    private TaiKhoan taiKhoan;

    @OneToMany(mappedBy = "nhanVien", fetch = FetchType.LAZY)
    private transient List<ChamCong> danhSachChamCong;

    @OneToMany(mappedBy = "nhanVienBanGiao", fetch = FetchType.LAZY)
    private transient List<GiaoCa> danhSachBanGiao;

    @OneToMany(mappedBy = "nhanVienNhanGiao", fetch = FetchType.LAZY)
    private transient List<GiaoCa> danhSachNhanGiao;
}