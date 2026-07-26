package com.example.th06876_java202.Storefront;

import com.example.th06876_java202.Entity.HoTroTinNhan;
import com.example.th06876_java202.Entity.KhachHang;
import com.example.th06876_java202.Repository.HoTroTinNhanRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dịch vụ CHAT HỖ TRỢ: nhận tin của khách → lưu → phát realtime → để CHATBOT
 * trả lời (trừ khi nhân viên đang trực tiếp hỗ trợ phiên đó) → nhân viên có thể
 * nhảy vào trả lời bất cứ lúc nào từ module "Hỗ trợ trực tuyến".
 *
 * Kênh realtime:
 *  - /topic/hotro/phien/{maPhien} : mọi tin nhắn của 1 phiên (widget khách + màn nhân viên đang mở phiên)
 *  - /topic/hotro/quanly          : bản sao mọi tin nhắn → module quản lý cập nhật danh sách + chuông báo
 */
@Service
@RequiredArgsConstructor
public class HoTroChatService {

    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm dd/MM");
    /** Nhân viên vừa nhắn trong khoảng này → bot im lặng, nhường người thật. */
    private static final Duration NHAN_VIEN_DANG_TRUC = Duration.ofMinutes(30);

    private final HoTroTinNhanRepository repo;
    private final ChatBotService chatBotService;
    private final SimpMessagingTemplate messagingTemplate;

    /* ================= phiên ================= */

    /** Xác định mã phiên: khách đăng nhập = KH-<mã>, khách vãng lai = GUEST-xxxx (theo session). */
    public String maPhien(HttpSession session, KhachHang kh) {
        if (kh != null && kh.getMaKH() != null) return "KH-" + kh.getMaKH();
        Object cu = session.getAttribute("FS_CHAT_PHIEN");
        if (cu instanceof String s && !s.isBlank()) return s;
        String moi = "GUEST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        session.setAttribute("FS_CHAT_PHIEN", moi);
        return moi;
    }

    public String tenHienThi(String maPhien, KhachHang kh) {
        if (kh != null && kh.getHoTen() != null && !kh.getHoTen().isBlank()) return kh.getHoTen();
        if (maPhien != null && maPhien.startsWith("GUEST-")) {
            return "Khách vãng lai #" + maPhien.substring(Math.max(6, maPhien.length() - 4));
        }
        return "Khách hàng";
    }

    /* ================= luồng tin nhắn ================= */

    /** Khách gửi 1 tin → lưu, phát realtime, để bot xử lý nếu phù hợp. Trả về các tin mới tạo. */
    @Transactional
    public List<Map<String, Object>> khachGui(String maPhien, KhachHang kh, String tenHienThi, String noiDung) {
        List<Map<String, Object>> ketQua = new ArrayList<>();
        if (noiDung == null || noiDung.isBlank()) return ketQua;
        String sach = noiDung.trim();
        if (sach.length() > 1000) sach = sach.substring(0, 1000);

        HoTroTinNhan tinKhach = luuVaPhat(maPhien, kh, tenHienThi, "KHACH", sach);
        ketQua.add(toMap(tinKhach));

        // Khách muốn gặp người thật → bot xác nhận chuyển tiếp rồi im lặng
        if (chatBotService.muonGapNhanVien(sach)) {
            HoTroTinNhan tinBot = luuVaPhat(maPhien, kh, tenHienThi, "BOT",
                    chatBotService.traLoiChuyenNhanVien());
            ketQua.add(toMap(tinBot));
            return ketQua;
        }

        // Nhân viên đang trực tiếp hỗ trợ phiên này → bot không chen ngang
        HoTroTinNhan nvGanNhat = repo.findTopByMaPhienAndNguoiGuiOrderByMaTinNhanDesc(maPhien, "NHANVIEN");
        if (nvGanNhat != null && nvGanNhat.getThoiGian() != null
                && Duration.between(nvGanNhat.getThoiGian(), LocalDateTime.now())
                .compareTo(NHAN_VIEN_DANG_TRUC) < 0) {
            return ketQua;
        }

        // Bot trả lời tự động
        String traLoi;
        try {
            traLoi = chatBotService.traLoi(sach, kh);
        } catch (Exception e) {
            traLoi = "Xin lỗi, mình gặp trục trặc nhỏ khi tra cứu 😅. "
                    + "Bạn thử hỏi lại, hoặc nhắn \"gặp nhân viên\" để được hỗ trợ trực tiếp nhé!";
        }
        HoTroTinNhan tinBot = luuVaPhat(maPhien, kh, tenHienThi, "BOT", traLoi);
        ketQua.add(toMap(tinBot));
        return ketQua;
    }

    /** Nhân viên trả lời 1 phiên từ module Hỗ trợ trực tuyến. */
    @Transactional
    public Map<String, Object> nhanVienTraLoi(String maPhien, String noiDung, String tenNhanVien) {
        String sach = noiDung == null ? "" : noiDung.trim();
        if (sach.isEmpty()) return null;
        if (sach.length() > 1000) sach = sach.substring(0, 1000);
        repo.danhDauDaXem(maPhien);
        HoTroTinNhan tin = luuVaPhat(maPhien, null, tenNhanVien, "NHANVIEN", sach);
        return toMap(tin);
    }

    @Transactional
    public void danhDauDaXem(String maPhien) {
        repo.danhDauDaXem(maPhien);
    }

    private HoTroTinNhan luuVaPhat(String maPhien, KhachHang kh, String tenHienThi,
                                   String nguoiGui, String noiDung) {
        HoTroTinNhan t = new HoTroTinNhan();
        t.setMaPhien(maPhien);
        t.setMaKhachHang(kh != null ? kh.getMaKH() : null);
        t.setTenHienThi(tenHienThi);
        t.setNguoiGui(nguoiGui);
        t.setNoiDung(noiDung);
        t.setThoiGian(LocalDateTime.now());
        t.setDaXem(!"KHACH".equals(nguoiGui));   // tin của khách = chưa đọc với nhân viên
        t = repo.save(t);

        Map<String, Object> payload = toMap(t);
        // 1 phiên cụ thể (widget khách + nhân viên đang mở phiên)
        messagingTemplate.convertAndSend("/topic/hotro/phien/" + maPhien, payload);
        // kênh tổng cho module quản lý (danh sách hội thoại + chuông báo)
        messagingTemplate.convertAndSend("/topic/hotro/quanly", payload);
        return t;
    }

    /* ================= dữ liệu cho giao diện ================= */

    public List<Map<String, Object>> lichSu(String maPhien) {
        List<Map<String, Object>> ds = new ArrayList<>();
        for (HoTroTinNhan t : repo.findByMaPhienOrderByMaTinNhanAsc(maPhien)) ds.add(toMap(t));
        return ds;
    }

    /** Danh sách hội thoại cho module quản lý: tin cuối + số chưa đọc, mới nhất lên đầu. */
    public List<Map<String, Object>> danhSachPhien() {
        Map<String, Long> chuaDoc = new HashMap<>();
        for (Object[] o : repo.demChuaDocMoiPhien()) {
            chuaDoc.put((String) o[0], (Long) o[1]);
        }
        // maPhien -> id tin cuối, sắp theo id giảm dần (phiên mới nhắn lên đầu)
        Map<String, Integer> tinCuoi = new LinkedHashMap<>();
        List<Object[]> raw = new ArrayList<>(repo.tinCuoiMoiPhien());
        raw.sort((a, b) -> ((Integer) b[1]).compareTo((Integer) a[1]));
        for (Object[] o : raw) tinCuoi.put((String) o[0], (Integer) o[1]);

        List<Map<String, Object>> ds = new ArrayList<>();
        for (Map.Entry<String, Integer> e : tinCuoi.entrySet()) {
            HoTroTinNhan cuoi = repo.findById(e.getValue()).orElse(null);
            if (cuoi == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("maPhien", e.getKey());
            m.put("tenHienThi", cuoi.getTenHienThi() != null ? cuoi.getTenHienThi() : e.getKey());
            m.put("nguoiGuiCuoi", cuoi.getNguoiGui());
            m.put("tinCuoi", cuoi.getNoiDung());
            m.put("thoiGian", cuoi.getThoiGian() != null ? GIO.format(cuoi.getThoiGian()) : "");
            m.put("chuaDoc", chuaDoc.getOrDefault(e.getKey(), 0L));
            ds.add(m);
        }
        return ds;
    }

    public long tongChuaDoc() {
        return repo.tongChuaDoc();
    }

    private Map<String, Object> toMap(HoTroTinNhan t) {
        Map<String, Object> m = new HashMap<>();
        m.put("maTinNhan", t.getMaTinNhan());
        m.put("maPhien", t.getMaPhien());
        m.put("tenHienThi", t.getTenHienThi());
        m.put("nguoiGui", t.getNguoiGui());
        m.put("noiDung", t.getNoiDung());
        m.put("thoiGian", t.getThoiGian() != null ? GIO.format(t.getThoiGian()) : "");
        return m;
    }
}