package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.NhanVienHieuSuatDTO;
import com.example.th06876_java202.Entity.SanPhamBanChayDTO;
import com.example.th06876_java202.Entity.ThongKeDoanhThuDTO;
import com.example.th06876_java202.Entity.ThongKeTheoThangDTO;
import com.example.th06876_java202.Entity.ThongKeTongQuanDTO;
import com.example.th06876_java202.Repository.ChamCongRepository;
import com.example.th06876_java202.Repository.HoaDonChiTietRepository;
import com.example.th06876_java202.Repository.HoaDonRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ThongKeService {
    private final HoaDonRepo hoaDonRepository;
    private final ChamCongRepository chamCongRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    // [THÊM] Dùng cho báo cáo "Sản phẩm sắp hết hàng" ở trang Thống kê
    private final com.example.th06876_java202.Repository.SanPhamChiTietRepository sanPhamChiTietRepository;

    // [THÊM] Top sản phẩm bán chạy nhất trong khoảng thời gian (dùng cho dashboard trang chủ)
    public List<SanPhamBanChayDTO> layTopSanPhamBanChay(LocalDateTime startDate, LocalDateTime endDate, int soLuong) {
        return hoaDonChiTietRepository.topSanPhamBanChay(startDate, endDate, PageRequest.of(0, soLuong));
    }

    public List<ThongKeDoanhThuDTO> thongKeDoanhThuTheoNgay(
            LocalDateTime startDate, LocalDateTime endDate) {
        return mapDoanhThuTheoNgay(hoaDonRepository.thongKeDoanhThuTheoNgay(startDate, endDate));
    }

    // Thống kê doanh thu theo ngày CHỈ của 1 nhân viên (STAFF - "Thống kê của tôi")
    public List<ThongKeDoanhThuDTO> thongKeDoanhThuCaNhanTheoNgay(
            String maNhanVien, LocalDateTime startDate, LocalDateTime endDate) {
        return mapDoanhThuTheoNgay(hoaDonRepository.thongKeDoanhThuCaNhanTheoNgay(maNhanVien, startDate, endDate));
    }

    private List<ThongKeDoanhThuDTO> mapDoanhThuTheoNgay(List<Object[]> results) {
        List<ThongKeDoanhThuDTO> list = new ArrayList<>();
        if (results != null) {
            for (Object[] row : results) {
                if (row != null && row.length >= 4) {
                    try {
                        ThongKeDoanhThuDTO dto = new ThongKeDoanhThuDTO();

                        Object ngayObj = row[0];
                        if (ngayObj != null) {
                            if (ngayObj instanceof LocalDate) {
                                dto.setNgay((LocalDate) ngayObj);
                            } else if (ngayObj instanceof java.sql.Date) {
                                dto.setNgay(((java.sql.Date) ngayObj).toLocalDate());
                            } else if (ngayObj instanceof String) {
                                dto.setNgay(LocalDate.parse((String) ngayObj));
                            }
                        }

                        if (row[1] != null) {
                            dto.setSoDonHang(((Number) row[1]).intValue());
                        }
                        if (row[2] != null) {
                            dto.setDoanhThu((BigDecimal) row[2]);
                        }
                        if (row[3] != null) {
                            dto.setTrungBinhDon((BigDecimal) row[3]);
                        }

                        if (dto.getNgay() != null) {
                            list.add(dto);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return list;
    }

    // Thống kê doanh thu theo tháng
    public List<ThongKeTheoThangDTO> thongKeDoanhThuTheoThang(
            LocalDateTime startDate, LocalDateTime endDate) {

        List<Object[]> results = hoaDonRepository.thongKeDoanhThuTheoThang(startDate, endDate);
        List<ThongKeTheoThangDTO> list = new ArrayList<>();

        if (results != null) {
            for (Object[] row : results) {
                if (row != null && row.length >= 4) {
                    try {
                        ThongKeTheoThangDTO dto = new ThongKeTheoThangDTO();

                        if (row[0] != null) {
                            dto.setNam(((Number) row[0]).intValue());
                        }
                        if (row[1] != null) {
                            dto.setThang(((Number) row[1]).intValue());
                        }
                        if (row[2] != null) {
                            dto.setSoDonHang(((Number) row[2]).intValue());
                        }
                        if (row[3] != null) {
                            dto.setDoanhThu((BigDecimal) row[3]);
                        }

                        list.add(dto);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return list;
    }

    // [SỬA] Trước đây không nhận khoảng ngày -> luôn trả về số liệu toàn bộ lịch sử,
    // không khớp với bộ lọc ngày người dùng chọn. Nay lọc đúng theo khoảng ngày.
    public ThongKeTongQuanDTO thongKeTongQuan(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<Object[]> results = hoaDonRepository.thongKeTongQuan(startDate, endDate);

            if (results == null || results.isEmpty()) {
                return new ThongKeTongQuanDTO();
            }

            Object[] row = results.get(0);
            if (row == null || row.length < 5) {
                return new ThongKeTongQuanDTO();
            }

            ThongKeTongQuanDTO dto = new ThongKeTongQuanDTO();

            if (row[0] != null) {
                dto.setTongDonHang(((Number) row[0]).intValue());
            }
            if (row[1] != null) {
                dto.setTongDoanhThu((BigDecimal) row[1]);
            }
            if (row[2] != null) {
                dto.setTrungBinhDon((BigDecimal) row[2]);
            }

            Object ngayDauObj = row[3];
            if (ngayDauObj != null) {
                if (ngayDauObj instanceof LocalDate) {
                    dto.setNgayDau((LocalDate) ngayDauObj);
                } else if (ngayDauObj instanceof java.sql.Date) {
                    dto.setNgayDau(((java.sql.Date) ngayDauObj).toLocalDate());
                } else if (ngayDauObj instanceof String) {
                    dto.setNgayDau(LocalDate.parse((String) ngayDauObj));
                }
            }
            // Nếu trong khoảng ngày lọc không có đơn hàng nào (MIN/MAX trả về NULL),
            // vẫn hiển thị đúng khoảng ngày người dùng đã chọn thay vì để trống.
            if (dto.getNgayDau() == null) {
                dto.setNgayDau(startDate.toLocalDate());
            }

            Object ngayCuoiObj = row[4];
            if (ngayCuoiObj != null) {
                if (ngayCuoiObj instanceof LocalDate) {
                    dto.setNgayCuoi((LocalDate) ngayCuoiObj);
                } else if (ngayCuoiObj instanceof java.sql.Date) {
                    dto.setNgayCuoi(((java.sql.Date) ngayCuoiObj).toLocalDate());
                } else if (ngayCuoiObj instanceof String) {
                    dto.setNgayCuoi(LocalDate.parse((String) ngayCuoiObj));
                }
            }
            if (dto.getNgayCuoi() == null) {
                dto.setNgayCuoi(endDate.toLocalDate());
            }

            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return new ThongKeTongQuanDTO();
        }
    }

    // Tổng quan doanh số CHỈ của 1 nhân viên (STAFF)
    public ThongKeTongQuanDTO thongKeTongQuanCaNhan(String maNhanVien, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<Object[]> results = hoaDonRepository.thongKeTongQuanCaNhan(maNhanVien, startDate, endDate);
            ThongKeTongQuanDTO dto = new ThongKeTongQuanDTO();
            dto.setNgayDau(startDate.toLocalDate());
            dto.setNgayCuoi(endDate.toLocalDate());
            if (results == null || results.isEmpty()) {
                return dto;
            }
            Object[] row = results.get(0);
            if (row == null || row.length < 3) {
                return dto;
            }
            if (row[0] != null) dto.setTongDonHang(((Number) row[0]).intValue());
            if (row[1] != null) dto.setTongDoanhThu((BigDecimal) row[1]);
            if (row[2] != null) dto.setTrungBinhDon((BigDecimal) row[2]);
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return new ThongKeTongQuanDTO();
        }
    }

    // [MỚI] Bảng hiệu suất từng nhân viên (doanh số bán hàng + chấm công) - dùng cho ADMIN.
    // Gộp 2 nguồn dữ liệu (HoaDon và ChamCong) theo mã nhân viên vì 1 nhân viên có thể
    // có lịch chấm công nhưng chưa bán được đơn nào (hoặc ngược lại).
    public List<NhanVienHieuSuatDTO> thongKeHieuSuatNhanVien(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, NhanVienHieuSuatDTO> map = new LinkedHashMap<>();

        List<Object[]> banHang = hoaDonRepository.thongKeHieuSuatBanHangTheoNhanVien(startDate, endDate);
        if (banHang != null) {
            for (Object[] row : banHang) {
                if (row == null || row.length < 4 || row[0] == null) continue;
                String ma = row[0].toString();
                NhanVienHieuSuatDTO dto = map.computeIfAbsent(ma, k -> new NhanVienHieuSuatDTO());
                dto.setMaNhanVien(ma);
                dto.setHoTen(row[1] != null ? row[1].toString() : "");
                dto.setSoDonHang(row[2] != null ? ((Number) row[2]).intValue() : 0);
                dto.setDoanhThu(row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO);
            }
        }

        List<Object[]> chamCong = chamCongRepository.thongKeChamCongTheoNhanVien(
                startDate.toLocalDate(), endDate.toLocalDate());
        if (chamCong != null) {
            for (Object[] row : chamCong) {
                if (row == null || row.length < 6 || row[0] == null) continue;
                String ma = row[0].toString();
                NhanVienHieuSuatDTO dto = map.computeIfAbsent(ma, k -> new NhanVienHieuSuatDTO());
                dto.setMaNhanVien(ma);
                if (dto.getHoTen() == null || dto.getHoTen().isEmpty()) {
                    dto.setHoTen(row[1] != null ? row[1].toString() : "");
                }
                dto.setSoNgayCong(row[2] != null ? ((Number) row[2]).intValue() : 0);
                dto.setTongGioLam(toBigDecimal(row[3]));
                dto.setSoLanTre(row[4] != null ? ((Number) row[4]).intValue() : 0);
                dto.setSoLanVangMat(row[5] != null ? ((Number) row[5]).intValue() : 0);
            }
        }

        List<NhanVienHieuSuatDTO> list = new ArrayList<>(map.values());
        list.sort((a, b) -> b.getDoanhThu().compareTo(a.getDoanhThu()));
        return list;
    }

    // [MỚI] Số liệu chấm công (giờ công, số ngày công, số lần trễ/vắng) CHỈ của 1 nhân viên - STAFF
    public NhanVienHieuSuatDTO thongKeChamCongCaNhan(String maNhanVien, LocalDate tuNgay, LocalDate denNgay) {
        NhanVienHieuSuatDTO dto = new NhanVienHieuSuatDTO();
        dto.setMaNhanVien(maNhanVien);
        List<Object[]> results = chamCongRepository.thongKeChamCongCaNhan(maNhanVien, tuNgay, denNgay);
        if (results != null && !results.isEmpty()) {
            Object[] row = results.get(0);
            if (row != null && row.length >= 6) {
                dto.setHoTen(row[1] != null ? row[1].toString() : "");
                dto.setSoNgayCong(row[2] != null ? ((Number) row[2]).intValue() : 0);
                dto.setTongGioLam(toBigDecimal(row[3]));
                dto.setSoLanTre(row[4] != null ? ((Number) row[4]).intValue() : 0);
                dto.setSoLanVangMat(row[5] != null ? ((Number) row[5]).intValue() : 0);
            }
        }
        return dto;
    }

    // Cột SoGioLam trong DB là kiểu FLOAT (không phải DECIMAL), nên driver JDBC trả về
    // Double thay vì BigDecimal khi chạy native query -> ép kiểu (BigDecimal) trực tiếp
    // sẽ ném ClassCastException lúc chạy. Hàm này chuyển an toàn từ mọi kiểu Number.
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return new BigDecimal(value.toString());
    }

    // ==================================================================
    // THỐNG KÊ NÂNG CAO
    // ==================================================================

    /** Ngưỡng cảnh báo sắp hết hàng (tồn <= 10) */
    public static final int NGUONG_SAP_HET = 10;

    /**
     * DOANH THU THỰC TẾ: tiền đã thực sự thu được
     * ('Đã thanh toán' tại quầy + 'Đã giao' online thành công).
     */
    public BigDecimal doanhThuThucTe(LocalDateTime tuNgay, LocalDateTime denNgay) {
        try {
            BigDecimal v = hoaDonRepository.doanhThuThucTe(tuNgay, denNgay);
            return v == null ? BigDecimal.ZERO : v;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * DOANH THU DỰ KIẾN: tiền sắp thu, đơn còn đang chạy
     * ('Chờ xác nhận', 'Đã xác nhận', 'Đang giao', 'Đang xử lý').
     * KHÔNG tính đơn đã huỷ / đã trả hàng.
     */
    public BigDecimal doanhThuDuKien(LocalDateTime tuNgay, LocalDateTime denNgay) {
        try {
            BigDecimal v = hoaDonRepository.doanhThuDuKien(tuNgay, denNgay);
            return v == null ? BigDecimal.ZERO : v;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * PHÂN BỔ TRẠNG THÁI ĐƠN HÀNG -> danh sách Map để vẽ biểu đồ tròn.
     * Mỗi phần tử: {trangThai, soDon, tongTien}
     */
    public List<java.util.Map<String, Object>> phanBoTrangThaiDonHang(LocalDateTime tuNgay,
                                                                      LocalDateTime denNgay) {
        List<java.util.Map<String, Object>> ds = new ArrayList<>();
        try {
            List<Object[]> rows = hoaDonRepository.phanBoTrangThaiDonHang(tuNgay, denNgay);
            if (rows == null) return ds;
            for (Object[] r : rows) {
                if (r == null || r.length < 3) continue;
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("trangThai", r[0] != null ? r[0].toString() : "Không rõ");
                m.put("soDon", r[1] != null ? ((Number) r[1]).intValue() : 0);
                m.put("tongTien", toBigDecimal(r[2]));
                ds.add(m);
            }
        } catch (Exception e) {
            System.out.println("Lỗi phân bổ trạng thái đơn: " + e.getMessage());
        }
        return ds;
    }

    /** SẢN PHẨM SẮP HẾT HÀNG (tồn <= ngưỡng), ít nhất lên đầu, giới hạn số dòng. */
    public List<com.example.th06876_java202.Entity.SanPhamChiTiet> sanPhamSapHetHang(int gioiHan) {
        try {
            List<com.example.th06876_java202.Entity.SanPhamChiTiet> ds =
                    sanPhamChiTietRepository.sanPhamSapHetHang(NGUONG_SAP_HET);
            if (ds == null) return List.of();
            return ds.size() > gioiHan ? ds.subList(0, gioiHan) : ds;
        } catch (Exception e) {
            System.out.println("Lỗi lấy sản phẩm sắp hết hàng: " + e.getMessage());
            return List.of();
        }
    }
}
