package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.*;
import com.example.th06876_java202.Service.*;
import com.example.th06876_java202.Storefront.DatHangException;
import com.example.th06876_java202.Storefront.DoiMatKhauDTO;
import com.example.th06876_java202.Storefront.DonHangOnlineService;
import com.example.th06876_java202.Storefront.GioHang;
import com.example.th06876_java202.Storefront.GioHangService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Khu vực "Tài khoản của tôi" cho khách hàng (ROLE_USER):
 * hồ sơ, đổi mật khẩu, sổ địa chỉ, đơn hàng của tôi (xem / huỷ / mua lại), voucher của tôi.
 */
@Controller
@RequestMapping("/ca-nhan")
@RequiredArgsConstructor
public class TaiKhoanCaNhanController {

    private final KhachHangService khachHangService;
    private final TaiKhoanService taiKhoanService;
    private final DiaChiService diaChiService;
    private final HoaDonService hoaDonService;
    private final HoaDonChiTietService hoaDonChiTietService;
    private final DonHangOnlineService donHangOnlineService;
    private final GiamGiaService giamGiaService;
    private final DanhGiaService danhGiaService;
    private final GioHang gioHang;
    private final GioHangService gioHangService;

    private KhachHang khachHangHienTai(Authentication authentication) {
        return khachHangService.findByTenDangNhap(authentication.getName());
    }

    // =====================================================================
    // HỒ SƠ CÁ NHÂN
    // =====================================================================

    @GetMapping
    public String hoSo(Model model, Authentication authentication) {
        model.addAttribute("khachHang", khachHangHienTai(authentication));
        model.addAttribute("tabActive", "ho-so");
        return "taikhoan/ho-so";
    }

    @PostMapping("/cap-nhat")
    public String capNhatHoSo(@Valid @ModelAttribute("khachHang") KhachHang form,
                              BindingResult result,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        KhachHang hienTai = khachHangHienTai(authentication);
        if (hienTai == null) return "redirect:/";

        // Không cho đổi các trường nhạy cảm qua form này
        form.setMaKH(hienTai.getMaKH());
        form.setTaiKhoan(hienTai.getTaiKhoan());
        form.setNgayDangKy(hienTai.getNgayDangKy());
        form.setTrangThai(hienTai.isTrangThai());
        form.setDanhSachDiaChi(hienTai.getDanhSachDiaChi());

        khachHangService.validateKhachHang(form, result);

        if (result.hasErrors()) {
            model.addAttribute("khachHang", form);
            model.addAttribute("tabActive", "ho-so");
            return "taikhoan/ho-so";
        }

        khachHangService.save(form);
        redirectAttributes.addFlashAttribute("thongBao", "Cập nhật thông tin cá nhân thành công.");
        return "redirect:/ca-nhan";
    }

    // =====================================================================
    // ĐỔI MẬT KHẨU
    // =====================================================================

    @GetMapping("/doi-mat-khau")
    public String trangDoiMatKhau(Model model) {
        model.addAttribute("doiMatKhau", new DoiMatKhauDTO());
        model.addAttribute("tabActive", "doi-mat-khau");
        return "taikhoan/doi-mat-khau";
    }

    @PostMapping("/doi-mat-khau")
    public String xuLyDoiMatKhau(@Valid @ModelAttribute("doiMatKhau") DoiMatKhauDTO dto,
                                 BindingResult result,
                                 Authentication authentication,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!result.hasErrors() && !dto.getMatKhauMoi().equals(dto.getXnMatKhauMoi())) {
            result.rejectValue("xnMatKhauMoi", "error.doiMatKhau", "Mật khẩu xác nhận không khớp.");
        }

        KhachHang kh = khachHangHienTai(authentication);
        if (kh == null || kh.getTaiKhoan() == null) return "redirect:/";

        if (!result.hasErrors()) {
            String loi = taiKhoanService.doiMatKhau(kh.getTaiKhoan().getMaTaiKhoan(),
                    dto.getMatKhauCu(), dto.getMatKhauMoi());
            if (loi != null) {
                result.rejectValue("matKhauCu", "error.doiMatKhau", loi);
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("tabActive", "doi-mat-khau");
            return "taikhoan/doi-mat-khau";
        }

        redirectAttributes.addFlashAttribute("thongBao", "Đổi mật khẩu thành công.");
        return "redirect:/ca-nhan/doi-mat-khau";
    }

    // =====================================================================
    // SỔ ĐỊA CHỈ
    // =====================================================================

    @GetMapping("/dia-chi")
    public String trangDiaChi(Model model, Authentication authentication) {
        KhachHang kh = khachHangHienTai(authentication);
        if (kh == null) return "redirect:/";
        model.addAttribute("diaChis", diaChiService.findByKhachHang(kh.getMaKH()));
        model.addAttribute("tabActive", "dia-chi");
        return "taikhoan/dia-chi";
    }

    @PostMapping("/dia-chi/luu")
    public String luuDiaChi(@RequestParam(required = false) Integer maDiaChi,
                            @RequestParam String tenNguoiNhan,
                            @RequestParam String soDienThoaiNguoiNhan,
                            @RequestParam String tinhThanh,
                            @RequestParam String quanHuyen,
                            @RequestParam String phuongXa,
                            @RequestParam String diaChiCuThe,
                            @RequestParam(defaultValue = "false") boolean datMacDinh,
                            Authentication authentication,
                            RedirectAttributes ra) {
        KhachHang kh = khachHangHienTai(authentication);
        if (kh == null) return "redirect:/";

        if (tenNguoiNhan.isBlank() || tinhThanh.isBlank() || quanHuyen.isBlank()
                || phuongXa.isBlank() || diaChiCuThe.isBlank()) {
            ra.addFlashAttribute("loiDiaChi", "Vui lòng nhập đầy đủ thông tin địa chỉ.");
            return "redirect:/ca-nhan/dia-chi";
        }
        if (!soDienThoaiNguoiNhan.matches("^(0(3|5|7|8|9))[0-9]{8}$")) {
            ra.addFlashAttribute("loiDiaChi", "Số điện thoại người nhận không hợp lệ.");
            return "redirect:/ca-nhan/dia-chi";
        }

        DiaChi diaChi;
        if (maDiaChi != null) {
            diaChi = diaChiService.findById(maDiaChi).orElse(null);
            if (diaChi == null || diaChi.getKhachHang() == null
                    || !diaChi.getKhachHang().getMaKH().equals(kh.getMaKH())) {
                ra.addFlashAttribute("loiDiaChi", "Địa chỉ không hợp lệ.");
                return "redirect:/ca-nhan/dia-chi";
            }
        } else {
            diaChi = new DiaChi();
            diaChi.setKhachHang(kh);
        }

        diaChi.setTenNguoiNhan(tenNguoiNhan.trim());
        diaChi.setSoDienThoaiNguoiNhan(soDienThoaiNguoiNhan.trim());
        diaChi.setTinhThanh(tinhThanh.trim());
        diaChi.setQuanHuyen(quanHuyen.trim());
        diaChi.setPhuongXa(phuongXa.trim());
        diaChi.setDiaChiCuThe(diaChiCuThe.trim());

        List<DiaChi> hienCo = diaChiService.findByKhachHang(kh.getMaKH());
        boolean laDauTien = hienCo.isEmpty()
                || (hienCo.size() == 1 && maDiaChi != null && hienCo.get(0).getMaDiaChi().equals(maDiaChi));

        if (datMacDinh || laDauTien) {
            for (DiaChi d : hienCo) {
                if (Boolean.TRUE.equals(d.getDiaChiMacDinh()) && !d.getMaDiaChi().equals(diaChi.getMaDiaChi())) {
                    d.setDiaChiMacDinh(false);
                    diaChiService.save(d);
                }
            }
            diaChi.setDiaChiMacDinh(true);
        } else if (diaChi.getDiaChiMacDinh() == null) {
            diaChi.setDiaChiMacDinh(false);
        }

        diaChiService.save(diaChi);
        ra.addFlashAttribute("thongBao", maDiaChi != null ? "Cập nhật địa chỉ thành công." : "Thêm địa chỉ mới thành công.");
        return "redirect:/ca-nhan/dia-chi";
    }

    @PostMapping("/dia-chi/xoa/{maDiaChi}")
    public String xoaDiaChi(@PathVariable Integer maDiaChi,
                            Authentication authentication,
                            RedirectAttributes ra) {
        KhachHang kh = khachHangHienTai(authentication);
        DiaChi d = diaChiService.findById(maDiaChi).orElse(null);
        if (kh == null || d == null || d.getKhachHang() == null
                || !d.getKhachHang().getMaKH().equals(kh.getMaKH())) {
            ra.addFlashAttribute("loiDiaChi", "Địa chỉ không hợp lệ.");
        } else {
            diaChiService.delete(maDiaChi);
            ra.addFlashAttribute("thongBao", "Đã xoá địa chỉ.");
        }
        return "redirect:/ca-nhan/dia-chi";
    }

    @PostMapping("/dia-chi/mac-dinh/{maDiaChi}")
    public String datMacDinh(@PathVariable Integer maDiaChi,
                             Authentication authentication,
                             RedirectAttributes ra) {
        KhachHang kh = khachHangHienTai(authentication);
        DiaChi d = diaChiService.findById(maDiaChi).orElse(null);
        if (kh == null || d == null || d.getKhachHang() == null
                || !d.getKhachHang().getMaKH().equals(kh.getMaKH())) {
            ra.addFlashAttribute("loiDiaChi", "Địa chỉ không hợp lệ.");
            return "redirect:/ca-nhan/dia-chi";
        }
        for (DiaChi dc : diaChiService.findByKhachHang(kh.getMaKH())) {
            boolean laMacDinh = dc.getMaDiaChi().equals(maDiaChi);
            if (!java.util.Objects.equals(dc.getDiaChiMacDinh(), laMacDinh)) {
                dc.setDiaChiMacDinh(laMacDinh);
                diaChiService.save(dc);
            }
        }
        ra.addFlashAttribute("thongBao", "Đã đặt địa chỉ mặc định.");
        return "redirect:/ca-nhan/dia-chi";
    }

    // =====================================================================
    // ĐƠN HÀNG CỦA TÔI
    // =====================================================================

    @GetMapping("/don-hang")
    public String donHangCuaToi(@RequestParam(required = false) String tt,
                                @RequestParam(defaultValue = "0") int trang,
                                Model model,
                                Authentication authentication) {
        KhachHang kh = khachHangHienTai(authentication);
        if (kh == null) return "redirect:/";

        // Lấy toàn bộ đơn của khách rồi lọc + phân trang trong bộ nhớ (quy mô phù hợp)
        List<HoaDon> tatCa = hoaDonService.findByKhachHang(kh.getMaKH(),
                org.springframework.data.domain.PageRequest.of(0, 1000)).getContent();

        String loc = (tt == null || tt.isBlank()) ? null : tt.trim();
        List<HoaDon> daLoc = loc == null ? tatCa
                : tatCa.stream().filter(h -> loc.equals(h.getTrangThai())).toList();

        int kichThuoc = 8;
        int tongTrang = Math.max(1, (int) Math.ceil((double) daLoc.size() / kichThuoc));
        int trangHienTai = Math.min(Math.max(trang, 0), tongTrang - 1);
        int tu = trangHienTai * kichThuoc;
        int den = Math.min(tu + kichThuoc, daLoc.size());

        // Đếm nhanh theo trạng thái cho các tab
        Map<String, Long> demTrangThai = new HashMap<>();
        for (HoaDon h : tatCa) {
            demTrangThai.merge(h.getTrangThai() != null ? h.getTrangThai() : "?", 1L, Long::sum);
        }

        model.addAttribute("dsDonHang", tu < den ? daLoc.subList(tu, den) : List.<HoaDon>of());
        model.addAttribute("tongDon", tatCa.size());
        model.addAttribute("demTrangThai", demTrangThai);
        model.addAttribute("ttDangLoc", loc);
        model.addAttribute("trangHienTai", trangHienTai);
        model.addAttribute("tongTrang", tongTrang);
        model.addAttribute("tabActive", "don-hang");
        return "taikhoan/don-hang";
    }

    @GetMapping("/don-hang/{maHoaDon}")
    public String chiTietDonHang(@PathVariable String maHoaDon,
                                 Model model,
                                 Authentication authentication) {
        KhachHang kh = khachHangHienTai(authentication);
        HoaDon hd = hoaDonService.findById(maHoaDon);
        if (kh == null || hd == null || hd.getMaKhachHang() == null
                || !hd.getMaKhachHang().getMaKH().equals(kh.getMaKH())) {
            return "redirect:/ca-nhan/don-hang";
        }

        List<HoaDonChiTiet> chiTiet = hoaDonChiTietService.findByHoaDOn(hd);

        // Sản phẩm nào trong đơn còn được phép đánh giá (đơn Đã giao + chưa đánh giá)
        Map<String, Boolean> coTheDanhGia = new HashMap<>();
        List<Map<String, Object>> spDanhGia = new ArrayList<>();
        if ("Đã giao".equals(hd.getTrangThai())) {
            Map<String, Map<String, Object>> daGom = new java.util.LinkedHashMap<>();
            for (HoaDonChiTiet ct : chiTiet) {
                if (ct.getSanPhamChiTiet() == null || ct.getSanPhamChiTiet().getSanPham() == null) continue;
                var sp = ct.getSanPhamChiTiet().getSanPham();
                String anh = ct.getSanPhamChiTiet().getAnhDaiDien();
                daGom.computeIfAbsent(sp.getMaSanPham(), ma -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("ma", ma);
                    m.put("ten", sp.getTenSanPham());
                    m.put("anh", anh != null && !anh.isBlank()
                            ? "/images/" + anh : "/storefront/img/no-image.svg");
                    return m;
                });
            }
            for (Map<String, Object> m : daGom.values()) {
                String ma = (String) m.get("ma");
                boolean coThe = danhGiaService.khachCoTheDanhGia(kh.getMaKH(), ma);
                coTheDanhGia.put(ma, coThe);
                m.put("coThe", coThe);
                spDanhGia.add(m);
            }
        }

        model.addAttribute("hd", hd);
        model.addAttribute("chiTiet", chiTiet);
        model.addAttribute("coTheDanhGia", coTheDanhGia);
        model.addAttribute("spDanhGia", spDanhGia);
        model.addAttribute("tabActive", "don-hang");
        return "taikhoan/don-hang-chi-tiet";
    }

    /** Khách tự huỷ đơn "Chờ xác nhận" — hoàn tồn kho + voucher, báo realtime cho quản lý. */
    @PostMapping("/don-hang/huy/{maHoaDon}")
    public String huyDon(@PathVariable String maHoaDon,
                         Authentication authentication,
                         RedirectAttributes ra) {
        KhachHang kh = khachHangHienTai(authentication);
        HoaDon hd = hoaDonService.findById(maHoaDon);
        if (kh == null || hd == null || hd.getMaKhachHang() == null
                || !hd.getMaKhachHang().getMaKH().equals(kh.getMaKH())) {
            ra.addFlashAttribute("loiDonHang", "Đơn hàng không hợp lệ.");
            return "redirect:/ca-nhan/don-hang";
        }
        try {
            donHangOnlineService.khachHuyDon(hd);
            ra.addFlashAttribute("thongBao", "Đã huỷ đơn hàng " + maHoaDon + ". Tồn kho và mã giảm giá (nếu có) đã được hoàn lại.");
        } catch (DatHangException ex) {
            ra.addFlashAttribute("loiDonHang", ex.getMessage());
        }
        return "redirect:/ca-nhan/don-hang/" + maHoaDon;
    }

    /** Mua lại: nạp toàn bộ sản phẩm của đơn cũ vào giỏ (theo tồn kho hiện tại). */
    @PostMapping("/don-hang/mua-lai/{maHoaDon}")
    public String muaLai(@PathVariable String maHoaDon,
                         Authentication authentication,
                         RedirectAttributes ra) {
        KhachHang kh = khachHangHienTai(authentication);
        HoaDon hd = hoaDonService.findById(maHoaDon);
        if (kh == null || hd == null || hd.getMaKhachHang() == null
                || !hd.getMaKhachHang().getMaKH().equals(kh.getMaKH())) {
            ra.addFlashAttribute("loiDonHang", "Đơn hàng không hợp lệ.");
            return "redirect:/ca-nhan/don-hang";
        }

        List<String> canhBao = new ArrayList<>();
        int themDuoc = 0;
        for (HoaDonChiTiet ct : hoaDonChiTietService.findByHoaDOn(hd)) {
            if (ct.getSanPhamChiTiet() == null) continue;
            String loi = gioHangService.themVaoGio(gioHang,
                    ct.getSanPhamChiTiet().getMaSanPhamChiTiet(),
                    ct.getSoLuong() != null ? ct.getSoLuong() : 1);
            if (loi == null || loi.startsWith("Chỉ còn")) {
                themDuoc++;
                if (loi != null) canhBao.add(loi);
            } else {
                canhBao.add(loi);
            }
        }

        if (themDuoc > 0) {
            ra.addFlashAttribute("thongBaoGioHang", "Đã thêm " + themDuoc + " sản phẩm từ đơn " + maHoaDon + " vào giỏ hàng.");
        }
        if (!canhBao.isEmpty()) {
            ra.addFlashAttribute("canhBaoGioHang", canhBao);
        }
        return "redirect:/gio-hang";
    }

    // =====================================================================
    // VOUCHER CỦA TÔI
    // =====================================================================

    @GetMapping("/voucher")
    public String voucherCuaToi(Model model, Authentication authentication) {
        KhachHang kh = khachHangHienTai(authentication);
        if (kh == null) return "redirect:/";
        List<GiamGia> voucher = List.of();
        try {
            voucher = giamGiaService.getVoucherKhaDungChoKhachHang(kh.getMaKH());
        } catch (Exception ignored) { }
        model.addAttribute("voucherKhaDung", voucher);
        model.addAttribute("tabActive", "voucher");
        return "taikhoan/voucher";
    }
}