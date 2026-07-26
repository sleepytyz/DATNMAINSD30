package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.DanhGia;
import com.example.th06876_java202.Repository.DanhGiaRepository;
import com.example.th06876_java202.Repository.SanPhamRepository;
import com.example.th06876_java202.Service.DanhGiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MODULE QUẢN LÝ ĐÁNH GIÁ (khu quản lý — ADMIN/STAFF):
 * xem toàn bộ đánh giá của khách, lọc theo sản phẩm / số sao / trạng thái /
 * từ khoá; Ẩn các đánh giá không phù hợp hoặc Hiện lại (đánh giá ẩn không xuất
 * hiện trên website bán hàng và không tính vào điểm trung bình).
 */
@Controller
@RequestMapping("/danhgia")
@RequiredArgsConstructor
public class DanhGiaQuanLyController {

    private final DanhGiaRepository danhGiaRepository;
    private final SanPhamRepository sanPhamRepository;
    private final DanhGiaService danhGiaService;

    @GetMapping("/index")
    public String index(@RequestParam(required = false) String sp,
                        @RequestParam(required = false) Integer sao,
                        @RequestParam(required = false) String tt,
                        @RequestParam(required = false) String q,
                        Model model) {
        List<DanhGia> ds = danhGiaRepository.findAllByOrderByNgayDanhGiaDesc();

        long tongTatCa = ds.size();
        long tongHien = ds.stream().filter(d -> Boolean.TRUE.equals(d.getTrangThai())).count();
        double diemTB = ds.stream().filter(d -> Boolean.TRUE.equals(d.getTrangThai()))
                .filter(d -> d.getSoSao() != null).mapToInt(DanhGia::getSoSao).average().orElse(0);

        String spLoc = sp != null && !sp.isBlank() ? sp : null;
        Integer saoLoc = sao != null && sao >= 1 && sao <= 5 ? sao : null;
        String ttLoc = tt != null && !tt.isBlank() ? tt : null;
        String qLoc = q != null && !q.isBlank() ? q.trim().toLowerCase() : null;

        List<DanhGia> daLoc = ds.stream().filter(d -> {
            if (spLoc != null && (d.getSanPham() == null || !spLoc.equals(d.getSanPham().getMaSanPham()))) return false;
            if (saoLoc != null && (d.getSoSao() == null || d.getSoSao().intValue() != saoLoc)) return false;
            if ("hien".equals(ttLoc) && !Boolean.TRUE.equals(d.getTrangThai())) return false;
            if ("an".equals(ttLoc) && Boolean.TRUE.equals(d.getTrangThai())) return false;
            if (qLoc != null) {
                String noiDung = d.getNoiDung() != null ? d.getNoiDung().toLowerCase() : "";
                String tenKh = d.getKhachHang() != null && d.getKhachHang().getHoTen() != null
                        ? d.getKhachHang().getHoTen().toLowerCase() : "";
                if (!noiDung.contains(qLoc) && !tenKh.contains(qLoc)) return false;
            }
            return true;
        }).collect(Collectors.toList());

        model.addAttribute("dsDanhGia", daLoc);
        model.addAttribute("dsSanPham", sanPhamRepository.findAll());
        model.addAttribute("tongTatCa", tongTatCa);
        model.addAttribute("tongHien", tongHien);
        model.addAttribute("tongAn", tongTatCa - tongHien);
        model.addAttribute("diemTB", Math.round(diemTB * 10.0) / 10.0);
        model.addAttribute("fSp", spLoc);
        model.addAttribute("fSao", saoLoc);
        model.addAttribute("fTt", ttLoc);
        model.addAttribute("fQ", qLoc != null ? q.trim() : null);
        return "danhgia/index";
    }

    /** Ẩn / hiện một đánh giá. */
    @GetMapping("/doi")
    public String doi(@RequestParam Integer id,
                      @RequestParam boolean hien,
                      RedirectAttributes ra) {
        danhGiaService.doiTrangThai(id, hien);
        ra.addFlashAttribute("thongBao", hien ? "Đã hiện lại đánh giá." : "Đã ẩn đánh giá khỏi website.");
        return "redirect:/danhgia/index";
    }
}