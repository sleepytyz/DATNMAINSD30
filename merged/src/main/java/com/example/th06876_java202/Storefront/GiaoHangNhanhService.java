package com.example.th06876_java202.Storefront;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TÍCH HỢP API GIAO HÀNG NHANH (GHN) — TÍNH CƯỚC VẬN CHUYỂN THẬT.
 *
 * Thay cho phí ship cố định, hệ thống gọi API GHN để lấy cước theo ĐÚNG tuyến
 * (kho gửi -> quận/huyện + phường/xã người nhận), khối lượng và giá trị đơn.
 *
 * Các endpoint sử dụng (tài liệu chính thức GHN):
 *   GET  /master-data/province                  -> danh sách tỉnh/thành
 *   GET  /master-data/district?province_id=     -> quận/huyện của tỉnh
 *   GET  /master-data/ward?district_id=         -> phường/xã của quận/huyện
 *   POST /v2/shipping-order/available-services  -> các gói dịch vụ khả dụng của tuyến
 *   POST /v2/shipping-order/fee                 -> CƯỚC vận chuyển của tuyến
 *
 * Xác thực bằng header "Token" (và "ShopId" cho nhóm shipping-order).
 * Đăng ký tài khoản + lấy Token/ShopId tại trang quản trị GHN, rồi điền
 * ghn.token / ghn.shop-id trong application.properties.
 *
 * AN TOÀN KHI DEMO: mọi lỗi (chưa cấu hình, mất mạng, API đổi, quá thời gian chờ)
 * đều được nuốt và trả về null/rỗng -> tầng gọi TỰ ĐỘNG quay về biểu phí cố định,
 * website không bao giờ vỡ vì phụ thuộc dịch vụ ngoài.
 */
@Service
public class GiaoHangNhanhService {

    /** Kích thước quy ước của MỘT hộp giày (cm) — cấu hình được vì ảnh hưởng trực tiếp
     *  tới cước: GHN tính theo khối lượng quy đổi = Dài×Rộng×Cao/5000 (kg) nếu lớn hơn
     *  khối lượng thật. Hộp càng to khai càng lớn thì cước càng cao. */

    /** GHN chỉ nhận khai giá tối đa 5 triệu cho đơn thường. */
    private static final long GIA_TRI_KHAI_TOI_DA = 5_000_000L;

    @Value("${ghn.api.token:${ghn.token:}}")
    private String token;

    @Value("${ghn.shop.id:${ghn.shop-id:}}")
    private String shopId;

    @Value("${ghn.api.url:${ghn.base-url:https://online-gateway.ghn.vn/shiip/public-api}}")
    private String baseUrl;

    /** Mã quận/huyện KHO GỬI của cửa hàng (lấy trong trang quản trị GHN). */
    @Value("${ghn.shop.district.id:${ghn.from-district-id:1454}}")
    private int quanHuyenGui;

    /** 2 = hàng nhẹ (chuẩn cho giày dép), 5 = hàng nặng. */
    @Value("${ghn.service-type-id:2}")
    private int loaiDichVu;

    /** Khối lượng quy ước mỗi đôi giày (gram). */
    @Value("${ghn.khoi-luong-moi-san-pham:800}")
    private int gamMoiSanPham;

    @Value("${ghn.dai-cm:28}")
    private int daiCm;

    @Value("${ghn.rong-cm:18}")
    private int rongCm;

    /** Chiều cao MỖI hộp (nhiều đôi thì xếp chồng lên nhau). */
    @Value("${ghn.cao-moi-hop-cm:11}")
    private int caoMoiHopCm;

    /** Có KHAI GIÁ hàng với GHN không? Khai giá được bồi thường khi mất hàng nhưng
     *  bị tính thêm phí bảo hiểm (~0,5-1% giá trị) -> cước cao hơn báo giá thường. */
    @Value("${ghn.khai-gia:false}")
    private boolean khaiGia;

    /** Chi tiết cước lần tính gần nhất (phí dịch vụ / bảo hiểm / tổng) — để đối chiếu. */
    private volatile Map<String, Object> chiTietCuoc = new LinkedHashMap<>();

    public Map<String, Object> getChiTietCuoc() {
        return chiTietCuoc;
    }

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    /** Cache dữ liệu địa giới (hầu như không đổi) để không gọi lại API mỗi lần mở trang. */
    private final Map<String, List<Map<String, Object>>> boNho = new ConcurrentHashMap<>();

    /** Lý do thất bại gần nhất khi gọi GHN — phục vụ chẩn đoán (/api/giao-hang/kiem-tra). */
    private volatile String loiCuoi = "";

    public String getLoiCuoi() {
        return loiCuoi;
    }

    /**
     * Gốc API. Cấu hình cũ của dự án trỏ thẳng tới endpoint tính cước
     * (.../shiip/public-api/v2/shipping-order/fee) nên cắt về phần gốc dùng chung.
     */
    private String goc() {
        String u = baseUrl == null ? "" : baseUrl.trim();
        int i = u.indexOf("/shiip/public-api");
        if (i >= 0) return u.substring(0, i + "/shiip/public-api".length());
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u.isEmpty() ? "https://online-gateway.ghn.vn/shiip/public-api" : u;
    }

    /* ==================== TRẠNG THÁI ==================== */

    /** Đã điền Token + ShopId thật chưa? Chưa thì hệ thống dùng biểu phí cố định. */
    public boolean daCauHinh() {
        return token != null && !token.isBlank() && !token.startsWith("DIEN_")
                && shopId != null && !shopId.isBlank() && !shopId.startsWith("DIEN_");
    }

    public int getGamMoiSanPham() {
        return gamMoiSanPham;
    }

    /* ==================== DỮ LIỆU ĐỊA GIỚI ==================== */

    /** Danh sách tỉnh/thành: [{id, ten}]. Rỗng nếu chưa cấu hình hoặc lỗi mạng. */
    public List<Map<String, Object>> layTinhThanh() {
        return boNho.computeIfAbsent("tinh", k -> {
            JsonNode data = goiGET("/master-data/province");
            return docDanhSach(data, "ProvinceID", "ProvinceName");
        });
    }

    /** Quận/huyện thuộc một tỉnh: [{id, ten}]. */
    public List<Map<String, Object>> layQuanHuyen(int maTinh) {
        return boNho.computeIfAbsent("huyen-" + maTinh, k -> {
            JsonNode data = goiGET("/master-data/district?province_id=" + maTinh);
            return docDanhSach(data, "DistrictID", "DistrictName");
        });
    }

    /** Phường/xã thuộc một quận/huyện: [{id, ten}] — id ở đây là WardCode dạng chuỗi. */
    public List<Map<String, Object>> layPhuongXa(int maQuanHuyen) {
        return boNho.computeIfAbsent("xa-" + maQuanHuyen, k -> {
            JsonNode data = goiGET("/master-data/ward?district_id=" + maQuanHuyen);
            return docDanhSach(data, "WardCode", "WardName");
        });
    }

    /**
     * DÒ MÃ ĐỊA GIỚI TỪ TÊN — dùng cho các địa chỉ đã lưu TRƯỚC khi tích hợp GHN
     * (chỉ có chữ "Hà Nội / Cầu Giấy / Dịch Vọng"). So khớp không dấu, bỏ tiền tố
     * Tỉnh/Thành phố/Quận/Huyện/Phường/Xã. Trả về {districtId, wardCode} nếu tìm thấy.
     */
    public Map<String, Object> doDiaChi(String tinhThanh, String quanHuyen, String phuongXa) {
        Map<String, Object> kq = new LinkedHashMap<>();
        if (!daCauHinh() || tinhThanh == null || quanHuyen == null) return kq;

        Object maTinh = timTheoTen(layTinhThanh(), tinhThanh);
        if (maTinh == null) return kq;
        Object maHuyen = timTheoTen(layQuanHuyen(Integer.parseInt(String.valueOf(maTinh))), quanHuyen);
        if (maHuyen == null) return kq;
        kq.put("districtId", Integer.parseInt(String.valueOf(maHuyen)));

        if (phuongXa != null && !phuongXa.isBlank()) {
            Object maXa = timTheoTen(layPhuongXa(Integer.parseInt(String.valueOf(maHuyen))), phuongXa);
            if (maXa != null) kq.put("wardCode", String.valueOf(maXa));
        }
        return kq;
    }

    /** So khớp tên (không dấu, bỏ tiền tố hành chính): ưu tiên khớp đúng, sau đó khớp chứa. */
    private Object timTheoTen(List<Map<String, Object>> ds, String ten) {
        String can = chuanHoaTen(ten);
        if (can.isEmpty() || ds == null || ds.isEmpty()) return null;
        for (Map<String, Object> m : ds) {
            if (chuanHoaTen(String.valueOf(m.get("ten"))).equals(can)) return m.get("id");
        }
        for (Map<String, Object> m : ds) {
            String t = chuanHoaTen(String.valueOf(m.get("ten")));
            if (!t.isEmpty() && (t.contains(can) || can.contains(t))) return m.get("id");
        }
        return null;
    }

    private static String chuanHoaTen(String s) {
        if (s == null) return "";
        String t = java.text.Normalizer.normalize(s.trim().toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('\u0111', 'd');
        t = t.replaceAll("[^a-z0-9 ]", " ");
        t = t.replaceAll("\\b(tinh|thanh pho|tp|quan|huyen|thi xa|thi tran|phuong|xa)\\b", " ");
        return t.replaceAll("\\s+", " ").trim();
    }

    /* ==================== TÍNH CƯỚC ==================== */

    /**
     * Cước vận chuyển GHN cho tuyến kho gửi -> (quận/huyện, phường/xã) người nhận.
     *
     * @param maQuanHuyenNhan mã quận/huyện GHN của người nhận
     * @param maPhuongXaNhan  mã phường/xã (WardCode) của người nhận
     * @param khoiLuongGam    tổng khối lượng đơn (gram)
     * @param giaTriHang      giá trị hàng để khai giá (VNĐ)
     * @return số tiền cước, hoặc null nếu không tính được (tầng gọi tự dùng phí cố định)
     */
    public Integer tinhPhi(Integer maQuanHuyenNhan, String maPhuongXaNhan, int soLuongSanPham, long giaTriHang) {
        if (!daCauHinh() || maQuanHuyenNhan == null || maPhuongXaNhan == null || maPhuongXaNhan.isBlank()) {
            return null;
        }
        int soDoi = Math.max(1, soLuongSanPham);
        int khoiLuong = Math.max(200, soDoi * gamMoiSanPham);
        int cao = Math.max(1, caoMoiHopCm * soDoi);                  // xếp chồng các hộp
        long tienKhai = khaiGia ? Math.min(Math.max(giaTriHang, 0L), GIA_TRI_KHAI_TOI_DA) : 0L;

        // Lần 1: dùng loại dịch vụ mặc định (hàng nhẹ)
        Integer phi = goiTinhPhi(maQuanHuyenNhan, maPhuongXaNhan, khoiLuong, cao, tienKhai, loaiDichVu, null);
        if (phi != null) return phi;

        // Lần 2: tuyến này không hỗ trợ loại dịch vụ mặc định -> hỏi gói khả dụng rồi tính lại
        Integer maDichVu = layDichVuKhaDung(maQuanHuyenNhan);
        if (maDichVu != null) {
            return goiTinhPhi(maQuanHuyenNhan, maPhuongXaNhan, khoiLuong, cao, tienKhai, null, maDichVu);
        }
        return null;
    }

    /** Khối lượng đơn = số sản phẩm × khối lượng quy ước mỗi đôi. */
    public int khoiLuongCho(int tongSoSanPham) {
        return Math.max(1, tongSoSanPham) * gamMoiSanPham;
    }

    /**
     * DANH SÁCH CỬA HÀNG gắn với Token hiện tại — mỗi cửa hàng kèm ShopId và
     * district_id của ĐỊA CHỈ LẤY HÀNG. Đây chính là số cần điền vào
     * ghn.shop-id và ghn.from-district-id.
     */
    public List<Map<String, Object>> danhSachCuaHang() {
        List<Map<String, Object>> ds = new ArrayList<>();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("offset", 0);
        body.put("limit", 50);
        body.put("client_phone", "");

        JsonNode data = goiPOST("/v2/shop/all", body);
        if (data == null) return ds;
        JsonNode shops = data.isArray() ? data : data.get("shops");
        if (shops == null || !shops.isArray()) return ds;

        for (JsonNode sh : shops) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("shopId_DIEN_VAO_ghn.shop-id", sh.hasNonNull("_id") ? sh.get("_id").asInt() : null);
            m.put("tenCuaHang", sh.hasNonNull("name") ? sh.get("name").asText() : "");
            m.put("diaChiLayHang", sh.hasNonNull("address") ? sh.get("address").asText() : "");
            m.put("districtId_DIEN_VAO_ghn.from-district-id",
                    sh.hasNonNull("district_id") ? sh.get("district_id").asInt() : null);
            m.put("wardCode", sh.hasNonNull("ward_code") ? sh.get("ward_code").asText() : "");
            ds.add(m);
        }
        return ds;
    }

    /**
     * CHẨN ĐOÁN: thử tính cước cho một tuyến và trả về mọi thông tin cần thiết để
     * biết vì sao cước không ra (sai token / sai mã kho gửi / tuyến không hỗ trợ...).
     */
    public Map<String, Object> kiemTra(Integer maQuanHuyenNhan, String maPhuongXaNhan) {
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("daCauHinh", daCauHinh());
        kq.put("baseUrl", goc());
        kq.put("shopId", shopId);
        kq.put("maKhoGui_fromDistrictId", quanHuyenGui);
        kq.put("loaiDichVu_serviceTypeId", loaiDichVu);
        kq.put("tokenDangDung", moTaToken());
        kq.put("kiemTraMoiTruongToken", thuHaiMoiTruong());
        loiCuoi = "";

        // Cửa hàng thật sự gắn với Token -> đối chiếu ngay với cấu hình đang khai báo
        List<Map<String, Object>> cuaHang = danhSachCuaHang();
        kq.put("cuaHangCuaBan", cuaHang);
        if (cuaHang.isEmpty()) {
            kq.put("canhBao", "Không đọc được cửa hàng nào từ Token này — kiểm tra lại Token "
                    + "hoặc bạn đang dùng Token của môi trường khác với ghn.base-url.");
        } else {
            boolean khop = cuaHang.stream().anyMatch(c ->
                    Integer.valueOf(quanHuyenGui).equals(c.get("districtId_DIEN_VAO_ghn.from-district-id")));
            kq.put("khoGuiDangKhaiBaoCoDung", khop ? "ĐÚNG" :
                    "SAI — hãy đổi ghn.from-district-id thành districtId của cửa hàng ở trên");
        }

        List<Map<String, Object>> tinh = layTinhThanh();
        kq.put("soTinhThanhTaiVe", tinh.size());
        kq.put("ketNoiMasterData", !tinh.isEmpty() ? "OK" : "THẤT BẠI");

        if (maQuanHuyenNhan != null && maPhuongXaNhan != null && !maPhuongXaNhan.isBlank()) {
            kq.put("tuyenThu", "kho " + quanHuyenGui + " -> huyện " + maQuanHuyenNhan + " / xã " + maPhuongXaNhan);
            Integer phi = tinhPhi(maQuanHuyenNhan, maPhuongXaNhan, 1, 300_000L);
            kq.put("cuocThu", phi);
            kq.put("ketQua", phi != null ? "OK — tính được cước" : "THẤT BẠI — xem lyDo");
            Integer dv = layDichVuKhaDung(maQuanHuyenNhan);
            kq.put("goiDichVuKhaDung_serviceId", dv);
            kq.put("chiTietCuoc", chiTietCuoc);
        } else {
            kq.put("huongDan", "Gọi kèm tham số: /api/giao-hang/kiem-tra?huyen=1442&xa=21012");
        }
        kq.put("lyDo", loiCuoi == null || loiCuoi.isBlank() ? "(không có lỗi)" : loiCuoi);
        return kq;
    }

    /** Mô tả token đang dùng (che bớt) — để phát hiện dán thiếu/thừa ký tự. */
    private String moTaToken() {
        if (token == null || token.isBlank()) return "(trống)";
        String t = token.trim();
        String rutGon = t.length() <= 14 ? t
                : t.substring(0, 8) + "..." + t.substring(t.length() - 6);
        return rutGon + "  (" + t.length() + " ký tự"
                + (t.length() != token.length() ? ", CẢNH BÁO: có khoảng trắng thừa trong file cấu hình" : "")
                + ")";
    }

    /** Thử chính Token này trên CẢ HAI cổng để biết token thuộc môi trường nào. */
    private Map<String, Object> thuHaiMoiTruong() {
        String dev = "https://dev-online-gateway.ghn.vn/shiip/public-api";
        String that = "https://online-gateway.ghn.vn/shiip/public-api";
        Map<String, Object> m = new LinkedHashMap<>();
        String kqDev = thuMotCong(dev);
        String kqThat = thuMotCong(that);
        m.put("congTHU_NGHIEM_dev", kqDev);
        m.put("congTHAT_production", kqThat);

        boolean okDev = kqDev.startsWith("OK");
        boolean okThat = kqThat.startsWith("OK");
        if (okDev && goc().contains("dev-online-gateway")) {
            m.put("ketLuan", "Token hợp lệ và base-url đang đúng môi trường.");
        } else if (okThat && !goc().contains("dev-online-gateway")) {
            m.put("ketLuan", "Token hợp lệ và base-url đang đúng môi trường.");
        } else if (okThat) {
            m.put("ketLuan", "Token này thuộc MÔI TRƯỜNG THẬT. Hãy sửa trong application.properties: "
                    + "ghn.api.url=" + that + "/v2/shipping-order/fee (và ShopID của tài khoản thật).");
        } else if (okDev) {
            m.put("ketLuan", "Token này thuộc MÔI TRƯỜNG THỬ NGHIỆM. Hãy sửa: ghn.api.url=" + dev + "/v2/shipping-order/fee");
        } else {
            m.put("ketLuan", "Cả hai cổng đều từ chối Token -> Token sai/hết hạn hoặc bị dán thiếu ký tự. "
                    + "Vào trang quản trị GHN lấy lại API Token rồi dán nguyên vẹn (không kèm dấu cách, "
                    + "không kèm chú thích ở cuối dòng trong application.properties).");
        }
        return m;
    }

    private String thuMotCong(String goc) {
        if (token == null || token.isBlank()) return "Chưa có token";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(goc + "/master-data/province"))
                    .timeout(Duration.ofSeconds(6))
                    .header("Token", token.trim())
                    .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() == 200) return "OK — Token hợp lệ ở cổng này";
            String than = res.body() != null && res.body().length() > 160
                    ? res.body().substring(0, 160) : String.valueOf(res.body());
            return "HTTP " + res.statusCode() + " — " + than;
        } catch (Exception ex) {
            return "Không kết nối được: " + ex.getMessage();
        }
    }

    /* ==================== GỌI API ==================== */

    private Integer goiTinhPhi(int huyenNhan, String xaNhan, int gam, int cao, long tienKhai,
                               Integer serviceTypeId, Integer serviceId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from_district_id", quanHuyenGui);
        body.put("to_district_id", huyenNhan);
        body.put("to_ward_code", xaNhan);
        body.put("weight", gam);
        body.put("length", daiCm);
        body.put("width", rongCm);
        body.put("height", cao);
        body.put("insurance_value", tienKhai);
        if (serviceId != null) body.put("service_id", serviceId);
        if (serviceTypeId != null) body.put("service_type_id", serviceTypeId);

        JsonNode data = goiPOST("/v2/shipping-order/fee", body);
        if (data == null || !data.hasNonNull("total")) return null;

        Map<String, Object> ct = new LinkedHashMap<>();
        ct.put("khoiLuongKhai_gram", gam);
        ct.put("kichThuocKhai_cm", daiCm + "x" + rongCm + "x" + cao);
        ct.put("khoiLuongQuyDoi_gram", daiCm * rongCm * cao / 5);   // D*R*C/5000 kg -> gram
        ct.put("giaTriKhaiGia", tienKhai);
        for (String k : new String[]{"service_fee", "insurance_fee", "total"}) {
            if (data.hasNonNull(k)) ct.put(k, data.get(k).asLong());
        }
        chiTietCuoc = ct;

        int tong = data.get("total").asInt(0);
        return tong > 0 ? tong : null;
    }

    private Integer layDichVuKhaDung(int huyenNhan) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            body.put("shop_id", Integer.parseInt(shopId.trim()));
        } catch (NumberFormatException ex) {
            return null;
        }
        body.put("from_district", quanHuyenGui);
        body.put("to_district", huyenNhan);

        JsonNode data = goiPOST("/v2/shipping-order/available-services", body);
        if (data == null || !data.isArray() || data.isEmpty()) return null;
        JsonNode dv = data.get(0);
        return dv.hasNonNull("service_id") ? dv.get("service_id").asInt() : null;
    }

    /** GET -> trả về nhánh "data" của phản hồi, null nếu hỏng. */
    private JsonNode goiGET(String duongDan) {
        if (!daCauHinh()) return null;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(goc() + duongDan))
                    .timeout(Duration.ofSeconds(6))
                    .header("Token", token.trim())
                    .header("ShopId", shopId.trim())
                    .GET().build();
            return docData(http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            loiCuoi = "Không gọi được GHN (" + duongDan + "): " + ex.getMessage();
            System.err.println("[GHN] " + loiCuoi);
            return null;   // mất mạng / quá thời gian chờ -> tầng gọi tự xử lý
        }
    }

    /** POST JSON -> trả về nhánh "data" của phản hồi, null nếu hỏng. */
    private JsonNode goiPOST(String duongDan, Map<String, Object> body) {
        if (!daCauHinh()) return null;
        try {
            String chuoi = json.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder(URI.create(goc() + duongDan))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json")
                    .header("Token", token.trim())
                    .header("ShopId", shopId.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(chuoi, StandardCharsets.UTF_8))
                    .build();
            return docData(http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            loiCuoi = "Không gọi được GHN (" + duongDan + "): " + ex.getMessage();
            System.err.println("[GHN] " + loiCuoi);
            return null;
        }
    }

    private JsonNode docData(HttpResponse<String> res) throws Exception {
        if (res == null || res.body() == null || res.body().isBlank()) return null;
        JsonNode goc = json.readTree(res.body());
        // GHN trả {code:200, message:"Success", data:{...}} — code khác 200 coi như thất bại
        if (res.statusCode() != 200 || (goc.hasNonNull("code") && goc.get("code").asInt() != 200)) {
            String tb = goc.hasNonNull("message") ? goc.get("message").asText()
                    : (goc.hasNonNull("message_display") ? goc.get("message_display").asText() : res.body());
            loiCuoi = "GHN trả về HTTP " + res.statusCode()
                    + (goc.hasNonNull("code") ? " / code " + goc.get("code").asInt() : "") + ": " + tb;
            System.err.println("[GHN] " + loiCuoi);
            return null;
        }
        loiCuoi = "";
        return goc.get("data");
    }

    private List<Map<String, Object>> docDanhSach(JsonNode data, String khoaId, String khoaTen) {
        List<Map<String, Object>> ds = new ArrayList<>();
        if (data == null || !data.isArray()) return ds;
        for (JsonNode n : data) {
            if (!n.hasNonNull(khoaId) || !n.hasNonNull(khoaTen)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            JsonNode id = n.get(khoaId);
            m.put("id", id.isNumber() ? id.asInt() : id.asText());
            m.put("ten", n.get(khoaTen).asText());
            ds.add(m);
        }
        ds.sort((a, b) -> String.valueOf(a.get("ten")).compareToIgnoreCase(String.valueOf(b.get("ten"))));
        return ds;
    }
}