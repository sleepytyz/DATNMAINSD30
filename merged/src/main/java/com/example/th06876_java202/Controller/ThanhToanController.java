package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.DiaChi;
import com.example.th06876_java202.Entity.GiamGia;
import com.example.th06876_java202.Entity.HoaDon;
import com.example.th06876_java202.Entity.KhachHang;
import com.example.th06876_java202.Service.DiaChiService;
import com.example.th06876_java202.Service.GiamGiaService;
import com.example.th06876_java202.Service.HoaDonChiTietService;
import com.example.th06876_java202.Service.HoaDonService;
import com.example.th06876_java202.Service.KhachHangService;
import com.example.th06876_java202.Storefront.DatHangException;
import com.example.th06876_java202.Storefront.DonHangOnlineService;
import com.example.th06876_java202.Storefront.GioHang;
import com.example.th06876_java202.Storefront.GioHangService;
import com.example.th06876_java202.Storefront.GioHangView;
import com.example.th06876_java202.Storefront.GiaoHangNhanhService;
import jakarta.servlet.http.HttpSession;
import com.example.th06876_java202.Storefront.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Thanh toán / đặt hàng online (yêu cầu đăng nhập tài khoản khách hàng).
 * Hỗ trợ sổ địa chỉ, địa chỉ mới, COD hoặc Chuyển khoản (hiện mã VietQR tự động
 * theo cấu hình vietqr.* trong application.properties).
 */
@Controller
@RequestMapping("/thanh-toan")
@RequiredArgsConstructor
public class ThanhToanController {

    private final GioHang gioHang;
    private final VNPayService vnPayService;
    private final GiaoHangNhanhService giaoHangNhanhService;

    /* ===== MUA NGAY: giữ riêng trong phiên, KHÔNG đụng tới giỏ hàng của khách ===== */
    public static final String MN_SPCT = "MUA_NGAY_SPCT";
    public static final String MN_SL = "MUA_NGAY_SL";
    public static final String MN_VOUCHER = "MUA_NGAY_VOUCHER";
    private final GioHangService gioHangService;
    private final KhachHangService khachHangService;
    private final DiaChiService diaChiService;
    private final DonHangOnlineService donHangOnlineService;
    private final HoaDonService hoaDonService;
    private final HoaDonChiTietService hoaDonChiTietService;
    private final GiamGiaService giamGiaService;

    @Value("${vietqr.bank:MB}")
    private String vietqrBank;

    @Value("${vietqr.account:0000000000}")
    private String vietqrAccount;

    @Value("${vietqr.account-name:FS%20SHOES}")
    private String vietqrAccountName;

    private KhachHang khachHangHienTai(Authentication authentication) {
        return khachHangService.findByTenDangNhap(authentication.getName());
    }

    /**
     * Giỏ dùng cho trang thanh toán: nếu khách bấm "Mua ngay" thì dựng một giỏ TẠM
     * chỉ chứa đúng sản phẩm đó (giỏ hàng thật giữ nguyên, không bị thêm/xoá gì).
     */
    private GioHang gioHangDangThanhToan(HttpSession session) {
        Object ma = session.getAttribute(MN_SPCT);
        if (ma == null) return gioHang;
        GioHang tam = new GioHang();
        Object sl = session.getAttribute(MN_SL);
        tam.themSanPham((String) ma, sl instanceof Integer ? (Integer) sl : 1);
        Object v = session.getAttribute(MN_VOUCHER);
        if (v != null) tam.setMaGiamGiaApDung((String) v);
        return tam;
    }

    private boolean dangMuaNgay(HttpSession session) {
        return session.getAttribute(MN_SPCT) != null;
    }

    private void xoaMuaNgay(HttpSession session) {
        session.removeAttribute(MN_SPCT);
        session.removeAttribute(MN_SL);
        session.removeAttribute(MN_VOUCHER);
    }

    /** Bấm "Mua ngay" ở trang chi tiết: đi thẳng tới thanh toán, KHÔNG thêm vào giỏ. */
    @PostMapping("/mua-ngay")
    public String muaNgay(@RequestParam String maSanPhamChiTiet,
                          @RequestParam(defaultValue = "1") int soLuong,
                          HttpSession session,
                          RedirectAttributes ra) {
        GioHang thu = new GioHang();
        String loi = gioHangService.themVaoGio(thu, maSanPhamChiTiet, Math.max(1, soLuong));
        if (loi != null && thu.isEmpty()) {          // không thêm được (hết hàng / ngừng bán...)
            ra.addFlashAttribute("thongBaoGioHang", loi);
            return "redirect:/cua-hang/san-pham";
        }
        session.setAttribute(MN_SPCT, maSanPhamChiTiet);
        session.setAttribute(MN_SL, Math.max(1, soLuong));
        session.removeAttribute(MN_VOUCHER);
        return "redirect:/thanh-toan";
    }

    /** Thoát chế độ "Mua ngay" để quay về thanh toán cả giỏ hàng. */
    @GetMapping("/huy-mua-ngay")
    public String huyMuaNgay(HttpSession session) {
        xoaMuaNgay(session);
        return "redirect:/gio-hang";
    }

    /** Áp dụng voucher NGAY TẠI trang thanh toán (dùng chung cho giỏ thường và mua ngay). */
    @PostMapping("/ap-dung-voucher")
    public String apDungVoucher(@RequestParam String maVoucher,
                                Authentication authentication,
                                HttpSession session,
                                RedirectAttributes ra) {
        KhachHang kh = khachHangHienTai(authentication);
        String maKH = kh != null ? kh.getMaKH() : null;
        GioHang gio = gioHangDangThanhToan(session);
        String loi = gioHangService.apDungVoucher(gio, maKH, maVoucher);
        if (loi != null) {
            ra.addFlashAttribute("loiVoucher", loi);
        } else {
            if (dangMuaNgay(session)) session.setAttribute(MN_VOUCHER, gio.getMaGiamGiaApDung());
            ra.addFlashAttribute("thongBaoVoucher", "Đã áp dụng mã giảm giá \"" + maVoucher + "\".");
        }
        return "redirect:/thanh-toan";
    }

    /**
     * ÁP DỤNG / BỎ VOUCHER KHÔNG TẢI LẠI TRANG (AJAX).
     * Trả về JSON để trang thanh toán tự cập nhật khối tiền — tránh reload làm mất
     * lựa chọn địa chỉ, phương thức thanh toán và ô ghi chú khách đang gõ dở.
     */
    @PostMapping("/voucher-ajax")
    @ResponseBody
    public java.util.Map<String, Object> voucherAjax(
            @RequestParam(required = false) String maVoucher,
            @RequestParam(defaultValue = "ap-dung") String hanhDong,
            @RequestParam(required = false) Integer maQuanHuyenGHN,
            @RequestParam(required = false) String maPhuongXaGHN,
            Authentication authentication,
            HttpSession session) {

        java.util.Map<String, Object> kq = new java.util.LinkedHashMap<>();
        KhachHang kh = khachHangHienTai(authentication);
        String maKH = kh != null ? kh.getMaKH() : null;
        GioHang gio = gioHangDangThanhToan(session);

        if ("bo".equals(hanhDong)) {
            if (dangMuaNgay(session)) {
                session.removeAttribute(MN_VOUCHER);
            } else {
                gioHang.setMaGiamGiaApDung(null);
            }
            gio.setMaGiamGiaApDung(null);
            kq.put("thanhCong", true);
            kq.put("thongBao", "Đã bỏ mã giảm giá.");
        } else {
            String loi = gioHangService.apDungVoucher(gio, maKH, maVoucher);
            if (loi != null) {
                kq.put("thanhCong", false);
                kq.put("thongBao", loi);
            } else {
                if (dangMuaNgay(session)) session.setAttribute(MN_VOUCHER, gio.getMaGiamGiaApDung());
                kq.put("thanhCong", true);
                kq.put("thongBao", "Đã áp dụng mã giảm giá \"" + maVoucher + "\".");
            }
        }

        // Dựng lại khối tiền theo đúng địa chỉ khách đang chọn để số liệu khớp tuyệt đối
        GioHangView view = gioHangService.xemGioHang(gioHangDangThanhToan(session), maKH,
                maQuanHuyenGHN, maPhuongXaGHN);
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,###",
                new java.text.DecimalFormatSymbols(java.util.Locale.US));
        kq.put("coVoucher", view.getVoucherApDung() != null);
        kq.put("tenVoucher", view.getVoucherApDung() != null ? view.getVoucherApDung().getTenGiamGia() : null);
        kq.put("giamVoucherText", "-" + df.format(view.getSoTienGiamVoucher()) + "\u20AB");
        kq.put("tongTienHangText", df.format(view.getTongTienHang()) + "\u20AB");
        kq.put("phiShipText", view.getTienShip().signum() > 0
                ? df.format(view.getTienShip()) + "\u20AB" : "Miễn phí");
        kq.put("tongThanhToanText", df.format(view.getTongThanhToan()) + "\u20AB");
        return kq;
    }

    /** Bỏ voucher đang áp dụng. */
    @PostMapping("/bo-voucher")
    public String boVoucher(HttpSession session, RedirectAttributes ra) {
        if (dangMuaNgay(session)) {
            session.removeAttribute(MN_VOUCHER);
        } else {
            gioHang.setMaGiamGiaApDung(null);
        }
        ra.addFlashAttribute("thongBaoVoucher", "Đã bỏ mã giảm giá.");
        return "redirect:/thanh-toan";
    }

    // =====================================================================
    // TRANG THANH TOÁN
    // =====================================================================

    @GetMapping
    public String trangThanhToan(Model model, Authentication authentication,
                                 HttpSession session,
                                 @RequestParam(value = "tuGio", required = false) String tuGio,
                                 RedirectAttributes redirectAttributes) {
        if (tuGio != null) xoaMuaNgay(session);       // bấm thanh toán từ giỏ -> thoát chế độ mua ngay
        KhachHang kh = khachHangHienTai(authentication);
        if (kh == null) {
            redirectAttributes.addFlashAttribute("thongBaoGioHang", "Không tìm thấy hồ sơ khách hàng cho tài khoản này.");
            return "redirect:/gio-hang";
        }

        // Cước ban đầu tính luôn theo địa chỉ mặc định (nếu địa chỉ đó đã có mã GHN)
        List<DiaChi> soDiaChi = diaChiService.findByKhachHang(kh.getMaKH());
        // Địa chỉ lưu TRƯỚC khi tích hợp GHN chỉ có chữ (Hà Nội / Cầu Giấy / Dịch Vọng)
        // nên không tính được cước -> tự dò mã địa giới từ tên rồi lưu lại, lần sau dùng ngay.
        if (giaoHangNhanhService.daCauHinh()) {
            for (DiaChi d : soDiaChi) {
                if (d.getMaQuanHuyenGHN() != null && d.getMaPhuongXaGHN() != null) continue;
                try {
                    java.util.Map<String, Object> ma = giaoHangNhanhService.doDiaChi(
                            d.getTinhThanh(), d.getQuanHuyen(), d.getPhuongXa());
                    Object huyen = ma.get("districtId");
                    Object xa = ma.get("wardCode");
                    if (huyen != null && xa != null) {
                        d.setMaQuanHuyenGHN(Integer.parseInt(String.valueOf(huyen)));
                        d.setMaPhuongXaGHN(String.valueOf(xa));
                        diaChiService.save(d);
                    }
                } catch (Exception ignored) { }
            }
        }
        DiaChi macDinh = soDiaChi.stream()
                .filter(d -> Boolean.TRUE.equals(d.getDiaChiMacDinh()))
                .findFirst()
                .orElse(soDiaChi.isEmpty() ? null : soDiaChi.get(0));
        GioHang gioDung = gioHangDangThanhToan(session);
        GioHangView view = gioHangService.xemGioHang(gioDung, kh.getMaKH(),
                macDinh != null ? macDinh.getMaQuanHuyenGHN() : null,
                macDinh != null ? macDinh.getMaPhuongXaGHN() : null);
        if (view.getDongHang().isEmpty() || view.getTongTienHang().signum() <= 0) {
            xoaMuaNgay(session);
            redirectAttributes.addFlashAttribute("thongBaoGioHang",
                    "Giỏ hàng của bạn đang trống, vui lòng chọn sản phẩm trước khi thanh toán.");
            return "redirect:/gio-hang";
        }

        List<DiaChi> diaChis = soDiaChi;
        List<GiamGia> voucherKhaDung = List.of();
        try {
            voucherKhaDung = giamGiaService.getVoucherKhaDungChoKhachHang(kh.getMaKH());
        } catch (Exception ignored) { }

        model.addAttribute("gio", view);
        model.addAttribute("diaChis", diaChis);
        model.addAttribute("khachHang", kh);
        model.addAttribute("voucherKhaDung", voucherKhaDung);
        model.addAttribute("vnpayBat", vnPayService.daCauHinh());
        model.addAttribute("ghnBat", giaoHangNhanhService.daCauHinh());
        model.addAttribute("muaNgay", dangMuaNgay(session));
        return "thanhtoan/index";
    }

    // =====================================================================
    // ĐẶT HÀNG
    // =====================================================================

    @PostMapping("/dat-hang")
    public String datHang(@RequestParam String chonDiaChi,
                          @RequestParam(required = false) String tenNguoiNhan,
                          @RequestParam(required = false) String soDienThoaiNguoiNhan,
                          @RequestParam(required = false) String tinhThanh,
                          @RequestParam(required = false) String quanHuyen,
                          @RequestParam(required = false) String phuongXa,
                          @RequestParam(required = false) Integer maQuanHuyenGHN,
                          @RequestParam(required = false) String maPhuongXaGHN,
                          @RequestParam(required = false) String diaChiCuThe,
                          @RequestParam(defaultValue = "false") boolean luuDiaChi,
                          @RequestParam(defaultValue = "false") boolean datMacDinh,
                          @RequestParam String phuongThucThanhToan,
                          @RequestParam(required = false) String ghiChu,
                          Authentication authentication,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {

        KhachHang kh = khachHangHienTai(authentication);
        if (kh == null) {
            redirectAttributes.addFlashAttribute("loiDatHang", "Không tìm thấy hồ sơ khách hàng cho tài khoản này.");
            return "redirect:/thanh-toan";
        }

        String diaChiGiaoHangText;
        String tenNhan = null, sdtNhan = null;   // để lưu vào hoá đơn
        try {
            if ("moi".equals(chonDiaChi)) {
                if (isBlank(tenNguoiNhan) || isBlank(soDienThoaiNguoiNhan) || isBlank(tinhThanh)
                        || isBlank(quanHuyen) || isBlank(phuongXa) || isBlank(diaChiCuThe)) {
                    redirectAttributes.addFlashAttribute("loiDatHang", "Vui lòng nhập đầy đủ thông tin địa chỉ giao hàng mới.");
                    return "redirect:/thanh-toan";
                }
                if (!soDienThoaiNguoiNhan.matches("^(0(3|5|7|8|9))[0-9]{8}$")) {
                    redirectAttributes.addFlashAttribute("loiDatHang", "Số điện thoại người nhận không hợp lệ.");
                    return "redirect:/thanh-toan";
                }

                DiaChi diaChiMoi = new DiaChi();
                diaChiMoi.setKhachHang(kh);
                diaChiMoi.setTenNguoiNhan(tenNguoiNhan.trim());
                diaChiMoi.setSoDienThoaiNguoiNhan(soDienThoaiNguoiNhan.trim());
                diaChiMoi.setTinhThanh(tinhThanh.trim());
                diaChiMoi.setQuanHuyen(quanHuyen.trim());
                diaChiMoi.setPhuongXa(phuongXa.trim());
                diaChiMoi.setDiaChiCuThe(diaChiCuThe.trim());
                diaChiMoi.setMaQuanHuyenGHN(maQuanHuyenGHN);
                diaChiMoi.setMaPhuongXaGHN(maPhuongXaGHN != null && !maPhuongXaGHN.isBlank()
                        ? maPhuongXaGHN.trim() : null);

                boolean chuaCoDiaChiNao = diaChiService.findByKhachHang_MaKH(kh.getMaKH()).isEmpty();
                diaChiMoi.setDiaChiMacDinh(datMacDinh || chuaCoDiaChiNao);

                diaChiGiaoHangText = dinhDangDiaChi(diaChiMoi);
                tenNhan = tenNguoiNhan.trim();
                sdtNhan = soDienThoaiNguoiNhan.trim();

                if (luuDiaChi || chuaCoDiaChiNao) {
                    diaChiService.save(diaChiMoi);
                }
            } else {
                Integer maDiaChi;
                try {
                    maDiaChi = Integer.parseInt(chonDiaChi);
                } catch (NumberFormatException ex) {
                    redirectAttributes.addFlashAttribute("loiDatHang", "Vui lòng chọn địa chỉ giao hàng.");
                    return "redirect:/thanh-toan";
                }
                DiaChi diaChi = diaChiService.findById(maDiaChi).orElse(null);
                if (diaChi == null || diaChi.getKhachHang() == null
                        || !diaChi.getKhachHang().getMaKH().equals(kh.getMaKH())) {
                    redirectAttributes.addFlashAttribute("loiDatHang", "Địa chỉ giao hàng không hợp lệ.");
                    return "redirect:/thanh-toan";
                }
                diaChiGiaoHangText = dinhDangDiaChi(diaChi);
                maQuanHuyenGHN = diaChi.getMaQuanHuyenGHN();
                maPhuongXaGHN = diaChi.getMaPhuongXaGHN();
                tenNhan = diaChi.getTenNguoiNhan();
                sdtNhan = diaChi.getSoDienThoaiNguoiNhan();
            }

            HoaDon hoaDon = donHangOnlineService.datHang(kh, gioHangDangThanhToan(session), diaChiGiaoHangText,
                    phuongThucThanhToan, ghiChu, maQuanHuyenGHN, maPhuongXaGHN, tenNhan, sdtNhan);
            xoaMuaNgay(session);          // xong "mua ngay" -> trả trang thanh toán về giỏ thường
            if (hoaDon.getPhuongThucThanhToan() != null
                    && hoaDon.getPhuongThucThanhToan().toLowerCase().contains("vnpay")) {
                // Chuyển thẳng sang cổng VNPay (sandbox) để thanh toán ngay
                return "redirect:/thanh-toan/vnpay/" + hoaDon.getMaHoaDon();
            }
            return "redirect:/thanh-toan/thanh-cong/" + hoaDon.getMaHoaDon();

        } catch (DatHangException ex) {
            redirectAttributes.addFlashAttribute("loiDatHang", ex.getMessage());
            return "redirect:/thanh-toan";
        }
    }


    @GetMapping({"/thanh-cong/{id}", "/ket-qua/{id}"})
    public String thanhCong(@PathVariable String id, Model model, Authentication authentication) {
        KhachHang kh = khachHangHienTai(authentication);
        HoaDon hoaDon = hoaDonService.findById(id);
        if (hoaDon == null || kh == null || hoaDon.getMaKhachHang() == null
                || !hoaDon.getMaKhachHang().getMaKH().equals(kh.getMaKH())) {
            return "redirect:/";
        }

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("chiTiet", hoaDonChiTietService.findByHoaDOn(hoaDon));

        // Chuyển khoản → tạo ảnh VietQR động (đúng số tiền + nội dung là mã đơn)
        boolean chuyenKhoan = hoaDon.getPhuongThucThanhToan() != null
                && hoaDon.getPhuongThucThanhToan().toLowerCase().contains("chuyển khoản");
        model.addAttribute("chuyenKhoan", chuyenKhoan);
        boolean vnPay = hoaDon.getPhuongThucThanhToan() != null
                && hoaDon.getPhuongThucThanhToan().toLowerCase().contains("vnpay");
        model.addAttribute("vnPay", vnPay);
        if (chuyenKhoan) {
            long soTien = hoaDon.getTongTien() != null ? hoaDon.getTongTien().longValue() : 0L;
            String qrUrl = "https://img.vietqr.io/image/" + vietqrBank + "-" + vietqrAccount
                    + "-compact2.png?amount=" + soTien
                    + "&addInfo=" + hoaDon.getMaHoaDon()
                    + "&accountName=" + vietqrAccountName;
            model.addAttribute("qrUrl", qrUrl);
            model.addAttribute("nganHang", vietqrBank);
            model.addAttribute("soTaiKhoan", vietqrAccount);
            model.addAttribute("chuTaiKhoan", vietqrAccountName.replace("%20", " "));
        }
        return "thanhtoan/thanh-cong";
    }

    // =====================================================================
    // KHÁCH XÁC NHẬN ĐÃ CHUYỂN KHOẢN
    // =====================================================================

    @PostMapping("/da-chuyen-khoan/{id}")
    public String daChuyenKhoan(@PathVariable String id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        KhachHang kh = khachHangHienTai(authentication);
        HoaDon hoaDon = hoaDonService.findById(id);
        if (hoaDon == null || kh == null || hoaDon.getMaKhachHang() == null
                || !hoaDon.getMaKhachHang().getMaKH().equals(kh.getMaKH())) {
            return "redirect:/";
        }
        try {
            donHangOnlineService.khachXacNhanChuyenKhoan(hoaDon, kh);
            redirectAttributes.addFlashAttribute("thongBaoThanhToan",
                    "Đã ghi nhận bạn chuyển khoản — đơn chuyển sang CHỜ XÁC NHẬN. "
                            + "Cửa hàng sẽ đối soát giao dịch và duyệt đơn trong thời gian sớm nhất.");
        } catch (DatHangException ex) {
            redirectAttributes.addFlashAttribute("canhBaoThanhToan", ex.getMessage());
        }
        return "redirect:/thanh-toan/thanh-cong/" + id;
    }

    // =====================================================================
    // CỔNG THANH TOÁN VNPAY (SANDBOX)
    // =====================================================================

    /** Bước 1: chuyển hướng khách sang trang thanh toán VNPay (đơn phải đang "Chờ thanh toán"). */
    @GetMapping("/vnpay/{id}")
    public String vnpay(@PathVariable String id,
                        Authentication authentication,
                        HttpServletRequest request,
                        RedirectAttributes ra) {
        KhachHang kh = khachHangHienTai(authentication);
        HoaDon hoaDon = hoaDonService.findById(id);
        if (hoaDon == null || kh == null || hoaDon.getMaKhachHang() == null
                || !hoaDon.getMaKhachHang().getMaKH().equals(kh.getMaKH())) {
            return "redirect:/";
        }
        if (!vnPayService.daCauHinh()) {
            ra.addFlashAttribute("canhBaoThanhToan",
                    "Cổng VNPay chưa được cấu hình (điền vnpay.tmn-code và vnpay.hash-secret "
                            + "trong application.properties — đăng ký sandbox miễn phí tại sandbox.vnpayment.vn/devreg).");
            return "redirect:/thanh-toan/thanh-cong/" + id;
        }
        if (!"Chờ thanh toán".equals(hoaDon.getTrangThai())) {
            return "redirect:/thanh-toan/thanh-cong/" + id;
        }
        return "redirect:" + vnPayService.taoUrlThanhToan(hoaDon, request);
    }

    /** Bước 2: VNPay đưa khách quay về đây kèm kết quả + chữ ký — kiểm chữ ký rồi mới xác nhận đơn. */
    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request, RedirectAttributes ra) {
        java.util.Map<String, String> p = new java.util.HashMap<>();
        request.getParameterMap().forEach((k, v) -> p.put(k, v.length > 0 ? v[0] : ""));

        String maHoaDon = vnPayService.maDonTuTxnRef(p.get("vnp_TxnRef"));
        HoaDon hoaDon = maHoaDon != null ? hoaDonService.findById(maHoaDon) : null;
        if (hoaDon == null) return "redirect:/";
        String ve = "redirect:/thanh-toan/thanh-cong/" + maHoaDon;

        if (!vnPayService.chuKyHopLe(p)) {
            ra.addFlashAttribute("canhBaoThanhToan",
                    "Dữ liệu VNPay trả về có CHỮ KÝ KHÔNG HỢP LỆ — giao dịch bị từ chối để bảo đảm an toàn.");
            return ve;
        }
        boolean thanhCong = "00".equals(p.get("vnp_ResponseCode"))
                && "00".equals(p.get("vnp_TransactionStatus"));
        if (!thanhCong) {
            String maLoi = p.getOrDefault("vnp_ResponseCode", "?");
            ra.addFlashAttribute("canhBaoThanhToan",
                    "Thanh toán VNPay chưa hoàn tất (mã " + maLoi
                            + ("24".equals(maLoi) ? " — bạn đã huỷ giao dịch" : "")
                            + "). Đơn vẫn ở trạng thái Chờ thanh toán — bạn có thể thanh toán lại bên dưới.");
            return ve;
        }
        if (!"Chờ thanh toán".equals(hoaDon.getTrangThai())) {
            ra.addFlashAttribute("thongBaoThanhToan", "Đơn hàng đã được xác nhận thanh toán trước đó.");
            return ve;    // idempotent: webhook IPN có thể đã xử lý xong
        }
        long soTienDon = (hoaDon.getTongTien() != null ? hoaDon.getTongTien().longValue() : 0L) * 100L;
        if (!String.valueOf(soTienDon).equals(p.get("vnp_Amount"))) {
            ra.addFlashAttribute("canhBaoThanhToan",
                    "Số tiền VNPay báo về KHÔNG KHỚP với đơn hàng — giao dịch bị từ chối, vui lòng liên hệ cửa hàng.");
            return ve;
        }
        try {
            donHangOnlineService.thanhToanOnlineThanhCong(hoaDon);
            ra.addFlashAttribute("thongBaoThanhToan",
                    "VNPay xác nhận thanh toán THÀNH CÔNG (GD " + p.getOrDefault("vnp_TransactionNo", "-")
                            + ") — đơn hàng đã chuyển sang ĐÃ XÁC NHẬN và sẽ sớm được giao.");
        } catch (DatHangException ex) {
            // Phân biệt 2 tình huống nhờ đọc lại trạng thái MỚI NHẤT:
            //  - Request song song (IPN) vừa xác nhận xong -> coi là thành công (idempotent).
            //  - Hết hàng thật (đơn khác đã lấy hết tồn) -> HUỶ đơn + báo lỗi rõ ràng.
            HoaDon kiemTraLai = hoaDonService.findById(maHoaDon);
            String ttMoi = kiemTraLai != null ? kiemTraLai.getTrangThai() : null;
            if (ttMoi != null && !"Chờ thanh toán".equals(ttMoi) && !"Đã huỷ".equals(ttMoi)) {
                ra.addFlashAttribute("thongBaoThanhToan", "Đơn hàng đã được xác nhận thanh toán.");
            } else {
                donHangOnlineService.huyDonHetHangSauThanhToan(kiemTraLai != null ? kiemTraLai : hoaDon);
                ve = "redirect:/thanh-toan/ket-qua/" + maHoaDon;   // đơn huỷ -> URL trung tính
                ra.addFlashAttribute("loiThanhToan",
                        "ĐẶT HÀNG KHÔNG THÀNH CÔNG: " + ex.getMessage()
                                + " Sản phẩm đã được khách khác mua hết trong lúc bạn thanh toán nên đơn đã bị HUỶ. "
                                + "(Môi trường sandbox không trừ tiền thật; ở môi trường thật, giao dịch VNPay này sẽ được hoàn tiền.)");
            }
        }
        return ve;
    }

    /** Bước 3 (webhook IPN — server VNPay gọi thẳng khi có URL công khai, ví dụ chạy qua ngrok). */
    @GetMapping("/vnpay-ipn")
    @ResponseBody
    public java.util.Map<String, String> vnpayIpn(HttpServletRequest request) {
        java.util.Map<String, String> p = new java.util.HashMap<>();
        request.getParameterMap().forEach((k, v) -> p.put(k, v.length > 0 ? v[0] : ""));

        if (!vnPayService.chuKyHopLe(p)) {
            return java.util.Map.of("RspCode", "97", "Message", "Invalid signature");
        }
        String maHoaDon = vnPayService.maDonTuTxnRef(p.get("vnp_TxnRef"));
        HoaDon hoaDon = maHoaDon != null ? hoaDonService.findById(maHoaDon) : null;
        if (hoaDon == null) return java.util.Map.of("RspCode", "01", "Message", "Order not found");
        long soTienDon = (hoaDon.getTongTien() != null ? hoaDon.getTongTien().longValue() : 0L) * 100L;
        if (!String.valueOf(soTienDon).equals(p.get("vnp_Amount"))) {
            return java.util.Map.of("RspCode", "04", "Message", "Invalid amount");
        }
        if (!"Chờ thanh toán".equals(hoaDon.getTrangThai())) {
            return java.util.Map.of("RspCode", "02", "Message", "Order already confirmed");
        }
        if (!"00".equals(p.get("vnp_ResponseCode")) || !"00".equals(p.get("vnp_TransactionStatus"))) {
            return java.util.Map.of("RspCode", "00", "Message", "Payment failed - recorded");
        }
        try {
            donHangOnlineService.thanhToanOnlineThanhCong(hoaDon);
        } catch (DatHangException ex) {
            HoaDon kiemTraLai = hoaDonService.findById(maHoaDon);
            String ttMoi = kiemTraLai != null ? kiemTraLai.getTrangThai() : null;
            if (ttMoi != null && !"Chờ thanh toán".equals(ttMoi) && !"Đã huỷ".equals(ttMoi)) {
                return java.util.Map.of("RspCode", "02", "Message", "Order already confirmed");
            }
            donHangOnlineService.huyDonHetHangSauThanhToan(kiemTraLai != null ? kiemTraLai : hoaDon);
        }
        return java.util.Map.of("RspCode", "00", "Message", "Confirm success");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String dinhDangDiaChi(DiaChi d) {
        return d.getTenNguoiNhan() + " - " + d.getSoDienThoaiNguoiNhan() + " | "
                + d.getDiaChiCuThe() + ", " + d.getPhuongXa() + ", " + d.getQuanHuyen() + ", " + d.getTinhThanh();
    }
}