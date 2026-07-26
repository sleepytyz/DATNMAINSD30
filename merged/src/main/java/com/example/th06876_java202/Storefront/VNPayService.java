package com.example.th06876_java202.Storefront;

import com.example.th06876_java202.Entity.HoaDon;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * CỔNG THANH TOÁN VNPAY — MÔI TRƯỜNG SANDBOX (thử nghiệm, không dùng tiền thật).
 *
 * Luồng chuẩn cổng thanh toán:
 *   1. Website tạo URL thanh toán CÓ CHỮ KÝ (HMAC-SHA512) → chuyển hướng khách sang VNPay.
 *   2. Khách thanh toán bằng thẻ test → VNPay chuyển khách về vnp_ReturnUrl kèm kết quả + chữ ký.
 *   3. (Song song) VNPay gọi webhook IPN của website để xác nhận server-to-server.
 *   4. Website KIỂM TRA CHỮ KÝ + số tiền rồi mới xác nhận đơn — không tin dữ liệu trần.
 *
 * Đăng ký tài khoản sandbox miễn phí tại https://sandbox.vnpayment.vn/devreg
 * rồi điền vnpay.tmn-code và vnpay.hash-secret trong application.properties.
 * Thẻ test: Ngân hàng NCB — 9704198526191432198 — NGUYEN VAN A — 07/15 — OTP 123456.
 */
@Service
public class VNPayService {

    @Value("${vnpay.tmn-code:}")
    private String tmnCode;

    @Value("${vnpay.hash-secret:}")
    private String hashSecret;

    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String payUrl;

    /** Đã điền TmnCode + HashSecret thật chưa (chưa thì ẩn lựa chọn VNPay ở trang thanh toán). */
    public boolean daCauHinh() {
        return tmnCode != null && !tmnCode.isBlank() && !tmnCode.startsWith("DIEN_")
                && hashSecret != null && !hashSecret.isBlank() && !hashSecret.startsWith("DIEN_");
    }

    /** Tạo URL chuyển hướng sang VNPay cho một đơn hàng đang "Chờ thanh toán". */
    public String taoUrlThanhToan(HoaDon hoaDon, HttpServletRequest request) {
        long soTien = (hoaDon.getTongTien() != null ? hoaDon.getTongTien().longValue() : 0L) * 100L;

        Calendar lich = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT-7"));
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
        fmt.setTimeZone(TimeZone.getTimeZone("Etc/GMT-7"));
        String taoLuc = fmt.format(lich.getTime());
        // Mỗi lượt bấm thanh toán = 1 mã giao dịch riêng (VNPay không cho trùng TxnRef trong ngày)
        String txnRef = hoaDon.getMaHoaDon() + "-" + new SimpleDateFormat("HHmmssSSS").format(lich.getTime());
        lich.add(Calendar.MINUTE, 15);
        String hetHan = fmt.format(lich.getTime());

        Map<String, String> p = new TreeMap<>();
        p.put("vnp_Version", "2.1.0");
        p.put("vnp_Command", "pay");
        p.put("vnp_TmnCode", tmnCode);
        p.put("vnp_Amount", String.valueOf(soTien));
        p.put("vnp_CurrCode", "VND");
        p.put("vnp_TxnRef", txnRef);
        p.put("vnp_OrderInfo", "Thanh toan don hang " + hoaDon.getMaHoaDon());
        p.put("vnp_OrderType", "other");
        p.put("vnp_Locale", "vn");
        p.put("vnp_ReturnUrl", baseUrl(request) + "/thanh-toan/vnpay-return");
        p.put("vnp_IpAddr", layIp(request));
        p.put("vnp_CreateDate", taoLuc);
        p.put("vnp_ExpireDate", hetHan);

        String duLieu = ghepQuery(p);
        return payUrl + "?" + duLieu + "&vnp_SecureHash=" + hmacSHA512(hashSecret, duLieu);
    }

    /** Kiểm tra chữ ký của dữ liệu VNPay gửi về (return / IPN). */
    public boolean chuKyHopLe(Map<String, String> params) {
        String chuKy = params.get("vnp_SecureHash");
        if (chuKy == null || chuKy.isBlank()) return false;
        Map<String, String> sao = new TreeMap<>(params);
        sao.remove("vnp_SecureHash");
        sao.remove("vnp_SecureHashType");
        String tinhLai = hmacSHA512(hashSecret, ghepQuery(sao));
        return tinhLai.equalsIgnoreCase(chuKy);
    }

    /** Mã đơn hàng từ vnp_TxnRef (định dạng MAHOADON-hhmmssSSS). */
    public String maDonTuTxnRef(String txnRef) {
        if (txnRef == null || txnRef.isBlank()) return null;
        int i = txnRef.indexOf('-');
        return i > 0 ? txnRef.substring(0, i) : txnRef;
    }

    /* ================= tiện ích ================= */

    /** Ghép query đã URL-encode, khoá xếp alphabet — đúng chuẩn ký của VNPay 2.1.0. */
    private String ghepQuery(Map<String, String> p) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : p.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) continue;
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.US_ASCII))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII));
        }
        return sb.toString();
    }

    private String hmacSHA512(String khoa, String duLieu) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(khoa.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(duLieu.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được chữ ký VNPay", e);
        }
    }

    /** Địa chỉ gốc của website (hỗ trợ chạy sau ngrok/proxy nhờ X-Forwarded-*). */
    private String baseUrl(HttpServletRequest req) {
        String scheme = req.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isBlank()) scheme = req.getScheme();
        String host = req.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) host = req.getHeader("Host");
        if (host == null || host.isBlank()) {
            host = req.getServerName() + (req.getServerPort() > 0 ? ":" + req.getServerPort() : "");
        }
        return scheme + "://" + host;
    }

    private String layIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        String diaChi = req.getRemoteAddr();
        return diaChi != null ? diaChi : "127.0.0.1";
    }
}