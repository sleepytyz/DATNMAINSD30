package com.example.th06876_java202.realtime;

import com.example.th06876_java202.Service.HoaDonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API JSON phục vụ phần realtime bên Quản lý bán hàng
 * (nạp dữ liệu ban đầu khi tải trang; các cập nhật tiếp theo đi qua WebSocket).
 * Đường dẫn /api/quan-ly/** được giới hạn ADMIN/STAFF trong SecurityConfig.
 */
@RestController
@RequestMapping("/api/quan-ly")
@RequiredArgsConstructor
public class QuanLyRealtimeApiController {

    private final TrangThaiModuleService trangThaiModuleService;
    private final HoaDonService hoaDonService;

    /** Trạng thái hoạt động của từng module (kết quả kiểm tra gần nhất). */
    @GetMapping("/trang-thai-module")
    public Map<String, Object> trangThaiModule() {
        return trangThaiModuleService.layKetQuaGanNhat();
    }

    /** Bộ đếm đơn hàng theo trạng thái — hiển thị badge/thẻ đếm khi tải trang. */
    @GetMapping("/thong-ke-don-hang")
    public Map<String, Object> thongKeDonHang() {
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("choXacNhan", hoaDonService.countByTrangThai("Chờ xác nhận"));
        kq.put("daXacNhan", hoaDonService.countByTrangThai("Đã xác nhận"));
        kq.put("dangGiao", hoaDonService.countByTrangThai("Đang giao"));
        kq.put("daGiao", hoaDonService.countByTrangThai("Đã giao"));
        kq.put("daHuy", hoaDonService.countByTrangThai("Đã huỷ"));
        return kq;
    }
}
