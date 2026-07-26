package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "SanPham")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SanPham {

    @Id
    @Column(name = "MaSanPham")
    private String maSanPham;

    @ManyToOne
    @JoinColumn(name = "MaDanhMuc")
    private DanhMucSanPham danhMucSanPham;

    @NotBlank(message = "Không bỏ trống tên sản phẩm")
    @Pattern( regexp = "^[\\p{L}\\d\\s]*$", message = "Tên sản phẩm chỉ được chứa chữ cái và khoảng trắng")
    @Column(name = "TenSanPham")
    private String tenSanPham;

    @Pattern( regexp = "^[\\p{L}\\d\\s]*$", message = "Mô tả sản phẩm chỉ được chứa chữ cái và khoảng trắng")
    @Column(name = "MoTa")
    private String moTa;

    @ManyToOne
    @JoinColumn(name = "MaChatLieu")
    private ChatLieu chatLieu;

    @NotNull(message = "Vui lòng chọn trạng thái")
    @Column(name = "TrangThai")
    private Boolean trangThai;

    @Column(name = "NgayTao")
    private LocalDateTime ngayTao;

    @ManyToOne
    @JoinColumn(name = "MaThuongHieu")
    private ThuongHieu thuongHieu;

    @ManyToOne
    @JoinColumn(name = "MaKieuGiay")
    private KieuGiay kieuGiay;

    @Column( name = "GiaBanTrungBinh")
    private BigDecimal giaBanTrungBinh;

    @OneToMany(mappedBy = "sanPham")
    private List<SanPhamChiTiet> sanPhamChiTiets;

    public int getTongTon() {
        if (sanPhamChiTiets == null) {
            return 0;
        }

        return sanPhamChiTiets.stream()
                .mapToInt(spct -> spct.getSoLuongTon() == null ? 0 : spct.getSoLuongTon())
                .sum();
    }
}