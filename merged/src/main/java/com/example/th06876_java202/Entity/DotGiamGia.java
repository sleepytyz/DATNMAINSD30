package com.example.th06876_java202.Entity;

import jakarta.persistence.*;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "DotGiamGia", schema = "dbo")
public class DotGiamGia {

    @Id
    @Column(name = "MaGiamGia")
    private String maGiamGia;

    @NotBlank(message = "Tên đợt giảm giá không được để trống!")
    @Column(name = "TenGiamGia")
    private String tenGiamGia;

    @Column(name = "MoTa")
    private String moTa;

    @NotNull(message = "Giá trị giảm không được để trống!")
    @DecimalMin(value = "1", message = "Giá trị giảm phải từ 1 đến 100!")
    @DecimalMax(value = "100", message = "Giá trị giảm phải từ 1 đến 100!")
    @Column(name = "GiaTriGiam")
    private BigDecimal giaTriGiam;

    @NotNull(message = "Ngày bắt đầu không được để trống!")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "NgayBatDau")
    private LocalDate ngayBatDau;

    @NotNull(message = "Ngày kết thúc không được để trống!")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "NgayKetThuc")
    private LocalDate ngayKetThuc;

    @Column(name = "TrangThai")
    private String trangThai;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "NgayTao")
    private LocalDateTime ngayTao;

    @OneToMany(mappedBy = "dotGiamGia", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChiTietDotGiamGia> chiTietDotGiamGia;

}