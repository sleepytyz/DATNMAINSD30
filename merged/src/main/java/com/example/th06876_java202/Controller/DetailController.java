package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.FileUploadUtil;
import com.example.th06876_java202.Entity.SanPhamChiTiet;
import com.example.th06876_java202.Service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@Controller
@RequestMapping("/detail")
public class DetailController {

    private final SanPhamChiTietService sanPhamChiTietService;
    private final DanhMucSanPhamService danhMucSanPhamService;
    private final SanPhamService sanPhamService;
    private final MauSacService mauSacService;
    private final KichThuocService kichThuocService;

    public DetailController( SanPhamChiTietService sanPhamChiTietService, DanhMucSanPhamService danhMucSanPhamService,
                                     SanPhamService sanPhamService,
                                     MauSacService mauSacService, KichThuocService kichThuocService) {
        this.sanPhamChiTietService = sanPhamChiTietService;
        this.danhMucSanPhamService = danhMucSanPhamService;
        this.sanPhamService = sanPhamService;
        this.mauSacService = mauSacService;
        this.kichThuocService = kichThuocService;
    }

    private void setupPageModel(Model model, Page<SanPhamChiTiet> page, String attrName, Object attrValue) {
        model.addAttribute("currentPage", page.getNumber());
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        if (attrName != null) model.addAttribute(attrName, attrValue);
        prepareModel(model);
        model.addAttribute("sanphamct", new SanPhamChiTiet());
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") String id,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", required = false) String size,
                       @RequestParam(value = "msac", required = false) String msac,
                       @RequestParam(value = "tt", required = false) String tt,
                       Model model) {

        Pageable pageable = PageRequest.of(page, 5, Sort.by("maSanPhamChiTiet").descending());
        Page<SanPhamChiTiet> p = sanPhamChiTietService.getall(pageable);
        model.addAttribute("listspct", p.getContent());
        setupPageModel(model, p, null, null);

        SanPhamChiTiet sanPhamChiTiet = sanPhamChiTietService.findbyId(id).orElse(null);
        model.addAttribute("sanphamct", sanPhamChiTiet);

        model.addAttribute("selectedSize", size);
        model.addAttribute("selectedMauSac", msac);
        model.addAttribute("selectedStatus", tt);

        model.addAttribute("showModal", true);
        model.addAttribute("isEdit", true);
        return "sanphamct/index";
    }

    private void prepareModel(Model model) {
        model.addAttribute("listsp", sanPhamService.getAll());
        model.addAttribute("listdmsp", danhMucSanPhamService.getAll());
        model.addAttribute("listms", mauSacService.findAll());
        model.addAttribute("lists", kichThuocService.getall());
    }


}
