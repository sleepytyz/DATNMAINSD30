/* ============================================================================
   MIGRATION: Bảng KHACHHANG_VOUCHER (voucher gán riêng cho từng khách hàng)
   ----------------------------------------------------------------------------
   Bắt buộc chạy trước khi khởi động website bán hàng mới, vì:
     • Entity GiamGiaChiTiet (@Table "KHACHHANG_VOUCHER") được dùng bởi
       tính năng voucher cá nhân + trang "Voucher của tôi" + áp mã ở giỏ hàng.
     • Bảng này CHƯA có trong database_FSShoes.sql bản gốc (đã được bổ sung
       ở cuối file đó; script này dành cho DB đã tạo từ trước).
   An toàn chạy nhiều lần (IF NOT EXISTS).
   ============================================================================ */
USE Duantotnghiep_FSShoes;
GO

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
    PRINT N'Đã tạo bảng KHACHHANG_VOUCHER.';
END
ELSE
    PRINT N'Bảng KHACHHANG_VOUCHER đã tồn tại — bỏ qua.';
GO
