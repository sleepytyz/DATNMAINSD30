-- ============================================================
-- DATABASE: Duantotnghiep_FSShoes
-- Ứng dụng: FS Shoes - Quản lý bán giày thể thao
-- Framework: Spring Boot 3 + JPA + SQL Server
-- Phiên bản: Hoàn chỉnh (khớp với Entity classes thực tế)
-- ============================================================
-- Mật khẩu mặc định (BCrypt cost 10):
--   ADMIN account   : Admin@123
--   STAFF account   : Staff@123
--   USER  account   : User@123
-- Sau khi import, đổi mật khẩu trong ứng dụng lần đầu đăng nhập
-- ============================================================

USE master;
GO

IF EXISTS (SELECT name FROM sys.databases WHERE name = N'Duantotnghiep_FSShoes')
BEGIN
    ALTER DATABASE Duantotnghiep_FSShoes SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE Duantotnghiep_FSShoes;
END
GO

CREATE DATABASE Duantotnghiep_FSShoes
    COLLATE Vietnamese_CI_AS;
GO

USE Duantotnghiep_FSShoes;
GO

-- ============================================================
-- PHẦN 1: TẠO BẢNG (theo thứ tự quan hệ khoá ngoại)
-- ============================================================

-- 1. TaiKhoan (trung tâm xác thực)
CREATE TABLE TaiKhoan (
    MaTaiKhoan  INT IDENTITY(1,1) PRIMARY KEY,
    TenDangNhap NVARCHAR(100)  NOT NULL UNIQUE,
    MatKhau     NVARCHAR(255)  NOT NULL,
    VaiTro      NVARCHAR(20)   NOT NULL,   -- 'ADMIN' | 'STAFF' | 'USER'
    TrangThai   BIT            NOT NULL DEFAULT 1
);
GO

-- 2. Danh mục sản phẩm
CREATE TABLE DanhMucSanPham (
    MaDanhMuc  VARCHAR(20)    NOT NULL PRIMARY KEY,
    TenDanhMuc NVARCHAR(255)  NOT NULL,
    MoTa       NVARCHAR(MAX)  NULL,
    TrangThai  BIT            NOT NULL DEFAULT 1,
    NgayTao    DATETIME2      NOT NULL DEFAULT SYSDATETIME()
);
GO

-- 3. Thương hiệu
CREATE TABLE ThuongHieu (
    MaThuongHieu  VARCHAR(20)   NOT NULL PRIMARY KEY,
    TenThuongHieu NVARCHAR(255) NOT NULL,
    TrangThai     BIT           NOT NULL DEFAULT 1,
    NgayTao       DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO

-- 4. Chất liệu
CREATE TABLE ChatLieu (
    MaChatLieu  VARCHAR(20)   NOT NULL PRIMARY KEY,
    TenChatLieu NVARCHAR(255) NOT NULL,
    TrangThai   BIT           NOT NULL DEFAULT 1,
    NgayTao     DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO

-- 5. Kiểu giày
CREATE TABLE KieuGiay (
    MaKieuGiay  VARCHAR(20)   NOT NULL PRIMARY KEY,
    TenKieuGiay NVARCHAR(255) NOT NULL,
    TrangThai   BIT           NOT NULL DEFAULT 1,
    NgayTao     DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO

-- 6. Màu sắc
CREATE TABLE MauSac (
    MaMauSac  VARCHAR(50)   NOT NULL PRIMARY KEY,
    TenMauSac NVARCHAR(100) NOT NULL,
    TrangThai BIT           NOT NULL DEFAULT 1,
    NgayTao   DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO

-- 7. Kích thước
CREATE TABLE KichThuoc (
    MaKichThuoc  VARCHAR(20)  NOT NULL PRIMARY KEY,
    TenKichThuoc NVARCHAR(50) NOT NULL,
    TrangThai    BIT          NOT NULL DEFAULT 1,
    NgayTao      DATETIME2    NOT NULL DEFAULT SYSDATETIME()
);
GO

-- 8. Ca làm việc
CREATE TABLE CaLamViec (
    MaCa       INT IDENTITY(1,1) PRIMARY KEY,
    TenCa      NVARCHAR(50)  NOT NULL,
    GioBatDau  TIME          NOT NULL,
    GioKetThuc TIME          NOT NULL,
    MoTa       NVARCHAR(500) NULL
);
GO

-- 9. Nhà cung cấp
CREATE TABLE NhaCungCap (
    MaNhaCungCap  INT IDENTITY(1,1) PRIMARY KEY,
    TenNhaCungCap NVARCHAR(200) NOT NULL,
    SoDienThoai   NVARCHAR(15)  NULL,
    Email         NVARCHAR(100) NULL,
    DiaChi        NVARCHAR(255) NULL,
    NguoiLienHe  NVARCHAR(100) NULL,
    TrangThai     BIT           NOT NULL DEFAULT 1
);
GO

-- 10. Nhân viên
CREATE TABLE NhanVien (
    MaNhanVien  VARCHAR(20)    NOT NULL PRIMARY KEY,
    HoTen       NVARCHAR(100)  NOT NULL,
    SoDienThoai NVARCHAR(10)   NULL UNIQUE,
    Email       NVARCHAR(100)  NULL UNIQUE,
    NgaySinh    DATE           NULL,
    DiaChi      NVARCHAR(255)  NULL,
    GioiTinh    BIT            NULL,           -- 1=Nam, 0=Nữ
    ChucVu      NVARCHAR(50)   NULL,
    LuongCoBan  DECIMAL(18,2)  NULL,
    NgayVaoLam  DATETIME       NULL,
    TrangThai   BIT            NOT NULL DEFAULT 1,
    GhiChu      NVARCHAR(255)  NULL,
    MaTaiKhoan  INT            NULL REFERENCES TaiKhoan(MaTaiKhoan)
);
GO

-- 11. Khách hàng
CREATE TABLE KhachHang (
    MaKhachHang VARCHAR(20)   NOT NULL PRIMARY KEY,
    HoTen       NVARCHAR(100) NOT NULL,
    SoDienThoai NVARCHAR(15)  NOT NULL UNIQUE,
    Email       NVARCHAR(100) NULL,
    NgaySinh    DATE          NULL,
    GioiTinh    BIT           NULL,           -- 1=Nam, 0=Nữ
    NgayDangKy  DATE          NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    GhiChu      NVARCHAR(MAX) NULL,
    TrangThai   BIT           NOT NULL DEFAULT 1,
    MaTaiKhoan  INT           NULL REFERENCES TaiKhoan(MaTaiKhoan)
);
GO

-- 12. Địa chỉ giao hàng
CREATE TABLE DiaChi (
    MaDiaChi               INT IDENTITY(1,1) PRIMARY KEY,
    MaKhachHang            VARCHAR(20)   NULL REFERENCES KhachHang(MaKhachHang),
    TenNguoiNhan           NVARCHAR(100) NULL,
    SoDienThoaiNguoiNhan   NVARCHAR(15)  NULL,
    DiaChiCuThe            NVARCHAR(500) NOT NULL,
    MaQuanHuyenGHN   INT            NULL,                      -- mã quận/huyện GHN (tính cước)
    MaPhuongXaGHN    NVARCHAR(20)   NULL,                      -- mã phường/xã (WardCode) GHN
    PhuongXa               NVARCHAR(100) NULL,
    QuanHuyen              NVARCHAR(100) NULL,
    TinhThanh              NVARCHAR(100) NULL,
    DiaChiMacDinh          BIT           NOT NULL DEFAULT 0
);
GO

-- 13. Sản phẩm
CREATE TABLE SanPham (
    MaSanPham        VARCHAR(20)   NOT NULL PRIMARY KEY,
    MaDanhMuc        VARCHAR(20)   NULL REFERENCES DanhMucSanPham(MaDanhMuc),
    TenSanPham       NVARCHAR(255) NOT NULL,
    MoTa             NVARCHAR(MAX) NULL,
    MaChatLieu       VARCHAR(20)   NULL REFERENCES ChatLieu(MaChatLieu),
    MaThuongHieu     VARCHAR(20)   NULL REFERENCES ThuongHieu(MaThuongHieu),
    MaKieuGiay       VARCHAR(20)   NULL REFERENCES KieuGiay(MaKieuGiay),
    TrangThai        BIT           NOT NULL DEFAULT 1,
    NgayTao          DATETIME      NOT NULL DEFAULT GETDATE(),
    GiaBanTrungBinh  DECIMAL(18,2) NULL
);
GO

-- 14. Sản phẩm chi tiết (biến thể)
CREATE TABLE SanPhamChiTiet (
    MaSanPhamChiTiet VARCHAR(50)     NOT NULL PRIMARY KEY,
    MaSanPham        VARCHAR(20)     NOT NULL REFERENCES SanPham(MaSanPham),
    MaMauSac         VARCHAR(50)     NOT NULL REFERENCES MauSac(MaMauSac),
    MaKichThuoc      VARCHAR(20)     NOT NULL REFERENCES KichThuoc(MaKichThuoc),
    GiaBan           DECIMAL(18,2)   NOT NULL,
    SoLuongTon       INT             NOT NULL DEFAULT 0,
    SoLuongDangGiu   INT            NOT NULL CONSTRAINT DF_SPCT_DangGiu DEFAULT 0,   -- đang GIỮ CHỖ bởi đơn online chờ xử lý
    TrangThai        NVARCHAR(50)    NOT NULL DEFAULT N'Hoạt động',
    -- 'Hoạt động' | 'Ngừng kinh doanh'
    DuongDanAnh      NVARCHAR(500)   NULL,
    DanhSachAnh      NVARCHAR(MAX)   NULL,
    NgayTao          DATETIME        NOT NULL DEFAULT GETDATE()
);
GO

-- 15. Giảm giá (Voucher / Mã giảm giá)
CREATE TABLE GiamGia (
    MaGiamGia       VARCHAR(20)    NOT NULL PRIMARY KEY,
    TenChuongTrinh  NVARCHAR(255)  NOT NULL,
    LoaiGiamGia     NVARCHAR(20)   NOT NULL,   -- 'PhanTram' | 'SoTien'
    GiaTriGiam      DECIMAL(18,2)  NOT NULL,
    GiamToiDa       DECIMAL(18,2)  NULL,
    DonToiThieu     DECIMAL(18,2)  NOT NULL DEFAULT 0,
    NgayBatDau      DATETIME       NOT NULL,
    NgayKetThuc     DATETIME       NOT NULL,
    TrangThai       NVARCHAR(50)   NOT NULL DEFAULT N'Sắp hoạt động',
    -- 'Sắp hoạt động' | 'Đang hoạt động' | 'Hết hạn' | 'Đã huỷ'
    LoaiVoucher     NVARCHAR(50)   NULL,
    LoaiApDung      INT            NOT NULL DEFAULT 1, -- 1=Công khai, 2=Cá nhân
    SoLuong         INT            NULL,
    IsVoHan         BIT            NOT NULL DEFAULT 0,
    NgayTao         DATETIME       NOT NULL DEFAULT GETDATE()
);
GO

-- 16. Đợt giảm giá (sale theo sản phẩm)
CREATE TABLE DotGiamGia (
    MaGiamGia  VARCHAR(20)   NOT NULL PRIMARY KEY,
    TenGiamGia NVARCHAR(255) NOT NULL,
    MoTa       NVARCHAR(MAX) NULL,
    GiaTriGiam DECIMAL(5,2)  NOT NULL, -- % giảm: 1..100
    NgayBatDau DATE          NOT NULL,
    NgayKetThuc DATE         NOT NULL,
    TrangThai  NVARCHAR(50)  NULL DEFAULT N'Sắp hoạt động',
    NgayTao    DATETIME      NOT NULL DEFAULT GETDATE()
);
GO

-- 17. Chi tiết đợt giảm giá (liên kết sản phẩm)
CREATE TABLE ChiTietDotGiamGia (
    MaChiTietGiamGia  INT IDENTITY(1,1) PRIMARY KEY,
    MaGiamGia         VARCHAR(20) NOT NULL REFERENCES DotGiamGia(MaGiamGia),
    MaSanPham         VARCHAR(20) NULL     REFERENCES SanPham(MaSanPham),
    MaSanPhamChiTiet  VARCHAR(50) NULL     REFERENCES SanPhamChiTiet(MaSanPhamChiTiet)
);
GO

-- 18. Hoá đơn
CREATE TABLE HoaDon (
    MaHoaDon           VARCHAR(20)    NOT NULL PRIMARY KEY,
    MaKhachHang        VARCHAR(20)    NULL     REFERENCES KhachHang(MaKhachHang),
    MaNhanVien         VARCHAR(20)    NULL     REFERENCES NhanVien(MaNhanVien),
    MaGiamGia          VARCHAR(20)    NULL     REFERENCES GiamGia(MaGiamGia),
    TongTien           DECIMAL(18,2)  NOT NULL DEFAULT 0,
    TienKhachDua       DECIMAL(18,2)  NULL,
    TienThua           DECIMAL(18,2)  NULL,
    TienShip           DECIMAL(18,2)  NOT NULL DEFAULT 0,
    PhuongThucThanhToan NVARCHAR(50)  NULL,   -- 'Tiền mặt' | 'Chuyển khoản' | 'VNPay'
    TrangThai          NVARCHAR(50)   NOT NULL DEFAULT N'Chờ xác nhận',
    -- Online: 'Chờ xác nhận'|'Đã xác nhận'|'Đang giao'|'Đã giao'|'Đã huỷ'|'Đã trả hàng'
    -- POS: 'Đã thanh toán'|'Đang xử lý'|'Đã huỷ'
    GhiChu             NVARCHAR(MAX)  NULL,
    NgayTao            DATETIME       NOT NULL DEFAULT GETDATE(),
    NgayThanhToan      DATETIME       NULL,
    LoaiBan            NVARCHAR(20)   NULL,    -- 'Online' | 'Counter'
    DiaChiGiaoHang     NVARCHAR(1000) NULL,
    TenNguoiNhan       NVARCHAR(100)  NULL,    -- người nhận hàng (có thể khác chủ tài khoản)
    SdtNguoiNhan       NVARCHAR(20)   NULL     -- SĐT người nhận hàng
);
GO

-- 19. Chi tiết hoá đơn
CREATE TABLE ChiTietHoaDon (
    MaChiTiet        INT IDENTITY(1,1) PRIMARY KEY,
    MaHoaDon         VARCHAR(20)   NOT NULL REFERENCES HoaDon(MaHoaDon),
    MaSanPhamChiTiet VARCHAR(50)   NOT NULL REFERENCES SanPhamChiTiet(MaSanPhamChiTiet),
    SoLuong          INT           NOT NULL DEFAULT 1,
    DonGia           DECIMAL(18,2) NOT NULL,
    TienGiam         DECIMAL(18,2) NOT NULL DEFAULT 0,
    ThanhTien        DECIMAL(18,2) NOT NULL
);
GO

-- 20. Phiếu nhập hàng
CREATE TABLE PhieuNhapHang (
    MaPhieuNhap  INT IDENTITY(1,1) PRIMARY KEY,
    MaNhaCungCap INT            NOT NULL REFERENCES NhaCungCap(MaNhaCungCap),
    MaNhanVien   VARCHAR(20)    NOT NULL REFERENCES NhanVien(MaNhanVien),
    NgayNhap     DATETIME       NOT NULL DEFAULT GETDATE(),
    TongTienNhap DECIMAL(18,2)  NOT NULL DEFAULT 0,
    TrangThai    NVARCHAR(50)   NULL DEFAULT N'Chờ duyệt',
    GhiChu       NVARCHAR(500)  NULL
);
GO

-- 21. Chi tiết phiếu nhập hàng
CREATE TABLE ChiTietNhapHang (
    MaChiTietNhap    INT IDENTITY(1,1) PRIMARY KEY,
    MaPhieuNhap      INT           NOT NULL REFERENCES PhieuNhapHang(MaPhieuNhap),
    MaSanPhamChiTiet VARCHAR(50)   NOT NULL REFERENCES SanPhamChiTiet(MaSanPhamChiTiet),
    SoLuongNhap      INT           NOT NULL DEFAULT 1,
    DonGiaNhap       DECIMAL(18,2) NOT NULL,
    ThanhTien        DECIMAL(18,2) NOT NULL
);
GO

-- 22. Giao ca
CREATE TABLE GiaoCa (
    MaGiaoCa             INT IDENTITY(1,1) PRIMARY KEY,
    NgayGiao             DATETIME      NOT NULL DEFAULT GETDATE(),
    MaNhanVienBanGiao    VARCHAR(20)   NOT NULL REFERENCES NhanVien(MaNhanVien),
    MaNhanVienNhanGiao   VARCHAR(20)   NOT NULL REFERENCES NhanVien(MaNhanVien),
    TienMatBanGiao       DECIMAL(18,2) NOT NULL DEFAULT 0,
    SoHoaDonTrongCa      INT           NOT NULL DEFAULT 0,
    DoanhThuTrongCa      DECIMAL(18,2) NOT NULL DEFAULT 0,
    GhiChu               NVARCHAR(MAX) NULL,
    XacNhan              BIT           NOT NULL DEFAULT 0
);
GO

-- 23. Chấm công
CREATE TABLE ChamCong (
    MaChamCong    INT IDENTITY(1,1) PRIMARY KEY,
    MaNhanVien    VARCHAR(20) NOT NULL REFERENCES NhanVien(MaNhanVien),
    MaCa          INT         NOT NULL REFERENCES CaLamViec(MaCa),
    NgayChamCong  DATE        NOT NULL,
    GioVao        TIME        NULL,
    GioRa         TIME        NULL,
    TrangThai     BIT         NOT NULL DEFAULT 1,  -- 1=Đã làm, 0=Lịch sắp tới
    SoGioLam      DECIMAL(5,2) NULL,
    GhiChu        NVARCHAR(MAX) NULL
);
GO

-- ============================================================
-- PHẦN 2: DỮ LIỆU MẪU
-- ============================================================

-- ==================== TAIKHOAN ====================
-- Mật khẩu BCrypt cost 10:
--   admin      / Admin@123
--   nv001      / Staff@123
--   nv002      / Staff@123
--   kh001      / User@123
--   kh002      / User@123
--   kh003      / User@123
-- BCrypt hash được tạo bởi Spring Security BCryptPasswordEncoder
INSERT INTO TaiKhoan (TenDangNhap, MatKhau, VaiTro, TrangThai) VALUES
(N'admin',  N'$2a$10$N051cdSuwPQ9k4dqtTweIeoN4W./Vri9JVhC13oJaZfchuFsBoTTe', N'ADMIN', 1),
(N'nv001',  N'$2a$10$wnLINBPsl4TSaY3.M4HDy.kzB9frF7guYT8P7KUmBbEF6ec0myOhG', N'STAFF', 1),
(N'nv002',  N'$2a$10$wnLINBPsl4TSaY3.M4HDy.kzB9frF7guYT8P7KUmBbEF6ec0myOhG', N'STAFF', 1),
(N'kh001',  N'$2a$10$jKLukENc1.u.j3YZ4Xl5GOlSpVPZQJzPn1PS/0iY/ARMURH2M8G9a', N'USER',  1),
(N'kh002',  N'$2a$10$jKLukENc1.u.j3YZ4Xl5GOlSpVPZQJzPn1PS/0iY/ARMURH2M8G9a', N'USER',  1),
(N'kh003',  N'$2a$10$jKLukENc1.u.j3YZ4Xl5GOlSpVPZQJzPn1PS/0iY/ARMURH2M8G9a', N'USER',  1);
GO

-- QUAN TRỌNG: Nếu các hash trên không hoạt động, chạy lệnh SQL sau
-- để cập nhật mật khẩu sau khi khởi động ứng dụng lần đầu:
/*
  -- Tạo hash mới bằng cách thêm endpoint tạm thời, hoặc chạy:
  -- Trong ứng dụng Spring Boot, dùng BCryptPasswordEncoder:
  String hash = new BCryptPasswordEncoder().encode("Admin@123");
  -- Sau đó UPDATE TaiKhoan SET MatKhau = '<hash>' WHERE TenDangNhap = 'admin'
*/

-- ==================== CA LÀM VIỆC ====================
INSERT INTO CaLamViec (TenCa, GioBatDau, GioKetThuc, MoTa) VALUES
(N'Ca sáng',  '07:30', '12:00', N'Ca buổi sáng từ 7h30 đến 12h00'),
(N'Ca chiều', '12:00', '17:00', N'Ca buổi chiều từ 12h00 đến 17h00'),
(N'Ca tối',   '17:00', '21:30', N'Ca buổi tối từ 17h00 đến 21h30');
GO

-- ==================== NHÀ CUNG CẤP ====================
INSERT INTO NhaCungCap (TenNhaCungCap, SoDienThoai, Email, DiaChi, NguoiLienHe, TrangThai) VALUES
(N'Công ty TNHH Nike Việt Nam',   N'0281234567', N'supply@nike.vn',   N'Số 10 Lý Thái Tổ, Q1, TP.HCM',        N'Nguyễn Minh Khoa',  1),
(N'Công ty TNHH Adidas Việt Nam', N'0289876543', N'supply@adidas.vn', N'Số 25 Nguyễn Huệ, Q1, TP.HCM',        N'Trần Thị Lan',      1),
(N'Công ty Phân phối Puma HN',    N'0243456789', N'order@pumavn.vn',  N'Số 48 Trần Hưng Đạo, Hoàn Kiếm, HN', N'Lê Văn Đức',        1);
GO

-- ==================== NHÂN VIÊN ====================
INSERT INTO NhanVien (MaNhanVien, HoTen, SoDienThoai, Email, NgaySinh, DiaChi, GioiTinh, ChucVu, LuongCoBan, NgayVaoLam, TrangThai, MaTaiKhoan) VALUES
(N'NV001', N'Nguyễn Văn Admin',  N'0901234567', N'admin@fsshoes.vn',  '1990-03-15', N'12 Lý Thường Kiệt, Hà Nội',           1, N'Quản lý',          15000000, '2022-01-10 08:00:00', 1, 1),
(N'NV002', N'Trần Thị Bình',     N'0912345678', N'binh.nv@fsshoes.vn','1995-07-22', N'34 Nguyễn Du, Hà Nội',                0, N'Nhân viên bán hàng', 9000000, '2023-03-01 08:00:00', 1, 2),
(N'NV003', N'Lê Minh Cường',     N'0923456789', N'cuong.nv@fsshoes.vn','1997-11-05', N'67 Đinh Tiên Hoàng, Hà Nội',         1, N'Nhân viên bán hàng', 9000000, '2023-06-15 08:00:00', 1, 3);
GO

-- ==================== KHÁCH HÀNG ====================
INSERT INTO KhachHang (MaKhachHang, HoTen, SoDienThoai, Email, NgaySinh, GioiTinh, NgayDangKy, TrangThai, MaTaiKhoan) VALUES
(N'KH001', N'Phạm Quốc Anh',    N'0934567890', N'quocanh@gmail.com',    '1998-04-12', 1, '2024-01-05', 1, 4),
(N'KH002', N'Nguyễn Thị Hoa',   N'0945678901', N'thihoanguyen@gmail.com','2000-09-18', 0, '2024-02-20', 1, 5),
(N'KH003', N'Vũ Thanh Tùng',    N'0956789012', N'vttung2001@gmail.com', '2001-12-30', 1, '2024-05-10', 1, 6);
GO

-- ==================== ĐỊA CHỈ ====================
INSERT INTO DiaChi (MaKhachHang, TenNguoiNhan, SoDienThoaiNguoiNhan, DiaChiCuThe, PhuongXa, QuanHuyen, TinhThanh, DiaChiMacDinh) VALUES
(N'KH001', N'Phạm Quốc Anh',  N'0934567890', N'Số 15 Ngõ 36 Đường Hoàng Quốc Việt', N'Nghĩa Đô',     N'Cầu Giấy',   N'Hà Nội',      1),
(N'KH001', N'Phạm Quốc Anh',  N'0934567890', N'Tòa CT1 Chung cư Xa La',              N'Phúc La',      N'Hà Đông',    N'Hà Nội',      0),
(N'KH002', N'Nguyễn Thị Hoa', N'0945678901', N'Số 8 Đường Lê Văn Sỹ',               N'Phường 6',     N'Quận 3',     N'TP.HCM',      1),
(N'KH003', N'Vũ Thanh Tùng',  N'0956789012', N'Số 102 Trần Phú',                     N'Dương Nội',    N'Hà Đông',    N'Hà Nội',      1);
GO

-- ==================== DANH MỤC ====================
INSERT INTO DanhMucSanPham (MaDanhMuc, TenDanhMuc, MoTa, TrangThai) VALUES
(N'DM001', N'Giày chạy bộ',     N'Dòng giày thiết kế cho vận động chạy bộ, đế đệm tốt, thoáng khí',     1),
(N'DM002', N'Giày bóng rổ',     N'Dòng giày cổ cao, đế bám tốt cho sân bóng rổ',                         1),
(N'DM003', N'Giày tennis',      N'Dòng giày ổn định bàn chân, chống trơn trượt trên sân tennis',          1),
(N'DM004', N'Giày thường ngày', N'Giày thể thao phong cách dành cho mặc hàng ngày, thoải mái',           1),
(N'DM005', N'Giày tập gym',     N'Dòng giày đế phẳng, ổn định, hỗ trợ các bài tập nặng tại phòng gym',  1);
GO

-- ==================== THƯƠNG HIỆU ====================
INSERT INTO ThuongHieu (MaThuongHieu, TenThuongHieu, TrangThai) VALUES
(N'TH001', N'Nike',      1),
(N'TH002', N'Adidas',    1),
(N'TH003', N'Puma',      1),
(N'TH004', N'New Balance',1),
(N'TH005', N'Converse',  1),
(N'TH006', N'Vans',      1);
GO

-- ==================== CHẤT LIỆU ====================
INSERT INTO ChatLieu (MaChatLieu, TenChatLieu, TrangThai) VALUES
(N'CL001', N'Vải lưới thoáng khí',  1),
(N'CL002', N'Da thật cao cấp',      1),
(N'CL003', N'Da tổng hợp PU',       1),
(N'CL004', N'Vải Flyknit',          1);
GO

-- ==================== KIỂU GIÀY ====================
INSERT INTO KieuGiay (MaKieuGiay, TenKieuGiay, TrangThai) VALUES
(N'KG001', N'Cổ thấp',   1),
(N'KG002', N'Cổ cao',    1),
(N'KG003', N'Slip-on',   1),
(N'KG004', N'Sandal thể thao', 1);
GO

-- ==================== MÀU SẮC ====================
INSERT INTO MauSac (MaMauSac, TenMauSac, TrangThai) VALUES
(N'MAU001', N'Trắng',      1),
(N'MAU002', N'Đen',        1),
(N'MAU003', N'Đỏ',         1),
(N'MAU004', N'Xanh dương', 1),
(N'MAU005', N'Xám',        1),
(N'MAU006', N'Xanh lá',    1);
GO

-- ==================== KÍCH THƯỚC ====================
INSERT INTO KichThuoc (MaKichThuoc, TenKichThuoc, TrangThai) VALUES
(N'KT38', N'38', 1),
(N'KT39', N'39', 1),
(N'KT40', N'40', 1),
(N'KT41', N'41', 1),
(N'KT42', N'42', 1),
(N'KT43', N'43', 1),
(N'KT44', N'44', 1),
(N'KT45', N'45', 1);
GO

-- ==================== SẢN PHẨM ====================
INSERT INTO SanPham (MaSanPham, MaDanhMuc, TenSanPham, MoTa, MaChatLieu, MaThuongHieu, MaKieuGiay, TrangThai, NgayTao, GiaBanTrungBinh) VALUES
(N'SP001', N'DM001', N'Nike Air Zoom Pegasus 40',
 N'Giày chạy bộ huyền thoại Nike Pegasus phiên bản 40 với đệm Air Zoom thế hệ mới. Thiết kế nhẹ, êm ái, phù hợp mọi cung đường chạy.',
 N'CL001', N'TH001', N'KG001', 1, '2024-01-15 08:00:00', 2800000),

(N'SP002', N'DM002', N'Nike Air Jordan 1 Retro High',
 N'Đôi giày bóng rổ biểu tượng Air Jordan 1, thiết kế cổ cao cổ điển với đệm Air Unit êm ái, chất liệu da tổng hợp cao cấp.',
 N'CL003', N'TH001', N'KG002', 1, '2024-01-20 08:00:00', 3500000),

(N'SP003', N'DM004', N'Adidas Superstar Classic',
 N'Đôi giày thường ngày biểu tượng Adidas Superstar với phần mũi vỏ sò đặc trưng, chất liệu da tổng hợp trắng sọc ba màu đen.',
 N'CL003', N'TH002', N'KG001', 1, '2024-02-05 08:00:00', 2200000),

(N'SP004', N'DM001', N'Adidas Ultraboost 23',
 N'Công nghệ Boost mang lại cảm giác hoàn hảo, upper Primeknit co giãn ôm chân, lý tưởng cho việc chạy đường dài.',
 N'CL004', N'TH002', N'KG001', 1, '2024-02-10 08:00:00', 3200000),

(N'SP005', N'DM004', N'Puma RS-X Puzzle',
 N'Thiết kế chunky retro đặc trưng của Puma RS-X với hệ thống Running System đệm giảm chấn. Phù hợp phong cách đường phố.',
 N'CL003', N'TH003', N'KG001', 1, '2024-03-01 08:00:00', 1800000);
GO

-- ==================== SẢN PHẨM CHI TIẾT ====================
-- SP001: Nike Air Zoom Pegasus 40
INSERT INTO SanPhamChiTiet (MaSanPhamChiTiet, MaSanPham, MaMauSac, MaKichThuoc, GiaBan, SoLuongTon, TrangThai) VALUES
(N'SP001-TRANG-40', N'SP001', N'MAU001', N'KT40', 2800000, 15, N'Hoạt động'),
(N'SP001-TRANG-41', N'SP001', N'MAU001', N'KT41', 2800000, 20, N'Hoạt động'),
(N'SP001-TRANG-42', N'SP001', N'MAU001', N'KT42', 2800000, 18, N'Hoạt động'),
(N'SP001-TRANG-43', N'SP001', N'MAU001', N'KT43', 2800000,  8, N'Hoạt động'),
(N'SP001-DEN-40',   N'SP001', N'MAU002', N'KT40', 2800000, 12, N'Hoạt động'),
(N'SP001-DEN-41',   N'SP001', N'MAU002', N'KT41', 2800000, 10, N'Hoạt động'),
(N'SP001-DEN-42',   N'SP001', N'MAU002', N'KT42', 2800000, 14, N'Hoạt động'),
(N'SP001-XANH-41',  N'SP001', N'MAU004', N'KT41', 2900000,  5, N'Hoạt động'),
(N'SP001-XANH-42',  N'SP001', N'MAU004', N'KT42', 2900000,  3, N'Hoạt động');
GO

-- SP002: Nike Air Jordan 1 Retro High
INSERT INTO SanPhamChiTiet (MaSanPhamChiTiet, MaSanPham, MaMauSac, MaKichThuoc, GiaBan, SoLuongTon, TrangThai) VALUES
(N'SP002-TRANG-40', N'SP002', N'MAU001', N'KT40', 3500000, 10, N'Hoạt động'),
(N'SP002-TRANG-41', N'SP002', N'MAU001', N'KT41', 3500000, 12, N'Hoạt động'),
(N'SP002-TRANG-42', N'SP002', N'MAU001', N'KT42', 3500000,  8, N'Hoạt động'),
(N'SP002-DO-41',    N'SP002', N'MAU003', N'KT41', 3600000,  6, N'Hoạt động'),
(N'SP002-DO-42',    N'SP002', N'MAU003', N'KT42', 3600000,  4, N'Hoạt động'),
(N'SP002-DEN-40',   N'SP002', N'MAU002', N'KT40', 3500000,  0, N'Ngừng kinh doanh');
GO

-- SP003: Adidas Superstar Classic
INSERT INTO SanPhamChiTiet (MaSanPhamChiTiet, MaSanPham, MaMauSac, MaKichThuoc, GiaBan, SoLuongTon, TrangThai) VALUES
(N'SP003-TRANG-38', N'SP003', N'MAU001', N'KT38', 2200000, 10, N'Hoạt động'),
(N'SP003-TRANG-39', N'SP003', N'MAU001', N'KT39', 2200000, 15, N'Hoạt động'),
(N'SP003-TRANG-40', N'SP003', N'MAU001', N'KT40', 2200000, 20, N'Hoạt động'),
(N'SP003-TRANG-41', N'SP003', N'MAU001', N'KT41', 2200000, 18, N'Hoạt động'),
(N'SP003-TRANG-42', N'SP003', N'MAU001', N'KT42', 2200000, 12, N'Hoạt động'),
(N'SP003-DEN-39',   N'SP003', N'MAU002', N'KT39', 2200000,  8, N'Hoạt động'),
(N'SP003-DEN-40',   N'SP003', N'MAU002', N'KT40', 2200000, 10, N'Hoạt động'),
(N'SP003-DEN-41',   N'SP003', N'MAU002', N'KT41', 2200000,  9, N'Hoạt động');
GO

-- SP004: Adidas Ultraboost 23
INSERT INTO SanPhamChiTiet (MaSanPhamChiTiet, MaSanPham, MaMauSac, MaKichThuoc, GiaBan, SoLuongTon, TrangThai) VALUES
(N'SP004-XAML-40',  N'SP004', N'MAU006', N'KT40', 3200000, 10, N'Hoạt động'),
(N'SP004-XAML-41',  N'SP004', N'MAU006', N'KT41', 3200000, 12, N'Hoạt động'),
(N'SP004-XAML-42',  N'SP004', N'MAU006', N'KT42', 3200000,  8, N'Hoạt động'),
(N'SP004-XAM-40',   N'SP004', N'MAU005', N'KT40', 3100000,  7, N'Hoạt động'),
(N'SP004-XAM-41',   N'SP004', N'MAU005', N'KT41', 3100000, 10, N'Hoạt động'),
(N'SP004-TRANG-41', N'SP004', N'MAU001', N'KT41', 3300000,  5, N'Hoạt động'),
(N'SP004-TRANG-42', N'SP004', N'MAU001', N'KT42', 3300000,  4, N'Hoạt động');
GO

-- SP005: Puma RS-X Puzzle
INSERT INTO SanPhamChiTiet (MaSanPhamChiTiet, MaSanPham, MaMauSac, MaKichThuoc, GiaBan, SoLuongTon, TrangThai) VALUES
(N'SP005-TRANG-39', N'SP005', N'MAU001', N'KT39', 1800000, 8, N'Hoạt động'),
(N'SP005-TRANG-40', N'SP005', N'MAU001', N'KT40', 1800000,12, N'Hoạt động'),
(N'SP005-TRANG-41', N'SP005', N'MAU001', N'KT41', 1800000,10, N'Hoạt động'),
(N'SP005-DO-40',    N'SP005', N'MAU003', N'KT40', 1850000, 6, N'Hoạt động'),
(N'SP005-DO-41',    N'SP005', N'MAU003', N'KT41', 1850000, 5, N'Hoạt động'),
(N'SP005-XAM-40',   N'SP005', N'MAU005', N'KT40', 1800000, 4, N'Hoạt động');
GO

-- ==================== GIẢM GIÁ (VOUCHER) ====================
INSERT INTO GiamGia (MaGiamGia, TenChuongTrinh, LoaiGiamGia, GiaTriGiam, GiamToiDa, DonToiThieu, NgayBatDau, NgayKetThuc, TrangThai, LoaiApDung, SoLuong, IsVoHan) VALUES
(N'GG001', N'Ưu đãi khách mới 10%',    N'PhanTram', 10, 200000, 500000,
 '2025-01-01 00:00:00', '2027-12-31 23:59:59', N'Đang hoạt động', 1, 200, 0),
(N'GG002', N'Sale 50K đơn từ 1 triệu', N'SoTien',   50000, NULL, 1000000,
 '2025-06-01 00:00:00', '2027-06-30 23:59:59', N'Đang hoạt động', 1, 100, 0),
(N'GG003', N'VIP giảm 15% tối đa 500K', N'PhanTram', 15, 500000, 2000000,
 '2025-01-01 00:00:00', '2027-12-31 23:59:59', N'Đang hoạt động', 2, NULL, 1);
GO

-- ==================== ĐỢT GIẢM GIÁ ====================
INSERT INTO DotGiamGia (MaGiamGia, TenGiamGia, MoTa, GiaTriGiam, NgayBatDau, NgayKetThuc, TrangThai) VALUES
(N'DGG001', N'Sale Hè 2025 giảm 20%',
 N'Chương trình giảm giá mùa hè, áp dụng cho toàn bộ giày Nike và Adidas',
 20.00, '2025-06-01', '2025-08-31', N'Hết hạn'),
(N'DGG002', N'Sale Cuối Năm 2025 giảm 15%',
 N'Chương trình khuyến mãi cuối năm cho tất cả các dòng giày thể thao',
 15.00, '2025-11-11', '2025-12-31', N'Đang hoạt động');
GO

-- Chi tiết đợt giảm giá
INSERT INTO ChiTietDotGiamGia (MaGiamGia, MaSanPham, MaSanPhamChiTiet) VALUES
(N'DGG001', N'SP001', N'SP001-TRANG-40'),
(N'DGG001', N'SP001', N'SP001-TRANG-41'),
(N'DGG001', N'SP001', N'SP001-TRANG-42'),
(N'DGG001', N'SP003', N'SP003-TRANG-39'),
(N'DGG001', N'SP003', N'SP003-TRANG-40'),
(N'DGG002', N'SP002', N'SP002-TRANG-40'),
(N'DGG002', N'SP002', N'SP002-TRANG-41'),
(N'DGG002', N'SP002', N'SP002-TRANG-42'),
(N'DGG002', N'SP004', N'SP004-XAML-40'),
(N'DGG002', N'SP004', N'SP004-XAML-41'),
(N'DGG002', N'SP005', N'SP005-TRANG-40'),
(N'DGG002', N'SP005', N'SP005-TRANG-41');
GO

-- ==================== HOÁ ĐƠN ====================
INSERT INTO HoaDon (MaHoaDon, MaKhachHang, MaNhanVien, MaGiamGia, TongTien, TienShip, PhuongThucThanhToan, TrangThai, NgayTao, NgayThanhToan, LoaiBan, DiaChiGiaoHang) VALUES
-- Hoá đơn online đã giao
(N'HD001', N'KH001', NULL, NULL,
 5600000, 0, N'Chuyển khoản', N'Đã giao',
 '2024-06-10 10:25:00', '2024-06-13 14:00:00', N'Online',
 N'Số 15 Ngõ 36 Đường Hoàng Quốc Việt, Nghĩa Đô, Cầu Giấy, Hà Nội'),

-- Hoá đơn bán tại quầy
(N'HD002', N'KH002', N'NV002', NULL,
 2200000, 0, N'Tiền mặt', N'Đã thanh toán',
 '2024-07-15 09:45:00', '2024-07-15 09:45:00', N'Counter', NULL),

-- Hoá đơn online chờ xác nhận
(N'HD003', N'KH003', NULL, N'GG001',
 7630000, 30000, N'VNPay', N'Chờ xác nhận',
 '2024-09-01 16:30:00', NULL, N'Online',
 N'Số 102 Trần Phú, Dương Nội, Hà Đông, Hà Nội'),

-- Hoá đơn online đã xác nhận
(N'HD004', N'KH001', NULL, N'GG002',
 3450000, 0, N'VNPay', N'Đang giao',
 '2024-09-10 14:00:00', NULL, N'Online',
 N'Số 15 Ngõ 36 Đường Hoàng Quốc Việt, Nghĩa Đô, Cầu Giấy, Hà Nội'),

-- Hoá đơn đã huỷ
(N'HD005', N'KH002', NULL, NULL,
 1800000, 30000, N'VNPay', N'Đã huỷ',
 '2024-09-12 11:20:00', NULL, N'Online',
 N'Số 8 Đường Lê Văn Sỹ, Phường 6, Quận 3, TP.HCM');
GO

-- ==================== CHI TIẾT HOÁ ĐƠN ====================
INSERT INTO ChiTietHoaDon (MaHoaDon, MaSanPhamChiTiet, SoLuong, DonGia, TienGiam, ThanhTien) VALUES
-- HD001: 2 đôi Pegasus
(N'HD001', N'SP001-TRANG-41', 1, 2800000, 0, 2800000),
(N'HD001', N'SP001-DEN-42',   1, 2800000, 0, 2800000),

-- HD002: 1 đôi Superstar
(N'HD002', N'SP003-TRANG-40', 1, 2200000, 0, 2200000),

-- HD003: 1 Jordan + 1 Ultraboost (có voucher GG001 giảm 10%)
(N'HD003', N'SP002-DO-41',    1, 3600000, 360000, 3240000),
(N'HD003', N'SP004-XAML-41', 1, 3200000, 320000, 2880000),

-- HD004: 1 đôi Ultraboost (có voucher GG002 giảm 50K)
(N'HD004', N'SP004-TRANG-41', 1, 3300000, 50000, 3250000),

-- HD005 (đã huỷ): 1 đôi Puma
(N'HD005', N'SP005-TRANG-40', 1, 1800000, 0, 1800000);
GO

-- ==================== PHIẾU NHẬP HÀNG ====================
INSERT INTO PhieuNhapHang (MaNhaCungCap, MaNhanVien, NgayNhap, TongTienNhap, TrangThai, GhiChu) VALUES
(1, N'NV001', '2024-01-10 09:00:00', 62500000, N'Đã nhập kho', N'Nhập hàng đầu năm 2024, giày Nike Air Zoom Pegasus và Jordan'),
(2, N'NV001', '2024-02-08 10:00:00', 38500000, N'Đã nhập kho', N'Nhập hàng tháng 2, bổ sung Adidas Superstar và Ultraboost');
GO

-- Chi tiết phiếu nhập hàng
INSERT INTO ChiTietNhapHang (MaPhieuNhap, MaSanPhamChiTiet, SoLuongNhap, DonGiaNhap, ThanhTien) VALUES
-- Phiếu 1: Nike
(1, N'SP001-TRANG-40', 20, 1500000, 30000000),
(1, N'SP001-TRANG-41', 25, 1500000, 37500000),
(1, N'SP002-TRANG-40', 15, 2000000, 30000000),
(1, N'SP002-DO-41',    10, 2100000, 21000000),
-- Phiếu 2: Adidas
(2, N'SP003-TRANG-39', 20, 1200000, 24000000),
(2, N'SP003-TRANG-40', 25, 1200000, 30000000),
(2, N'SP004-XAML-40',  15, 1800000, 27000000);
GO

-- ==================== CHẤM CÔNG ====================
INSERT INTO ChamCong (MaNhanVien, MaCa, NgayChamCong, GioVao, GioRa, TrangThai, SoGioLam, GhiChu) VALUES
(N'NV002', 1, '2024-09-01', '07:32', '12:00', 1, 4.50, NULL),
(N'NV002', 2, '2024-09-01', '12:02', '17:00', 1, 4.97, NULL),
(N'NV003', 1, '2024-09-01', '07:29', '12:00', 1, 4.52, NULL),
(N'NV002', 1, '2024-09-02', '07:35', '12:00', 1, 4.42, NULL),
(N'NV003', 2, '2024-09-02', '12:00', '17:02', 1, 5.03, NULL),
(N'NV002', 1, '2024-09-03', '07:31', '12:00', 1, 4.48, NULL),
(N'NV003', 1, '2024-09-03', '07:45', '12:00', 1, 4.25, N'Đến muộn 15 phút'),
-- Lịch sắp tới
(N'NV002', 1, CAST(GETDATE()+1 AS DATE), NULL, NULL, 0, NULL, N'Lịch làm việc'),
(N'NV003', 2, CAST(GETDATE()+1 AS DATE), NULL, NULL, 0, NULL, N'Lịch làm việc');
GO

-- ==================== GIAO CA ====================
INSERT INTO GiaoCa (NgayGiao, MaNhanVienBanGiao, MaNhanVienNhanGiao, TienMatBanGiao, SoHoaDonTrongCa, DoanhThuTrongCa, GhiChu, XacNhan) VALUES
('2024-09-01 12:00:00', N'NV002', N'NV003', 500000, 3, 7800000, N'Ca sáng 01/09 bàn giao bình thường', 1),
('2024-09-01 17:00:00', N'NV003', N'NV002', 350000, 2, 4200000, N'Ca chiều 01/09 bàn giao bình thường', 1),
('2024-09-02 12:00:00', N'NV002', N'NV003', 420000, 2, 5500000, N'Ca sáng 02/09', 1);
GO

-- ============================================================
-- 24. Đánh giá sản phẩm (DanhGia)
-- ============================================================
CREATE TABLE DanhGia (
    MaDanhGia    INT IDENTITY(1,1) PRIMARY KEY,
    MaSanPham    VARCHAR(20)   NULL REFERENCES SanPham(MaSanPham),
    MaKhachHang  VARCHAR(20)   NULL REFERENCES KhachHang(MaKhachHang),
    MaHoaDon     VARCHAR(20)   NULL REFERENCES HoaDon(MaHoaDon),
    SoSao        INT           NOT NULL DEFAULT 5,   -- 1..5
    NoiDung      NVARCHAR(2000) NULL,
    NgayDanhGia  DATETIME      NOT NULL DEFAULT GETDATE(),
    TrangThai    BIT           NOT NULL DEFAULT 1    -- 1=hiển thị, 0=ẩn
);
GO

INSERT INTO DanhGia (MaSanPham, MaKhachHang, MaHoaDon, SoSao, NoiDung, NgayDanhGia, TrangThai) VALUES
(N'SP001', N'KH001', NULL, 5, N'Giày chạy rất êm, đi cả ngày không mỏi chân. Rất hài lòng!', '2024-06-10 09:30:00', 1),
(N'SP001', N'KH002', NULL, 4, N'Chất lượng tốt, giao hàng nhanh. Size hơi rộng một chút.',     '2024-06-12 14:10:00', 1),
(N'SP002', N'KH003', NULL, 5, N'Air Jordan chính hãng, form đẹp, đóng gói cẩn thận.',           '2024-06-15 16:45:00', 1),
(N'SP003', N'KH001', NULL, 4, N'Superstar cổ điển đẹp, phối đồ dễ. Đáng mua trong tầm giá.',    '2024-06-18 10:20:00', 1),
(N'SP004', N'KH002', NULL, 5, N'Ultraboost đế êm thật sự, chạy bộ rất thích.',                  '2024-06-20 08:05:00', 1);
GO

-- ============================================================
-- PHẦN 3: VIEW TIỆN ÍCH
-- ============================================================

-- View: Tổng quan sản phẩm (dùng cho trang quản lý)
CREATE OR ALTER VIEW vw_SanPhamTongQuan AS
SELECT
    sp.MaSanPham,
    sp.TenSanPham,
    dm.TenDanhMuc,
    th.TenThuongHieu,
    cl.TenChatLieu,
    kg.TenKieuGiay,
    sp.GiaBanTrungBinh,
    sp.TrangThai,
    sp.NgayTao,
    COUNT(spct.MaSanPhamChiTiet)                                           AS SoBienThe,
    SUM(spct.SoLuongTon)                                                   AS TongTon,
    MIN(CASE WHEN spct.TrangThai = N'Hoạt động' THEN spct.GiaBan END)     AS GiaThapNhat,
    MAX(CASE WHEN spct.TrangThai = N'Hoạt động' THEN spct.GiaBan END)     AS GiaCaoNhat
FROM SanPham sp
LEFT JOIN DanhMucSanPham    dm ON sp.MaDanhMuc    = dm.MaDanhMuc
LEFT JOIN ThuongHieu        th ON sp.MaThuongHieu = th.MaThuongHieu
LEFT JOIN ChatLieu          cl ON sp.MaChatLieu   = cl.MaChatLieu
LEFT JOIN KieuGiay          kg ON sp.MaKieuGiay   = kg.MaKieuGiay
LEFT JOIN SanPhamChiTiet   spct ON sp.MaSanPham   = spct.MaSanPham
GROUP BY sp.MaSanPham, sp.TenSanPham, dm.TenDanhMuc, th.TenThuongHieu,
         cl.TenChatLieu, kg.TenKieuGiay, sp.GiaBanTrungBinh, sp.TrangThai, sp.NgayTao;
GO

-- View: Doanh thu theo tháng
CREATE OR ALTER VIEW vw_DoanhThuTheoThang AS
SELECT
    YEAR(NgayTao)  AS Nam,
    MONTH(NgayTao) AS Thang,
    COUNT(*)       AS SoHoaDon,
    SUM(TongTien)  AS TongDoanhThu,
    SUM(TienShip)  AS TongTienShip,
    LoaiBan
FROM HoaDon
WHERE TrangThai IN (N'Đã thanh toán', N'Đã giao')
GROUP BY YEAR(NgayTao), MONTH(NgayTao), LoaiBan;
GO

-- View: Biến thể đang hoạt động với giá khuyến mãi
CREATE OR ALTER VIEW vw_BienTheDangBan AS
SELECT
    spct.MaSanPhamChiTiet,
    sp.MaSanPham,
    sp.TenSanPham,
    th.TenThuongHieu,
    ms.TenMauSac,
    kt.TenKichThuoc,
    spct.GiaBan,
    spct.SoLuongTon,
    spct.DuongDanAnh,
    -- Giá sau giảm (lấy đợt giảm giá đang hoạt động)
    ISNULL(
        spct.GiaBan * (1 - MAX(dgg.GiaTriGiam) / 100.0),
        spct.GiaBan
    ) AS GiaSauGiam,
    ISNULL(MAX(dgg.GiaTriGiam), 0) AS PhanTramGiam
FROM SanPhamChiTiet spct
JOIN SanPham sp             ON spct.MaSanPham    = sp.MaSanPham
JOIN ThuongHieu th          ON sp.MaThuongHieu   = th.MaThuongHieu
JOIN MauSac ms              ON spct.MaMauSac     = ms.MaMauSac
JOIN KichThuoc kt           ON spct.MaKichThuoc  = kt.MaKichThuoc
LEFT JOIN ChiTietDotGiamGia ctdgg ON ctdgg.MaSanPhamChiTiet = spct.MaSanPhamChiTiet
LEFT JOIN DotGiamGia dgg    ON ctdgg.MaGiamGia   = dgg.MaGiamGia
                            AND dgg.TrangThai = N'Đang hoạt động'
                            AND CAST(GETDATE() AS DATE) BETWEEN dgg.NgayBatDau AND dgg.NgayKetThuc
WHERE spct.TrangThai = N'Hoạt động'
  AND sp.TrangThai   = 1
GROUP BY spct.MaSanPhamChiTiet, sp.MaSanPham, sp.TenSanPham, th.TenThuongHieu,
         ms.TenMauSac, kt.TenKichThuoc, spct.GiaBan, spct.SoLuongTon, spct.DuongDanAnh;
GO

-- ============================================================
-- PHẦN 4: KIỂM TRA DỮ LIỆU
-- ============================================================
PRINT N'=== KIỂM TRA DỮ LIỆU ===';
PRINT N'Tài khoản: '       + CAST((SELECT COUNT(*) FROM TaiKhoan) AS NVARCHAR);
PRINT N'Nhân viên: '       + CAST((SELECT COUNT(*) FROM NhanVien) AS NVARCHAR);
PRINT N'Khách hàng: '      + CAST((SELECT COUNT(*) FROM KhachHang) AS NVARCHAR);
PRINT N'Sản phẩm: '        + CAST((SELECT COUNT(*) FROM SanPham) AS NVARCHAR);
PRINT N'Biến thể SPCT: '   + CAST((SELECT COUNT(*) FROM SanPhamChiTiet) AS NVARCHAR);
PRINT N'Hoá đơn: '         + CAST((SELECT COUNT(*) FROM HoaDon) AS NVARCHAR);
PRINT N'Phiếu nhập hàng: ' + CAST((SELECT COUNT(*) FROM PhieuNhapHang) AS NVARCHAR);
GO


/* ============================================================================
   BỔ SUNG (website bán hàng online): Bảng KHACHHANG_VOUCHER
   Voucher gán riêng cho từng khách hàng (LoaiApDung = 2 trong bảng GiamGia).
   Được entity GiamGiaChiTiet sử dụng. Guarded — an toàn chạy lại nhiều lần.
   ============================================================================ */
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'KHACHHANG_VOUCHER')
BEGIN
    CREATE TABLE KHACHHANG_VOUCHER (
        MaKhachHang     VARCHAR(20) NOT NULL,
        MaGiamGia       VARCHAR(20) NOT NULL,
        NgayNhan        DATETIME    NULL DEFAULT GETDATE(),
        TrangThaiSuDung INT         NULL DEFAULT 0,  -- 0 = chưa dùng, 1 = đã dùng
        CONSTRAINT PK_KHACHHANG_VOUCHER PRIMARY KEY (MaKhachHang, MaGiamGia),
        CONSTRAINT FK_KHV_KhachHang FOREIGN KEY (MaKhachHang)
            REFERENCES KhachHang(MaKhachHang) ON DELETE CASCADE,
        CONSTRAINT FK_KHV_GiamGia FOREIGN KEY (MaGiamGia)
            REFERENCES GiamGia(MaGiamGia) ON DELETE CASCADE
    );
END
GO


/* ============================================================================
   BỔ SUNG (Chatbot & Hỗ trợ trực tuyến): bảng HoTroTinNhan — lưu hội thoại
   giữa khách hàng / khách vãng lai, chatbot và nhân viên. Guarded.
   ============================================================================ */
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'HoTroTinNhan')
BEGIN
    CREATE TABLE HoTroTinNhan (
        MaTinNhan    INT IDENTITY(1,1) PRIMARY KEY,
        MaPhien      VARCHAR(50)    NOT NULL,
        MaKhachHang  VARCHAR(20)    NULL,
        TenHienThi   NVARCHAR(100)  NULL,
        NguoiGui     VARCHAR(10)    NOT NULL,      -- 'KHACH' | 'BOT' | 'NHANVIEN'
        NoiDung      NVARCHAR(2000) NOT NULL,
        ThoiGian     DATETIME       NOT NULL DEFAULT GETDATE(),
        DaXem        BIT            NOT NULL DEFAULT 0
    );
    CREATE INDEX IX_HoTroTinNhan_MaPhien ON HoTroTinNhan (MaPhien, MaTinNhan);
END
GO


/* ============================================================================
   BỔ SUNG (module Liên hệ): bảng LienHe — tin nhắn từ trang Liên hệ của khách.
   ============================================================================ */
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'LienHe')
BEGIN
    CREATE TABLE LienHe (
        MaLienHe  INT IDENTITY(1,1) PRIMARY KEY,
        HoTen     NVARCHAR(100)  NOT NULL,
        Email     NVARCHAR(150)  NOT NULL,
        NoiDung   NVARCHAR(2000) NOT NULL,
        ThoiGian  DATETIME       NOT NULL DEFAULT GETDATE(),
        TrangThai NVARCHAR(20)   NOT NULL DEFAULT N'Chưa xử lý'
    );
END
GO
