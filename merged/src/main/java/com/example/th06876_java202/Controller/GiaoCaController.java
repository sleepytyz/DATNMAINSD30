// GiaoCaController.java
package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.*;
import com.example.th06876_java202.Service.GiaoCaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Controller
@RequestMapping("/giao-ca")
@RequiredArgsConstructor
public class GiaoCaController {
    private final GiaoCaService giaoCaService;

    @GetMapping("/ca-lam-viec")
    public String showGiaoCa(Model model) {
        model.addAttribute("listCaLamViec", giaoCaService.findCaAll());
        return "giaoca/calamviec";
    }

    @GetMapping("/filter-ca")
    public String filterCa(
            @RequestParam(value = "tenCa", required = false) String tenCa,
            @RequestParam(value = "gioBatDau", required = false) String gioBatDau,
            @RequestParam(value = "gioKetThuc", required = false) String gioKetThuc,
            Model model) {

        List<CaLamViec> listCa;

        if (tenCa != null && !tenCa.isEmpty()) {
            listCa = giaoCaService.findCaByTenCa(tenCa);
        } else if (gioBatDau != null && !gioBatDau.isEmpty() && gioKetThuc != null && !gioKetThuc.isEmpty()) {
            LocalTime start = LocalTime.parse(gioBatDau);
            LocalTime end = LocalTime.parse(gioKetThuc);
            listCa = giaoCaService.findCaByGio(start, end);
        } else {
            listCa = giaoCaService.findCaAll();
        }

        model.addAttribute("listCaLamViec", listCa);
        return "giaoca/calamviec";
    }

    @GetMapping("/filter")
    public String filter(@RequestParam(value = "tenNhanVien", required = false) String tenNhanVien,
                         @RequestParam(value = "maCa", required = false) Integer maCa,
                         @RequestParam(value = "tuNgay", required = false) LocalDate tuNgay,
                         @RequestParam(value = "denNgay", required = false) LocalDate denNgay,
                         Model model){

        if (tuNgay != null && denNgay == null) {
            denNgay = tuNgay.plusDays(7);
        }

        if (tuNgay == null) {
            LocalDate homNay = LocalDate.now();
            tuNgay = homNay.minusDays(3);
            denNgay = tuNgay.plusDays(7);
        }

        model.addAttribute("today", LocalDate.now());

        List<CaLamViec> allCa = giaoCaService.findCaAll();
        model.addAttribute("allCa", allCa);

        // SỬA: Dùng ChamCong thay vì ChamCongController
        Map<LocalDate, Map<CaLamViec, List<ChamCong>>> chamCongTheoNgayVaCa =
                giaoCaService.getChamCongTheoNgayVaCa(tenNhanVien, maCa, tuNgay, denNgay);
        model.addAttribute("chamCongData", chamCongTheoNgayVaCa);

        List<LocalDate> listNgay = giaoCaService.getListNgayTrongKhoang(tuNgay, denNgay);
        model.addAttribute("listNgay", listNgay);

        model.addAttribute("listNhanVien", giaoCaService.findAllNhanVien());

        model.addAttribute("tuNgayValue", tuNgay);
        model.addAttribute("denNgayValue", denNgay);

        return "giaoca/lichnhanvien";
    }

    @GetMapping("/lich-nhan-vien")
    public String showLichNhanVien(Model model) {
        LocalDate homNay = LocalDate.now();
        LocalDate tuNgay = homNay.minusDays(3);
        LocalDate denNgay = tuNgay.plusDays(7);

        model.addAttribute("today", homNay);

        List<CaLamViec> allCa = giaoCaService.findCaAll();
        model.addAttribute("allCa", allCa);

        // SỬA: Dùng ChamCong thay vì ChamCongController
        Map<LocalDate, Map<CaLamViec, List<ChamCong>>> chamCongTheoNgayVaCa =
                giaoCaService.getChamCongTheoNgayVaCa(null, null, tuNgay, denNgay);
        model.addAttribute("chamCongData", chamCongTheoNgayVaCa);

        List<LocalDate> listNgay = giaoCaService.getListNgayTrongKhoang(tuNgay, denNgay);
        model.addAttribute("listNgay", listNgay);

        model.addAttribute("listNhanVien", giaoCaService.findAllNhanVien());

        model.addAttribute("tuNgayValue", tuNgay);
        model.addAttribute("denNgayValue", denNgay);

        return "giaoca/lichnhanvien";
    }

    @GetMapping("/tao-lich-hang-loat")
    public String showTaoLichHangLoat(
            @RequestParam(value = "tuan", required = false, defaultValue = "0") Integer tuan,
            Model model) {

        LocalDate homNay = LocalDate.now();
        // Lấy ngày đầu tuần (Thứ 2)
        LocalDate tuNgay = homNay.with(java.time.DayOfWeek.MONDAY);
        tuNgay = tuNgay.plusDays(tuan * 7L);
        LocalDate denNgay = tuNgay.plusDays(6);

        model.addAttribute("tuNgay", tuNgay);
        model.addAttribute("denNgay", denNgay);
        model.addAttribute("tuanHienTai", tuan);
        model.addAttribute("today", homNay);

        // Lấy danh sách nhân viên
        List<NhanVien> listNhanVien = giaoCaService.findAllNhanVien();
        model.addAttribute("listNhanVien", listNhanVien);

        // Lấy danh sách ca
        List<CaLamViec> listCa = giaoCaService.findCaAll();
        model.addAttribute("listCa", listCa);

        // Lấy danh sách ngày trong tuần
        List<LocalDate> listNgay = giaoCaService.getListNgayTrongKhoang(tuNgay, denNgay);
        model.addAttribute("listNgay", listNgay);

        return "giaoca/taolichhangloat";
    }

    @PostMapping("/tao-lich-hang-loat")
    @ResponseBody
    public ResponseEntity<?> taoLichHangLoat(@RequestBody LichLamViecDTO dto) {
        try {
            List<LichNhanVienDTO> danhSach = dto.getDanhSachNhanVien();

            if (danhSach == null || danhSach.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Vui lòng chọn ít nhất 1 nhân viên và ca làm việc"
                ));
            }

            // SỬA: Dùng ChamCong thay vì ChamCongController
            List<ChamCong> result = giaoCaService.taoLichLamViecHangLoatChiTiet(danhSach);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã tạo lịch thành công cho " + result.size() + " ca làm việc"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/xoa-lich")
    @ResponseBody
    public ResponseEntity<?> xoaLich(@RequestBody Map<String, Integer> payload) {
        try {
            Integer maChamCong = payload.get("maChamCong");
            if (maChamCong == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thiếu mã chấm công"
                ));
            }

            giaoCaService.xoaChamCong(maChamCong);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xóa lịch thành công"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/add-ca")
    @ResponseBody
    public ResponseEntity<?> addCa(@Valid @RequestBody CaLamViec caLamViec, BindingResult bindingResult) {
        try {
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                for (FieldError error : bindingResult.getFieldErrors()) {
                    errors.put(error.getField(), error.getDefaultMessage());
                }
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "errors", errors,
                        "message", "Vui lòng kiểm tra lại thông tin"
                ));
            }

            if (caLamViec.getGioBatDau() != null && caLamViec.getGioKetThuc() != null) {
                if (caLamViec.getGioBatDau().isAfter(caLamViec.getGioKetThuc()) ||
                        caLamViec.getGioBatDau().equals(caLamViec.getGioKetThuc())) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Giờ kết thúc phải sau giờ bắt đầu"
                    ));
                }
            }

            // Kiểm tra trùng tên ca
            List<CaLamViec> allCa = giaoCaService.findCaAll();
            boolean isDuplicate = allCa.stream()
                    .anyMatch(c -> c.getTenCa().equalsIgnoreCase(caLamViec.getTenCa().trim()));

            if (isDuplicate) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Tên ca '" + caLamViec.getTenCa() + "' đã tồn tại"
                ));
            }

            caLamViec.setTenCa(caLamViec.getTenCa().trim());
            if (caLamViec.getMoTa() != null) {
                caLamViec.setMoTa(caLamViec.getMoTa().trim());
            }
            giaoCaService.addCaLamViec(caLamViec);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thêm ca làm việc thành công"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/edit")
    @ResponseBody
    public ResponseEntity<?> editCa(@Valid @RequestBody CaLamViec caLamViec, BindingResult bindingResult) {
        try {
            // Kiểm tra validation từ entity
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                for (FieldError error : bindingResult.getFieldErrors()) {
                    errors.put(error.getField(), error.getDefaultMessage());
                }
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "errors", errors,
                        "message", "Vui lòng kiểm tra lại thông tin"
                ));
            }

            // Validate giờ bắt đầu < giờ kết thúc (business logic)
            if (caLamViec.getGioBatDau() != null && caLamViec.getGioKetThuc() != null) {
                if (caLamViec.getGioBatDau().isAfter(caLamViec.getGioKetThuc()) ||
                        caLamViec.getGioBatDau().equals(caLamViec.getGioKetThuc())) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Giờ kết thúc phải sau giờ bắt đầu"
                    ));
                }
            }

            // Kiểm tra trùng tên ca (trừ chính nó)
            CaLamViec existing = giaoCaService.findCaById(caLamViec.getMaCa());
            if (existing == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy ca làm việc"
                ));
            }

            // Kiểm tra tên ca đã tồn tại chưa (trừ chính nó)
            List<CaLamViec> allCa = giaoCaService.findCaAll();
            boolean isDuplicate = allCa.stream()
                    .anyMatch(c -> c.getTenCa().equalsIgnoreCase(caLamViec.getTenCa().trim())
                            && !c.getMaCa().equals(caLamViec.getMaCa()));

            if (isDuplicate) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Tên ca '" + caLamViec.getTenCa() + "' đã tồn tại"
                ));
            }

            giaoCaService.editCaLamViec(caLamViec);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật ca làm việc thành công"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }
}