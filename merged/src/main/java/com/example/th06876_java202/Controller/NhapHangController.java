package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.PhieuNhapHangDTO;
import com.example.th06876_java202.Service.NhapHangService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/nhap-hang")
@RequiredArgsConstructor
public class NhapHangController {
    private final NhapHangService nhapHangService;

    @GetMapping("/index")
    public String hienthi(Model model,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size) {
        // Lấy dữ liệu phân trang
        Pageable pageable = PageRequest.of(page, size);
        Page<PhieuNhapHangDTO> phieuNhapPage = nhapHangService.getAllPhieuNhapPhanTrang(pageable);

        model.addAttribute("phieuNhapHangs", phieuNhapPage.getContent());
        model.addAttribute("page", phieuNhapPage);
        model.addAttribute("ctNhapHang", null);
        return "nhaphang/index";
    }

    @GetMapping("/detail/{id}")
    public String detail(Model model,
                         @PathVariable("id") int maPhieuNhap,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PhieuNhapHangDTO> phieuNhapPage = nhapHangService.getAllPhieuNhapPhanTrang(pageable);

        model.addAttribute("phieuNhapHangs", phieuNhapPage.getContent());
        model.addAttribute("page", phieuNhapPage);
        model.addAttribute("ctNhapHang", nhapHangService.getChiTietPhieu(maPhieuNhap));
        return "nhaphang/index";
    }

    @GetMapping("/nhap-hang/filter")
    public String filterPhieuNhap(@RequestParam String trangThai, Model model) {
        List<PhieuNhapHangDTO> list = nhapHangService.getPhieuNhapByStatus(trangThai);
        model.addAttribute("listPhieuNhap", list);
        return "ten_trang_html_cua_ban"; // Trả về trang hiển thị danh sách
    }
}