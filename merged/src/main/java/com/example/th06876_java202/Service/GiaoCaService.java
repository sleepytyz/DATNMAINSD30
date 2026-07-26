package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.CaLamViec;
import com.example.th06876_java202.Entity.ChamCong;
import com.example.th06876_java202.Entity.LichNhanVienDTO;
import com.example.th06876_java202.Entity.NhanVien;
import com.example.th06876_java202.Repository.CaLamViecRepository;
import com.example.th06876_java202.Repository.ChamCongRepository;
import com.example.th06876_java202.Repository.GiaoCaRepository;
import com.example.th06876_java202.Repository.NhanVienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GiaoCaService {
    private final GiaoCaRepository giaoCaRepository;
    private final ChamCongRepository chamCongRepository;
    private final CaLamViecRepository caLamViecRepository;
    private final NhanVienRepository nhanVienRepository;

    // ===== CA LÀM VIỆC =====
    public List<CaLamViec> findCaAll() {
        return caLamViecRepository.findAll();
    }

    public CaLamViec findCaById(Integer maCa) {
        return caLamViecRepository.findById(maCa).orElse(null);
    }

    public List<CaLamViec> findCaByTenCa(String tenCa) {
        return caLamViecRepository.findByTenCaContaining(tenCa);
    }

    public List<CaLamViec> findCaByGio(LocalTime gioBatDau, LocalTime gioKetThuc) {
        return caLamViecRepository.findByGio(gioBatDau, gioKetThuc);
    }

    @Transactional
    public CaLamViec addCaLamViec(CaLamViec caLamViec) {
        return caLamViecRepository.save(caLamViec);
    }

    @Transactional
    public void editCaLamViec(CaLamViec caLamViec) {
        CaLamViec existing = caLamViecRepository.findById(caLamViec.getMaCa())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc với mã: " + caLamViec.getMaCa()));

        existing.setTenCa(caLamViec.getTenCa().trim());
        existing.setGioBatDau(caLamViec.getGioBatDau());
        existing.setGioKetThuc(caLamViec.getGioKetThuc());
        existing.setMoTa(caLamViec.getMoTa());

        caLamViecRepository.save(existing);
    }

    // ===== CHẤM CÔNG =====
    public List<ChamCong> findChamCongAll() {
        return chamCongRepository.findAll();
    }

    public List<ChamCong> findChamCongByTenNhanVien(String hoTen) {
        return chamCongRepository.findByNhanVien_HoTenContaining(hoTen);
    }

    @Transactional
    public void xoaChamCong(Integer maChamCong) {
        ChamCong chamCong = chamCongRepository.findById(maChamCong)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chấm công với mã: " + maChamCong));

        if (chamCong.getTrangThai() != null && !chamCong.getTrangThai()) {
            throw new RuntimeException("Không thể xóa lịch đã qua hoặc đã chấm công!");
        }

        chamCongRepository.delete(chamCong);
    }

    // ===== NHÂN VIÊN =====
    public List<NhanVien> findAllNhanVien() {
        return nhanVienRepository.findAll();
    }

    /**
     * Lấy danh sách nhân viên chưa có lịch trong khoảng thời gian
     */
    public List<NhanVien> getNhanVienChuaCoLich(LocalDate tuNgay, LocalDate denNgay) {
        List<NhanVien> allNhanVien = nhanVienRepository.findAll();
        List<ChamCong> allChamCong = chamCongRepository.findAll();

        Set<String> maNhanVienCoLich = allChamCong.stream()
                .filter(cc -> cc.getNgayChamCong() != null)
                .filter(cc -> !cc.getNgayChamCong().isBefore(tuNgay)
                        && !cc.getNgayChamCong().isAfter(denNgay))
                .map(cc -> cc.getNhanVien().getMaNhanVien())
                .collect(Collectors.toSet());

        return allNhanVien.stream()
                .filter(nv -> !maNhanVienCoLich.contains(nv.getMaNhanVien()))
                .collect(Collectors.toList());
    }

    // ===== TIỆN ÍCH =====
    /**
     * Lấy danh sách ngày trong khoảng thời gian (tối đa 7 ngày)
     */
    public List<LocalDate> getListNgayTrongKhoang(LocalDate tuNgay, LocalDate denNgay) {
        LocalDate startDate = tuNgay;
        LocalDate endDate = denNgay;

        if (startDate == null || endDate == null) {
            LocalDate homNay = LocalDate.now();
            startDate = homNay.minusDays(3);
            endDate = startDate.plusDays(7);
        }

        if (startDate.plusDays(7).isBefore(endDate)) {
            endDate = startDate.plusDays(7);
        }

        List<LocalDate> listNgay = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            listNgay.add(current);
            current = current.plusDays(1);
        }
        return listNgay;
    }

    // ===== LẤY DỮ LIỆU CHẤM CÔNG =====
    /**
     * Lấy dữ liệu chấm công theo ngày và ca làm việc
     */
    public Map<LocalDate, Map<CaLamViec, List<ChamCong>>> getChamCongTheoNgayVaCa(
            String tenNhanVien, Integer maCa, LocalDate tuNgay, LocalDate denNgay) {

        LocalDate startDate = tuNgay;
        LocalDate endDate = denNgay;

        if (startDate == null || endDate == null) {
            LocalDate homNay = LocalDate.now();
            startDate = homNay.minusDays(3);
            endDate = startDate.plusDays(7);
        }

        if (startDate.plusDays(7).isBefore(endDate)) {
            endDate = startDate.plusDays(7);
        }

        final LocalDate finalStartDate = startDate;
        final LocalDate finalEndDate = endDate;

        List<ChamCong> listChamCong;
        if (tenNhanVien != null && !tenNhanVien.isEmpty()) {
            listChamCong = chamCongRepository.findByNhanVien_HoTenContaining(tenNhanVien);
        } else {
            listChamCong = chamCongRepository.findAll();
        }

        listChamCong = listChamCong.stream()
                .filter(cc -> cc.getNgayChamCong() != null)
                .filter(cc -> !cc.getNgayChamCong().isBefore(finalStartDate)
                        && !cc.getNgayChamCong().isAfter(finalEndDate))
                .collect(Collectors.toList());

        if (maCa != null) {
            listChamCong = listChamCong.stream()
                    .filter(cc -> cc.getCaLamViec() != null && cc.getCaLamViec().getMaCa().equals(maCa))
                    .collect(Collectors.toList());
        }

        Map<LocalDate, Map<CaLamViec, List<ChamCong>>> result = new LinkedHashMap<>();
        List<CaLamViec> allCa = caLamViecRepository.findAll();
        List<LocalDate> allNgay = getListNgayTrongKhoang(finalStartDate, finalEndDate);

        for (LocalDate ngay : allNgay) {
            Map<CaLamViec, List<ChamCong>> caMap = new LinkedHashMap<>();
            for (CaLamViec ca : allCa) {
                caMap.put(ca, new ArrayList<>());
            }
            result.put(ngay, caMap);
        }

        for (ChamCong chamCong : listChamCong) {
            LocalDate ngay = chamCong.getNgayChamCong();
            CaLamViec ca = chamCong.getCaLamViec();

            if (ngay != null && ca != null && result.containsKey(ngay) && result.get(ngay).containsKey(ca)) {
                result.get(ngay).get(ca).add(chamCong);
            }
        }

        return result;
    }

    /**
     * Lấy dữ liệu chấm công theo nhân viên và ngày
     */
    public Map<NhanVien, Map<LocalDate, ChamCong>> getChamCongTheoNhanVienVaNgay(
            String tenNhanVien, Integer maCa, LocalDate tuNgay, LocalDate denNgay) {

        LocalDate startDate = tuNgay;
        LocalDate endDate = denNgay;

        if (startDate == null || endDate == null) {
            LocalDate homNay = LocalDate.now();
            startDate = homNay.minusDays(3);
            endDate = startDate.plusDays(7);
        }

        if (startDate.plusDays(7).isBefore(endDate)) {
            endDate = startDate.plusDays(7);
        }

        final LocalDate finalStartDate = startDate;
        final LocalDate finalEndDate = endDate;

        List<NhanVien> listNhanVien;
        if (tenNhanVien != null && !tenNhanVien.isEmpty()) {
            listNhanVien = nhanVienRepository.findByHoTenContaining(tenNhanVien);
        } else {
            listNhanVien = nhanVienRepository.findAll();
        }

        List<ChamCong> listChamCong = chamCongRepository.findAll().stream()
                .filter(cc -> cc.getNgayChamCong() != null)
                .filter(cc -> !cc.getNgayChamCong().isBefore(finalStartDate)
                        && !cc.getNgayChamCong().isAfter(finalEndDate))
                .collect(Collectors.toList());

        if (maCa != null) {
            listChamCong = listChamCong.stream()
                    .filter(cc -> cc.getCaLamViec() != null && cc.getCaLamViec().getMaCa().equals(maCa))
                    .collect(Collectors.toList());
        }

        List<LocalDate> listNgay = getListNgayTrongKhoang(finalStartDate, finalEndDate);

        Map<NhanVien, Map<LocalDate, ChamCong>> result = new LinkedHashMap<>();

        for (NhanVien nv : listNhanVien) {
            Map<LocalDate, ChamCong> ngayMap = new LinkedHashMap<>();
            for (LocalDate ngay : listNgay) {
                ngayMap.put(ngay, null);
            }
            result.put(nv, ngayMap);
        }

        for (ChamCong cc : listChamCong) {
            NhanVien nv = cc.getNhanVien();
            LocalDate ngay = cc.getNgayChamCong();
            if (nv != null && ngay != null && result.containsKey(nv) && result.get(nv).containsKey(ngay)) {
                result.get(nv).put(ngay, cc);
            }
        }

        return result;
    }

    // ===== TẠO LỊCH LÀM VIỆC =====

    /**
     * Tạo lịch làm việc hàng loạt - Hỗ trợ nhiều ca cho 1 nhân viên trong 1 ngày
     * SỬA: Tham số Map<String, List<Integer>> thay vì Map<Integer, List<String>>
     */
    @Transactional
    public List<ChamCong> taoLichLamViecHangLoat(LocalDate tuNgay, LocalDate denNgay,
                                                 Map<String, List<Integer>> lichNhanVien) {
        List<ChamCong> listChamCong = new ArrayList<>();
        LocalDate current = tuNgay;

        List<ChamCong> allChamCong = chamCongRepository.findAll();

        while (!current.isAfter(denNgay)) {
            final LocalDate ngayHienTai = current;

            for (Map.Entry<String, List<Integer>> entry : lichNhanVien.entrySet()) {
                String maNhanVien = entry.getKey();
                List<Integer> listMaCa = entry.getValue();

                boolean daCoLich = allChamCong.stream()
                        .anyMatch(cc -> cc.getNhanVien() != null
                                && cc.getNhanVien().getMaNhanVien().equals(maNhanVien)
                                && cc.getNgayChamCong() != null
                                && cc.getNgayChamCong().equals(ngayHienTai));

                if (!daCoLich) {
                    NhanVien nv = nhanVienRepository.findById(maNhanVien).orElse(null);
                    if (nv != null) {
                        for (Integer maCa : listMaCa) {
                            if (maCa == null || maCa <= 0) continue;

                            CaLamViec ca = caLamViecRepository.findById(maCa).orElse(null);
                            if (ca != null) {
                                ChamCong chamCong = new ChamCong();
                                chamCong.setNhanVien(nv);
                                chamCong.setCaLamViec(ca);
                                chamCong.setNgayChamCong(ngayHienTai);
                                chamCong.setTrangThai(true);
                                chamCong.setGhiChu("Lịch đã xếp");
                                listChamCong.add(chamCong);
                            }
                        }
                    }
                }
            }
            current = current.plusDays(1);
        }

        return chamCongRepository.saveAll(listChamCong);
    }

    /**
     * Tạo lịch làm việc chi tiết - mỗi item là 1 cặp (ngày, ca, nhân viên)
     * KHÔNG bị lặp dữ liệu
     */
    @Transactional
    public List<ChamCong> taoLichLamViecHangLoatChiTiet(List<LichNhanVienDTO> danhSach) {
        List<ChamCong> listChamCong = new ArrayList<>();

        List<ChamCong> allExisting = chamCongRepository.findAll();

        Set<String> existingKeys = new HashSet<>();
        for (ChamCong cc : allExisting) {
            if (cc.getNgayChamCong() != null && cc.getNhanVien() != null && cc.getCaLamViec() != null) {
                String key = cc.getNgayChamCong().toString() + "_"
                        + cc.getNhanVien().getMaNhanVien() + "_"
                        + cc.getCaLamViec().getMaCa();
                existingKeys.add(key);
            }
        }

        for (LichNhanVienDTO item : danhSach) {
            if (item.getMaNhanVien() == null || item.getMaCa() == null || item.getNgay() == null) {
                continue;
            }

            String key = item.getNgay() + "_" + item.getMaNhanVien() + "_" + item.getMaCa();

            if (existingKeys.contains(key)) {
                continue;
            }

            LocalDate ngay = LocalDate.parse(item.getNgay());

            NhanVien nv = nhanVienRepository.findById(item.getMaNhanVien()).orElse(null);
            CaLamViec ca = caLamViecRepository.findById(item.getMaCa()).orElse(null);

            if (nv == null || ca == null) {
                continue;
            }

            ChamCong chamCong = new ChamCong();
            chamCong.setNhanVien(nv);
            chamCong.setCaLamViec(ca);
            chamCong.setNgayChamCong(ngay);
            chamCong.setTrangThai(true);
            chamCong.setGhiChu("Lịch đã xếp");

            listChamCong.add(chamCong);
            existingKeys.add(key);
        }

        return chamCongRepository.saveAll(listChamCong);
    }
}