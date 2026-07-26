package com.example.th06876_java202.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "SanPhamChiTiet")
@JsonIgnoreProperties({"sanPham", "kichThuoc", "mauSac"})
public class SanPhamChiTiet {

    @Id
    @Column(name = "MaSanPhamChiTiet")
    private String maSanPhamChiTiet;

    @ManyToOne
    @JoinColumn(name = "MaSanPham")
    private SanPham sanPham;

    @ManyToOne
    @JoinColumn(name = "MaKichThuoc")
    private KichThuoc kichThuoc;

    @ManyToOne
    @JoinColumn(name = "MaMauSac")
    private MauSac mauSac;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0", message = "Giá bán phải lớn hơn 0")
    @Column(name = "GiaBan")
    private BigDecimal giaBan;

    @NotNull(message = "Số lượng tồn không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    @Column(name = "SoLuongTon")
    private Integer soLuongTon;

    @Column(name = "SoLuongDangGiu")
    private Integer soLuongDangGiu;

    @Column(name = "NgayTao")
    private LocalDateTime ngayTao;

    @Column(name = "TrangThai")
    private String trangThai;

    @Column(name = "DuongDanAnh")
    private String duongDanAnh;

    @Column(name = "DanhSachAnh", columnDefinition = "NVARCHAR(MAX)")
    private String danhSachAnh;

    public List<String> getDanhSachAnhList() {
        if (danhSachAnh == null || danhSachAnh.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(danhSachAnh, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            String[] parts = danhSachAnh.split(",");
            List<String> result = new ArrayList<>();
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return result;
        }
    }

    public void setDanhSachAnhList(List<String> images) {
        if (images == null || images.isEmpty()) {
            this.danhSachAnh = null;
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            this.danhSachAnh = mapper.writeValueAsString(images);
        } catch (Exception e) {
            this.danhSachAnh = String.join(",", images);
        }
    }

    public String getAnhDaiDien() {
        if (duongDanAnh != null && !duongDanAnh.isEmpty()) {
            return duongDanAnh;
        }
        List<String> images = getDanhSachAnhList();
        return images.isEmpty() ? null : images.get(0);
    }

    public int getSoLuongAnhPhu() {
        List<String> images = getDanhSachAnhList();
        return images != null ? images.size() : 0;
    }

    public boolean hasAnhPhu() {
        return getSoLuongAnhPhu() > 0;
    }

}