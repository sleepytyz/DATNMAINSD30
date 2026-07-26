package com.example.th06876_java202.Entity;

import lombok.Data;

import java.util.List;

@Data
public class SanPhamWrapperDTO {
    private SanPhamDTO sanPham; // DTO chứa thông tin sản phẩm
    private List<SanPhamChiTietDTO> chiTietList; // DTO chứa danh sách biến thể

    // Getter và Setter
    public SanPhamDTO getSanPham() { return sanPham; }
    public void setSanPham(SanPhamDTO sanPham) { this.sanPham = sanPham; }
    public List<SanPhamChiTietDTO> getChiTietList() { return chiTietList; }
    public void setChiTietList(List<SanPhamChiTietDTO> chiTietList) { this.chiTietList = chiTietList; }
}
