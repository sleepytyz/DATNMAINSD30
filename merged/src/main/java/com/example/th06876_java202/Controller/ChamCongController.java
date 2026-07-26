package com.example.th06876_java202.Controller;

import com.example.th06876_java202.config.GioVN;
import com.example.th06876_java202.Entity.CaLamViec;
import com.example.th06876_java202.Entity.ChamCong;
import com.example.th06876_java202.Entity.NhanVien;
import com.example.th06876_java202.Service.ChamCongService;
import com.example.th06876_java202.Service.GiaoCaService;
import com.example.th06876_java202.Service.NhanVienService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Controller
@RequestMapping("/cham-cong")
@RequiredArgsConstructor
public class ChamCongController {

    private final ChamCongService chamCongService;
    private final GiaoCaService giaoCaService;
    private final NhanVienService nhanVienService;

    @GetMapping
    public String showChamCong(@RequestParam(value = "tuan", required = false, defaultValue = "0") Integer tuan,
                               Model model) {
        NhanVien nhanVien = getCurrentNhanVien();
        if (nhanVien == null) {
            // Tài khoản đã đăng nhập hợp lệ nhưng chưa được gán hồ sơ nhân viên (thiếu liên kết
            // NhanVien.MaTaiKhoan). Trước đây code redirect thẳng về /login khiến người dùng
            // tưởng bị đăng xuất/hết phiên. Giờ hiển thị thông báo rõ ràng ngay trên trang.
            model.addAttribute("nhanVien", null);
            model.addAttribute("chamCongMap", java.util.Collections.emptyMap());
            model.addAttribute("chamCongHomNay", java.util.Collections.emptyList());
            model.addAttribute("listNgay", java.util.Collections.emptyList());
            model.addAttribute("allCa", java.util.Collections.emptyList());
            model.addAttribute("today", GioVN.ngayHomNay());
            model.addAttribute("tuanHienTai", tuan);
            model.addAttribute("soNgayDaCham", 0L);
            model.addAttribute("tongSoGioLam", 0.0);
            return "chamcong/chamcong";
        }

        LocalDate today = GioVN.ngayHomNay();
        LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY).plusDays(tuan * 7L);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        List<ChamCong> listChamCong = chamCongService.getChamCongByNhanVienAndDateRange(
                nhanVien.getMaNhanVien(), startOfWeek, endOfWeek);

        Map<LocalDate, Map<CaLamViec, ChamCong>> chamCongMap = new LinkedHashMap<>();
        List<CaLamViec> allCa = giaoCaService.findCaAll();

        for (LocalDate date = startOfWeek; !date.isAfter(endOfWeek); date = date.plusDays(1)) {
            Map<CaLamViec, ChamCong> caMap = new LinkedHashMap<>();
            for (CaLamViec ca : allCa) {
                caMap.put(ca, null);
            }
            chamCongMap.put(date, caMap);
        }

        for (ChamCong cc : listChamCong) {
            if (cc.getNgayChamCong() != null && cc.getCaLamViec() != null) {
                Map<CaLamViec, ChamCong> caMap = chamCongMap.get(cc.getNgayChamCong());
                if (caMap != null && caMap.containsKey(cc.getCaLamViec())) {
                    caMap.put(cc.getCaLamViec(), cc);
                }
            }
        }

        // [SỬA GỐC] Không so sánh ngày trong template nữa (Thymeleaf so LocalDate rất
        // dễ sai). Tính sẵn tập các mã chấm công (maChamCong) mà ngày trùng HÔM NAY
        // ngay tại Java, rồi truyền xuống. Gán cờ laHomNay cho từng bản ghi để template
        // chỉ cần đọc chamCong.laHomNay (tránh Set.contains dễ sai kiểu Integer/Long).
        for (ChamCong cc : listChamCong) {
            boolean laHomNay = cc.getNgayChamCong() != null
                    && cc.getNgayChamCong().isEqual(today);
            cc.setLaHomNay(laHomNay);
        }

        long soNgayDaCham = listChamCong.stream()
                .filter(cc -> cc.getGioVao() != null)
                .map(ChamCong::getNgayChamCong)
                .distinct()
                .count();

        double tongSoGioLam = listChamCong.stream()
                .filter(cc -> cc.getSoGioLam() != null)
                .mapToDouble(cc -> cc.getSoGioLam().doubleValue())
                .sum();

        List<LocalDate> listNgay = new ArrayList<>();
        for (LocalDate date = startOfWeek; !date.isAfter(endOfWeek); date = date.plusDays(1)) {
            listNgay.add(date);
        }

        // [THÊM] Danh sách ca cần chấm công của RIÊNG HÔM NAY, tính sẵn ở Java để hiện
        // nút chấm công nổi bật ở đầu trang. Không phụ thuộc vào việc so ngày trong
        // Thymeleaf hay truy cập map lồng (nguồn gốc khiến nút không hiện trước đây).
        List<ChamCong> chamCongHomNay = listChamCong.stream()
                .filter(cc -> cc.getNgayChamCong() != null && cc.getNgayChamCong().isEqual(today))
                .sorted((a, b) -> {
                    java.time.LocalTime ta = a.getCaLamViec() != null ? a.getCaLamViec().getGioBatDau() : null;
                    java.time.LocalTime tb = b.getCaLamViec() != null ? b.getCaLamViec().getGioBatDau() : null;
                    if (ta == null) return 1;
                    if (tb == null) return -1;
                    return ta.compareTo(tb);
                })
                .toList();
        model.addAttribute("chamCongHomNay", chamCongHomNay);

        model.addAttribute("nhanVien", nhanVien);
        model.addAttribute("chamCongMap", chamCongMap);
        model.addAttribute("listNgay", listNgay);
        model.addAttribute("allCa", allCa);
        model.addAttribute("today", today);
        model.addAttribute("startOfWeek", startOfWeek);
        model.addAttribute("endOfWeek", endOfWeek);
        model.addAttribute("tuanHienTai", tuan);
        model.addAttribute("soNgayDaCham", soNgayDaCham);
        model.addAttribute("tongSoGioLam", Math.round(tongSoGioLam * 10) / 10.0);

        // SỬA: Trả về đúng đường dẫn
        return "chamcong/chamcong";
    }

    @PostMapping("/checkin")
    @ResponseBody
    public ResponseEntity<?> checkin(@RequestBody Map<String, Integer> payload) {
        try {
            Integer maChamCong = payload.get("maChamCong");
            if (maChamCong == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thiếu mã chấm công"
                ));
            }

            NhanVien nhanVien = getCurrentNhanVien();
            if (nhanVien == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Vui lòng đăng nhập lại"
                ));
            }

            ChamCong result = chamCongService.checkin(maChamCong, nhanVien.getMaNhanVien());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Chấm công vào thành công lúc " + result.getGioVao(),
                    "gioVao", result.getGioVao().toString(),
                    "trangThai", "Đã chấm công"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/checkout")
    @ResponseBody
    public ResponseEntity<?> checkout(@RequestBody Map<String, Integer> payload) {
        try {
            Integer maChamCong = payload.get("maChamCong");
            if (maChamCong == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thiếu mã chấm công"
                ));
            }

            NhanVien nhanVien = getCurrentNhanVien();
            if (nhanVien == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Vui lòng đăng nhập lại"
                ));
            }

            ChamCong result = chamCongService.checkout(maChamCong, nhanVien.getMaNhanVien());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Chấm công ra thành công lúc " + result.getGioRa(),
                    "gioRa", result.getGioRa().toString(),
                    "soGioLam", result.getSoGioLam().toString(),
                    "trangThai", "Đã kết thúc"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    private NhanVien getCurrentNhanVien() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String username = auth.getName();
        return nhanVienService.findByUsername(username);
    }
}