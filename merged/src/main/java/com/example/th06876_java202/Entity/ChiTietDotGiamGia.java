    package com.example.th06876_java202.Entity;

    import jakarta.persistence.*;
    import lombok.AllArgsConstructor;
    import lombok.Getter;
    import lombok.NoArgsConstructor;
    import lombok.Setter;

    @Entity
    @Table(name = "ChiTietDotGiamGia")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public class ChiTietDotGiamGia {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "MaChiTietGiamGia")
        private Integer maChiTietGiamGia;

        @ManyToOne
        @JoinColumn(name = "MaGiamGia")
        private DotGiamGia dotGiamGia;

        @ManyToOne
        @JoinColumn(name = "MaSanPham")
        private SanPham sanPham;

        @ManyToOne
        @JoinColumn(name = "MaSanPhamChiTiet")
        private SanPhamChiTiet sanPhamChiTiet;
    }
