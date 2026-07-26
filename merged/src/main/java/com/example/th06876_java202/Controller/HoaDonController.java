package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.HoaDon;
import com.example.th06876_java202.Entity.HoaDonChiTiet;
import com.example.th06876_java202.Service.ExcelExportService;
import com.example.th06876_java202.Service.HoaDonChiTietService;
import com.example.th06876_java202.Service.HoaDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/hoa-don")
public class HoaDonController {

    @Autowired
    private HoaDonService service;

    @Autowired
    private ExcelExportService excelExportService;

    private final HoaDonChiTietService hoaDonChiTietService;

    public HoaDonController(HoaDonChiTietService hoaDonChiTietService) {
        this.hoaDonChiTietService = hoaDonChiTietService;
    }

    @GetMapping("/index")
    public String index(
            @PageableDefault(size = 5, sort = "ngayTao", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String mahd,
            @RequestParam(required = false) String tt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay2,
            @RequestParam(required = false) String filterType,
            Model model) {

        model.addAttribute("activeMenu", "hoadon");

        List<String> allowedStatuses = Arrays.asList("Đã thanh toán", "Đã giao", "Đã trả hàng", "Đã huỷ");

        if (filterType != null && !filterType.isEmpty()) {
            LocalDate today = LocalDate.now();
            switch (filterType) {
                case "today":
                    ngay = today;
                    ngay2 = today;
                    break;
                case "yesterday":
                    ngay = today.minusDays(1);
                    ngay2 = today.minusDays(1);
                    break;
                case "week":
                    ngay = today.minusDays(7);
                    ngay2 = today;
                    break;
                case "month":
                    ngay = today.minusDays(30);
                    ngay2 = today;
                    break;
                case "thisMonth":
                    ngay = today.withDayOfMonth(1);
                    ngay2 = today;
                    break;
                default:
                    break;
            }
            model.addAttribute("filterType", filterType);
        }

        Page<HoaDon> page = null;
        if (tt != null && !tt.trim().isEmpty()) {
            if (allowedStatuses.contains(tt)) {
                page = service.findByTrangThai(tt, pageable);
            } else {
                page = service.findByTrangThaiIn(allowedStatuses, pageable);
            }
        } else if (ngay != null || ngay2 != null) {
            LocalDateTime startDateTime = ngay != null ? ngay.atStartOfDay() : null;
            LocalDateTime endDateTime = ngay2 != null ? ngay2.atTime(23, 59, 59) : null;
            page = service.searchByNgayTaodhAndStatus(startDateTime, endDateTime, allowedStatuses, pageable);
        } else {
            page = service.findByTrangThaiIn(allowedStatuses, pageable);
        }

        if (page == null) {
            page = service.findByTrangThaiIn(allowedStatuses, pageable);
        }

        model.addAttribute("list", page.getContent());
        model.addAttribute("currentPage", page.getNumber());
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("tt", tt);
        model.addAttribute("ngay", ngay);
        model.addAttribute("ngay2", ngay2);

        // Thống kê số lượng theo từng trạng thái
        model.addAttribute("totalDaThanhToan", service.countByTrangThai("Đã thanh toán"));
        model.addAttribute("totalDaGiao", service.countByTrangThai("Đã giao"));
        model.addAttribute("totalDaTraHang", service.countByTrangThai("Đã trả hàng"));
        model.addAttribute("totalDaHuy", service.countByTrangThai("Đã huỷ"));

        // ⭐ QUAN TRỌNG: Khởi tạo hd = null để tránh lỗi khi không có mahd
        model.addAttribute("hd", null);
        model.addAttribute("listsp", new ArrayList<>());
        model.addAttribute("hoaDon", new HoaDon());

        // Nếu có mahd thì lấy chi tiết
        if (mahd != null && !mahd.trim().isEmpty()) {
            HoaDon hd = service.findById(mahd);
            if (hd != null && allowedStatuses.contains(hd.getTrangThai())) {
                model.addAttribute("hd", hd);
                model.addAttribute("listsp", hoaDonChiTietService.findById(mahd));
            }
        }

        return "hoadon/index";
    }

    // =============================================
    // CHI TIẾT HÓA ĐƠN - TRANG RIÊNG
    // =============================================
    @GetMapping("/detail")
    public String detail(@RequestParam("mahd") String maHoaDon, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("activeMenu", "hoadon");

        HoaDon hd = service.findById(maHoaDon);
        if (hd == null) {
            redirectAttributes.addFlashAttribute("errorMess", "Không tìm thấy hóa đơn!");
            return "redirect:/hoa-don/index";
        }

        // Kiểm tra trạng thái hợp lệ cho hóa đơn
        List<String> allowedStatuses = Arrays.asList("Đã thanh toán", "Đã giao", "Đã trả hàng", "Đã huỷ");
        if (!allowedStatuses.contains(hd.getTrangThai())) {
            redirectAttributes.addFlashAttribute("errorMess", "Hóa đơn chưa hoàn tất, vui lòng kiểm tra lại!");
            return "redirect:/hoa-don/index";
        }

        model.addAttribute("hd", hd);

        // Lấy danh sách sản phẩm
        List<HoaDonChiTiet> chiTietList = hoaDonChiTietService.findById(maHoaDon);
        model.addAttribute("listsp", chiTietList != null ? chiTietList : new ArrayList<>());

        return "hoadon/detail";
    }

    @GetMapping("/export-excel")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam(required = false) String mahd,
            @RequestParam(required = false) String tt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay2,
            @RequestParam(required = false) String filterType) {

        try {
            System.out.println("========== EXPORT EXCEL HÓA ĐƠN ==========");
            System.out.println("mahd: [" + mahd + "]");
            System.out.println("tt: [" + tt + "]");
            System.out.println("ngay: [" + ngay + "]");
            System.out.println("ngay2: [" + ngay2 + "]");
            System.out.println("filterType: [" + filterType + "]");

            List<String> allowedStatuses = Arrays.asList("Đã thanh toán", "Đã giao", "Đã trả hàng", "Đã huỷ");

            if (filterType != null && !filterType.isEmpty()) {
                LocalDate today = LocalDate.now();
                switch (filterType) {
                    case "today":
                        ngay = today;
                        ngay2 = today;
                        break;
                    case "yesterday":
                        ngay = today.minusDays(1);
                        ngay2 = today.minusDays(1);
                        break;
                    case "week":
                        ngay = today.minusDays(7);
                        ngay2 = today;
                        break;
                    case "month":
                        ngay = today.minusDays(30);
                        ngay2 = today;
                        break;
                    case "thisMonth":
                        ngay = today.withDayOfMonth(1);
                        ngay2 = today;
                        break;
                    default:
                        break;
                }
            }

            List<HoaDon> hoaDonList;

            if (mahd != null && !mahd.trim().isEmpty()) {
                List<HoaDonChiTiet> chiTietList = hoaDonChiTietService.findById(mahd);
                if (chiTietList != null && !chiTietList.isEmpty()) {
                    ByteArrayInputStream in = excelExportService.exportChiTietHoaDonToExcel(chiTietList);
                    if (in == null) {
                        return ResponseEntity.badRequest().build();
                    }

                    String fileName = "Chi_tiet_hoa_don_HD" + mahd + "_" +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Content-Disposition", "attachment; filename=" + fileName);
                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(new InputStreamResource(in));
                }

                HoaDon hd = service.findById(mahd);
                if (hd != null && allowedStatuses.contains(hd.getTrangThai())) {
                    hoaDonList = List.of(hd);
                } else {
                    return ResponseEntity.badRequest().build();
                }
            } else {
                if (tt != null && !tt.trim().isEmpty() && allowedStatuses.contains(tt)) {
                    hoaDonList = service.findAllByTrangThai(tt);
                } else if (ngay != null || ngay2 != null) {
                    LocalDateTime startDateTime = ngay != null ? ngay.atStartOfDay() : null;
                    LocalDateTime endDateTime = ngay2 != null ? ngay2.atTime(23, 59, 59) : null;
                    hoaDonList = service.searchByNgayTaodhAndStatusList(startDateTime, endDateTime, allowedStatuses);
                } else {
                    hoaDonList = service.findByTrangThaiInList(allowedStatuses);
                }
            }

            if (hoaDonList == null || hoaDonList.isEmpty()) {
                System.out.println("⚠️ Không có dữ liệu để xuất!");
                return ResponseEntity.badRequest().build();
            }

            System.out.println("✅ Số lượng hóa đơn: " + hoaDonList.size());

            ByteArrayInputStream in = excelExportService.exportHoaDonToExcel(hoaDonList);

            if (in == null) {
                System.err.println("❌ InputStream bị null!");
                return ResponseEntity.badRequest().build();
            }

            String fileName = "Danh_sach_hoa_don_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + fileName);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(in));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi export Excel: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // =============================================
    // IN HÓA ĐƠN
    // =============================================
    @GetMapping("/inhoadon")
    public String inHoaDon(@RequestParam("mahd") String maHoaDon, Model model) {
        HoaDon hd = service.findById(maHoaDon);
        if (hd == null) {
            return "redirect:/hoa-don/index";
        }

        model.addAttribute("hd", hd);
        model.addAttribute("listsp", hoaDonChiTietService.findById(maHoaDon));

        return "hoadon/inhoadon";
    }
}