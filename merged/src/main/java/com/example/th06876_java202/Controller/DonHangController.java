package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.HoaDon;
import com.example.th06876_java202.Entity.HoaDonChiTiet;
import com.example.th06876_java202.Service.HoaDonChiTietService;
import com.example.th06876_java202.Service.HoaDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/donhang")
public class DonHangController {

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private HoaDonChiTietService hoaDonChiTietService;

    @Autowired
    private com.example.th06876_java202.Storefront.DonHangOnlineService donHangOnlineService;

    private static final int PAGE_SIZE = 5;

    private static final List<String> ACTIVE_STATUSES = Arrays.asList("Chờ xác nhận", "Đã xác nhận", "Đang giao");

    @GetMapping("/index")
    public String index(
            @PageableDefault(size = PAGE_SIZE, sort = "ngayTao", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String mahd,
            @RequestParam(required = false) String tt,
            @RequestParam(required = false) String ngay,
            @RequestParam(required = false) String ngay2,
            Model model) {

        model.addAttribute("activeMenu", "donhang");
        model.addAttribute("pageSize", PAGE_SIZE);

        Page<HoaDon> page = null;

        try {
            if (tt != null && !tt.trim().isEmpty()) {
                // ⭐ CHỈ CHO PHÉP LỌC THEO 3 TRẠNG THÁI
                if (ACTIVE_STATUSES.contains(tt)) {
                    page = hoaDonService.findByTrangThai(tt, pageable);
                } else {
                    // Nếu chọn trạng thái khác, mặc định hiển thị 3 trạng thái
                    page = hoaDonService.findByTrangThaiIn(ACTIVE_STATUSES, pageable);
                }
            } else if ((ngay != null && !ngay.isEmpty()) || (ngay2 != null && !ngay2.isEmpty())) {
                // Lọc theo ngày - CHỈ LẤY 3 TRẠNG THÁI
                LocalDateTime ngayStart = null;
                LocalDateTime ngayEnd = null;

                if (ngay != null && !ngay.isEmpty()) {
                    ngayStart = LocalDateTime.parse(ngay + "T00:00:00");
                }
                if (ngay2 != null && !ngay2.isEmpty()) {
                    ngayEnd = LocalDateTime.parse(ngay2 + "T23:59:59");
                }

                page = hoaDonService.searchByNgayTaodhAndStatus(ngayStart, ngayEnd, ACTIVE_STATUSES, pageable);
            } else if (mahd != null && !mahd.trim().isEmpty()) {
                // Tìm theo mã - CHỈ LẤY 3 TRẠNG THÁI
                page = hoaDonService.searchByMaAndStatus(mahd, ACTIVE_STATUSES, pageable);
            } else {
                // ⭐ Mặc định: CHỈ HIỂN THỊ 3 TRẠNG THÁI
                page = hoaDonService.findByTrangThaiIn(ACTIVE_STATUSES, pageable);
            }
        } catch (Exception e) {
            page = hoaDonService.findByTrangThaiIn(ACTIVE_STATUSES, pageable);
        }

        if (page == null) {
            page = hoaDonService.findByTrangThaiIn(ACTIVE_STATUSES, pageable);
        }

        model.addAttribute("list", page.getContent());
        model.addAttribute("currentPage", page.getNumber());
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());

        // ⭐ Thống kê 3 trạng thái
        model.addAttribute("totalChoXacNhan", hoaDonService.countByTrangThai("Chờ xác nhận"));
        model.addAttribute("totalDaXacNhan", hoaDonService.countByTrangThai("Đã xác nhận"));
        model.addAttribute("totalDangGiao", hoaDonService.countByTrangThai("Đang giao"));

        // Giữ lại giá trị filter
        model.addAttribute("mahd", mahd);
        model.addAttribute("tt", tt);
        model.addAttribute("ngay", ngay);
        model.addAttribute("ngay2", ngay2);

        return "donhang/index";
    }

    // =============================================
    // CHI TIẾT ĐƠN HÀNG - TRANG RIÊNG
    // =============================================
    @GetMapping("/detail")
    public String detail(@RequestParam("mahd") String maHoaDon, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("activeMenu", "donhang");

        HoaDon hd = hoaDonService.findById(maHoaDon);
        if (hd == null) {
            redirectAttributes.addFlashAttribute("errorMess", "Không tìm thấy đơn hàng!");
            return "redirect:/donhang/index";
        }

        model.addAttribute("hd", hd);

        // Lấy danh sách sản phẩm
        List<HoaDonChiTiet> chiTietList = hoaDonChiTietService.findById(maHoaDon);
        model.addAttribute("listsp", chiTietList != null ? chiTietList : new ArrayList<>());

        return "donhang/detail";
    }

    // =============================================
    // CÁC HÀM XỬ LÝ TRẠNG THÁI
    // =============================================

    /** Chờ xác nhận -> Đã xác nhận */
    @GetMapping("/suatt")
    public String suatt(@RequestParam String mahd, RedirectAttributes redirectAttributes) {
        try {
            hoaDonService.suatt(mahd);
            redirectAttributes.addFlashAttribute("successMess", "Đã xác nhận đơn hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMess", "Lỗi: " + e.getMessage());
        }
        return "redirect:/donhang/detail?mahd=" + mahd;
    }

    /** Đã xác nhận -> Đang giao */
    @GetMapping("/suattdg")
    public String suattdg(@RequestParam String mahd, RedirectAttributes redirectAttributes) {
        try {
            hoaDonService.suattdg(mahd);
            redirectAttributes.addFlashAttribute("successMess", "Đã chuyển sang trạng thái Đang giao!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMess", "Lỗi: " + e.getMessage());
        }
        return "redirect:/donhang/detail?mahd=" + mahd;
    }

    /** Đang giao -> Đã giao */
    @GetMapping("/suattdgg")
    public String suattdgg(@RequestParam String mahd, RedirectAttributes redirectAttributes) {
        try {
            hoaDonService.suattdgg(mahd);
            redirectAttributes.addFlashAttribute("successMess", "Đã hoàn thành đơn hàng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMess", "Lỗi: " + e.getMessage());
        }
        return "redirect:/donhang/detail?mahd=" + mahd;
    }

    /** Huỷ đơn hàng (hoàn lại tồn kho) */
    @GetMapping("/huy")
    public String huy(@RequestParam String mahd, RedirectAttributes redirectAttributes) {
        try {
            HoaDon hd = hoaDonService.findById(mahd);
            if (hd != null) {
                donHangOnlineService.huyDonAdmin(hd);
                redirectAttributes.addFlashAttribute("successMess", "Đã huỷ đơn hàng " + mahd + " và hoàn lại tồn kho!");
            } else {
                redirectAttributes.addFlashAttribute("errorMess", "Không tìm thấy đơn hàng!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMess", "Lỗi: " + e.getMessage());
        }
        return "redirect:/donhang/detail?mahd=" + mahd;
    }

    // =============================================
    // API
    // =============================================

    @GetMapping("/api/detail")
    @ResponseBody
    public Map<String, Object> getOrderDetail(@RequestParam("mahd") String maHoaDon) {
        Map<String, Object> result = new HashMap<>();
        try {
            HoaDon hd = hoaDonService.findById(maHoaDon);
            if (hd != null) {
                result.put("success", true);
                result.put("maHoaDon", hd.getMaHoaDon());
                result.put("nhanVien", hd.getMaNhanVien() != null ? hd.getMaNhanVien().getHoTen() : "");
                result.put("khachHang", hd.getMaKhachHang() != null ?
                        hd.getMaKhachHang().getHoTen() + " - " + hd.getMaKhachHang().getSdt() : "Khách lẻ");
                result.put("tongTien", String.format("%,d", hd.getTongTien()));
                result.put("thanhToan", hd.getPhuongThucThanhToan() != null ? hd.getPhuongThucThanhToan() : "Chưa TT");
                result.put("trangThai", hd.getTrangThai());
                result.put("loaiHoaDon", hd.getLoaiBan());
                result.put("ngayTao", hd.getNgayTao() != null ?
                        hd.getNgayTao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
                result.put("ghiChu", hd.getGhiChu() != null ? hd.getGhiChu() : "");
                result.put("tienShip", hd.getTienShip() != null ? hd.getTienShip() : 0);

                if (hd.getMaGiamGia() != null) {
                    result.put("maGiamGia", hd.getMaGiamGia().getMaGiamGia());
                    result.put("tenGiamGia", hd.getMaGiamGia().getTenGiamGia());
                    result.put("giaTriGiam", hd.getMaGiamGia().getGiaTriGiam());
                }
            } else {
                result.put("success", false);
                result.put("message", "Không tìm thấy đơn hàng");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/api/products")
    @ResponseBody
    public List<Map<String, Object>> getProducts(@RequestParam("mahd") String maHoaDon) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            List<HoaDonChiTiet> chiTietList = hoaDonChiTietService.findById(maHoaDon);
            if (chiTietList != null) {
                for (HoaDonChiTiet ct : chiTietList) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("tenSanPham", ct.getSanPhamChiTiet().getSanPham().getTenSanPham());
                    item.put("kichThuoc", ct.getSanPhamChiTiet().getKichThuoc().getTenKichThuoc());
                    item.put("mauSac", ct.getSanPhamChiTiet().getMauSac().getTenMauSac());
                    item.put("soLuong", ct.getSoLuong());
                    item.put("donGia", ct.getDonGia());
                    item.put("thanhTien", ct.getThanhTien());
                    result.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // =============================================
    // IN HOÁ ĐƠN
    // =============================================
    @GetMapping("/inhoadon")
    public String inHoaDon(@RequestParam("mahd") String maHoaDon, Model model) {
        HoaDon hd = hoaDonService.findById(maHoaDon);
        if (hd == null) {
            return "redirect:/donhang/index";
        }

        model.addAttribute("hd", hd);
        model.addAttribute("listsp", hoaDonChiTietService.findById(maHoaDon));

        return "donhang/inhoadon";
    }
}