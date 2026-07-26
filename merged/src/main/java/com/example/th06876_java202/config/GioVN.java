package com.example.th06876_java202.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Tiện ích lấy NGÀY / GIỜ theo múi giờ Việt Nam (UTC+7).
 *
 * <p><b>Vì sao cần lớp này?</b><br>
 * Ứng dụng đang ép JVM chạy ở múi giờ UTC trong {@code main()}:
 * <pre>TimeZone.setDefault(TimeZone.getTimeZone("UTC"));</pre>
 * Việc đó là BẮT BUỘC để tránh lỗi
 * {@code Invalid value for NanoOfSecond (valid values 0 - 999999999): -87000000}
 * khi Hibernate đọc cột kiểu {@code TIME} của SQL Server (lỗi do múi giờ
 * "Asia/Saigon" dùng giờ LMT +07:06:40 có phần giây lẻ). Nếu bỏ dòng đó,
 * trang Lịch nhân viên / Chấm công sẽ lỗi 500.
 *
 * <p>Tác dụng phụ: {@code LocalTime.now()} sẽ trả về giờ UTC, tức LỆCH 7 TIẾNG
 * so với giờ Việt Nam. Hậu quả là nhân viên check-in muộn vẫn bị chấm "Đúng giờ".
 *
 * <p><b>Giải pháp:</b> giữ nguyên JVM ở UTC (để không lỗi TIME), nhưng mọi chỗ
 * cần "bây giờ là mấy giờ" thì gọi qua lớp này để lấy đúng giờ Việt Nam.
 */
public final class GioVN {

    /** Múi giờ Việt Nam. Dùng "Asia/Ho_Chi_Minh" (offset chẵn +07:00). */
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private GioVN() { }

    /** Ngày hôm nay theo giờ Việt Nam. */
    public static LocalDate ngayHomNay() {
        return LocalDate.now(ZONE);
    }

    /** Giờ hiện tại theo giờ Việt Nam, ĐÃ LÀM TRÒN VỀ PHÚT (bỏ giây & nano). */
    public static LocalTime gioHienTai() {
        return LocalTime.now(ZONE).withSecond(0).withNano(0);
    }

    /** Giờ hiện tại theo giờ Việt Nam, giữ nguyên giây (khi cần độ chính xác). */
    public static LocalTime gioHienTaiCoGiay() {
        return LocalTime.now(ZONE).withNano(0);
    }

    /** Ngày + giờ hiện tại theo giờ Việt Nam (bỏ nano). */
    public static LocalDateTime bayGio() {
        return LocalDateTime.now(ZONE).withNano(0);
    }
}
