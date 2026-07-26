/* ============================================================================
   MIGRATION: Bảng LienHe — tin nhắn khách gửi từ trang "Liên hệ".
   Quản lý xem tại module Liên hệ (/lienhe/index), nhận thông báo realtime
   và trả lời khách qua Gmail. An toàn chạy nhiều lần.
   ============================================================================ */
USE Duantotnghiep_FSShoes;
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'LienHe')
BEGIN
    CREATE TABLE LienHe (
        MaLienHe  INT IDENTITY(1,1) PRIMARY KEY,
        HoTen     NVARCHAR(100)  NOT NULL,
        Email     NVARCHAR(150)  NOT NULL,
        NoiDung   NVARCHAR(2000) NOT NULL,
        ThoiGian  DATETIME       NOT NULL DEFAULT GETDATE(),
        TrangThai NVARCHAR(20)   NOT NULL DEFAULT N'Chưa xử lý'  -- 'Chưa xử lý' | 'Đã xử lý'
    );
    PRINT N'Đã tạo bảng LienHe.';
END
ELSE
    PRINT N'Bảng LienHe đã tồn tại — bỏ qua.';
GO
