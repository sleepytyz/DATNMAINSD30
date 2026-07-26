package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.DanhGia;
import com.example.th06876_java202.Entity.HoaDon;
import com.example.th06876_java202.Entity.HoaDonChiTiet;
import com.example.th06876_java202.Entity.KhachHang;
import com.example.th06876_java202.Entity.SanPham;
import com.example.th06876_java202.Service.DanhGiaService;
import com.example.th06876_java202.Service.HoaDonChiTietService;
import com.example.th06876_java202.Service.HoaDonService;
import com.example.th06876_java202.Service.KhachHangService;
import com.example.th06876_java202.Service.SanPhamService;
import com.example.th06876_java202.Storefront.SanPhamCardVM;
import com.example.th06876_java202.Storefront.SanPhamHienThiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

/**
 * Các trang nội dung của website bán hàng: Giới thiệu, Liên hệ,
 * Theo dõi đơn hàng (tra cứu công khai bằng mã đơn + SĐT — có cập nhật trạng thái
 * THỜI GIAN THỰC qua WebSocket ngay trên trang kết quả), và trang Đánh giá sản phẩm.
 */
@Controller
@RequiredArgsConstructor
public class TrangTinhController {

    private final HoaDonService hoaDonService;
    private final HoaDonChiTietService hoaDonChiTietService;
    private final KhachHangService khachHangService;
    private final SanPhamService sanPhamService;
    private final SanPhamHienThiService sanPhamHienThiService;
    private final com.example.th06876_java202.Repository.LienHeRepository lienHeRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final DanhGiaService danhGiaService;

    // ====== GIỚI THIỆU ======
    @GetMapping("/gioi-thieu")
    public String gioiThieu() {
        return "trangtinh/gioi-thieu";
    }

    // ====== LIÊN HỆ ======
    @GetMapping("/lien-he")
    public String lienHe() {
        return "trangtinh/lien-he";
    }

    @PostMapping("/lien-he")
    public String guiLienHe(@RequestParam(required = false) String hoTen,
                            @RequestParam(required = false) String email,
                            @RequestParam(required = false) String noiDung,
                            RedirectAttributes ra) {
        if (hoTen == null || hoTen.isBlank() || email == null || email.isBlank()
                || noiDung == null || noiDung.isBlank()) {
            ra.addFlashAttribute("loiLienHe", "Vui lòng điền đầy đủ họ tên, email và nội dung.");
        } else {
            // Lưu vào bảng LienHe để quầy quản lý xử lý
            com.example.th06876_java202.Entity.LienHe lh = new com.example.th06876_java202.Entity.LienHe();
            lh.setHoTen(hoTen.trim());
            lh.setEmail(email.trim());
            lh.setNoiDung(noiDung.trim().length() > 2000 ? noiDung.trim().substring(0, 2000) : noiDung.trim());
            lh.setThoiGian(java.time.LocalDateTime.now());
            lh.setTrangThai("Chưa xử lý");
            lh = lienHeRepository.save(lh);

            // Báo THỜI GIAN THỰC sang khu quản lý (chuông + module Liên hệ)
            java.util.Map<String, Object> tb = new java.util.HashMap<>();
            tb.put("loai", "LIEN_HE_MOI");
            tb.put("maLienHe", lh.getMaLienHe());
            tb.put("hoTen", lh.getHoTen());
            tb.put("email", lh.getEmail());
            tb.put("xemTruoc", lh.getNoiDung().length() > 120
                    ? lh.getNoiDung().substring(0, 120) + "…" : lh.getNoiDung());
            tb.put("thoiGian", java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM").format(lh.getThoiGian()));
            messagingTemplate.convertAndSend("/topic/quanly/lien-he", tb);

            ra.addFlashAttribute("thongBaoLienHe",
                    "Cảm ơn " + hoTen.trim() + "! FS Shoes đã nhận được tin nhắn và sẽ phản hồi qua email "
                            + email.trim() + " sớm nhất.");
        }
        return "redirect:/lien-he";
    }

    // ====== THEO DÕI ĐƠN HÀNG (tra cứu công khai bằng mã đơn + SĐT) ======
    @GetMapping("/theo-doi-don-hang")
    public String formTheoDoi() {
        return "trangtinh/theo-doi-don-hang";
    }

    @GetMapping("/theo-doi-don-hang/tra-cuu")
    public String traCuuDonHang(@RequestParam(required = false) String maHoaDon,
                                @RequestParam(required = false) String soDienThoai,
                                Model model) {
        String maHD = maHoaDon != null ? maHoaDon.trim() : "";
        String sdt = soDienThoai != null ? soDienThoai.trim() : "";

        if (maHD.isEmpty() || sdt.isEmpty()) {
            model.addAttribute("loi", "Vui lòng nhập mã đơn hàng và số điện thoại đặt hàng.");
            return "trangtinh/theo-doi-don-hang";
        }

        HoaDon hoaDon = hoaDonService.findById(maHD);
        // Xác thực: đơn tồn tại, có khách hàng, và SĐT khớp với SĐT khách hàng của đơn
        boolean hopLe = hoaDon != null
                && hoaDon.getMaKhachHang() != null
                && hoaDon.getMaKhachHang().getSdt() != null
                && hoaDon.getMaKhachHang().getSdt().equals(sdt);

        if (!hopLe) {
            model.addAttribute("loi", "Không tìm thấy đơn hàng khớp với thông tin bạn nhập. Vui lòng kiểm tra lại mã đơn và số điện thoại.");
            model.addAttribute("maHoaDon", maHD);
            model.addAttribute("soDienThoai", sdt);
            return "trangtinh/theo-doi-don-hang";
        }

        List<HoaDonChiTiet> chiTiet = hoaDonChiTietService.findByHoaDOn(hoaDon);
        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("chiTiet", chiTiet);
        return "trangtinh/theo-doi-ket-qua";
    }

    // ====== ĐÁNH GIÁ ======
    /** Trang tổng hợp đánh giá: sản phẩm được chấm cao + các đánh giá mới nhất. */
    @GetMapping("/danh-gia")
    public String trangDanhGia(Model model) {
        List<SanPham> sanPhams = sanPhamService.getAll().stream()
                .filter(sp -> Boolean.TRUE.equals(sp.getTrangThai()))
                .toList();
        List<SanPhamCardVM> cards = sanPhamHienThiService.taoDanhSachCard(sanPhams);

        // Sản phẩm có đánh giá, xếp theo điểm giảm dần
        List<SanPhamCardVM> coDanhGia = cards.stream()
                .filter(c -> c.getSoLuotDanhGia() > 0)
                .sorted(Comparator.comparingDouble(SanPhamCardVM::getDiemTrungBinh).reversed()
                        .thenComparing(Comparator.comparingLong(SanPhamCardVM::getSoLuotDanhGia).reversed()))
                .toList();
        // Sản phẩm chưa có đánh giá (mời khách là người đầu tiên)
        List<SanPhamCardVM> chuaCoDanhGia = cards.stream()
                .filter(c -> c.getSoLuotDanhGia() == 0)
                .limit(8)
                .toList();

        // Các đánh giá mới nhất toàn cửa hàng
        List<DanhGia> danhGiaMoiNhat = sanPhams.stream()
                .flatMap(sp -> danhGiaService.danhSachTheoSanPham(sp.getMaSanPham()).stream())
                .sorted(Comparator.comparing(DanhGia::getNgayDanhGia,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .toList();

        model.addAttribute("coDanhGia", coDanhGia);
        model.addAttribute("chuaCoDanhGia", chuaCoDanhGia);
        model.addAttribute("danhGiaMoiNhat", danhGiaMoiNhat);
        return "trangtinh/danh-gia";
    }

    /** Gửi đánh giá cho 1 sản phẩm (yêu cầu đăng nhập USER + đã mua & nhận hàng). */
    @PostMapping("/danh-gia/gui")
    public String guiDanhGia(@RequestParam String maSanPham,
                             @RequestParam int soSao,
                             @RequestParam(required = false) String noiDung,
                             @RequestParam(required = false) String veDonHang,
                             Authentication authentication,
                             RedirectAttributes ra) {
        // Gửi từ trang chi tiết đơn hàng -> quay về đúng trang đơn đó;
        // gửi từ trang sản phẩm -> quay về tab đánh giá của sản phẩm.
        boolean veDon = veDonHang != null && !veDonHang.isBlank();
        String diaChiVe = veDon
                ? "redirect:/ca-nhan/don-hang/" + veDonHang
                : "redirect:/cua-hang/san-pham/" + maSanPham + "#danh-gia";

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            ra.addFlashAttribute(veDon ? "loiDonHang" : "loiDanhGia",
                    "Bạn cần đăng nhập để đánh giá sản phẩm.");
            return diaChiVe;
        }

        KhachHang kh = khachHangService.findByTenDangNhap(authentication.getName());
        if (kh == null) {
            ra.addFlashAttribute(veDon ? "loiDonHang" : "loiDanhGia",
                    "Không tìm thấy hồ sơ khách hàng.");
            return diaChiVe;
        }

        String loi = danhGiaService.themDanhGia(kh, maSanPham, soSao, noiDung);
        if (loi != null) {
            ra.addFlashAttribute(veDon ? "loiDonHang" : "loiDanhGia", loi);
        } else {
            ra.addFlashAttribute(veDon ? "thongBao" : "thongBaoDanhGia",
                    "Cảm ơn bạn đã đánh giá sản phẩm!");
        }
        return diaChiVe;
    }
}