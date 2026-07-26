-- ============================================================
-- MIGRATION: Bảng ĐÁNH GIÁ SẢN PHẨM (DanhGia)
-- Chạy file này trên CSDL Duantotnghiep_FSShoes ĐÃ tồn tại
-- để bổ sung tính năng đánh giá mà không cần import lại toàn bộ.
-- An toàn khi chạy nhiều lần (kiểm tra tồn tại trước khi tạo/chèn).
-- ============================================================
USE Duantotnghiep_FSShoes;
GO

-- 1) Tạo bảng nếu chưa có
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'DanhGia')
BEGIN
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
    PRINT N'Đã tạo bảng DanhGia.';
END
ELSE
    PRINT N'Bảng DanhGia đã tồn tại - bỏ qua bước tạo.';
GO

-- 2) Dữ liệu mẫu (chỉ chèn khi bảng đang rỗng để tránh trùng)
IF NOT EXISTS (SELECT 1 FROM DanhGia)
BEGIN
    -- Gắn với các sản phẩm mẫu SP001..SP005 và khách KH001..KH003
    INSERT INTO DanhGia (MaSanPham, MaKhachHang, MaHoaDon, SoSao, NoiDung, NgayDanhGia, TrangThai) VALUES
    (N'SP001', N'KH001', NULL, 5, N'Giày chạy rất êm, đi cả ngày không mỏi chân. Rất hài lòng!', '2024-06-10 09:30:00', 1),
    (N'SP001', N'KH002', NULL, 4, N'Chất lượng tốt, giao hàng nhanh. Size hơi rộng một chút.',     '2024-06-12 14:10:00', 1),
    (N'SP002', N'KH003', NULL, 5, N'Air Jordan chính hãng, form đẹp, đóng gói cẩn thận.',           '2024-06-15 16:45:00', 1),
    (N'SP003', N'KH001', NULL, 4, N'Superstar cổ điển đẹp, phối đồ dễ. Đáng mua trong tầm giá.',    '2024-06-18 10:20:00', 1),
    (N'SP004', N'KH002', NULL, 5, N'Ultraboost đế êm thật sự, chạy bộ rất thích.',                  '2024-06-20 08:05:00', 1);
    PRINT N'Đã chèn 5 đánh giá mẫu.';
END
ELSE
    PRINT N'Bảng DanhGia đã có dữ liệu - bỏ qua chèn mẫu.';
GO

PRINT N'=== HOÀN TẤT MIGRATION DanhGia ===';
PRINT N'Số đánh giá: ' + CAST((SELECT COUNT(*) FROM DanhGia) AS NVARCHAR);
GO
