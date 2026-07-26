package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.DotGiamGia;
import com.example.th06876_java202.Entity.SanPham;
import com.example.th06876_java202.Repository.ChiTietDotGiamGiaRepo;
import com.example.th06876_java202.Repository.DotGiamGiaRepo;
import com.example.th06876_java202.Service.SanPhamService;
import com.example.th06876_java202.Storefront.SanPhamCardVM;
import com.example.th06876_java202.Storefront.SanPhamHienThiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TRANG "SĂN KHUYẾN MÃI" (/khuyen-mai) — tách riêng khỏi trang danh sách sản phẩm.
 * Trình bày theo TỪNG ĐỢT giảm giá đang chạy: tên chương trình, mức giảm, thời hạn
 * còn lại, và dàn sản phẩm thuộc đợt đó (thẻ kiểu flash-sale, bấm vào mở đúng
 * biến thể đang giảm). Dữ liệu lấy trực tiếp từ các đợt do quầy quản lý tạo.
 */
@Controller
@RequiredArgsConstructor
public class KhuyenMaiPageController {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DotGiamGiaRepo dotGiamGiaRepo;
    private final ChiTietDotGiamGiaRepo chiTietDotGiamGiaRepo;
    private final SanPhamService sanPhamService;
    private final SanPhamHienThiService sanPhamHienThiService;

    @GetMapping("/khuyen-mai")
    public String trangKhuyenMai(Model model) {
        LocalDate homNay = LocalDate.now();
        List<Map<String, Object>> dsDot = new ArrayList<>();
        int tongSanPham = 0;

        for (DotGiamGia dot : dotGiamGiaRepo.dangChayHomNay(homNay)) {
            List<SanPhamCardVM> cards = new ArrayList<>();
            for (String maSP : chiTietDotGiamGiaRepo.maSanPhamTrongDot(dot.getMaGiamGia())) {
                if (maSP == null) continue;
                SanPham sp = sanPhamService.findById(maSP).orElse(null);
                if (sp == null || !Boolean.TRUE.equals(sp.getTrangThai())) continue;
                SanPhamCardVM card = sanPhamHienThiService.taoCard(sp);
                if (card.getPhanTramGiam() != null && card.getPhanTramGiam() > 0) {
                    cards.add(card);
                }
            }
            if (cards.isEmpty()) continue;   // đợt không còn sản phẩm hợp lệ thì ẩn

            cards.sort((a, b) -> Integer.compare(
                    b.getPhanTramGiam() != null ? b.getPhanTramGiam() : 0,
                    a.getPhanTramGiam() != null ? a.getPhanTramGiam() : 0));

            long conLai = ChronoUnit.DAYS.between(homNay, dot.getNgayKetThuc());
            Map<String, Object> m = new HashMap<>();
            m.put("ten", dot.getTenGiamGia());
            m.put("moTa", dot.getMoTa());
            m.put("phanTram", dot.getGiaTriGiam() != null
                    ? dot.getGiaTriGiam().setScale(0, java.math.RoundingMode.HALF_UP).intValue() : 0);
            m.put("ketThuc", dot.getNgayKetThuc() != null ? NGAY.format(dot.getNgayKetThuc()) : "");
            m.put("conLai", Math.max(conLai, 0));
            m.put("sapHet", conLai <= 3);
            m.put("cards", cards);
            dsDot.add(m);
            tongSanPham += cards.size();
        }

        model.addAttribute("dsDot", dsDot);
        model.addAttribute("tongSanPhamSale", tongSanPham);
        return "cuahang/khuyen-mai";
    }
}