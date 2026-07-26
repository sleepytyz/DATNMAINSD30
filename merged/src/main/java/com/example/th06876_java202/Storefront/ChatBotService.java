package com.example.th06876_java202.Storefront;

import com.example.th06876_java202.Entity.GiamGia;
import com.example.th06876_java202.Entity.HoaDon;
import com.example.th06876_java202.Entity.KhachHang;
import com.example.th06876_java202.Entity.SanPham;
import com.example.th06876_java202.Entity.SanPhamChiTiet;
import com.example.th06876_java202.Repository.SanPhamChiTietRepository;
import com.example.th06876_java202.Repository.SanPhamRepository;
import com.example.th06876_java202.Service.GiamGiaService;
import com.example.th06876_java202.Service.HoaDonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CHATBOT bán hàng — trả lời tự động các câu hỏi thường gặp của khách,
 * tra cứu trực tiếp dữ liệu THẬT trong hệ thống (sản phẩm, giá sau khuyến mãi,
 * tồn kho, size/màu, đợt giảm giá, voucher, trạng thái đơn hàng).
 * So khớp KHÔNG DẤU nên khách gõ "giay chay bo" vẫn hiểu là "giày chạy bộ".
 */
@Service
@RequiredArgsConstructor
public class ChatBotService {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final KhuyenMaiService khuyenMaiService;
    private final GiamGiaService giamGiaService;
    private final HoaDonService hoaDonService;

    private static final Pattern MA_DON = Pattern.compile("(HD[A-Za-z0-9]{4,})", Pattern.CASE_INSENSITIVE);

    /* ================= tiện ích chuẩn hoá ================= */

    /** bỏ dấu tiếng Việt + thường hoá: "Giày Chạy Bộ" -> "giay chay bo" */
    static String boDau(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return t.toLowerCase(Locale.ROOT).trim();
    }

    private static boolean chua(String cau, String... tu) {
        for (String t : tu) if (cau.contains(t)) return true;
        return false;
    }

    private static String tien(BigDecimal v) {
        if (v == null) return "?";
        return String.format("%,.0f", v).replace(',', '.') + "₫";
    }

    /** Khách muốn gặp nhân viên thật? */
    public boolean muonGapNhanVien(String noiDung) {
        String c = boDau(noiDung);
        return chua(c, "nhan vien", "gap nguoi", "nguoi that", "tu van vien",
                "gap ho tro", "noi chuyen voi nguoi", "chuyen nhan vien", "can ho tro truc tiep");
    }

    /* ================= trả lời ================= */

    public String traLoi(String noiDung, KhachHang kh) {
        String c = boDau(noiDung);
        if (c.isEmpty()) return goiY();

        // 1) chào hỏi
        if (chua(c, "xin chao", "chao ban", "chao shop", "hello", "hi ", "alo") || c.equals("hi") || c.equals("chao")) {
            String ten = kh != null && kh.getHoTen() != null ? " " + kh.getHoTen() : "";
            return "Xin chào" + ten + "! 👟 Mình là trợ lý ảo của FS Shoes.\n" + goiY();
        }

        // 2) tra cứu đơn hàng (có mã HDxxxx trong câu)
        Matcher m = MA_DON.matcher(noiDung);
        if (m.find()) return traCuuDon(m.group(1).toUpperCase(Locale.ROOT), kh);
        if (chua(c, "don hang", "kiem tra don", "trang thai don", "don cua toi", "theo doi don")) {
            if (kh != null) {
                return "Bạn có thể xem toàn bộ đơn (kèm trạng thái cập nhật theo thời gian thực) tại:\n"
                        + "👉 /ca-nhan/don-hang\nHoặc gửi mình mã đơn (dạng HDxxxx) để mình tra nhanh nhé!";
            }
            return "Bạn tra cứu đơn công khai bằng MÃ ĐƠN + SĐT đặt hàng tại:\n👉 /theo-doi-don-hang\n"
                    + "Hoặc gửi mình mã đơn (dạng HDxxxx) kèm đăng nhập để mình kiểm tra giúp.";
        }

        // 3) khuyến mãi / voucher
        if (chua(c, "khuyen mai", "giam gia", "sale", "uu dai")) return khuyenMai();
        if (chua(c, "voucher", "ma giam", "coupon")) return voucher(kh);

        // 4) phí ship / giao hàng
        if (chua(c, "ship", "van chuyen", "giao hang", "freeship", "phi giao")) {
            return "🚚 Chính sách vận chuyển của FS Shoes:\n"
                    + "• MIỄN PHÍ với đơn từ 500.000₫\n"
                    + "• Đơn dưới 500.000₫: phí 30.000₫ toàn quốc\n"
                    + "• Đặt xong bạn theo dõi đơn theo thời gian thực, không cần tải lại trang.";
        }

        // 5) thanh toán
        if (chua(c, "thanh toan", "cod", "chuyen khoan", "quet ma", "vietqr", "tra tien")) {
            return "💳 FS Shoes hỗ trợ 2 hình thức thanh toán:\n"
                    + "• COD — kiểm tra hàng rồi mới trả tiền\n"
                    + "• Chuyển khoản VietQR — quét mã là xong, số tiền và nội dung điền sẵn.";
        }

        // 6) đổi trả / bảo hành
        if (chua(c, "doi tra", "doi size", "doi hang", "tra hang", "bao hanh", "hoan tien")) {
            return "🔄 Chính sách đổi trả: đổi size/mẫu trong 7 NGÀY kể từ khi nhận hàng "
                    + "(giày chưa qua sử dụng, còn hộp). Đơn chưa xác nhận có thể HUỶ trực tiếp trong "
                    + "trang \"Đơn hàng của tôi\". Cần hỗ trợ thêm, bạn nhắn \"gặp nhân viên\" nhé!";
        }

        // 7) liên hệ / giờ mở cửa
        if (chua(c, "dia chi", "cua hang o dau", "hotline", "so dien thoai", "lien he", "gio mo", "may gio")) {
            return "🏪 FS Shoes — 13 Trịnh Văn Bô, Nam Từ Liêm, Hà Nội\n"
                    + "🕗 Mở cửa 8:00 – 21:30 (Thứ 2 – Chủ nhật)\n"
                    + "📞 Hotline/Zalo: 0344 552 008 • ✉️ hotro@fsshoes.vn";
        }

        // 8) size tư vấn chung
        if (chua(c, "chon size", "size nao", "bao nhieu size", "tu van size") && !coTenSanPham(c)) {
            return "📏 Mẹo chọn size giày: đo chiều dài bàn chân (cm) rồi cộng 0,5–1cm. "
                    + "Thông thường: 25cm ≈ size 40, 25.5cm ≈ 41, 26cm ≈ 42, 26.5cm ≈ 43. "
                    + "Bạn cho mình tên mẫu giày, mình sẽ liệt kê các size đang còn hàng nhé!";
        }

        // 9) hỏi về sản phẩm (giá / tồn / size / màu / tìm kiếm)
        List<SanPham> khop = timSanPham(c);
        if (!khop.isEmpty()) {
            if (chua(c, "size", "kich thuoc", "mau ", "màu", "con hang", "ton kho", "con khong", "het hang")) {
                return chiTietTonKho(khop.get(0));
            }
            return danhSachSanPham(khop, chua(c, "gia", "bao nhieu tien", "bao nhieu"));
        }
        if (chua(c, "giay", "san pham", "mua", "tim", "co ban", "goi y", "tu van", "hot", "ban chay", "moi ve")) {
            return banChay();
        }

        // 10) không hiểu
        return "Mình chưa hiểu rõ câu hỏi này 😅. " + goiY()
                + "\nHoặc nhắn \"gặp nhân viên\" — nhân viên FS Shoes sẽ trả lời bạn ngay tại khung chat này!";
    }

    /** Câu trả lời khi khách yêu cầu gặp nhân viên. */
    public String traLoiChuyenNhanVien() {
        return "✅ Mình đã chuyển cuộc trò chuyện đến NHÂN VIÊN HỖ TRỢ của FS Shoes. "
                + "Bạn cứ nhắn câu hỏi tại đây — nhân viên sẽ trả lời bạn ngay trong khung chat này "
                + "(tin nhắn hiện theo thời gian thực, không cần tải lại trang).";
    }

    /* ================= các khối trả lời ================= */

    private String goiY() {
        return "Mình có thể giúp bạn:\n"
                + "• Tìm giày / hỏi GIÁ: \"giá giày chạy bộ Nike?\"\n"
                + "• Kiểm tra SIZE, MÀU còn hàng: \"giày X còn size 42 không?\"\n"
                + "• KHUYẾN MÃI đang chạy, VOUCHER của bạn\n"
                + "• Tra cứu ĐƠN HÀNG: gửi mã đơn HDxxxx\n"
                + "• Phí SHIP, cách THANH TOÁN, ĐỔI TRẢ, liên hệ cửa hàng";
    }

    private boolean coTenSanPham(String c) {
        return !timSanPham(c).isEmpty();
    }

    /** Tìm sản phẩm đang bán có tên khớp nhiều từ nhất với câu hỏi. */
    private List<SanPham> timSanPham(String cauKhongDau) {
        Set<String> STOP = Set.of("gia", "bao", "nhieu", "tien", "cua", "cai", "doi", "chiec", "mua", "ban",
                "co", "khong", "con", "hang", "size", "mau", "la", "the", "nao", "cho", "minh", "toi",
                "shop", "xin", "hoi", "ve", "tim", "kiem", "xem", "va", "hay", "voi", "kich", "thuoc");
        List<String> tuKhoa = new ArrayList<>();
        for (String w : cauKhongDau.split("[^a-z0-9]+")) {
            if (w.length() >= 2 && !STOP.contains(w)) tuKhoa.add(w);
        }
        if (tuKhoa.isEmpty()) return List.of();

        List<SanPham> tatCa = sanPhamRepository.findAll();
        List<SanPham> khop = new ArrayList<>();
        int diemMax = 0;
        for (SanPham sp : tatCa) {
            if (sp.getTrangThai() != null && !sp.getTrangThai()) continue;
            String ten = boDau(sp.getTenSanPham());
            String thuongHieu = sp.getThuongHieu() != null ? boDau(sp.getThuongHieu().getTenThuongHieu()) : "";
            int diem = 0;
            for (String w : tuKhoa) {
                if (ten.contains(w)) diem += 2;
                else if (thuongHieu.contains(w)) diem += 1;
            }
            if (diem > diemMax) { diemMax = diem; khop.clear(); khop.add(sp); }
            else if (diem == diemMax && diem > 0 && khop.size() < 3) khop.add(sp);
        }
        return diemMax >= 2 ? khop : List.of();   // cần khớp tối thiểu 1 từ trong tên
    }

    private String danhSachSanPham(List<SanPham> ds, boolean hoiGia) {
        StringBuilder sb = new StringBuilder(hoiGia ? "💰 Giá hiện tại:\n" : "Mình tìm thấy:\n");
        for (SanPham sp : ds) {
            BigDecimal giaMin = giaThapNhat(sp);
            int pt = khuyenMaiService.phanTramGiamSanPham(sp.getMaSanPham());
            sb.append("• ").append(sp.getTenSanPham());
            if (giaMin != null) {
                if (pt > 0) {
                    BigDecimal sau = giaMin.multiply(BigDecimal.valueOf(100 - pt))
                            .divide(BigDecimal.valueOf(100));
                    sb.append(" — ").append(tien(sau)).append(" (giảm ").append(pt).append("%, giá gốc ")
                            .append(tien(giaMin)).append(")");
                } else {
                    sb.append(" — từ ").append(tien(giaMin));
                }
            }
            sb.append("\n👉 /cua-hang/san-pham/").append(sp.getMaSanPham()).append("\n");
        }
        sb.append("Bạn muốn biết size/màu còn hàng của mẫu nào cứ hỏi mình nhé!");
        return sb.toString();
    }

    private String chiTietTonKho(SanPham sp) {
        List<SanPhamChiTiet> bienThe = sanPhamChiTietRepository.findByMaSanPham(sp.getMaSanPham());
        LinkedHashSet<String> size = new LinkedHashSet<>();
        LinkedHashSet<String> mau = new LinkedHashSet<>();
        int tong = 0;
        for (SanPhamChiTiet bt : bienThe) {
            int ton = bt.getSoLuongTon() != null ? bt.getSoLuongTon() : 0;
            if (ton <= 0) continue;
            tong += ton;
            if (bt.getKichThuoc() != null) size.add(bt.getKichThuoc().getTenKichThuoc());
            if (bt.getMauSac() != null) mau.add(bt.getMauSac().getTenMauSac());
        }
        if (tong == 0) {
            return "😢 \"" + sp.getTenSanPham() + "\" hiện đã HẾT HÀNG. "
                    + "Bạn xem các mẫu tương tự tại 👉 /cua-hang/san-pham nhé!";
        }
        return "📦 \"" + sp.getTenSanPham() + "\" đang CÒN HÀNG (" + tong + " sản phẩm):\n"
                + "• Size còn: " + String.join(", ", size) + "\n"
                + "• Màu còn: " + String.join(", ", mau) + "\n"
                + "👉 /cua-hang/san-pham/" + sp.getMaSanPham();
    }

    private BigDecimal giaThapNhat(SanPham sp) {
        BigDecimal min = null;
        for (SanPhamChiTiet bt : sanPhamChiTietRepository.findByMaSanPham(sp.getMaSanPham())) {
            if (bt.getGiaBan() == null) continue;
            if (min == null || bt.getGiaBan().compareTo(min) < 0) min = bt.getGiaBan();
        }
        return min;
    }

    private String khuyenMai() {
        List<SanPham> dangGiam = new ArrayList<>();
        for (SanPham sp : sanPhamRepository.findAll()) {
            if (sp.getTrangThai() != null && !sp.getTrangThai()) continue;
            if (khuyenMaiService.phanTramGiamSanPham(sp.getMaSanPham()) > 0) {
                dangGiam.add(sp);
                if (dangGiam.size() >= 4) break;
            }
        }
        if (dangGiam.isEmpty()) {
            return "Hiện chưa có đợt giảm giá nào đang chạy. Bạn theo dõi trang chủ để săn sale sớm nhất nhé! 👉 /";
        }
        StringBuilder sb = new StringBuilder("🔥 Đang KHUYẾN MÃI:\n");
        for (SanPham sp : dangGiam) {
            int pt = khuyenMaiService.phanTramGiamSanPham(sp.getMaSanPham());
            sb.append("• ").append(sp.getTenSanPham()).append(" — giảm ").append(pt)
                    .append("%\n👉 /cua-hang/san-pham/").append(sp.getMaSanPham()).append("\n");
        }
        sb.append("Xem tất cả sản phẩm đang giảm: 👉 /cua-hang/san-pham?khuyenMai=true");
        return sb.toString();
    }

    private String voucher(KhachHang kh) {
        if (kh == null) {
            return "🎟️ Bạn đăng nhập để xem voucher công khai còn lượt và voucher được tặng riêng nhé!\n"
                    + "👉 /login — sau đó vào \"Voucher của tôi\" hoặc áp mã ngay tại giỏ hàng.";
        }
        List<GiamGia> ds = giamGiaService.getVoucherKhaDungChoKhachHang(kh.getMaKH());
        if (ds == null || ds.isEmpty()) {
            return "Hiện bạn chưa có voucher khả dụng. Theo dõi trang chủ để săn mã mới nhé!";
        }
        StringBuilder sb = new StringBuilder("🎟️ Voucher bạn đang có (" + ds.size() + "):\n");
        int dem = 0;
        for (GiamGia v : ds) {
            sb.append("• ").append(v.getTenGiamGia());
            if ("PhanTram".equals(v.getLoaiGiamGia())) sb.append(" — giảm ").append(tien(v.getGiaTriGiam()).replace("₫", "%"));
            else sb.append(" — giảm ").append(tien(v.getGiaTriGiam()));
            sb.append("\n");
            if (++dem >= 5) break;
        }
        sb.append("Nhập TÊN MÃ tại giỏ hàng để áp dụng 👉 /gio-hang");
        return sb.toString();
    }

    private String banChay() {
        List<SanPham> ds = sanPhamRepository.findAll();
        StringBuilder sb = new StringBuilder("👟 Một vài gợi ý cho bạn:\n");
        int dem = 0;
        for (SanPham sp : ds) {
            if (sp.getTrangThai() != null && !sp.getTrangThai()) continue;
            BigDecimal gia = giaThapNhat(sp);
            sb.append("• ").append(sp.getTenSanPham());
            if (gia != null) sb.append(" — từ ").append(tien(gia));
            sb.append("\n👉 /cua-hang/san-pham/").append(sp.getMaSanPham()).append("\n");
            if (++dem >= 3) break;
        }
        sb.append("Xem toàn bộ: 👉 /cua-hang/san-pham");
        return sb.toString();
    }

    private String traCuuDon(String maHoaDon, KhachHang kh) {
        HoaDon hd = hoaDonService.findById(maHoaDon);
        if (hd == null) {
            return "Mình không tìm thấy đơn \"" + maHoaDon + "\". Bạn kiểm tra lại mã (dạng HDxxxx) "
                    + "hoặc tra cứu bằng MÃ ĐƠN + SĐT tại 👉 /theo-doi-don-hang nhé.";
        }
        boolean cuaKhach = kh != null && hd.getMaKhachHang() != null
                && kh.getMaKH().equals(hd.getMaKhachHang().getMaKH());
        if (!cuaKhach) {
            return "Vì lý do bảo mật, mình chỉ tra chi tiết đơn của chính bạn khi đã đăng nhập. "
                    + "Bạn có thể tra cứu công khai bằng MÃ ĐƠN + SĐT đặt hàng tại 👉 /theo-doi-don-hang";
        }
        return "📦 Đơn " + hd.getMaHoaDon() + " — trạng thái: " + hd.getTrangThai()
                + (hd.getTongTien() != null ? " • Tổng: " + tien(hd.getTongTien()) : "")
                + "\nXem chi tiết + theo dõi thời gian thực: 👉 /ca-nhan/don-hang/" + hd.getMaHoaDon();
    }
}