package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.ChiTietDotGiamGia;
import com.example.th06876_java202.Repository.DotGiamGiaRepo;
import com.example.th06876_java202.Repository.SanPhamRepository;
import com.example.th06876_java202.Service.ChiTietDotGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/chi-tiet-dot-giam-gia")
public class ChiTietDotGiamGiaCon {

    @Autowired
    private ChiTietDotGiamGiaService service;

    @Autowired
    private DotGiamGiaRepo dotGiamGiaRepo;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @GetMapping("/hien-thi")
    public String hienThi(Model model,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(value = "searchMaGiamGia", required = false) String searchMaGiamGia) {
        model.addAttribute("activeMenu", "ctdotgiamgia");

        int pageSize = 7;
        Pageable pageable = PageRequest.of(page, pageSize);
        List<ChiTietDotGiamGia> fullList;

        if (searchMaGiamGia != null && !searchMaGiamGia.isEmpty()) {
            fullList = service.filterByMaGiamGia(searchMaGiamGia);
            model.addAttribute("selectedMaGiamGia", searchMaGiamGia);
        } else {
            fullList = service.filterByMaGiamGia("");
            if (fullList == null || fullList.isEmpty()) {
                fullList = service.getAllPage(PageRequest.of(0, Integer.MAX_VALUE)).getContent();
            }
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), fullList.size());
        List<ChiTietDotGiamGia> pageContent = (start <= fullList.size()) ? fullList.subList(start, end) : Collections.emptyList();
        Page<ChiTietDotGiamGia> pageResult = new PageImpl<>(pageContent, pageable, fullList.size());

        model.addAttribute("listCTDGG", pageResult.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("totalItems", pageResult.getTotalElements());

        model.addAttribute("listDGG", dotGiamGiaRepo.findAll());
        model.addAttribute("listSP", sanPhamRepository.findAll());

        return "chitietdotgiamgia/index";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        service.delete(id);
        return "redirect:/chi-tiet-dot-giam-gia/hien-thi";
    }

    @PostMapping("/add")
    public String add(@RequestParam(required = false) String maGiamGia,
                      @RequestParam(required = false) List<String> maSanPhams,
                      RedirectAttributes redirectAttributes) {

        if (maGiamGia == null || maSanPhams == null || maSanPhams.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn đầy đủ đợt giảm giá và ít nhất một sản phẩm!");
            return "redirect:/chi-tiet-dot-giam-gia/hien-thi";
        }

        var dotGiamGia = dotGiamGiaRepo.findById(maGiamGia).orElse(null);
        if (dotGiamGia == null) {
            redirectAttributes.addFlashAttribute("error", "Đợt giảm giá không tồn tại!");
            return "redirect:/chi-tiet-dot-giam-gia/hien-thi";
        }

        int countAdded = 0;
        for (String maSp : maSanPhams) {
            if (!service.exists(maGiamGia, maSp)) {
                var sanPham = sanPhamRepository.findById(maSp).orElse(null);
                if (sanPham != null) {
                    ChiTietDotGiamGia ct = new ChiTietDotGiamGia();
                    ct.setDotGiamGia(dotGiamGia);
                    ct.setSanPham(sanPham);
                    service.save(ct);
                    countAdded++;
                }
            }
        }

        if (countAdded > 0) {
            redirectAttributes.addFlashAttribute("success", "Đã thêm thành công " + countAdded + " sản phẩm vào đợt giảm giá!");
        } else {
            redirectAttributes.addFlashAttribute("warning", "Các sản phẩm được chọn đã tồn tại trong đợt giảm giá này.");
        }

        return "redirect:/chi-tiet-dot-giam-gia/hien-thi";
    }

    @GetMapping("/loc")
    public String locTheoMaGiamGia(@RequestParam(value = "searchMaGiamGia", required = false) String searchMaGiamGia) {
        return "redirect:/chi-tiet-dot-giam-gia/hien-thi?searchMaGiamGia=" + (searchMaGiamGia != null ? searchMaGiamGia : "");
    }

    @GetMapping("/api/da-giam-gia/{maGiamGia}")
    @ResponseBody
    public List<String> getMaSanPhamsDaGiamTheoDot(@PathVariable Integer maGiamGia) {
        List<ChiTietDotGiamGia> chiTiets = service.filterByMaGiamGia(String.valueOf(maGiamGia));

        return chiTiets.stream()
                .map(ct -> ct.getSanPham().getMaSanPham())
                .collect(Collectors.toList());
    }
}