-- Cập nhật mật khẩu tài khoản mẫu bằng hash BCrypt hợp lệ.
-- Chạy nếu bạn ĐÃ import DB trước đó với hash cũ (không đăng nhập được).
USE Duantotnghiep_FSShoes;
UPDATE TaiKhoan SET MatKhau = '$2a$10$N051cdSuwPQ9k4dqtTweIeoN4W./Vri9JVhC13oJaZfchuFsBoTTe' WHERE TenDangNhap = 'admin';        -- Admin@123
UPDATE TaiKhoan SET MatKhau = '$2a$10$wnLINBPsl4TSaY3.M4HDy.kzB9frF7guYT8P7KUmBbEF6ec0myOhG' WHERE TenDangNhap IN ('nv001','nv002'); -- Staff@123
UPDATE TaiKhoan SET MatKhau = '$2a$10$jKLukENc1.u.j3YZ4Xl5GOlSpVPZQJzPn1PS/0iY/ARMURH2M8G9a'  WHERE TenDangNhap IN ('kh001','kh002','kh003'); -- User@123
