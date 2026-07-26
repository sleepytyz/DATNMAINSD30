package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.ChamCong;
import com.example.th06876_java202.Entity.HoaDon;
import com.example.th06876_java202.Entity.NhanVien;
import com.example.th06876_java202.Entity.PhienBanHang;
import com.example.th06876_java202.Service.NhanVienService;
import com.example.th06876_java202.Service.PhienBanHangService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GIAO CA - QUẢN LÝ QUỸ TIỀN TẠI QUẦY
 *
 * Luồng:
 *   GET  /giao-ca/phien       -> màn hình ca của tôi (mở ca / đang trong ca / đóng ca)
 *   POST /giao-ca/mo-ca       -> mở ca (nhập tiền đầu ca)
 *   POST /giao-ca/dong-ca     -> đóng ca (nhập tiền cuối ca + đối soát)
 *   GET  /giao-ca/lich-su     -> lịch sử các phiên của tôi
 *   GET  /giao-ca/giam-sat    -> (ADMIN) theo dõi ai đang trong ca + đối soát
 */
@Controller
@RequestMapping("/giao-ca")
@RequiredArgsConstructor
public class PhienBanHangController {

    private final PhienBanHangService phienService;
    private final NhanVienService nhanVienService;

    /** Lấy nhân viên đang đăng nhập; null nếu tài khoản chưa gắn hồ sơ nhân viên */
    private NhanVien nhanVienHienTai(Authentication auth) {
        if (auth == null) return null;
        return nhanVienService.findByUsername(auth.getName());
    }

    // ==================================================================
    // MÀN HÌNH "CA CỦA TÔI"
    // ==================================================================
    @GetMapping("/phien")
    public String manHinhPhien(Model model, Authentication auth) {
        model.addAttribute("activeMenu", "phienbanhang");

        NhanVien nv = nhanVienHienTai(auth);
        if (nv == null) {
            model.addAttribute("loi",
                    "Tài khoản của bạn chưa được liên kết với hồ sơ nhân viên nên không thể mở ca.");
            return "giaoca/phien";
        }
        model.addAttribute("nhanVien", nv);

        PhienBanHang phien = phienService.layPhienDangMo(nv.getMaNhanVien());
        if (phien != null) {
            // Đang trong ca -> tính doanh thu hiện tại để nhân viên thấy trước lúc đóng ca
            phienService.tinhDoanhThuHienTai(phien);
            List<HoaDon> hoaDons = phienService.hoaDonTrongPhien(phien);
            model.addAttribute("phien", phien);
            model.addAttribute("hoaDonTrongCa", hoaDons);
        } else {
            // Chưa mở ca -> kiểm tra có đủ điều kiện mở không (có lịch hôm nay chưa)
            ChamCong caHopLe = phienService.layCaHopLeDeMoCa(nv.getMaNhanVien());
            model.addAttribute("caHopLe", caHopLe);
            // Tiền bàn giao từ ca trước (hiển thị trong popup mở ca)
            model.addAttribute("tienCaTruoc", phienService.tienBanGiaoTuCaTruoc(nv.getMaNhanVien()));
        }

        return "giaoca/phien";
    }

    // ==================================================================
    // MỞ CA
    // ==================================================================
    @PostMapping("/mo-ca")
    @ResponseBody
    public Map<String, Object> moCa(@RequestBody Map<String, Object> payload, Authentication auth) {
        Map<String, Object> res = new HashMap<>();
        try {
            NhanVien nv = nhanVienHienTai(auth);
            if (nv == null) {
                res.put("success", false);
                res.put("message", "Tài khoản chưa liên kết hồ sơ nhân viên.");
                return res;
            }

            BigDecimal tienDauCa = docTien(payload.get("tienDauCa"));
            String ghiChu = payload.get("ghiChu") != null ? payload.get("ghiChu").toString() : null;

            PhienBanHang phien = phienService.moCa(nv, tienDauCa, ghiChu);

            res.put("success", true);
            res.put("message", "Mở ca thành công! Bạn có thể bắt đầu bán hàng.");
            res.put("maPhien", phien.getMaPhien());
        } catch (IllegalArgumentException | IllegalStateException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi mở ca: " + e.getMessage());
        }
        return res;
    }

    // ==================================================================
    // XEM TRƯỚC ĐỐI SOÁT (trước khi đóng ca)
    // ==================================================================
    @GetMapping("/tong-ket")
    @ResponseBody
    public Map<String, Object> tongKet(Authentication auth) {
        Map<String, Object> res = new HashMap<>();
        NhanVien nv = nhanVienHienTai(auth);
        if (nv == null) {
            res.put("success", false);
            res.put("message", "Không xác định được nhân viên.");
            return res;
        }
        PhienBanHang phien = phienService.layPhienDangMo(nv.getMaNhanVien());
        if (phien == null) {
            res.put("success", false);
            res.put("message", "Bạn không có ca nào đang mở.");
            return res;
        }
        phienService.tinhDoanhThuHienTai(phien);

        res.put("success", true);
        res.put("tienDauCa", phien.getTienDauCa());
        res.put("doanhThuTienMat", phien.getDoanhThuTienMat());
        res.put("doanhThuChuyenKhoan", phien.getDoanhThuChuyenKhoan());
        res.put("soHoaDon", phien.getSoHoaDon());
        res.put("tienDuKien", phien.getTienDuKien());
        return res;
    }

    // ==================================================================
    // ĐÓNG CA + ĐỐI SOÁT
    // ==================================================================
    @PostMapping("/dong-ca")
    @ResponseBody
    public Map<String, Object> dongCa(@RequestBody Map<String, Object> payload, Authentication auth) {
        Map<String, Object> res = new HashMap<>();
        try {
            NhanVien nv = nhanVienHienTai(auth);
            if (nv == null) {
                res.put("success", false);
                res.put("message", "Tài khoản chưa liên kết hồ sơ nhân viên.");
                return res;
            }

            BigDecimal tienCuoiCa = docTien(payload.get("tienCuoiCaThucTe"));
            String ghiChu = payload.get("ghiChu") != null ? payload.get("ghiChu").toString() : null;
            String lyDoThieuQuy = payload.get("lyDoThieuQuy") != null
                    ? payload.get("lyDoThieuQuy").toString() : null;

            PhienBanHang phien = phienService.dongCa(nv.getMaNhanVien(), tienCuoiCa, ghiChu, lyDoThieuQuy);

            BigDecimal chenhLech = phien.getChenhLech();
            res.put("success", true);
            String thongBao = "Đã đóng ca. " + phienService.moTaChenhLech(chenhLech) + ".";
            if (phien.isChoDuyet()) {
                thongBao += " Ca này thiếu quỹ vượt ngưỡng, đã ghi nhận lý do và chuyển quản lý duyệt.";
            }
            res.put("message", thongBao);
            res.put("tienDauCa", phien.getTienDauCa());
            res.put("doanhThuTienMat", phien.getDoanhThuTienMat());
            res.put("doanhThuChuyenKhoan", phien.getDoanhThuChuyenKhoan());
            res.put("tienDuKien", phien.getTienDuKien());
            res.put("tienCuoiCaThucTe", phien.getTienCuoiCaThucTe());
            res.put("chenhLech", chenhLech);
            res.put("moTaChenhLech", phienService.moTaChenhLech(chenhLech));
            res.put("canDuyet", phien.isChoDuyet());
        } catch (IllegalArgumentException | IllegalStateException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi đóng ca: " + e.getMessage());
        }
        return res;
    }

    // ==================================================================
    // LỊCH SỬ PHIÊN CỦA TÔI
    // ==================================================================
    @GetMapping("/lich-su")
    public String lichSu(Model model, Authentication auth) {
        model.addAttribute("activeMenu", "phienbanhang");
        NhanVien nv = nhanVienHienTai(auth);
        if (nv == null) {
            model.addAttribute("loi", "Tài khoản chưa liên kết hồ sơ nhân viên.");
            model.addAttribute("danhSachPhien", List.of());
            return "giaoca/lichsuphien";
        }
        model.addAttribute("nhanVien", nv);
        model.addAttribute("danhSachPhien", phienService.lichSu(nv.getMaNhanVien()));
        return "giaoca/lichsuphien";
    }

    // ==================================================================
    // LỊCH LÀM VIỆC CỦA TÔI (STAFF)
    // ==================================================================
    @GetMapping("/lich-cua-toi")
    public String lichCuaToi(Model model,
                             @RequestParam(required = false) String tuNgay,
                             @RequestParam(required = false) String denNgay,
                             Authentication auth) {
        model.addAttribute("activeMenu", "lichcuatoi");

        NhanVien nv = nhanVienHienTai(auth);
        if (nv == null) {
            model.addAttribute("loi", "Tài khoản chưa liên kết hồ sơ nhân viên.");
            model.addAttribute("danhSachLich", List.of());
            return "giaoca/lichcuatoi";
        }
        model.addAttribute("nhanVien", nv);

        // Mặc định: 30 ngày gần nhất
        java.time.LocalDate den = (denNgay != null && !denNgay.isBlank())
                ? java.time.LocalDate.parse(denNgay) : java.time.LocalDate.now().plusDays(7);
        java.time.LocalDate tu = (tuNgay != null && !tuNgay.isBlank())
                ? java.time.LocalDate.parse(tuNgay) : den.minusDays(30);

        model.addAttribute("danhSachLich", phienService.lichLamViecCuaToi(nv.getMaNhanVien(), tu, den));
        model.addAttribute("tuNgay", tu);
        model.addAttribute("denNgay", den);
        model.addAttribute("homNay", java.time.LocalDate.now());
        return "giaoca/lichcuatoi";
    }

    /** Chi tiết 1 ca: hoá đơn nhân viên đã tạo trong ca đó (lưu vết hoạt động) */
    @GetMapping("/chi-tiet-ca/{maPhien}")
    @ResponseBody
    public Map<String, Object> chiTietCa(@PathVariable Integer maPhien) {
        Map<String, Object> res = new HashMap<>();
        PhienBanHang phien = phienService.timTheoMa(maPhien);
        if (phien == null) {
            res.put("success", false);
            res.put("message", "Không tìm thấy ca.");
            return res;
        }
        List<HoaDon> hoaDons = phienService.hoaDonTrongPhien(phien);
        List<Map<String, Object>> ds = new java.util.ArrayList<>();
        for (HoaDon hd : hoaDons) {
            Map<String, Object> m = new HashMap<>();
            m.put("maHoaDon", hd.getMaHoaDon());
            m.put("thoiGian", hd.getNgayTao() != null
                    ? hd.getNgayTao().toLocalTime().withNano(0).toString() : "");
            m.put("phuongThuc", hd.getPhuongThucThanhToan());
            m.put("tongTien", hd.getTongTien());
            ds.add(m);
        }
        res.put("success", true);
        res.put("nhanVien", phien.getNhanVien() != null ? phien.getNhanVien().getHoTen() : "-");
        res.put("soHoaDon", ds.size());
        res.put("hoaDons", ds);
        return res;
    }

    // ==================================================================
    // ADMIN: GIÁM SÁT CA ĐANG MỞ + ĐỐI SOÁT
    // ==================================================================
    @GetMapping("/giam-sat")
    public String giamSat(Model model,
                          @RequestParam(required = false) String tuNgay,
                          @RequestParam(required = false) String denNgay) {
        model.addAttribute("activeMenu", "phienbanhang");

        // Các ca đang mở (ai đang trong ca)
        List<PhienBanHang> dangMo = phienService.layTatCaPhienDangMo();
        dangMo.forEach(phienService::tinhDoanhThuHienTai);
        model.addAttribute("phienDangMo", dangMo);

        // Lịch sử đối soát theo khoảng (mặc định 7 ngày gần nhất)
        LocalDateTime denLuc = (denNgay != null && !denNgay.isBlank())
                ? java.time.LocalDate.parse(denNgay).atTime(23, 59, 59)
                : LocalDateTime.now();
        LocalDateTime tuLuc = (tuNgay != null && !tuNgay.isBlank())
                ? java.time.LocalDate.parse(tuNgay).atStartOfDay()
                : denLuc.minusDays(7);

        model.addAttribute("lichSuPhien", phienService.lichSuTheoKhoang(tuLuc, denLuc));
        model.addAttribute("tuNgay", tuLuc.toLocalDate());
        model.addAttribute("denNgay", denLuc.toLocalDate());

        // Các ca thiếu quỹ đang chờ admin duyệt + tồn quỹ hiện có trong két
        model.addAttribute("caChoDuyet", phienService.layCacCaChoDuyet());
        model.addAttribute("tonQuy", phienService.layTonQuyHienTai());

        return "giaoca/giamsatca";
    }

    // ==================================================================
    // ADMIN: DUYỆT THIẾU QUỸ (chỉ ADMIN - do rule /giao-ca/** = hasRole ADMIN)
    // ==================================================================
    @PostMapping("/duyet-thieu-quy")
    @ResponseBody
    public Map<String, Object> duyetThieuQuy(@RequestBody Map<String, Object> payload, Authentication auth) {
        Map<String, Object> res = new HashMap<>();
        try {
            Integer maPhien = Integer.valueOf(payload.get("maPhien").toString());
            String ghiChu = payload.get("ghiChu") != null ? payload.get("ghiChu").toString() : null;
            String tenAdmin = auth != null ? auth.getName() : "admin";

            phienService.adminDuyetThieuQuy(maPhien, tenAdmin, ghiChu);
            res.put("success", true);
            res.put("message", "Đã duyệt khoản thiếu quỹ của ca #" + maPhien + ".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi duyệt: " + e.getMessage());
        }
        return res;
    }

    // ==================================================================
    // ADMIN: CHỐT / THU TIỀN KÉT (chỉ ADMIN được rút tiền khỏi két)
    // ==================================================================
    @PostMapping("/chot-ket")
    @ResponseBody
    public Map<String, Object> chotKet(@RequestBody Map<String, Object> payload, Authentication auth) {
        Map<String, Object> res = new HashMap<>();
        try {
            BigDecimal soTienThu = docTien(payload.get("soTienThu"));
            String ghiChu = payload.get("ghiChu") != null ? payload.get("ghiChu").toString() : null;
            String tenAdmin = auth != null ? auth.getName() : "admin";

            PhienBanHang phien = phienService.adminChotThuTienKet(soTienThu, tenAdmin, ghiChu);
            res.put("success", true);
            res.put("message", "Đã thu " + soTienThu.toPlainString() + "đ khỏi két.");
            res.put("tonQuyConLai", phienService.layTonQuyHienTai());
        } catch (IllegalArgumentException | IllegalStateException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi chốt két: " + e.getMessage());
        }
        return res;
    }

    /** Đọc số tiền từ payload (chấp nhận số hoặc chuỗi "1.000.000") */
    private BigDecimal docTien(Object raw) {
        if (raw == null) return null;
        String s = raw.toString().trim();
        if (s.isEmpty()) return null;
        // bỏ dấu phân cách nghìn và khoảng trắng
        s = s.replace(".", "").replace(",", "").replace(" ", "").replace("đ", "");
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số tiền không hợp lệ: " + raw);
        }
    }
}
