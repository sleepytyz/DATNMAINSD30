/* ============================================================================
   MIGRATION: Bảng HoTroTinNhan — Chatbot & Hỗ trợ trực tuyến
   Lưu toàn bộ hội thoại giữa KHÁCH (kể cả khách vãng lai), CHATBOT và NHÂN VIÊN.
   Bắt buộc chạy trước khi dùng tính năng chat. An toàn chạy nhiều lần.
   ============================================================================ */
USE Duantotnghiep_FSShoes;
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'HoTroTinNhan')
BEGIN
    CREATE TABLE HoTroTinNhan (
        MaTinNhan    INT IDENTITY(1,1) PRIMARY KEY,
        MaPhien      VARCHAR(50)    NOT NULL,      -- 1 phiên = 1 cuộc hội thoại (KH-xxx hoặc GUEST-xxx)
        MaKhachHang  VARCHAR(20)    NULL,          -- NULL nếu khách vãng lai
        TenHienThi   NVARCHAR(100)  NULL,          -- Tên hiển thị của chủ phiên
        NguoiGui     VARCHAR(10)    NOT NULL,      -- 'KHACH' | 'BOT' | 'NHANVIEN'
        NoiDung      NVARCHAR(2000) NOT NULL,
        ThoiGian     DATETIME       NOT NULL DEFAULT GETDATE(),
        DaXem        BIT            NOT NULL DEFAULT 0   -- nhân viên đã đọc tin của khách chưa
    );
    CREATE INDEX IX_HoTroTinNhan_MaPhien ON HoTroTinNhan (MaPhien, MaTinNhan);
    PRINT N'Đã tạo bảng HoTroTinNhan.';
END
ELSE
    PRINT N'Bảng HoTroTinNhan đã tồn tại — bỏ qua.';
GO
