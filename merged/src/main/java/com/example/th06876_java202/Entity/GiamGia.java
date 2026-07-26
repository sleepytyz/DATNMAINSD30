package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "GiamGia")
public class GiamGia {

    @Id
    @Column(name = "MaGiamGia")
    private String maGiamGia;

    @NotBlank(message = "Tên chương trình giảm giá không được để trống")
    @Size(max = 255, message = "Tên chương trình không được vượt quá 255 ký tự")
    @Column(name = "TenChuongTrinh")
    private String tenGiamGia;

    @NotBlank(message = "Loại giảm giá không được để trống")
    @Column(name = "LoaiGiamGia")
    private String loaiGiamGia;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá trị giảm phải lớn hơn 0")
    @Column(name = "GiaTriGiam")
    private BigDecimal giaTriGiam;

    @DecimalMin(value = "0.0", message = "Giá trị giảm tối đa phải lớn hơn hoặc bằng 0")
    @Column(name = "GiamToiDa")
    private BigDecimal giamToiDa;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @NotNull(message = "Ngày bắt đầu không được để trống")
    @Column(name = "NgayBatDau")
    private LocalDateTime ngayBatDau;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @NotNull(message = "Ngày kết thúc không được để trống")
    @Column(name = "NgayKetThuc")
    private LocalDateTime ngayKetThuc;

    @NotNull(message = "Đơn tối thiểu không được để trống")
    @DecimalMin(value = "0.0", message = "Đơn tối thiểu phải lớn hơn hoặc bằng 0")
    @Column(name = "DonToiThieu")
    private BigDecimal donToiThieu;

    @NotBlank(message = "Trạng thái không được để trống")
    @Column(name = "TrangThai")
    private String trangThai = "Sắp hoạt động";

    @Column(name = "LoaiVoucher")
    private String loaiVoucher;

    @NotNull(message = "Vui lòng chọn loại áp dụng")
    @Column(name = "LoaiApDung")
    private Integer loaiApDung;

    @Column(name = "SoLuong")
    @NotNull(groups = {PublicMode.class}, message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng phải lớn hơn hoặc bằng 0")
    private Integer soLuong;

    @Column(name = "NgayTao")
    private LocalDateTime ngayTao;

    @Column(name = "IsVoHan")
    private Boolean isVoHan = false;

    @AssertTrue(message = "Giá trị giảm không hợp lệ cho hình thức này")
    public boolean isGiaTriGiamValid() {
        if (loaiGiamGia == null || giaTriGiam == null) return true;
        if ("PhanTram".equals(loaiGiamGia)) {
            return giaTriGiam.doubleValue() >= 1 && giaTriGiam.doubleValue() <= 100;
        } else if (loaiGiamGia.equals("SoTien")) {
            return giaTriGiam.doubleValue() > 0;
        }
        return true;
    }


    @AssertTrue(message = "Ngày bắt đầu phải ở tương lai hoặc hiện tại")
    public boolean isNgayBatDauValid() {
        if (ngayBatDau == null) return true;
        return !ngayBatDau.isBefore(LocalDateTime.now());
    }

    @AssertTrue(message = "Ngày kết thúc phải ở tương lai và sau ngày bắt đầu")
    public boolean isNgayKetThucValid() {
        if (ngayKetThuc == null) return true;

        boolean isFuture = !ngayKetThuc.isBefore(LocalDateTime.now());

        boolean isAfterStart = (ngayBatDau == null) || ngayKetThuc.isAfter(ngayBatDau);

        return isFuture && isAfterStart;
    }

    @AssertTrue(message = "Số lượng phải lớn hơn 0 khi không chọn vô hạn")
    public boolean isSoLuongValid() {
        if (loaiApDung != null && loaiApDung == 2) {
            return true;
        }
        if (loaiApDung != null && loaiApDung == 1) {
            if (isVoHan != null && isVoHan) {
                return true;
            }
            return soLuong != null && soLuong > 0;
        }
        return true;
    }

    public interface PublicMode {}
    public interface PersonalMode {}

}