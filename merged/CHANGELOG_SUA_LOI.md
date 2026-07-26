# FS Shoes — Ghi chú các thay đổi (sửa lỗi & hoàn thiện)

Tài liệu này liệt kê toàn bộ thay đổi đã thực hiện trên dự án **DuAnTotNghiepSD-30**
(website bán giày thể thao nam + quản lý bán hàng, Spring Boot + SQL Server).

---

## 0. Tóm tắt nhanh

Trọng tâm: **rà soát & sửa lỗi nghiệp vụ** (đặt hàng, tồn kho, trạng thái đơn), **kết nối
website mua hàng với quản lý bán hàng**, và **bổ sung 3 tính năng còn thiếu**: Giới thiệu,
Theo dõi đơn hàng (tra cứu công khai), Đánh giá sản phẩm.

> ⚠️ Lưu ý: Môi trường xử lý không tải được thư viện Maven nên **chưa build/compile tự động**.
> Các thay đổi đã được kiểm tra thủ công (chữ ký hàm, cân bằng ngoặc, xung đột route, ánh xạ
> entity). Bạn hãy mở bằng IntelliJ/Eclipse để `mvn clean package` lần đầu (máy bạn tải được thư viện).

---

## 1. Cấu hình build & kết nối (bắt buộc để chạy được)

### 1.1. `pom.xml` — sửa lỗi không build được
- **Trước:** dùng `spring-boot-starter-parent` phiên bản `4.0.3` nhưng lại khai báo các
  artifact theo tên kiểu 3.x đã bị đổi/không tồn tại: `spring-boot-starter-webmvc`,
  `spring-boot-starter-data-jpa-test`, `spring-boot-starter-thymeleaf-test`,
  `spring-boot-starter-webmvc-test`. Ngoài ra `spring-boot-starter-security` bị ghi đè
  cứng version `3.3.0` (xung đột với version quản lý bởi parent), và biến `${lombok.version}`
  **không được định nghĩa** ⇒ Maven không resolve được.
- **Sau:** hạ về **Spring Boot 3.3.5 (ổn định)**, dùng đúng tên artifact
  (`spring-boot-starter-web`, `...-data-jpa`, `...-test`, `spring-security-test`),
  định nghĩa `lombok.version = 1.18.36`, bỏ version cứng của security.

### 1.2. `application.properties` — trỏ đúng CSDL
- **Trước:** `databaseName=Duantotnghiep2` (không khớp file SQL bạn gửi).
- **Sau:** `databaseName=Duantotnghiep_FSShoes` (khớp `database_FSShoes.sql`).

---

## 2. Sửa lỗi nghiệp vụ nghiêm trọng

### 2.1. Đặt hàng online bị lỗi vì **thiếu mã hoá đơn** (lỗi nặng — không đặt được hàng)
- **File:** `Storefront/DonHangOnlineService.java`
- **Vấn đề:** `HoaDon.maHoaDon` là khoá chính `VARCHAR(20)` **không tự sinh**
  (`@Id` không có `@GeneratedValue`). Hàm `datHang()` tạo `HoaDon` mà **không set `maHoaDon`**
  ⇒ lưu xuống DB sẽ lỗi khoá chính rỗng. (Bán tại quầy thì có sinh mã, nhưng đặt online thì quên.)
- **Sửa:** thêm hàm `taoMaHoaDon()` sinh mã duy nhất dạng `HDyyMMddHHmmss` (+ hậu tố nếu trùng),
  và gọi `hoaDon.setMaHoaDon(taoMaHoaDon())` trước khi lưu.

### 2.2. Đăng ký tài khoản bị lỗi vì **thiếu mã khách hàng**
- **File:** `Service/TaiKhoanService.java`
- **Vấn đề:** `registerCustomer()` không set `KhachHang.maKH` (khoá chính `String` không tự sinh)
  ⇒ đăng ký khách hàng mới sẽ lỗi khoá chính rỗng.
- **Sửa:** thêm `taoMaKhachHang()` sinh mã `KHxxxx` và set trước khi lưu.

### 2.3. Sản phẩm **biến mất khỏi website** sau khi cập nhật tồn kho (lỗi nặng, âm thầm)
- **File:** `Service/SanPhamChiTietService.java`, `Storefront/SanPhamHienThiService.java`
- **Vấn đề:** `capNhatTrangThaii()` ghi đè `TrangThai` của biến thể thành
  `"Còn hàng"/"Sắp hết"/"Hết hàng"`, trong khi website (`SanPhamHienThiService`) chỉ hiển thị
  biến thể có trạng thái `null` hoặc `"Hoạt động"`. Vì hàm này được gọi **mỗi lần đặt hàng /
  nhập kho / sửa biến thể**, nên chỉ cần một giao dịch là sản phẩm biến mất khỏi trang bán hàng.
  (3 bộ từ vựng trạng thái lẫn lộn: `Hoạt động` — `Còn hàng/Sắp hết/Hết hàng` — `Ngừng bán`.)
- **Sửa:**
  - `SanPhamHienThiService`: đổi bộ lọc sang **chỉ ẩn** biến thể `"Ngừng bán"/"Ngừng kinh doanh"`;
    các trạng thái tồn kho vẫn hiển thị (hết hàng thì hiện "hết hàng" như bình thường).
  - `capNhatTrangThaii`: **không tự bật lại** biến thể đã bị `"Ngừng bán"` thủ công khi cập nhật tồn kho.

### 2.4. Ánh xạ JPA sai trong entity `TaiKhoan` (rủi ro lỗi khởi động)
- **File:** `Entity/TaiKhoan.java`
- **Vấn đề:** có trường `@OneToOne taiKhoan` **tự trỏ vào chính nó** trên cùng cột khoá `MaTaiKhoan`,
  đồng thời `nhanVien`/`khachHang` (phía `mappedBy`) lại thừa `@JoinColumn` ⇒ dễ gây lỗi
  Hibernate "Repeated column in mapping". Trường tự trỏ này **không được dùng ở đâu**.
- **Sửa:** xoá trường tự trỏ và bỏ `@JoinColumn` thừa ở phía `mappedBy`.

### 2.5. Sai chính tả trạng thái "Đã huỷ" làm sai thống kê
- **File:** `Controller/UserController.java`
- **Vấn đề:** dashboard admin đếm đơn huỷ bằng chuỗi `"Đã hủy"` (u thường) trong khi toàn hệ
  thống dùng `"Đã huỷ"` (u có dấu hỏi) ⇒ số "đơn đã huỷ" luôn = 0.
- **Sửa:** đổi thành `"Đã huỷ"` cho khớp.

---

## 3. Kết nối Website mua hàng ⇄ Quản lý bán hàng (đúng nghiệp vụ)

> Khi khách đặt hàng online, hệ thống tạo `HoaDon` với `LoaiBan = "Online"`,
> `TrangThai = "Chờ xác nhận"`, đồng thời **trừ tồn kho ngay để giữ hàng**. Đơn này xuất hiện
> ở màn **Quản lý đơn hàng** (`/donhang/index`) để nhân viên xử lý. (Phần này vốn đã có, dưới
> đây là các sửa lỗi để vòng đời đơn chạy đúng.)

### 3.1. Vòng đời trạng thái đơn — sửa bước "giao hàng" bị sai
- **File:** `Service/HoaDonService.java`
- **Vấn đề:** `suattdgg()` (nút "Hoàn thành") lại yêu cầu trạng thái `"Đã xác nhận"` thay vì
  `"Đang giao"`, và không ghi `NgayThanhToan`.
- **Sửa:** `suattdgg()` chỉ chạy khi đơn đang `"Đang giao"` ⇒ chuyển sang `"Đã giao"` **và**
  ghi `NgayThanhToan` (COD = nhận tiền khi giao). Vòng đời chuẩn:
  **Chờ xác nhận → Đã xác nhận → Đang giao → Đã giao** (+ **Đã huỷ**).

### 3.2. Thêm chức năng **Huỷ đơn ở phía quản lý** + hoàn tồn kho
- **File:** `Storefront/DonHangOnlineService.java` (thêm `huyDonAdmin`),
  `Controller/DonHangController.java` (thêm endpoint `/donhang/huy`),
  `templates/donhang/index.html` (thêm nút "Huỷ đơn").
- **Vấn đề:** trước đây chỉ khách tự huỷ mới hoàn tồn kho; nhân viên **không có** nút huỷ,
  nếu huỷ tay thì tồn kho đã trừ sẽ mất vĩnh viễn.
- **Sửa:** `huyDonAdmin` cho phép huỷ khi đơn ở `Chờ xác nhận/Đã xác nhận/Đang giao`,
  **hoàn lại tồn kho và voucher**; không cho huỷ đơn đã kết thúc (`Đã giao/Đã huỷ/Đã trả hàng`).

### 3.3. Thống nhất trạng thái kết thúc "Đã giao" (bỏ "Hoàn thành" lẫn lộn)
- **File:** `templates/donhang/index.html`
- **Vấn đề:** template dùng `"Hoàn thành"` cho mốc cuối (timeline, badge, bộ lọc) trong khi
  service set `"Đã giao"` ⇒ trạng thái cuối không khớp, thẻ đếm dùng biến `totalHoanThanh`
  không được controller cấp.
- **Sửa:** đổi mọi so sánh/nhãn logic sang `"Đã giao"`; thẻ đếm dùng `totalDaGiao`.

### 3.4. Cấp số liệu cho các thẻ đếm trạng thái ở màn đơn hàng
- **File:** `Controller/DonHangController.java`
- **Sửa:** thêm `totalChoXacNhan / totalDaXacNhan / totalDangGiao / totalDaGiao / totalDaHuy`.

### 3.5. Doanh thu tính thiếu đơn online đã giao
- **File:** `Repository/HoaDonRepo.java`
- **Vấn đề:** các truy vấn doanh thu (`thongKeDoanhThuTheoNgay/TheoThang/TongQuan`) chỉ đếm
  `TrangThai = N'Đã thanh toán'` (bán quầy). Đơn online kết thúc ở `"Đã giao"` ⇒ **không được
  tính vào doanh thu**.
- **Sửa:** đếm cả `N'Đã thanh toán'` **và** `N'Đã giao'`.

---

## 4. Bổ sung tính năng còn thiếu cho website

### 4.1. Trang **Giới thiệu** — `/gioi-thieu`
- File: `Controller/TrangTinhController.java`, `templates/trangtinh/gioi-thieu.html`.
- Nội dung: giới thiệu cửa hàng, giá trị cốt lõi, cam kết, số liệu, nút CTA sang trang sản phẩm.

### 4.2. **Theo dõi đơn hàng** (tra cứu công khai) — `/theo-doi-don-hang`
- File: `TrangTinhController.java`, `templates/trangtinh/theo-doi-don-hang.html`,
  `templates/trangtinh/theo-doi-ket-qua.html`.
- Cho phép **khách không đăng nhập** tra cứu bằng **Mã đơn + Số điện thoại**. Xác thực SĐT
  nhập phải khớp SĐT khách hàng của đơn (tránh lộ đơn người khác). Hiển thị timeline trạng thái,
  chi tiết sản phẩm, tổng tiền.

### 4.3. **Đánh giá sản phẩm** — `/danh-gia` và trong trang chi tiết
- File mới:
  - Entity `Entity/DanhGia.java`, Repository `Repository/DanhGiaRepository.java`,
    Service `Service/DanhGiaService.java`.
  - Bảng CSDL `DanhGia` (thêm vào `database_FSShoes.sql` và file riêng `danhgia_migration.sql`).
  - Template `templates/trangtinh/danh-gia.html` (danh sách SP kèm điểm trung bình & số lượt).
  - Khối đánh giá trong `templates/cuahang/chi-tiet.html` (điểm trung bình, danh sách đánh giá,
    form gửi đánh giá).
- **Quy tắc nghiệp vụ:** khách **chỉ đánh giá được sản phẩm đã mua và đơn đã `"Đã giao"`**,
  mỗi sản phẩm trong một hoá đơn chỉ đánh giá **một lần**. Form chỉ hiện khi đủ điều kiện.
- Admin có thể ẩn/hiện đánh giá (`DanhGiaService.doiTrangThai`, cột `TrangThai`).

### 4.4. Menu điều hướng đầy đủ + danh mục
- File: `templates/fragments/client-header.html`, `config/GioHangGlobalAdvice.java`,
  `templates/trangchu/index.html`.
- Menu mới: **Trang chủ · Danh mục (dropdown) · Sản phẩm · Nổi bật · Đánh giá · Theo dõi đơn ·
  Giới thiệu**. Dropdown "Danh mục" lấy dữ liệu từ `@ControllerAdvice` (hiện trên mọi trang).
  Thêm mỏ neo `#noi-bat` cho mục "Sản phẩm nổi bật" ở trang chủ.

### 4.5. Phân quyền cho route mới
- File: `config/SecurityConfig.java`.
- `permitAll`: `/gioi-thieu`, `/theo-doi-don-hang/**`, `/danh-gia` (xem).
- Yêu cầu đăng nhập USER: `/danh-gia/gui` (gửi đánh giá).

### 4.6. CSS
- File: `static/storefront/css/storefront.css` — bổ sung style cho: dropdown danh mục, sao đánh giá,
  form đánh giá, danh sách đánh giá, timeline theo dõi đơn, trang giới thiệu (responsive).

---

## 5. Hướng dẫn chạy

1. **Tạo CSDL:** mở SSMS, chạy `database_FSShoes.sql` (tạo DB `Duantotnghiep_FSShoes` + dữ liệu mẫu,
   đã bao gồm bảng `DanhGia`).
   - Nếu **đã import DB từ trước** (chưa có bảng `DanhGia`): chỉ cần chạy thêm `danhgia_migration.sql`.
2. **Sửa kết nối** trong `src/main/resources/application.properties` nếu user/password SQL của bạn khác
   (`spring.datasource.username` / `password`).
3. **Build & chạy:** `mvn clean package` rồi chạy `JAVA202_B2-0.0.1-SNAPSHOT.war`, hoặc chạy trực tiếp
   `Th06876Java202Application` trong IDE. Truy cập `http://localhost:8080`.
4. **Tài khoản mẫu** (mật khẩu trong file SQL): `admin/Admin@123`, `nv001/Staff@123`,
   `kh001/User@123`.

## 6. Luồng kiểm thử gợi ý

1. Đăng nhập `kh001` → vào **Sản phẩm** → mở 1 sản phẩm → thêm vào giỏ → **Thanh toán** → đặt hàng.
2. Đăng nhập `admin` (hoặc `nv001`) → **Quản lý đơn hàng** (`/donhang/index`) → thấy đơn
   **"Chờ xác nhận"** → bấm **Xác nhận → Giao hàng → Hoàn thành** (đơn thành "Đã giao"), hoặc **Huỷ đơn**
   (tồn kho được hoàn lại).
3. Sau khi đơn "Đã giao": đăng nhập lại `kh001` → mở đúng sản phẩm đã mua → **viết đánh giá**.
4. Khách vãng lai: mở **Theo dõi đơn** → nhập mã đơn + SĐT của `kh001` → xem trạng thái.

---

## 7. Các lỗi phát sinh khi build & chạy thực tế (đã sửa)

Sau khi build/chạy trên máy thật (Maven tải được thư viện), lần lượt phát sinh và đã sửa:

### 7.1. `application.properties` sai encoding (build fail)
- Lỗi: `MalformedInputException: Input length = 1` khi Maven copy resource.
- Nguyên nhân: file lưu ISO-8859 (có chữ Việt trong comment/địa chỉ GHN).
- Sửa: ghi lại file thuần ASCII/UTF-8 (địa chỉ shop để không dấu), giữ nguyên mọi giá trị cấu hình.

### 7.2. Import Tomcat kiểu Spring Boot 4.x (compile fail)
- Lỗi: `package org.springframework.boot.tomcat.servlet does not exist` trong `SecurityConfig`.
- Sửa: đổi import về `org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory` (đúng Spring Boot 3.3.5).

### 7.3. `DaoAuthenticationProvider` dùng API 6.4+ (compile fail)
- Lỗi: constructor `new DaoAuthenticationProvider(userDetailsService)` không tồn tại ở Spring Security 6.3.
- Sửa: `new DaoAuthenticationProvider()` + `setUserDetailsService(...)`.

### 7.4. Test rác + test cần DB (test fail)
- Xoá `src/test/java/com/example/duantotnghiep/DuantotnghiepApplicationTests.java` (sai package, không có @SpringBootConfiguration).
- Bỏ `@SpringBootTest` khỏi `Th06876Java202ApplicationTests` để không cần khởi động context/DB khi build.

### 7.5. Query JPA sai kiểu tham số (app không khởi động)
- Lỗi: `Cannot compare left expression of type 'String' with right expression of type 'Integer'`.
- Nguyên nhân: `SanPhamChiTietRepository.findBySanPham_MaSanPhamAndMauSac_TenMauSac(Integer, String)` — `maSanPham` là String.
- Sửa: đổi tham số đầu thành `String`. (Đồng thời sửa `findByidmasp` nhận `List<String>` cho đúng kiểu.)

### 7.6. Mật khẩu mẫu trong DB là hash BCrypt không hợp lệ (không đăng nhập được)
- Sửa: tạo hash BCrypt thật và UPDATE. Xem mục "Tài khoản mẫu" — mật khẩu: admin `Admin@123`, nv001/nv002 `Staff@123`, kh001..kh003 `User@123`. Chạy các câu UPDATE trong SSMS (đã cung cấp trong hội thoại).

### 7.7. Lỗi serialize session sau khi đăng nhập (NotSerializableException: TaiKhoan)
- Nguyên nhân: app cấu hình lưu session vào DB (`spring-session-jdbc`); khi lưu `SecurityContext` phải serialize entity `TaiKhoan` nhưng entity chưa `implements Serializable`.
- Sửa (2 lớp):
  1. `application.properties`: chuyển `spring.session.store-type=none` (session lưu in-memory, không serialize xuống DB) + `server.servlet.session.timeout=30m`.
  2. Cho `TaiKhoan`, `KhachHang`, `NhanVien`, `DiaChi` **implements Serializable**; đánh dấu `transient` các collection LAZY (`danhSachDiaChi`, `danhSachChamCong`, `danhSachBanGiao`, `danhSachNhanGiao`) để không kéo theo khi serialize.

> Lưu ý: `spring-session-jdbc` vẫn còn trong `pom.xml` nhưng không được kích hoạt khi `store-type=none`. Có thể giữ nguyên.

---

## [MỚI] Xây lại toàn bộ website bán hàng online + realtime 2 chiều với trang quản lý

Toàn bộ storefront được thay bằng phiên bản mới (trang chủ, cửa hàng + lọc/tìm kiếm,
chi tiết biến thể, yêu thích, giỏ hàng AJAX + voucher + freeship, thanh toán COD/VietQR,
tài khoản + sổ địa chỉ + đơn hàng của tôi, theo dõi đơn công khai, đánh giá đã-mua-hàng),
kết nối **WebSocket thời gian thực 2 chiều**: đơn online hiện ngay tại quầy (hoá đơn
"Chờ xác nhận" kèm số lượng/giá + âm báo), quầy đổi trạng thái → timeline phía khách tự nhảy;
kèm **panel trạng thái hoạt động của từng module** trên dashboard. Đã bổ sung bảng
`KHACHHANG_VOUCHER` còn thiếu (xem `khachhang_voucher_migration.sql`).

➡ Chi tiết đầy đủ: **HUONG_DAN_WEBSITE_MOI.md**
