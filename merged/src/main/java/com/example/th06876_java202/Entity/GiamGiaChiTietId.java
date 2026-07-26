    package com.example.th06876_java202.Entity;

    import jakarta.persistence.Column;
    import jakarta.persistence.Embeddable;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.EqualsAndHashCode;
    import lombok.NoArgsConstructor;
    import java.io.Serializable;

    @Embeddable
    @EqualsAndHashCode
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class GiamGiaChiTietId implements Serializable {
        @Column(name = "MaKhachHang")
        private String maKhachHang;

        @Column(name = "MaGiamGia")
        private String maGiamGia;
    }