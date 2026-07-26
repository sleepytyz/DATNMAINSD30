# HƯỚNG DẪN — WEBSITE BÁN HÀNG ONLINE MỚI (FS Shoes)

> Tài liệu cho bản **xây lại toàn bộ website bán hàng** kết nối **thời gian thực 2 chiều**
> với trang quản lý bán hàng. Đọc kèm `CHANGELOG_SUA_LOI.md` (lịch sử sửa lỗi trước đó).

---

## 1. Tổng quan kiến trúc mới

```
KHÁCH HÀNG (storefront)                    QUẢN LÝ (admin/staff)
┌──────────────────────────┐              ┌────────────────────────────┐
│ Trang chủ / Danh sách SP │              │ /donhang/index             │
│ Chi tiết SP / Đánh giá   │   đặt hàng   │  • đơn online hiện NGAY    │
│ Giỏ hàng (AJAX) / Voucher├─────────────▶│  • bộ đếm tự cập nhật      │
│ Thanh toán COD / VietQR  │              │ Dashboard admin            │
│ Tài khoản / Sổ địa chỉ   │◀─────────────┤  • panel trạng thái module │
│ Theo dõi đơn (realtime)  │  đổi trạng   │ Widget chuông (mọi trang)  │
└──────────────────────────┘  thái đơn    └────────────────────────────┘
                 ▲                                      ▲
                 └────────── WebSocket /ws (STOMP) ─────┘
```

- **Backend:** Spring Boot 3.3.5, Java 17, WAR — package `com.example.th06876_java202`
- **Realtime:** `spring-boot-starter-websocket` (SockJS + STOMP, endpoint `/ws`)
- **Giao diện khách:** Thymeleaf + CSS tự thiết kế (`/storefront/css/storefront.css`),
  font Be Vietnam Pro, KHÔNG phụ thuộc Bootstrap
- **Giao diện quản lý:** giữ nguyên Bootstrap 5 hiện có, chỉ **bổ sung** widget realtime

## 2. Tính năng website bán hàng (khách hàng)

| Nhóm | Tính năng |
|---|---|
| Trang chủ `/` | Hero, danh mục, **đang khuyến mãi** (đồng bộ đợt giảm giá từ quản lý), bán chạy (theo số đã bán thật), hàng mới, được đánh giá cao, dải voucher công khai, thương hiệu |
| Cửa hàng `/cua-hang/san-pham` | Tìm kiếm + **gợi ý tức thì** khi gõ, lọc theo danh mục / thương hiệu / kiểu giày / khoảng giá / màu / size / khuyến mãi / còn hàng, 6 kiểu sắp xếp, phân trang giữ nguyên bộ lọc |
| Chi tiết `/cua-hang/san-pham/{ma}` | Chọn **màu + size theo biến thể thật** (giá, % giảm, tồn kho, bộ ảnh đổi theo lựa chọn), thêm giỏ / mua ngay, tab mô tả + **đánh giá có xác minh đã mua** (biểu đồ 5 sao), sản phẩm liên quan |
| Yêu thích `/cua-hang/yeu-thich` | Lưu sản phẩm theo phiên, trái tim đồng bộ mọi trang |
| Giỏ hàng `/gio-hang` | AJAX hoàn toàn: tăng/giảm/xoá **không tải lại trang**, kiểm tra tồn kho, tổng tiền + tiết kiệm + thanh **tiến độ freeship** (≥ 500.000₫), nhập **hoặc bấm chọn voucher** khả dụng |
| Thanh toán `/thanh-toan` | Sổ địa chỉ (chọn / thêm mới / lưu / đặt mặc định), COD hoặc **chuyển khoản VietQR** (QR sinh đúng số tiền + nội dung = mã đơn), ghi chú |
| Sau đặt hàng | Trang thành công có **trạng thái realtime**; đơn ghi `LoaiBan = Online`, `TrangThai = "Chờ xác nhận"` |
| Theo dõi đơn `/theo-doi-don-hang` | Tra cứu công khai bằng **mã đơn + SĐT** — timeline 4 bước **tự nhảy** khi quầy xử lý |
| Tài khoản `/ca-nhan/**` | Hồ sơ, đổi mật khẩu, sổ địa chỉ, **đơn hàng của tôi** (tab theo trạng thái + chi tiết realtime + **huỷ đơn** khi "Chờ xác nhận" + **mua lại**), voucher của tôi |
| Khác | `/gioi-thieu`, `/lien-he`, `/danh-gia` (tổng hợp đánh giá), đăng nhập / đăng ký giao diện mới |

Quy tắc nghiệp vụ: freeship đơn ≥ 500.000₫ (ngược lại 30.000₫); tối đa 10 sản phẩm/biến thể;
đặt hàng dùng **khoá bi quan (PESSIMISTIC_WRITE)** chống bán vượt tồn; huỷ đơn hoàn kho + hoàn voucher.

## 3. Kết nối thời gian thực 2 chiều

**Cấu hình:** `config/WebSocketConfig` — endpoint `/ws` (SockJS), broker `/topic`.

| Topic | Ai nghe | Nội dung |
|---|---|---|
| `/topic/quanly/don-hang` | Trang quản lý | `DON_HANG_MOI` (mã, khách, SĐT, tổng tiền, số SP, PTTT...) và `DOI_TRANG_THAI` |
| `/topic/quanly/ton-kho` | Trang quản lý | `HET_HANG` / `SAP_HET_HANG` sau mỗi lần đặt hàng |
| `/topic/quanly/module` | Trang quản lý | `TRANG_THAI_MODULE` — sức khoẻ từng module, quét mỗi **30 giây** + phát ngay khi có biến động |
| `/topic/don-hang/{maHoaDon}` | Trang khách | `DOI_TRANG_THAI` — timeline khách tự nhảy khi quầy thao tác |

**REST hỗ trợ:** `GET /api/quan-ly/trang-thai-module`, `GET /api/quan-ly/thong-ke-don-hang`.

**Phía quản lý:** widget chuông (fragment `fragments/quanly-realtime.html`) được nhúng vào cả 3 sidebar
(`sidebaradmin`, `sidebarstaff`, `sidebar`) → **mọi màn quản lý** đều có: toast + **âm báo** khi có đơn
online mới, badge chưa đọc, chấm màu sức khoẻ hệ thống, panel lịch sử sự kiện. Widget phát
`CustomEvent` (`fs-don-hang`, `fs-ton-kho`, `fs-module`) để từng trang tự cập nhật:
- `/donhang/index`: 4 bộ đếm trạng thái tự cập nhật + **dòng đơn mới chèn thẳng vào bảng** (nổi bật ⚡Mới)
- Dashboard admin: panel **"Trạng thái hoạt động của các module"** + bộ đếm "đơn hàng mới hôm nay" tự tăng

Các module được giám sát: Đơn hàng, Tồn kho, Khuyến mãi/Voucher, Khách hàng, Đánh giá, Cơ sở dữ liệu
(3 mức: `HOAT_DONG` / `CANH_BAO` / `SU_CO`).

## 4. File mới / sửa / xoá (tóm tắt)

**Java mới:** `config/WebSocketConfig`; gói `realtime/` (ThongBaoRealtimeService, TrangThaiModuleService,
QuanLyRealtimeApiController); gói `Storefront/` (GioHang, GioHangItem, YeuThich, các VM, KhuyenMaiService,
SanPhamHienThiService, GioHangService, DonHangOnlineService, DatHangException, 2 DTO);
Controller mới: `CuaHangController`, `GioHangController`, `ThanhToanController`, `TaiKhoanCaNhanController`,
`TrangTinhController`; `UserController` viết lại (dashboard admin/staff giữ nguyên model);
`HoaDonService` viết lại (bắn realtime khi đổi trạng thái); bổ sung query cho các Repository.

**Giao diện mới:** `fragments/client-layout.html`, `trangchu/`, `cuahang/` (3), `giohang/`, `thanhtoan/` (2),
`taikhoan/` (6), `trangtinh/` (5), `account/user/login+register`, `fragments/quanly-realtime.html`;
static: `storefront/css/storefront.css`, `storefront/js/storefront.js`, `storefront/js/chi-tiet.js`,
`storefront/img/no-image.svg`.

**Sửa admin (tối thiểu):** 3 sidebar (nhúng widget), `donhang/index.html` (id bộ đếm + script realtime),
`account/admin/home.html` (panel module + script). **Xoá:** toàn bộ storefront cũ.

**Lỗi cũ đã sửa kèm:** entity `DiaChi` thiếu FK `MaKhachHang`; thiếu bảng `KHACHHANG_VOUCHER` trong SQL;
staff dashboard so sánh ngày sai kiểu (`ngayTao.toLocalDate()`); vòng đời đơn trong `HoaDonService`
(set `NgayThanhToan` khi giao xong, phát sự kiện realtime, actor rõ ràng khi huỷ).

## 5. Cách chạy

1. **SQL Server:** chạy `database_FSShoes.sql` (đã kèm bảng mới ở cuối). Nếu DB đã tạo từ trước,
   chỉ cần chạy `khachhang_voucher_migration.sql`.
2. Kiểm tra `src/main/resources/application.properties` (chuỗi kết nối, `vietqr.*`).
3. Build & chạy: `mvn clean package` → deploy WAR, hoặc `mvn spring-boot:run`.
4. Tài khoản mẫu: `admin/Admin@123`, `nv001|nv002/Staff@123`, `kh001..kh003/User@123`.
5. **Thử realtime:** mở 2 cửa sổ — (A) đăng nhập `kh001`, đặt 1 đơn; (B) đăng nhập `admin` mở
   `/donhang/index` → nghe *"ting"*, thấy toast + dòng đơn mới; đổi trạng thái ở (B) → timeline ở (A) tự nhảy.

> **Lưu ý:** môi trường đóng gói này không truy cập được Maven Central nên **chưa biên dịch được**;
> toàn bộ mã đã được rà soát thủ công (cân bằng ngoặc, đối chiếu tên method/cột thực tế).
> Nếu gặp lỗi biên dịch lẻ tẻ, thường chỉ là khác tên getter — sửa theo thông báo của IDE.
