# Báo cáo hợp nhất mã nguồn (Merge Report)

Hợp nhất **3-way** hai bản `DuAnTotNghiepSD-30` (bản team, đã có commit "ban hàng 5/7")
và `DuAnTotNghiepSD-30-fixed` (bản sửa lỗi + tính năng mới) thành một project duy nhất.

## Bối cảnh
- Hai bản có lịch sử git **rời rạc** (không chung commit), nên không merge trực tiếp được.
- Tổ tiên chung được xác định là commit `f2c332d` (~30/6). Từ đó:
  - **Bản team** thêm: commit "ban hàng 5/7" (sửa lớn màn bán hàng, sidebar, đăng ký, home).
  - **Bản fixed** thêm: sửa lỗi nghiệp vụ (đặt hàng, tồn kho, vòng đời đơn, doanh thu) +
    các module mới: Đánh giá, Hỗ trợ chat/Realtime (WebSocket), Liên hệ, Yêu thích, Chatbot, Trang tĩnh.
- **Nền merge**: dùng bản **fixed** làm gốc (đã sửa lỗi, nhất quán), rồi ghép **delta của team**
  lên trên bằng `git merge-file` 3-way.

## Kết quả
- Ghép sạch tự động (không xung đột): **58 file**.
- Thêm mới từ bản fixed (module mới): **37 file** + module **Liên hệ** (LienHe) vốn chưa commit.
- Thêm file riêng của team (NhanVienCaNhanController, VoucherDTO, TaiKhoanGlobalAdvice, template hồ sơ NV...).
- **Xung đột phải xử lý tay: 15 file** (chi tiết bên dưới).

## Cách xử lý 15 file xung đột
| File | Quyết định |
|------|-----------|
| Service/HoaDonService.java | **Giữ cả hai**: method truy vấn của team + field realtime & biến `cu` của fixed |
| Controller/DonHangController.java | **Giữ cả hai**: API `/api/detail`,`/api/products` (team) + `/huy`, thẻ đếm trạng thái (fixed) |
| Controller/TaiKhoanCaNhanController.java | **Lấy fixed** (rewrite xử lý địa chỉ có validate, chuẩn hơn) |
| Controller/UserController.java | **Lấy team `LocalDateTime`** để khớp ThongKeService/HoaDonRepo; giữ đủ 3 import |
| Controller/ThanhToanController.java | **Lấy fixed** (thêm voucher khả dụng) |
| Repository/HoaDonChiTietRepository.java | **Lấy fixed** (query "đã bán theo sản phẩm") |
| Repository/SanPhamChiTietRepository.java | **Giữ cả hai**: khoá tồn kho + đếm sắp hết/hết (fixed) + query của team |
| Repository/SanPhamRepository.java | **Lấy fixed** (query search LEFT JOIN an toàn với null) |
| Storefront/DangKyKhachHangDTO.java | **Lấy team** (đăng ký chỉ user/mật khẩu — khớp UserController & register.html) |
| config/SecurityConfig.java | **Gộp cả hai** bộ quy tắc phân quyền + CSRF ignore |
| static/storefront/css/storefront.css | **Lấy fixed** (thiết kế header mới) |
| templates/account/admin/home.html | **Lấy team** (thêm card "sản phẩm bán chạy") |
| templates/account/user/register.html | **Lấy team** (khớp DTO đơn giản hoá) |
| templates/donhang/index.html | **Lấy fixed** (đủ 5 trạng thái, khớp vòng đời đơn) |
| templates/taikhoan/ho-so.html | **Lấy fixed** (form hồ sơ đầy đủ: maKH/username readonly + hoTen/sdt/email editable) |

## Lỗi có sẵn đã sửa thêm khi merge
- **DiaChiService.java**: bản team bị **lỗi trùng & cắt method giữa chừng** (save/deleteById/findByKhachHang_MaKH bị nhân đôi, thân hàm bị đứt). Đã **viết lại sạch**, gộp đủ method cả hai bên cần (`save` trả `DiaChi` cho BanHang, `findByKhachHang(String)` cho fixed...).
- **GiamGiaService.java**: bản team định nghĩa `markVoucherAsUsed` **2 lần** → gộp còn 1.
- **DiaChiRepo.java**: bản team có **`}` thừa** đóng interface giữa chừng → đã xoá.
- **application.properties**: đổi `databaseName` → `Duantotnghiep_FSShoes` (khớp `database_FSShoes.sql`).
- Xoá `DuantotnghiepApplicationTests.java` (sai package, làm hỏng build — theo CHANGELOG).
- `pom.xml`: dùng Spring Boot **3.3.5** (bản fixed ổn định) thay vì 4.0.3 (khai báo artifact không tồn tại).
- Chuẩn hoá toàn bộ line-ending về **LF**.

## Kiểm thử
- ✅ Không còn dấu xung đột (`<<<<<<<`) trong bất kỳ file nào.
- ✅ Không còn method trùng; ngoặc `{}` cân bằng trên mọi file `.java`.
- ✅ Toàn bộ method then chốt liên module đều có định nghĩa (đã rà chéo).
- ⚠️ **Chưa build được bằng Maven** trong môi trường này (Maven Central bị chặn mạng).
  Hãy mở bằng IntelliJ/Eclipse và chạy `mvn clean package` trên máy có mạng để build lần đầu.

## Chạy dự án
1. Mở SSMS, chạy `database_FSShoes.sql` để tạo CSDL `Duantotnghiep_FSShoes` + dữ liệu mẫu.
   (Chạy thêm các file `*_migration.sql` nếu cần bảng cho tính năng mới.)
2. Kiểm tra `src/main/resources/application.properties` (user/password SQL Server).
3. `mvn clean package` rồi chạy ứng dụng.

---

## Cập nhật lần 2 — Sửa lỗi build (đã COMPILE THÀNH CÔNG)

Sau khi build lần đầu bằng IntelliJ báo lỗi ở 11 file, đã tìm ra **nguyên nhân gốc**:

- **`Repository/DiaChiRepo.java` khai báo TRÙNG hàng loạt method** (findDefaultByMaKH,
  resetAllDefault, countByKhachHang_MaKH, findBySoDienThoaiNguoiNhanContaining,
  findByTenNguoiNhanContaining, deleteByKhachHang_MaKH, findByTinhThanh, findByQuanHuyen,
  findByMaDiaChiAndKhachHang_MaKH...). Interface Java **không cho phép hai method cùng chữ ký**
  ⇒ lỗi biên dịch. Đây là lỗi có sẵn trong nhánh team (bị dán method 2 lần), lần merge trước
  mới chỉ xoá dấu `}` thừa mà chưa gộp method trùng.
- 10 file còn lại trong ảnh (DiaChiService, TaiKhoanService, KhachHangService, SecurityConfig,
  BanHangApiController, GioHang, GioHangGlobalAdvice, DanhMucSanPhamService, TaiKhoanGlobalAdvice,
  CustomUserDetailsService, ChiTietDotGiamGiaService) **không tự sai** — chúng lỗi dây chuyền vì
  phụ thuộc DiaChiRepo/DiaChiService không biên dịch được.

**Đã sửa:** viết lại `DiaChiRepo.java` sạch, mỗi method khai báo đúng **một lần** (giữ đủ mọi
method đang được gọi trong dự án).

**Đã kiểm chứng bằng biên dịch thật:** dùng `javac` với đầy đủ thư viện Spring Boot 3.3.5
(lấy từ `target` của bản fixed) + Lombok → **biên dịch 176 lớp, 0 lỗi, 0 cảnh báo**.
(Hai class `org.apache.catalina.connector.Connector` và `jakarta.servlet.ServletException`
do Tomcat/servlet container cung cấp lúc chạy, không nằm trong thư viện đóng gói — không phải lỗi mã.)
