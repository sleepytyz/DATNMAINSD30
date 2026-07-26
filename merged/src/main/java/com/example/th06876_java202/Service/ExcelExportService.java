package com.example.th06876_java202.Service;

import com.example.th06876_java202.Entity.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class ExcelExportService {

    public ByteArrayInputStream exportSanPhamToExcel(List<SanPhamDTO> sanPhamList) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            System.out.println("📊 Bắt đầu xuất Excel - Số lượng DTO: " +
                    (sanPhamList != null ? sanPhamList.size() : 0));

            if (sanPhamList == null || sanPhamList.isEmpty()) {
                // Tạo file Excel với thông báo
                Sheet sheet = workbook.createSheet("Danh sách sản phẩm");
                Row headerRow = sheet.createRow(0);
                Cell cell = headerRow.createCell(0);
                cell.setCellValue("Không có dữ liệu để xuất!");
                workbook.write(out);
                return new ByteArrayInputStream(out.toByteArray());
            }

            // Tạo sheet
            Sheet sheet = workbook.createSheet("Danh sách sản phẩm");

            // ===== TẠO STYLE =====
            // Font header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontName("Arial");

            // Style header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);

            // Style data center
            CellStyle dataStyleCenter = workbook.createCellStyle();
            dataStyleCenter.setBorderBottom(BorderStyle.THIN);
            dataStyleCenter.setBorderTop(BorderStyle.THIN);
            dataStyleCenter.setBorderLeft(BorderStyle.THIN);
            dataStyleCenter.setBorderRight(BorderStyle.THIN);
            dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
            dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyleCenter.setWrapText(true);

            // Style data left
            CellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.cloneStyleFrom(dataStyleCenter);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);

            // Style data right (cho số)
            CellStyle dataStyleRight = workbook.createCellStyle();
            dataStyleRight.cloneStyleFrom(dataStyleCenter);
            dataStyleRight.setAlignment(HorizontalAlignment.RIGHT);

            // Style cho trạng thái
            CellStyle statusStyleActive = workbook.createCellStyle();
            statusStyleActive.cloneStyleFrom(dataStyleCenter);
            Font activeFont = workbook.createFont();
            activeFont.setColor(IndexedColors.GREEN.getIndex());
            activeFont.setBold(true);
            statusStyleActive.setFont(activeFont);

            CellStyle statusStyleInactive = workbook.createCellStyle();
            statusStyleInactive.cloneStyleFrom(dataStyleCenter);
            Font inactiveFont = workbook.createFont();
            inactiveFont.setColor(IndexedColors.RED.getIndex());
            inactiveFont.setBold(true);
            statusStyleInactive.setFont(inactiveFont);

            // ===== TẠO HEADER - BỎ 2 CỘT GIÁ =====
            String[] headers = {"STT", "Mã SP", "Tên sản phẩm", "Thương hiệu", "Danh mục",
                    "Kiểu giày", "Chất liệu", "Mô tả", "Tồn kho",
                    "SL biến thể", "Ngày tạo", "Trạng thái"};

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(35);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Điều chỉnh độ rộng cột
            sheet.setColumnWidth(0, 2000);   // STT
            sheet.setColumnWidth(1, 4000);   // Mã SP
            sheet.setColumnWidth(2, 8000);   // Tên sản phẩm
            sheet.setColumnWidth(3, 5000);   // Thương hiệu
            sheet.setColumnWidth(4, 5000);   // Danh mục
            sheet.setColumnWidth(5, 5000);   // Kiểu giày
            sheet.setColumnWidth(6, 5000);   // Chất liệu
            sheet.setColumnWidth(7, 8000);   // Mô tả
            sheet.setColumnWidth(8, 3000);   // Tồn kho
            sheet.setColumnWidth(9, 3500);   // SL biến thể
            sheet.setColumnWidth(10, 5000);  // Ngày tạo
            sheet.setColumnWidth(11, 4000);  // Trạng thái

            // ===== ĐỔ DỮ LIỆU =====
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            int rowNum = 1;

            System.out.println("📝 Bắt đầu ghi " + sanPhamList.size() + " dòng dữ liệu...");

            for (SanPhamDTO sp : sanPhamList) {
                try {
                    Row row = sheet.createRow(rowNum);
                    row.setHeightInPoints(25);

                    int col = 0;

                    // STT
                    Cell cell0 = row.createCell(col++);
                    cell0.setCellValue(rowNum);
                    cell0.setCellStyle(dataStyleCenter);

                    // Mã SP
                    Cell cell1 = row.createCell(col++);
                    cell1.setCellValue(sp.getMaSanPham() != null ? sp.getMaSanPham() : "");
                    cell1.setCellStyle(dataStyleCenter);

                    // Tên sản phẩm
                    Cell cell2 = row.createCell(col++);
                    cell2.setCellValue(sp.getTenSanPham() != null ? sp.getTenSanPham() : "");
                    cell2.setCellStyle(dataStyleLeft);

                    // Thương hiệu
                    Cell cell3 = row.createCell(col++);
                    cell3.setCellValue(sp.getTenThuongHieu() != null ? sp.getTenThuongHieu() : "");
                    cell3.setCellStyle(dataStyleCenter);

                    // Danh mục
                    Cell cell4 = row.createCell(col++);
                    cell4.setCellValue(sp.getTenDanhMuc() != null ? sp.getTenDanhMuc() : "");
                    cell4.setCellStyle(dataStyleCenter);

                    // Kiểu giày
                    Cell cell5 = row.createCell(col++);
                    cell5.setCellValue(sp.getTenKieuGiay() != null ? sp.getTenKieuGiay() : "");
                    cell5.setCellStyle(dataStyleCenter);

                    // Chất liệu
                    Cell cell6 = row.createCell(col++);
                    cell6.setCellValue(sp.getTenChatLieu() != null ? sp.getTenChatLieu() : "");
                    cell6.setCellStyle(dataStyleCenter);

                    // Mô tả
                    Cell cell7 = row.createCell(col++);
                    cell7.setCellValue(sp.getMoTa() != null ? sp.getMoTa() : "");
                    cell7.setCellStyle(dataStyleLeft);

                    // Tồn kho
                    Cell cell8 = row.createCell(col++);
                    cell8.setCellValue(sp.getTongTon() != null ? sp.getTongTon() : 0);
                    cell8.setCellStyle(dataStyleCenter);

                    // Số lượng biến thể
                    Cell cell9 = row.createCell(col++);
                    cell9.setCellValue(sp.getSoLuongBienThe() != null ? sp.getSoLuongBienThe() : 0);
                    cell9.setCellStyle(dataStyleCenter);

                    // Ngày tạo
                    Cell cell10 = row.createCell(col++);
                    cell10.setCellValue(sp.getNgayTaoDisplay() != null ? sp.getNgayTaoDisplay() : "");
                    cell10.setCellStyle(dataStyleCenter);

                    // Trạng thái
                    Cell cell11 = row.createCell(col++);
                    boolean isActive = sp.getTrangThai() != null && sp.getTrangThai();
                    cell11.setCellValue(isActive ? "Còn bán" : "Ngừng bán");
                    cell11.setCellStyle(isActive ? statusStyleActive : statusStyleInactive);

                    rowNum++;

                    if (rowNum % 10 == 0) {
                        System.out.println("  ✅ Đã ghi " + (rowNum - 1) + " dòng...");
                    }

                } catch (Exception e) {
                    System.err.println("❌ Lỗi khi ghi dòng " + rowNum + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            System.out.println("✅ Đã ghi tổng cộng " + (rowNum - 1) + " dòng dữ liệu!");

            // ===== TẠO FOOTER THỐNG KÊ =====
            int lastRowNum = rowNum;
            Row footerRow = sheet.createRow(lastRowNum);
            footerRow.setHeightInPoints(25);

            // Tính tổng
            int totalTon = sanPhamList.stream()
                    .mapToInt(sp -> sp.getTongTon() != null ? sp.getTongTon() : 0)
                    .sum();
            long totalActive = sanPhamList.stream()
                    .filter(sp -> sp.getTrangThai() != null && sp.getTrangThai())
                    .count();
            long totalInactive = sanPhamList.size() - totalActive;
            int totalVariants = sanPhamList.stream()
                    .mapToInt(sp -> sp.getSoLuongBienThe() != null ? sp.getSoLuongBienThe() : 0)
                    .sum();

            String footerText = String.format(
                    "Tổng: %d sản phẩm | Còn bán: %d | Ngừng bán: %d | Tổng tồn kho: %d | Tổng biến thể: %d",
                    sanPhamList.size(), totalActive, totalInactive, totalTon, totalVariants
            );

            Cell footerCell = footerRow.createCell(0);
            footerCell.setCellValue(footerText);
            footerCell.setCellStyle(dataStyleLeft);

            // Merge footer
            sheet.addMergedRegion(new CellRangeAddress(lastRowNum, lastRowNum, 0, 11));

            // Tạo freeze pane
            sheet.createFreezePane(0, 1);

            // Ghi workbook
            workbook.write(out);
            out.flush();

            System.out.println("📊 Dung lượng file: " + out.size() + " bytes");

            byte[] excelBytes = out.toByteArray();
            return new ByteArrayInputStream(excelBytes);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel: " + e.getMessage());
            return null;
        }
    }

    public ByteArrayInputStream exportSanPhamChiTietToExcel(List<SanPhamChiTiet> list) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            System.out.println("📊 Bắt đầu xuất Excel - Số lượng: " + (list != null ? list.size() : 0));

            Sheet sheet = workbook.createSheet("Danh sách biến thể");

            // Font header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            // Style header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style data
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style text left
            CellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.setBorderBottom(BorderStyle.THIN);
            dataStyleLeft.setBorderTop(BorderStyle.THIN);
            dataStyleLeft.setBorderLeft(BorderStyle.THIN);
            dataStyleLeft.setBorderRight(BorderStyle.THIN);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
            dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);

            // Headers
            String[] headers = {"STT", "Mã SPCT", "Sản phẩm", "Size", "Màu sắc", "Giá bán", "Tồn kho", "Trạng thái"};
            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Set column widths
            sheet.setColumnWidth(0, 2000);
            sheet.setColumnWidth(1, 5000);
            sheet.setColumnWidth(2, 6000);
            sheet.setColumnWidth(3, 3000);
            sheet.setColumnWidth(4, 4000);
            sheet.setColumnWidth(5, 5000);
            sheet.setColumnWidth(6, 3000);
            sheet.setColumnWidth(7, 4000);

            // Fill data
            int rowNum = 1;
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));

            if (list != null && !list.isEmpty()) {
                for (SanPhamChiTiet spct : list) {
                    Row row = sheet.createRow(rowNum);

                    // STT
                    Cell cell0 = row.createCell(0);
                    cell0.setCellValue(rowNum);
                    cell0.setCellStyle(dataStyle);

                    // Mã SPCT
                    Cell cell1 = row.createCell(1);
                    cell1.setCellValue(spct.getMaSanPhamChiTiet() != null ? spct.getMaSanPhamChiTiet() : "");
                    cell1.setCellStyle(dataStyle);

                    // Sản phẩm
                    Cell cell2 = row.createCell(2);
                    cell2.setCellValue(spct.getSanPham() != null && spct.getSanPham().getTenSanPham() != null
                            ? spct.getSanPham().getTenSanPham() : "");
                    cell2.setCellStyle(dataStyleLeft);

                    // Size
                    Cell cell3 = row.createCell(3);
                    cell3.setCellValue(spct.getKichThuoc() != null && spct.getKichThuoc().getTenKichThuoc() != null
                            ? spct.getKichThuoc().getTenKichThuoc() : "");
                    cell3.setCellStyle(dataStyle);

                    // Màu sắc
                    Cell cell4 = row.createCell(4);
                    cell4.setCellValue(spct.getMauSac() != null && spct.getMauSac().getTenMauSac() != null
                            ? spct.getMauSac().getTenMauSac() : "");
                    cell4.setCellStyle(dataStyle);

                    // Giá bán
                    Cell cell5 = row.createCell(5);
                    cell5.setCellValue(spct.getGiaBan() != null ? formatter.format(spct.getGiaBan()) + " ₫" : "0 ₫");
                    cell5.setCellStyle(dataStyle);

                    // Tồn kho
                    Cell cell6 = row.createCell(6);
                    cell6.setCellValue(spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0);
                    cell6.setCellStyle(dataStyle);

                    // Trạng thái
                    Cell cell7 = row.createCell(7);
                    cell7.setCellValue(spct.getTrangThai() != null ? spct.getTrangThai() : "");
                    cell7.setCellStyle(dataStyle);

                    rowNum++;
                    System.out.println("📝 Đã xuất dòng: " + rowNum);
                }
            }

            // 🔥 QUAN TRỌNG: Ghi workbook vào stream
            workbook.write(out);

            // 🔥 QUAN TRỌNG: Flush stream
            out.flush();

            System.out.println("✅ Xuất Excel thành công! " + (rowNum - 1) + " dòng dữ liệu.");
            System.out.println("📊 Dung lượng file: " + out.size() + " bytes");

            // Tạo ByteArrayInputStream từ dữ liệu đã ghi
            byte[] excelBytes = out.toByteArray();
            return new ByteArrayInputStream(excelBytes);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel: " + e.getMessage());
            return null;
        }
    }

    public ByteArrayInputStream exportNhanVienToExcel(List<NhanVien> nhanVienList) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            System.out.println("📊 Bắt đầu xuất Excel nhân viên - Số lượng: " + (nhanVienList != null ? nhanVienList.size() : 0));

            // Tạo sheet
            Sheet sheet = workbook.createSheet("Danh sách nhân viên");

            // Tạo font cho header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            // Tạo style cho header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Tạo style cho cell dữ liệu (căn giữa)
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Tạo style cho cell text (căn trái)
            CellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.setBorderBottom(BorderStyle.THIN);
            dataStyleLeft.setBorderTop(BorderStyle.THIN);
            dataStyleLeft.setBorderLeft(BorderStyle.THIN);
            dataStyleLeft.setBorderRight(BorderStyle.THIN);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
            dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);

            // Định dạng ngày tháng
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // Tạo header
            String[] headers = {"STT", "Mã NV", "Họ và tên", "SĐT", "Email", "Giới tính", "Chức vụ", "Địa chỉ", "Ngày vào làm", "Trạng thái"};
            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Điều chỉnh độ rộng cột
            sheet.setColumnWidth(0, 2000);  // STT
            sheet.setColumnWidth(1, 4000);  // Mã NV
            sheet.setColumnWidth(2, 6000);  // Họ tên
            sheet.setColumnWidth(3, 4000);  // SĐT
            sheet.setColumnWidth(4, 6000);  // Email
            sheet.setColumnWidth(5, 3000);  // Giới tính
            sheet.setColumnWidth(6, 4000);  // Chức vụ
            sheet.setColumnWidth(7, 8000);  // Địa chỉ
            sheet.setColumnWidth(8, 5000);  // Ngày vào làm
            sheet.setColumnWidth(9, 4000);  // Trạng thái

            // Đổ dữ liệu
            int rowNum = 1;

            if (nhanVienList != null && !nhanVienList.isEmpty()) {
                for (NhanVien nv : nhanVienList) {
                    Row row = sheet.createRow(rowNum);

                    // STT
                    Cell cell0 = row.createCell(0);
                    cell0.setCellValue(rowNum);
                    cell0.setCellStyle(dataStyle);

                    // Mã NV
                    Cell cell1 = row.createCell(1);
                    cell1.setCellValue(nv.getMaNhanVien() != null ? nv.getMaNhanVien() : "");
                    cell1.setCellStyle(dataStyle);

                    // Họ và tên
                    Cell cell2 = row.createCell(2);
                    cell2.setCellValue(nv.getHoTen() != null ? nv.getHoTen() : "");
                    cell2.setCellStyle(dataStyleLeft);

                    // SĐT
                    Cell cell3 = row.createCell(3);
                    cell3.setCellValue(nv.getSoDienThoai() != null ? nv.getSoDienThoai() : "");
                    cell3.setCellStyle(dataStyle);

                    // Email
                    Cell cell4 = row.createCell(4);
                    cell4.setCellValue(nv.getEmail() != null ? nv.getEmail() : "");
                    cell4.setCellStyle(dataStyle);

                    // Giới tính
                    Cell cell5 = row.createCell(5);
                    cell5.setCellValue(nv.getGioiTinh() != null ? (nv.getGioiTinh() ? "Nam" : "Nữ") : "");
                    cell5.setCellStyle(dataStyle);

                    // Chức vụ
                    Cell cell6 = row.createCell(6);
                    cell6.setCellValue(nv.getChucVu() != null ? nv.getChucVu() : "");
                    cell6.setCellStyle(dataStyle);

                    // Địa chỉ
                    Cell cell7 = row.createCell(7);
                    cell7.setCellValue(nv.getDiaChi() != null ? nv.getDiaChi() : "");
                    cell7.setCellStyle(dataStyleLeft);

                    // Ngày vào làm
                    Cell cell8 = row.createCell(8);
                    cell8.setCellValue(nv.getNgayVaoLam() != null ? nv.getNgayVaoLam().format(dateFormatter) : "");
                    cell8.setCellStyle(dataStyle);

                    // Trạng thái
                    Cell cell9 = row.createCell(9);
                    cell9.setCellValue(nv.getTrangThai() != null ? (nv.getTrangThai() ? "Đang làm việc" : "Nghỉ việc") : "");
                    cell9.setCellStyle(dataStyle);

                    rowNum++;
                    System.out.println("📝 Đã xuất dòng nhân viên: " + rowNum);
                }
            }

            // Ghi workbook vào stream
            workbook.write(out);
            out.flush();

            System.out.println("✅ Xuất Excel nhân viên thành công! " + (rowNum - 1) + " dòng dữ liệu.");
            System.out.println("📊 Dung lượng file: " + out.size() + " bytes");

            byte[] excelBytes = out.toByteArray();
            return new ByteArrayInputStream(excelBytes);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel nhân viên: " + e.getMessage());
            return null;
        }
    }

    public ByteArrayInputStream exportKhachHangToExcel(List<KhachHang> khachHangList) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            System.out.println("📊 Bắt đầu xuất Excel khách hàng - Số lượng: " +
                    (khachHangList != null ? khachHangList.size() : 0));

            Sheet sheet = workbook.createSheet("Danh sách khách hàng");

            // Font header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            // Style header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style data (căn giữa)
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style text left
            CellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.setBorderBottom(BorderStyle.THIN);
            dataStyleLeft.setBorderTop(BorderStyle.THIN);
            dataStyleLeft.setBorderLeft(BorderStyle.THIN);
            dataStyleLeft.setBorderRight(BorderStyle.THIN);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
            dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);

            // Headers
            String[] headers = {"STT", "Mã KH", "Họ tên", "SĐT", "Email", "Ngày sinh", "Giới tính", "Ngày đăng ký", "Trạng thái"};
            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Set column widths
            sheet.setColumnWidth(0, 2000);  // STT
            sheet.setColumnWidth(1, 4000);  // Mã KH
            sheet.setColumnWidth(2, 6000);  // Họ tên
            sheet.setColumnWidth(3, 4000);  // SĐT
            sheet.setColumnWidth(4, 6000);  // Email
            sheet.setColumnWidth(5, 4000);  // Ngày sinh
            sheet.setColumnWidth(6, 3000);  // Giới tính
            sheet.setColumnWidth(7, 4000);  // Ngày đăng ký
            sheet.setColumnWidth(8, 4000);  // Trạng thái

            // Fill data
            int rowNum = 1;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            if (khachHangList != null && !khachHangList.isEmpty()) {
                for (KhachHang kh : khachHangList) {
                    Row row = sheet.createRow(rowNum);

                    // STT
                    row.createCell(0).setCellValue(rowNum);
                    row.getCell(0).setCellStyle(dataStyle);

                    // Mã KH
                    row.createCell(1).setCellValue("KH" + kh.getMaKH());
                    row.getCell(1).setCellStyle(dataStyle);

                    // Họ tên
                    row.createCell(2).setCellValue(kh.getHoTen() != null ? kh.getHoTen() : "");
                    row.getCell(2).setCellStyle(dataStyleLeft);

                    // SĐT
                    row.createCell(3).setCellValue(kh.getSdt() != null ? kh.getSdt() : "");
                    row.getCell(3).setCellStyle(dataStyle);

                    // Email
                    row.createCell(4).setCellValue(kh.getEmail() != null ? kh.getEmail() : "");
                    row.getCell(4).setCellStyle(dataStyle);

                    // Ngày sinh
                    row.createCell(5).setCellValue(kh.getNgaySinh() != null ? kh.getNgaySinh().format(dateFormatter) : "");
                    row.getCell(5).setCellStyle(dataStyle);

                    // Giới tính
                    row.createCell(6).setCellValue(kh.getGioiTinh() != null ? (kh.getGioiTinh() ? "Nam" : "Nữ") : "");
                    row.getCell(6).setCellStyle(dataStyle);

                    // Ngày đăng ký
                    row.createCell(7).setCellValue(kh.getNgayDangKy() != null ? kh.getNgayDangKy().format(dateFormatter) : "");
                    row.getCell(7).setCellStyle(dataStyle);

                    // Trạng thái
                    row.createCell(8).setCellValue(kh.isTrangThai() ? "Hoạt động" : "Ngừng hoạt động");
                    row.getCell(8).setCellStyle(dataStyle);

                    rowNum++;
                }
            }

            workbook.write(out);
            out.flush();

            System.out.println("✅ Xuất Excel khách hàng thành công! " + (rowNum - 1) + " dòng.");
            System.out.println("📊 Dung lượng file: " + out.size() + " bytes");

            byte[] excelBytes = out.toByteArray();
            return new ByteArrayInputStream(excelBytes);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel khách hàng: " + e.getMessage());
            return null;
        }
    }

    public ByteArrayInputStream exportGiamGiaToExcel(List<GiamGia> giamGiaList) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            System.out.println("📊 Bắt đầu xuất Excel giảm giá - Số lượng: " +
                    (giamGiaList != null ? giamGiaList.size() : 0));

            Sheet sheet = workbook.createSheet("Danh sách giảm giá");

            // Font header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            // Style header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style data
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style text left
            CellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.setBorderBottom(BorderStyle.THIN);
            dataStyleLeft.setBorderTop(BorderStyle.THIN);
            dataStyleLeft.setBorderLeft(BorderStyle.THIN);
            dataStyleLeft.setBorderRight(BorderStyle.THIN);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
            dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);

            // Headers
            String[] headers = {"STT", "Mã GG", "Tên chương trình", "Loại áp dụng", "Loại giảm",
                    "Giá trị", "Đơn tối thiểu", "Số lượng", "Giảm tối đa",
                    "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái"};
            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Set column widths
            sheet.setColumnWidth(0, 2000);  // STT
            sheet.setColumnWidth(1, 4000);  // Mã GG
            sheet.setColumnWidth(2, 6000);  // Tên
            sheet.setColumnWidth(3, 4000);  // Loại áp dụng
            sheet.setColumnWidth(4, 3000);  // Loại giảm
            sheet.setColumnWidth(5, 5000);  // Giá trị
            sheet.setColumnWidth(6, 5000);  // Đơn tối thiểu
            sheet.setColumnWidth(7, 3000);  // Số lượng
            sheet.setColumnWidth(8, 5000);  // Giảm tối đa
            sheet.setColumnWidth(9, 5000);  // Ngày bắt đầu
            sheet.setColumnWidth(10, 5000); // Ngày kết thúc
            sheet.setColumnWidth(11, 4000); // Trạng thái

            // Định dạng số
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // Đổ dữ liệu
            int rowNum = 1;

            if (giamGiaList != null && !giamGiaList.isEmpty()) {
                for (GiamGia gg : giamGiaList) {
                    Row row = sheet.createRow(rowNum);

                    // STT
                    Cell cell0 = row.createCell(0);
                    cell0.setCellValue(rowNum);
                    cell0.setCellStyle(dataStyle);

                    // Mã GG
                    Cell cell1 = row.createCell(1);
                    cell1.setCellValue(gg.getMaGiamGia() != null ? gg.getMaGiamGia() : "");
                    cell1.setCellStyle(dataStyle);

                    // Tên chương trình
                    Cell cell2 = row.createCell(2);
                    cell2.setCellValue(gg.getTenGiamGia() != null ? gg.getTenGiamGia() : "");
                    cell2.setCellStyle(dataStyleLeft);

                    // Loại áp dụng
                    Cell cell3 = row.createCell(3);
                    String loaiApDungStr = gg.getLoaiApDung() == 1 ? "Công khai" : "Cá nhân";
                    cell3.setCellValue(loaiApDungStr);
                    cell3.setCellStyle(dataStyle);

                    // Loại giảm
                    Cell cell4 = row.createCell(4);
                    String loaiGiamStr = gg.getLoaiGiamGia() != null ?
                            (gg.getLoaiGiamGia().equals("PhanTram") ? "Phần trăm" : "Số tiền") : "";
                    cell4.setCellValue(loaiGiamStr);
                    cell4.setCellStyle(dataStyle);

                    // Giá trị giảm
                    Cell cell5 = row.createCell(5);
                    String giaTriStr = "";
                    if (gg.getGiaTriGiam() != null) {
                        giaTriStr = formatter.format(gg.getGiaTriGiam()) +
                                (gg.getLoaiGiamGia() != null && gg.getLoaiGiamGia().equals("PhanTram") ? "%" : "₫");
                    }
                    cell5.setCellValue(giaTriStr);
                    cell5.setCellStyle(dataStyle);

                    // Đơn tối thiểu
                    Cell cell6 = row.createCell(6);
                    String donToiThieuStr = "";
                    if (gg.getDonToiThieu() != null) {
                        donToiThieuStr = formatter.format(gg.getDonToiThieu()) + "₫";
                    }
                    cell6.setCellValue(donToiThieuStr);
                    cell6.setCellStyle(dataStyle);

                    // ===== SỬA: Số lượng - Dựa vào IsVoHan =====
                    Cell cell7 = row.createCell(7);
                    if (gg.getIsVoHan() != null && gg.getIsVoHan()) {
                        // Nếu IsVoHan = true -> Vô hạn
                        cell7.setCellValue("♾️ Vô hạn");
                    } else {
                        // Nếu IsVoHan = false -> Hiển thị số lượng thực tế
                        if (gg.getSoLuong() != null) {
                            cell7.setCellValue(gg.getSoLuong());
                        } else {
                            cell7.setCellValue(0);
                        }
                    }
                    cell7.setCellStyle(dataStyle);

                    // ===== SỬA: Giảm tối đa =====
                    Cell cell8 = row.createCell(8);
                    String giamToiDaStr = "";
                    if (gg.getGiamToiDa() != null && gg.getGiamToiDa().compareTo(BigDecimal.ZERO) > 0) {
                        giamToiDaStr = formatter.format(gg.getGiamToiDa()) + "₫";
                    } else {
                        giamToiDaStr = "Không giới hạn";
                    }
                    cell8.setCellValue(giamToiDaStr);
                    cell8.setCellStyle(dataStyle);

                    // Ngày bắt đầu
                    Cell cell9 = row.createCell(9);
                    cell9.setCellValue(gg.getNgayBatDau() != null ? gg.getNgayBatDau().format(dateFormatter) : "");
                    cell9.setCellStyle(dataStyle);

                    // Ngày kết thúc
                    Cell cell10 = row.createCell(10);
                    cell10.setCellValue(gg.getNgayKetThuc() != null ? gg.getNgayKetThuc().format(dateFormatter) : "");
                    cell10.setCellStyle(dataStyle);

                    // Trạng thái
                    Cell cell11 = row.createCell(11);
                    cell11.setCellValue(gg.getTrangThai() != null ? gg.getTrangThai() : "");
                    cell11.setCellStyle(dataStyle);

                    rowNum++;
                }
            }

            workbook.write(out);
            out.flush();

            System.out.println("✅ Xuất Excel giảm giá thành công! " + (rowNum - 1) + " dòng.");
            System.out.println("📊 Dung lượng file: " + out.size() + " bytes");

            byte[] excelBytes = out.toByteArray();
            return new ByteArrayInputStream(excelBytes);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel giảm giá: " + e.getMessage());
            return null;
        }
    }

    public ByteArrayInputStream exportHoaDonToExcel(List<HoaDon> hoaDonList) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            System.out.println("📊 Bắt đầu xuất Excel hóa đơn - Số lượng: " +
                    (hoaDonList != null ? hoaDonList.size() : 0));

            Sheet sheet = workbook.createSheet("Danh sách hóa đơn");

            // Font header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            // Style header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style data
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style text left
            CellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.setBorderBottom(BorderStyle.THIN);
            dataStyleLeft.setBorderTop(BorderStyle.THIN);
            dataStyleLeft.setBorderLeft(BorderStyle.THIN);
            dataStyleLeft.setBorderRight(BorderStyle.THIN);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
            dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style right
            CellStyle dataStyleRight = workbook.createCellStyle();
            dataStyleRight.setBorderBottom(BorderStyle.THIN);
            dataStyleRight.setBorderTop(BorderStyle.THIN);
            dataStyleRight.setBorderLeft(BorderStyle.THIN);
            dataStyleRight.setBorderRight(BorderStyle.THIN);
            dataStyleRight.setAlignment(HorizontalAlignment.RIGHT);
            dataStyleRight.setVerticalAlignment(VerticalAlignment.CENTER);

            // Headers
            String[] headers = {"STT", "Mã HĐ", "Nhân viên", "Khách hàng", "SĐT KH",
                    "Tổng tiền", "Thanh toán", "Loại HĐ", "Ngày tạo", "Trạng thái"};
            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Set column widths
            sheet.setColumnWidth(0, 2000);
            sheet.setColumnWidth(1, 4000);
            sheet.setColumnWidth(2, 6000);
            sheet.setColumnWidth(3, 7000);
            sheet.setColumnWidth(4, 4000);
            sheet.setColumnWidth(5, 5000);
            sheet.setColumnWidth(6, 4000);
            sheet.setColumnWidth(7, 3000);
            sheet.setColumnWidth(8, 5000);
            sheet.setColumnWidth(9, 4000);

            // Định dạng số và ngày
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));

            // ⭐ SỬA: Format ngày giờ đầy đủ (vì ngayTao là LocalDateTime)
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // Đổ dữ liệu
            int rowNum = 1;

            if (hoaDonList != null && !hoaDonList.isEmpty()) {
                for (HoaDon hd : hoaDonList) {
                    Row row = sheet.createRow(rowNum);

                    // STT
                    Cell cell0 = row.createCell(0);
                    cell0.setCellValue(rowNum);
                    cell0.setCellStyle(dataStyle);

                    // Mã HĐ
                    Cell cell1 = row.createCell(1);
                    cell1.setCellValue("HD" + hd.getMaHoaDon());
                    cell1.setCellStyle(dataStyle);

                    // Nhân viên
                    Cell cell2 = row.createCell(2);
                    String nhanVien = "";
                    if (hd.getMaNhanVien() != null) {
                        nhanVien = hd.getMaNhanVien().getHoTen();
                    }
                    cell2.setCellValue(nhanVien);
                    cell2.setCellStyle(dataStyleLeft);

                    // Khách hàng
                    Cell cell3 = row.createCell(3);
                    String khachHang = "Khách lẻ";
                    if (hd.getMaKhachHang() != null) {
                        khachHang = hd.getMaKhachHang().getHoTen();
                    }
                    cell3.setCellValue(khachHang);
                    cell3.setCellStyle(dataStyleLeft);

                    // SĐT KH
                    Cell cell4 = row.createCell(4);
                    String sdt = "";
                    if (hd.getMaKhachHang() != null && hd.getMaKhachHang().getSdt() != null) {
                        sdt = hd.getMaKhachHang().getSdt();
                    }
                    cell4.setCellValue(sdt);
                    cell4.setCellStyle(dataStyle);

                    // Tổng tiền
                    Cell cell5 = row.createCell(5);
                    cell5.setCellValue(hd.getTongTien() != null ?
                            formatter.format(hd.getTongTien()) + " ₫" : "0 ₫");
                    cell5.setCellStyle(dataStyleRight);

                    // Thanh toán
                    Cell cell6 = row.createCell(6);
                    cell6.setCellValue(hd.getPhuongThucThanhToan() != null ?
                            hd.getPhuongThucThanhToan() : "");
                    cell6.setCellStyle(dataStyle);

                    // Loại HĐ
                    Cell cell7 = row.createCell(7);
                    cell7.setCellValue(hd.getLoaiBan() != null ? hd.getLoaiBan() : "");
                    cell7.setCellStyle(dataStyle);

                    // ⭐ Ngày tạo - Format đầy đủ ngày giờ
                    Cell cell8 = row.createCell(8);
                    String ngayTao = "";
                    if (hd.getNgayTao() != null) {
                        ngayTao = hd.getNgayTao().format(dateFormatter);
                    }
                    cell8.setCellValue(ngayTao);
                    cell8.setCellStyle(dataStyle);

                    // Trạng thái
                    Cell cell9 = row.createCell(9);
                    String trangThai = hd.getTrangThai() != null ? hd.getTrangThai() : "";

                    CellStyle statusStyle = workbook.createCellStyle();
                    statusStyle.cloneStyleFrom(dataStyle);
                    Font statusFont = workbook.createFont();
                    statusFont.setBold(true);

                    if ("Hoàn thành".equals(trangThai) || "Đã thanh toán".equals(trangThai)) {
                        statusFont.setColor(IndexedColors.GREEN.getIndex());
                    } else if ("Chờ xác nhận".equals(trangThai)) {
                        statusFont.setColor(IndexedColors.ORANGE.getIndex());
                    } else if ("Đã xác nhận".equals(trangThai) || "Đã giao".equals(trangThai)) {
                        statusFont.setColor(IndexedColors.BLUE.getIndex());
                    } else if ("Đang giao".equals(trangThai)) {
                        statusFont.setColor(IndexedColors.TEAL.getIndex());
                    } else if ("Đã trả hàng".equals(trangThai)) {
                        statusFont.setColor(IndexedColors.ORANGE.getIndex());
                    } else if ("Đã huỷ".equals(trangThai)) {
                        statusFont.setColor(IndexedColors.RED.getIndex());
                    }
                    statusStyle.setFont(statusFont);

                    Cell cell9Status = row.createCell(9);
                    cell9Status.setCellValue(trangThai);
                    cell9Status.setCellStyle(statusStyle);

                    rowNum++;
                }
            }

            workbook.write(out);
            out.flush();

            System.out.println("✅ Xuất Excel hóa đơn thành công! " + (rowNum - 1) + " dòng dữ liệu.");
            System.out.println("📊 Dung lượng file: " + out.size() + " bytes");

            byte[] excelBytes = out.toByteArray();
            return new ByteArrayInputStream(excelBytes);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel hóa đơn: " + e.getMessage());
            return null;
        }
    }

    public ByteArrayInputStream exportChiTietHoaDonToExcel(List<HoaDonChiTiet> chiTietList) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            System.out.println("📊 Bắt đầu xuất Excel chi tiết hóa đơn - Số lượng: " +
                    (chiTietList != null ? chiTietList.size() : 0));

            Sheet sheet = workbook.createSheet("Chi tiết hóa đơn");

            // Font header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            // Style header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style data
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style text left
            CellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.setBorderBottom(BorderStyle.THIN);
            dataStyleLeft.setBorderTop(BorderStyle.THIN);
            dataStyleLeft.setBorderLeft(BorderStyle.THIN);
            dataStyleLeft.setBorderRight(BorderStyle.THIN);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
            dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style right
            CellStyle dataStyleRight = workbook.createCellStyle();
            dataStyleRight.setBorderBottom(BorderStyle.THIN);
            dataStyleRight.setBorderTop(BorderStyle.THIN);
            dataStyleRight.setBorderLeft(BorderStyle.THIN);
            dataStyleRight.setBorderRight(BorderStyle.THIN);
            dataStyleRight.setAlignment(HorizontalAlignment.RIGHT);
            dataStyleRight.setVerticalAlignment(VerticalAlignment.CENTER);

            // Headers
            String[] headers = {"STT", "Mã HĐ", "Sản phẩm", "Kích cỡ", "Màu sắc",
                    "Số lượng", "Đơn giá", "Giảm giá", "Thành tiền"};
            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Set column widths
            sheet.setColumnWidth(0, 2000);  // STT
            sheet.setColumnWidth(1, 4000);  // Mã HĐ
            sheet.setColumnWidth(2, 7000);  // Sản phẩm
            sheet.setColumnWidth(3, 3000);  // Kích cỡ
            sheet.setColumnWidth(4, 4000);  // Màu sắc
            sheet.setColumnWidth(5, 3000);  // Số lượng
            sheet.setColumnWidth(6, 5000);  // Đơn giá
            sheet.setColumnWidth(7, 5000);  // Giảm giá
            sheet.setColumnWidth(8, 5000);  // Thành tiền

            // Định dạng số
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));

            // Đổ dữ liệu
            int rowNum = 1;

            if (chiTietList != null && !chiTietList.isEmpty()) {
                for (HoaDonChiTiet ct : chiTietList) {
                    Row row = sheet.createRow(rowNum);

                    // STT
                    Cell cell0 = row.createCell(0);
                    cell0.setCellValue(rowNum);
                    cell0.setCellStyle(dataStyle);

                    // Mã HĐ
                    Cell cell1 = row.createCell(1);
                    cell1.setCellValue("HD" + (ct.getMaHoaDon() != null ?
                            ct.getMaHoaDon().getMaHoaDon() : ""));
                    cell1.setCellStyle(dataStyle);

                    // Sản phẩm
                    Cell cell2 = row.createCell(2);
                    String tenSanPham = "";
                    if (ct.getSanPhamChiTiet() != null &&
                            ct.getSanPhamChiTiet().getSanPham() != null) {
                        tenSanPham = ct.getSanPhamChiTiet().getSanPham().getTenSanPham();
                    }
                    cell2.setCellValue(tenSanPham);
                    cell2.setCellStyle(dataStyleLeft);

                    // Kích cỡ
                    Cell cell3 = row.createCell(3);
                    String kichThuoc = "";
                    if (ct.getSanPhamChiTiet() != null &&
                            ct.getSanPhamChiTiet().getKichThuoc() != null) {
                        kichThuoc = ct.getSanPhamChiTiet().getKichThuoc().getTenKichThuoc();
                    }
                    cell3.setCellValue(kichThuoc);
                    cell3.setCellStyle(dataStyle);

                    // Màu sắc
                    Cell cell4 = row.createCell(4);
                    String mauSac = "";
                    if (ct.getSanPhamChiTiet() != null &&
                            ct.getSanPhamChiTiet().getMauSac() != null) {
                        mauSac = ct.getSanPhamChiTiet().getMauSac().getTenMauSac();
                    }
                    cell4.setCellValue(mauSac);
                    cell4.setCellStyle(dataStyle);

                    // Số lượng
                    Cell cell5 = row.createCell(5);
                    cell5.setCellValue(ct.getSoLuong() != null ? ct.getSoLuong() : 0);
                    cell5.setCellStyle(dataStyle);

                    // Đơn giá
                    Cell cell6 = row.createCell(6);
                    cell6.setCellValue(ct.getDonGia() != null ?
                            formatter.format(ct.getDonGia()) + " ₫" : "0 ₫");
                    cell6.setCellStyle(dataStyleRight);

                    // Giảm giá
                    Cell cell7 = row.createCell(7);
                    String giamGia = "Không áp dụng";
                    if (ct.getMaHoaDon() != null && ct.getMaHoaDon().getMaGiamGia() != null) {
                        GiamGia gg = ct.getMaHoaDon().getMaGiamGia();
                        if (gg.getLoaiGiamGia() != null && "PhanTram".equals(gg.getLoaiGiamGia())) {
                            giamGia = gg.getTenGiamGia() + " (-" +
                                    formatter.format(gg.getGiaTriGiam()) + "%)";
                        } else {
                            giamGia = gg.getTenGiamGia() + " (-" +
                                    formatter.format(gg.getGiaTriGiam()) + "₫)";
                        }
                    }
                    cell7.setCellValue(giamGia);
                    cell7.setCellStyle(dataStyleLeft);

                    // Thành tiền
                    Cell cell8 = row.createCell(8);
                    cell8.setCellValue(ct.getThanhTien() != null ?
                            formatter.format(ct.getThanhTien()) + " ₫" : "0 ₫");
                    cell8.setCellStyle(dataStyleRight);

                    rowNum++;
                    System.out.println("📝 Đã xuất dòng chi tiết: " + rowNum);
                }
            }

            workbook.write(out);
            out.flush();

            System.out.println("✅ Xuất Excel chi tiết hóa đơn thành công! " + (rowNum - 1) + " dòng.");
            System.out.println("📊 Dung lượng file: " + out.size() + " bytes");

            byte[] excelBytes = out.toByteArray();
            return new ByteArrayInputStream(excelBytes);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel chi tiết hóa đơn: " + e.getMessage());
            return null;
        }
    }

    public ByteArrayInputStream exportDotGiamGiaToExcel(List<DotGiamGia> dotGiamGiaList) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            System.out.println("📊 Bắt đầu xuất Excel đợt giảm giá - Số lượng: " +
                    (dotGiamGiaList != null ? dotGiamGiaList.size() : 0));

            Sheet sheet = workbook.createSheet("Danh sách đợt giảm giá");

            // Font header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            // Style header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style data (căn giữa)
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style text left
            CellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.setBorderBottom(BorderStyle.THIN);
            dataStyleLeft.setBorderTop(BorderStyle.THIN);
            dataStyleLeft.setBorderLeft(BorderStyle.THIN);
            dataStyleLeft.setBorderRight(BorderStyle.THIN);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
            dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);

            // Headers
            String[] headers = {"STT", "Mã đợt GG", "Tên đợt giảm giá", "Giá trị giảm",
                    "Ngày bắt đầu", "Ngày kết thúc", "Mô tả", "Trạng thái"};
            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Set column widths
            sheet.setColumnWidth(0, 2000);  // STT
            sheet.setColumnWidth(1, 4000);  // Mã đợt GG
            sheet.setColumnWidth(2, 8000);  // Tên đợt giảm giá
            sheet.setColumnWidth(3, 4000);  // Giá trị giảm
            sheet.setColumnWidth(4, 5000);  // Ngày bắt đầu
            sheet.setColumnWidth(5, 5000);  // Ngày kết thúc
            sheet.setColumnWidth(6, 10000); // Mô tả
            sheet.setColumnWidth(7, 4000);  // Trạng thái

            // Định dạng số và ngày
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // Đổ dữ liệu
            int rowNum = 1;

            if (dotGiamGiaList != null && !dotGiamGiaList.isEmpty()) {
                for (DotGiamGia dgg : dotGiamGiaList) {
                    Row row = sheet.createRow(rowNum);

                    // STT
                    Cell cell0 = row.createCell(0);
                    cell0.setCellValue(rowNum);
                    cell0.setCellStyle(dataStyle);

                    // Mã đợt GG
                    Cell cell1 = row.createCell(1);
                    cell1.setCellValue(dgg.getMaGiamGia() != null ? dgg.getMaGiamGia() : "");
                    cell1.setCellStyle(dataStyle);

                    // Tên đợt giảm giá
                    Cell cell2 = row.createCell(2);
                    cell2.setCellValue(dgg.getTenGiamGia() != null ? dgg.getTenGiamGia() : "");
                    cell2.setCellStyle(dataStyleLeft);

                    // Giá trị giảm
                    Cell cell3 = row.createCell(3);
                    String giaTriStr = "";
                    if (dgg.getGiaTriGiam() != null) {
                        giaTriStr = formatter.format(dgg.getGiaTriGiam()) + "%";
                    }
                    cell3.setCellValue(giaTriStr);
                    cell3.setCellStyle(dataStyle);

                    // Ngày bắt đầu
                    Cell cell4 = row.createCell(4);
                    cell4.setCellValue(dgg.getNgayBatDau() != null ? dgg.getNgayBatDau().format(dateFormatter) : "");
                    cell4.setCellStyle(dataStyle);

                    // Ngày kết thúc
                    Cell cell5 = row.createCell(5);
                    cell5.setCellValue(dgg.getNgayKetThuc() != null ? dgg.getNgayKetThuc().format(dateFormatter) : "");
                    cell5.setCellStyle(dataStyle);

                    // Mô tả
                    Cell cell6 = row.createCell(6);
                    cell6.setCellValue(dgg.getMoTa() != null ? dgg.getMoTa() : "");
                    cell6.setCellStyle(dataStyleLeft);

                    // Trạng thái
                    Cell cell7 = row.createCell(7);
                    String trangThai = dgg.getTrangThai() != null ? dgg.getTrangThai() : "";

                    // Tạo style cho trạng thái với màu sắc
                    CellStyle statusStyle = workbook.createCellStyle();
                    statusStyle.cloneStyleFrom(dataStyle);

                    // Tạo font cho status
                    Font statusFont = workbook.createFont();
                    statusFont.setBold(true);
                    statusStyle.setFont(statusFont);

                    // Màu sắc theo trạng thái
                    if ("Hoạt động".equals(trangThai)) {
                        statusFont.setColor(IndexedColors.GREEN.getIndex());
                    } else if ("Sắp hoạt động".equals(trangThai)) {
                        statusFont.setColor(IndexedColors.ORANGE.getIndex());
                    } else if ("Ngừng hoạt động".equals(trangThai) || "Đã huỷ".equals(trangThai)) {
                        statusFont.setColor(IndexedColors.RED.getIndex());
                    }

                    Cell cell7Status = row.createCell(7);
                    cell7Status.setCellValue(trangThai);
                    cell7Status.setCellStyle(statusStyle);

                    rowNum++;
                    System.out.println("📝 Đã xuất dòng đợt giảm giá: " + rowNum);
                }
            }

            workbook.write(out);
            out.flush();

            System.out.println("✅ Xuất Excel đợt giảm giá thành công! " + (rowNum - 1) + " dòng dữ liệu.");
            System.out.println("📊 Dung lượng file: " + out.size() + " bytes");

            byte[] excelBytes = out.toByteArray();
            return new ByteArrayInputStream(excelBytes);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel đợt giảm giá: " + e.getMessage());
            return null;
        }
    }

    public ByteArrayInputStream exportChatLieuToExcel(List<ChatLieu> chatLieuList) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            System.out.println("📊 Bắt đầu xuất Excel chất liệu - Số lượng: " +
                    (chatLieuList != null ? chatLieuList.size() : 0));

            if (chatLieuList == null || chatLieuList.isEmpty()) {
                Sheet sheet = workbook.createSheet("Danh sách chất liệu");
                Row headerRow = sheet.createRow(0);
                Cell cell = headerRow.createCell(0);
                cell.setCellValue("Không có dữ liệu để xuất!");
                workbook.write(out);
                return new ByteArrayInputStream(out.toByteArray());
            }

            Sheet sheet = workbook.createSheet("Danh sách chất liệu");

            // ===== TẠO STYLE =====
            // Font header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontName("Arial");

            // Style header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);

            // Style data center
            CellStyle dataStyleCenter = workbook.createCellStyle();
            dataStyleCenter.setBorderBottom(BorderStyle.THIN);
            dataStyleCenter.setBorderTop(BorderStyle.THIN);
            dataStyleCenter.setBorderLeft(BorderStyle.THIN);
            dataStyleCenter.setBorderRight(BorderStyle.THIN);
            dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
            dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style data left
            CellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.cloneStyleFrom(dataStyleCenter);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);

            // Style cho trạng thái hoạt động
            CellStyle statusStyleActive = workbook.createCellStyle();
            statusStyleActive.cloneStyleFrom(dataStyleCenter);
            Font activeFont = workbook.createFont();
            activeFont.setColor(IndexedColors.GREEN.getIndex());
            activeFont.setBold(true);
            statusStyleActive.setFont(activeFont);

            // Style cho trạng thái ngừng hoạt động
            CellStyle statusStyleInactive = workbook.createCellStyle();
            statusStyleInactive.cloneStyleFrom(dataStyleCenter);
            Font inactiveFont = workbook.createFont();
            inactiveFont.setColor(IndexedColors.RED.getIndex());
            inactiveFont.setBold(true);
            statusStyleInactive.setFont(inactiveFont);

            // ===== TẠO HEADER =====
            String[] headers = {"STT", "Mã chất liệu", "Tên chất liệu", "Trạng thái"};
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(30);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Điều chỉnh độ rộng cột
            sheet.setColumnWidth(0, 2500);  // STT
            sheet.setColumnWidth(1, 5000);  // Mã chất liệu
            sheet.setColumnWidth(2, 8000);  // Tên chất liệu
            sheet.setColumnWidth(3, 5000);  // Trạng thái

            // ===== ĐỔ DỮ LIỆU =====
            int rowNum = 1;

            for (ChatLieu cl : chatLieuList) {
                Row row = sheet.createRow(rowNum);
                row.setHeightInPoints(25);

                // STT
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum);
                cell0.setCellStyle(dataStyleCenter);

                // Mã chất liệu
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(cl.getMaChatLieu() != null ? cl.getMaChatLieu() : "");
                cell1.setCellStyle(dataStyleCenter);

                // Tên chất liệu
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(cl.getTenChatLieu() != null ? cl.getTenChatLieu() : "");
                cell2.setCellStyle(dataStyleLeft);

                // Trạng thái
                Cell cell3 = row.createCell(3);
                if (cl.isTrangThai()) {
                    cell3.setCellValue("Đang hoạt động");
                    cell3.setCellStyle(statusStyleActive);
                } else {
                    cell3.setCellValue("Ngừng hoạt động");
                    cell3.setCellStyle(statusStyleInactive);
                }

                rowNum++;
            }

            // ===== TẠO FOOTER THỐNG KÊ =====
            int lastRowNum = rowNum;
            Row footerRow = sheet.createRow(lastRowNum);
            footerRow.setHeightInPoints(25);

            long totalActive = chatLieuList.stream().filter(ChatLieu::isTrangThai).count();
            long totalInactive = chatLieuList.size() - totalActive;

            String footerText = String.format(
                    "Tổng: %d chất liệu | Đang hoạt động: %d | Ngừng hoạt động: %d",
                    chatLieuList.size(), totalActive, totalInactive
            );

            Cell footerCell = footerRow.createCell(0);
            footerCell.setCellValue(footerText);
            footerCell.setCellStyle(dataStyleLeft);

            // Merge footer
            sheet.addMergedRegion(new CellRangeAddress(lastRowNum, lastRowNum, 0, 3));

            // Tạo freeze pane (cố định header)
            sheet.createFreezePane(0, 1);

            // Ghi workbook
            workbook.write(out);
            out.flush();

            System.out.println("✅ Xuất Excel chất liệu thành công! " + (rowNum - 1) + " dòng dữ liệu.");
            System.out.println("📊 Dung lượng file: " + out.size() + " bytes");

            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel chất liệu: " + e.getMessage());
            return null;
        }
    }

    public ByteArrayInputStream exportDanhMucToExcel(List<DanhMucSanPham> danhMucList) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            System.out.println("📊 Bắt đầu xuất Excel danh mục - Số lượng: " +
                    (danhMucList != null ? danhMucList.size() : 0));

            if (danhMucList == null || danhMucList.isEmpty()) {
                Sheet sheet = workbook.createSheet("Danh sách danh mục");
                Row headerRow = sheet.createRow(0);
                Cell cell = headerRow.createCell(0);
                cell.setCellValue("Không có dữ liệu để xuất!");
                workbook.write(out);
                return new ByteArrayInputStream(out.toByteArray());
            }

            Sheet sheet = workbook.createSheet("Danh sách danh mục");

            // ===== TẠO STYLE =====
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontName("Arial");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);

            CellStyle dataStyleCenter = workbook.createCellStyle();
            dataStyleCenter.setBorderBottom(BorderStyle.THIN);
            dataStyleCenter.setBorderTop(BorderStyle.THIN);
            dataStyleCenter.setBorderLeft(BorderStyle.THIN);
            dataStyleCenter.setBorderRight(BorderStyle.THIN);
            dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
            dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.cloneStyleFrom(dataStyleCenter);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);

            CellStyle statusStyleActive = workbook.createCellStyle();
            statusStyleActive.cloneStyleFrom(dataStyleCenter);
            Font activeFont = workbook.createFont();
            activeFont.setColor(IndexedColors.GREEN.getIndex());
            activeFont.setBold(true);
            statusStyleActive.setFont(activeFont);

            CellStyle statusStyleInactive = workbook.createCellStyle();
            statusStyleInactive.cloneStyleFrom(dataStyleCenter);
            Font inactiveFont = workbook.createFont();
            inactiveFont.setColor(IndexedColors.RED.getIndex());
            inactiveFont.setBold(true);
            statusStyleInactive.setFont(inactiveFont);

            // ===== TẠO HEADER =====
            String[] headers = {"STT", "Mã danh mục", "Tên danh mục", "Mô tả", "Trạng thái"};
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(30);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            sheet.setColumnWidth(0, 2500);
            sheet.setColumnWidth(1, 5000);
            sheet.setColumnWidth(2, 8000);
            sheet.setColumnWidth(3, 12000);
            sheet.setColumnWidth(4, 5000);

            // ===== ĐỔ DỮ LIỆU =====
            int rowNum = 1;

            for (DanhMucSanPham dm : danhMucList) {
                Row row = sheet.createRow(rowNum);
                row.setHeightInPoints(25);

                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum);
                cell0.setCellStyle(dataStyleCenter);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(dm.getMaDanhMuc() != null ? dm.getMaDanhMuc() : "");
                cell1.setCellStyle(dataStyleCenter);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(dm.getTenDanhMuc() != null ? dm.getTenDanhMuc() : "");
                cell2.setCellStyle(dataStyleLeft);

                Cell cell3 = row.createCell(3);
                cell3.setCellValue(dm.getMoTa() != null ? dm.getMoTa() : "");
                cell3.setCellStyle(dataStyleLeft);

                Cell cell4 = row.createCell(4);
                if (dm.isTrangThai()) {
                    cell4.setCellValue("Đang hoạt động");
                    cell4.setCellStyle(statusStyleActive);
                } else {
                    cell4.setCellValue("Ngừng hoạt động");
                    cell4.setCellStyle(statusStyleInactive);
                }

                rowNum++;
            }

            // ===== TẠO FOOTER THỐNG KÊ =====
            int lastRowNum = rowNum;
            Row footerRow = sheet.createRow(lastRowNum);
            footerRow.setHeightInPoints(25);

            long totalActive = danhMucList.stream().filter(DanhMucSanPham::isTrangThai).count();
            long totalInactive = danhMucList.size() - totalActive;

            String footerText = String.format(
                    "Tổng: %d danh mục | Đang hoạt động: %d | Ngừng hoạt động: %d",
                    danhMucList.size(), totalActive, totalInactive
            );

            Cell footerCell = footerRow.createCell(0);
            footerCell.setCellValue(footerText);
            footerCell.setCellStyle(dataStyleLeft);

            sheet.addMergedRegion(new CellRangeAddress(lastRowNum, lastRowNum, 0, 4));
            sheet.createFreezePane(0, 1);

            workbook.write(out);
            out.flush();

            System.out.println("✅ Xuất Excel danh mục thành công! " + (rowNum - 1) + " dòng dữ liệu.");
            System.out.println("📊 Dung lượng file: " + out.size() + " bytes");

            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel danh mục: " + e.getMessage());
            return null;
        }
    }

    public ByteArrayInputStream exportKichThuocToExcel(List<KichThuoc> kichThuocList) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            System.out.println("📊 Bắt đầu xuất Excel kích thước - Số lượng: " +
                    (kichThuocList != null ? kichThuocList.size() : 0));

            if (kichThuocList == null || kichThuocList.isEmpty()) {
                Sheet sheet = workbook.createSheet("Danh sách kích thước");
                Row headerRow = sheet.createRow(0);
                Cell cell = headerRow.createCell(0);
                cell.setCellValue("Không có dữ liệu để xuất!");
                workbook.write(out);
                return new ByteArrayInputStream(out.toByteArray());
            }

            Sheet sheet = workbook.createSheet("Danh sách kích thước");

            // ===== TẠO STYLE =====
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontName("Arial");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);

            CellStyle dataStyleCenter = workbook.createCellStyle();
            dataStyleCenter.setBorderBottom(BorderStyle.THIN);
            dataStyleCenter.setBorderTop(BorderStyle.THIN);
            dataStyleCenter.setBorderLeft(BorderStyle.THIN);
            dataStyleCenter.setBorderRight(BorderStyle.THIN);
            dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
            dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.cloneStyleFrom(dataStyleCenter);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);

            CellStyle statusStyleActive = workbook.createCellStyle();
            statusStyleActive.cloneStyleFrom(dataStyleCenter);
            Font activeFont = workbook.createFont();
            activeFont.setColor(IndexedColors.GREEN.getIndex());
            activeFont.setBold(true);
            statusStyleActive.setFont(activeFont);

            CellStyle statusStyleInactive = workbook.createCellStyle();
            statusStyleInactive.cloneStyleFrom(dataStyleCenter);
            Font inactiveFont = workbook.createFont();
            inactiveFont.setColor(IndexedColors.RED.getIndex());
            inactiveFont.setBold(true);
            statusStyleInactive.setFont(inactiveFont);

            // ===== TẠO HEADER =====
            String[] headers = {"STT", "Mã kích thước", "Tên kích thước", "Trạng thái"};
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(30);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            sheet.setColumnWidth(0, 2500);
            sheet.setColumnWidth(1, 5000);
            sheet.setColumnWidth(2, 5000);
            sheet.setColumnWidth(3, 5000);

            // ===== ĐỔ DỮ LIỆU =====
            int rowNum = 1;

            for (KichThuoc kt : kichThuocList) {
                Row row = sheet.createRow(rowNum);
                row.setHeightInPoints(25);

                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum);
                cell0.setCellStyle(dataStyleCenter);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(kt.getMaKichThuoc() != null ? kt.getMaKichThuoc() : "");
                cell1.setCellStyle(dataStyleCenter);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(kt.getTenKichThuoc() != null ? kt.getTenKichThuoc() : "");
                cell2.setCellStyle(dataStyleLeft);

                Cell cell3 = row.createCell(3);
                if (kt.isTrangThai()) {
                    cell3.setCellValue("Đang hoạt động");
                    cell3.setCellStyle(statusStyleActive);
                } else {
                    cell3.setCellValue("Ngừng hoạt động");
                    cell3.setCellStyle(statusStyleInactive);
                }

                rowNum++;
            }

            // ===== TẠO FOOTER THỐNG KÊ =====
            int lastRowNum = rowNum;
            Row footerRow = sheet.createRow(lastRowNum);
            footerRow.setHeightInPoints(25);

            long totalActive = kichThuocList.stream().filter(KichThuoc::isTrangThai).count();
            long totalInactive = kichThuocList.size() - totalActive;

            String footerText = String.format(
                    "Tổng: %d kích thước | Đang hoạt động: %d | Ngừng hoạt động: %d",
                    kichThuocList.size(), totalActive, totalInactive
            );

            Cell footerCell = footerRow.createCell(0);
            footerCell.setCellValue(footerText);
            footerCell.setCellStyle(dataStyleLeft);

            sheet.addMergedRegion(new CellRangeAddress(lastRowNum, lastRowNum, 0, 3));
            sheet.createFreezePane(0, 1);

            workbook.write(out);
            out.flush();

            System.out.println("✅ Xuất Excel kích thước thành công! " + (rowNum - 1) + " dòng dữ liệu.");
            System.out.println("📊 Dung lượng file: " + out.size() + " bytes");

            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel kích thước: " + e.getMessage());
            return null;
        }
    }

    public ByteArrayInputStream exportKieuGiayToExcel(List<KieuGiay> kieuGiayList) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            System.out.println("📊 Bắt đầu xuất Excel kiểu giày - Số lượng: " +
                    (kieuGiayList != null ? kieuGiayList.size() : 0));

            if (kieuGiayList == null || kieuGiayList.isEmpty()) {
                Sheet sheet = workbook.createSheet("Danh sách kiểu giày");
                Row headerRow = sheet.createRow(0);
                Cell cell = headerRow.createCell(0);
                cell.setCellValue("Không có dữ liệu để xuất!");
                workbook.write(out);
                return new ByteArrayInputStream(out.toByteArray());
            }

            Sheet sheet = workbook.createSheet("Danh sách kiểu giày");

            // Font header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontName("Arial");

            // Style header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);

            // Style data
            CellStyle dataStyleCenter = workbook.createCellStyle();
            dataStyleCenter.setBorderBottom(BorderStyle.THIN);
            dataStyleCenter.setBorderTop(BorderStyle.THIN);
            dataStyleCenter.setBorderLeft(BorderStyle.THIN);
            dataStyleCenter.setBorderRight(BorderStyle.THIN);
            dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
            dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.cloneStyleFrom(dataStyleCenter);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);

            // Style status
            CellStyle statusStyleActive = workbook.createCellStyle();
            statusStyleActive.cloneStyleFrom(dataStyleCenter);
            Font activeFont = workbook.createFont();
            activeFont.setColor(IndexedColors.GREEN.getIndex());
            activeFont.setBold(true);
            statusStyleActive.setFont(activeFont);

            CellStyle statusStyleInactive = workbook.createCellStyle();
            statusStyleInactive.cloneStyleFrom(dataStyleCenter);
            Font inactiveFont = workbook.createFont();
            inactiveFont.setColor(IndexedColors.RED.getIndex());
            inactiveFont.setBold(true);
            statusStyleInactive.setFont(inactiveFont);

            // Headers
            String[] headers = {"STT", "Mã kiểu giày", "Tên kiểu giày", "Trạng thái"};
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(30);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Set column widths
            sheet.setColumnWidth(0, 2500);
            sheet.setColumnWidth(1, 5000);
            sheet.setColumnWidth(2, 8000);
            sheet.setColumnWidth(3, 5000);

            // Fill data
            int rowNum = 1;

            for (KieuGiay kg : kieuGiayList) {
                Row row = sheet.createRow(rowNum);
                row.setHeightInPoints(25);

                // STT
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum);
                cell0.setCellStyle(dataStyleCenter);

                // Mã kiểu giày
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(kg.getMaKieuGiay() != null ? kg.getMaKieuGiay() : "");
                cell1.setCellStyle(dataStyleCenter);

                // Tên kiểu giày
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(kg.getTenKieuGiay() != null ? kg.getTenKieuGiay() : "");
                cell2.setCellStyle(dataStyleLeft);

                // Trạng thái
                Cell cell3 = row.createCell(3);
                if (kg.isTrangThai()) {
                    cell3.setCellValue("Đang hoạt động");
                    cell3.setCellStyle(statusStyleActive);
                } else {
                    cell3.setCellValue("Ngừng hoạt động");
                    cell3.setCellStyle(statusStyleInactive);
                }

                rowNum++;
            }

            // Footer
            int lastRowNum = rowNum;
            Row footerRow = sheet.createRow(lastRowNum);
            footerRow.setHeightInPoints(25);

            long totalActive = kieuGiayList.stream().filter(KieuGiay::isTrangThai).count();
            long totalInactive = kieuGiayList.size() - totalActive;

            String footerText = String.format(
                    "Tổng: %d kiểu giày | Đang hoạt động: %d | Ngừng hoạt động: %d",
                    kieuGiayList.size(), totalActive, totalInactive
            );

            Cell footerCell = footerRow.createCell(0);
            footerCell.setCellValue(footerText);
            footerCell.setCellStyle(dataStyleLeft);

            sheet.addMergedRegion(new CellRangeAddress(lastRowNum, lastRowNum, 0, 3));
            sheet.createFreezePane(0, 1);

            workbook.write(out);
            out.flush();

            System.out.println("✅ Xuất Excel kiểu giày thành công! " + (rowNum - 1) + " dòng dữ liệu.");
            System.out.println("📊 Dung lượng file: " + out.size() + " bytes");

            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel kiểu giày: " + e.getMessage());
            return null;
        }
    }

    public ByteArrayInputStream exportMauSacToExcel(List<MauSac> mauSacList) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            System.out.println("📊 Bắt đầu xuất Excel màu sắc - Số lượng: " +
                    (mauSacList != null ? mauSacList.size() : 0));

            if (mauSacList == null || mauSacList.isEmpty()) {
                Sheet sheet = workbook.createSheet("Danh sách màu sắc");
                Row headerRow = sheet.createRow(0);
                Cell cell = headerRow.createCell(0);
                cell.setCellValue("Không có dữ liệu để xuất!");
                workbook.write(out);
                return new ByteArrayInputStream(out.toByteArray());
            }

            Sheet sheet = workbook.createSheet("Danh sách màu sắc");

            // Font header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontName("Arial");

            // Style header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);

            // Style data
            CellStyle dataStyleCenter = workbook.createCellStyle();
            dataStyleCenter.setBorderBottom(BorderStyle.THIN);
            dataStyleCenter.setBorderTop(BorderStyle.THIN);
            dataStyleCenter.setBorderLeft(BorderStyle.THIN);
            dataStyleCenter.setBorderRight(BorderStyle.THIN);
            dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
            dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.cloneStyleFrom(dataStyleCenter);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);

            // Style status
            CellStyle statusStyleActive = workbook.createCellStyle();
            statusStyleActive.cloneStyleFrom(dataStyleCenter);
            Font activeFont = workbook.createFont();
            activeFont.setColor(IndexedColors.GREEN.getIndex());
            activeFont.setBold(true);
            statusStyleActive.setFont(activeFont);

            CellStyle statusStyleInactive = workbook.createCellStyle();
            statusStyleInactive.cloneStyleFrom(dataStyleCenter);
            Font inactiveFont = workbook.createFont();
            inactiveFont.setColor(IndexedColors.RED.getIndex());
            inactiveFont.setBold(true);
            statusStyleInactive.setFont(inactiveFont);

            // Headers
            String[] headers = {"STT", "Mã màu sắc", "Tên màu sắc", "Trạng thái"};
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(30);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Set column widths
            sheet.setColumnWidth(0, 2500);
            sheet.setColumnWidth(1, 5000);
            sheet.setColumnWidth(2, 8000);
            sheet.setColumnWidth(3, 5000);

            // Fill data
            int rowNum = 1;

            for (MauSac ms : mauSacList) {
                Row row = sheet.createRow(rowNum);
                row.setHeightInPoints(25);

                // STT
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum);
                cell0.setCellStyle(dataStyleCenter);

                // Mã màu sắc
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(ms.getMaMauSac() != null ? ms.getMaMauSac() : "");
                cell1.setCellStyle(dataStyleCenter);

                // Tên màu sắc
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(ms.getTenMauSac() != null ? ms.getTenMauSac() : "");
                cell2.setCellStyle(dataStyleLeft);

                // Trạng thái
                Cell cell3 = row.createCell(3);
                if (ms.isTrangThai()) {
                    cell3.setCellValue("Đang hoạt động");
                    cell3.setCellStyle(statusStyleActive);
                } else {
                    cell3.setCellValue("Ngừng hoạt động");
                    cell3.setCellStyle(statusStyleInactive);
                }

                rowNum++;
            }

            // Footer
            int lastRowNum = rowNum;
            Row footerRow = sheet.createRow(lastRowNum);
            footerRow.setHeightInPoints(25);

            long totalActive = mauSacList.stream().filter(MauSac::isTrangThai).count();
            long totalInactive = mauSacList.size() - totalActive;

            String footerText = String.format(
                    "Tổng: %d màu sắc | Đang hoạt động: %d | Ngừng hoạt động: %d",
                    mauSacList.size(), totalActive, totalInactive
            );

            Cell footerCell = footerRow.createCell(0);
            footerCell.setCellValue(footerText);
            footerCell.setCellStyle(dataStyleLeft);

            sheet.addMergedRegion(new CellRangeAddress(lastRowNum, lastRowNum, 0, 3));
            sheet.createFreezePane(0, 1);

            workbook.write(out);
            out.flush();

            System.out.println("✅ Xuất Excel màu sắc thành công! " + (rowNum - 1) + " dòng dữ liệu.");
            System.out.println("📊 Dung lượng file: " + out.size() + " bytes");

            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel màu sắc: " + e.getMessage());
            return null;
        }
    }

    public ByteArrayInputStream exportThuongHieuToExcel(List<ThuongHieu> thuongHieuList) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            System.out.println("📊 Bắt đầu xuất Excel thương hiệu - Số lượng: " +
                    (thuongHieuList != null ? thuongHieuList.size() : 0));

            if (thuongHieuList == null || thuongHieuList.isEmpty()) {
                Sheet sheet = workbook.createSheet("Danh sách thương hiệu");
                Row headerRow = sheet.createRow(0);
                Cell cell = headerRow.createCell(0);
                cell.setCellValue("Không có dữ liệu để xuất!");
                workbook.write(out);
                return new ByteArrayInputStream(out.toByteArray());
            }

            Sheet sheet = workbook.createSheet("Danh sách thương hiệu");

            // Font header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontName("Arial");

            // Style header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);

            // Style data
            CellStyle dataStyleCenter = workbook.createCellStyle();
            dataStyleCenter.setBorderBottom(BorderStyle.THIN);
            dataStyleCenter.setBorderTop(BorderStyle.THIN);
            dataStyleCenter.setBorderLeft(BorderStyle.THIN);
            dataStyleCenter.setBorderRight(BorderStyle.THIN);
            dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
            dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dataStyleLeft = workbook.createCellStyle();
            dataStyleLeft.cloneStyleFrom(dataStyleCenter);
            dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);

            // Style status
            CellStyle statusStyleActive = workbook.createCellStyle();
            statusStyleActive.cloneStyleFrom(dataStyleCenter);
            Font activeFont = workbook.createFont();
            activeFont.setColor(IndexedColors.GREEN.getIndex());
            activeFont.setBold(true);
            statusStyleActive.setFont(activeFont);

            CellStyle statusStyleInactive = workbook.createCellStyle();
            statusStyleInactive.cloneStyleFrom(dataStyleCenter);
            Font inactiveFont = workbook.createFont();
            inactiveFont.setColor(IndexedColors.RED.getIndex());
            inactiveFont.setBold(true);
            statusStyleInactive.setFont(inactiveFont);

            // Headers
            String[] headers = {"STT", "Mã thương hiệu", "Tên thương hiệu", "Trạng thái"};
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(30);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Set column widths
            sheet.setColumnWidth(0, 2500);
            sheet.setColumnWidth(1, 5000);
            sheet.setColumnWidth(2, 8000);
            sheet.setColumnWidth(3, 5000);

            // Fill data
            int rowNum = 1;

            for (ThuongHieu th : thuongHieuList) {
                Row row = sheet.createRow(rowNum);
                row.setHeightInPoints(25);

                // STT
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum);
                cell0.setCellStyle(dataStyleCenter);

                // Mã thương hiệu
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(th.getMaThuongHieu() != null ? th.getMaThuongHieu() : "");
                cell1.setCellStyle(dataStyleCenter);

                // Tên thương hiệu
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(th.getTenThuongHieu() != null ? th.getTenThuongHieu() : "");
                cell2.setCellStyle(dataStyleLeft);

                // Trạng thái
                Cell cell3 = row.createCell(3);
                if (th.isTrangThai()) {
                    cell3.setCellValue("Đang hoạt động");
                    cell3.setCellStyle(statusStyleActive);
                } else {
                    cell3.setCellValue("Ngừng hoạt động");
                    cell3.setCellStyle(statusStyleInactive);
                }

                rowNum++;
            }

            // Footer
            int lastRowNum = rowNum;
            Row footerRow = sheet.createRow(lastRowNum);
            footerRow.setHeightInPoints(25);

            long totalActive = thuongHieuList.stream().filter(ThuongHieu::isTrangThai).count();
            long totalInactive = thuongHieuList.size() - totalActive;

            String footerText = String.format(
                    "Tổng: %d thương hiệu | Đang hoạt động: %d | Ngừng hoạt động: %d",
                    thuongHieuList.size(), totalActive, totalInactive
            );

            Cell footerCell = footerRow.createCell(0);
            footerCell.setCellValue(footerText);
            footerCell.setCellStyle(dataStyleLeft);

            sheet.addMergedRegion(new CellRangeAddress(lastRowNum, lastRowNum, 0, 3));
            sheet.createFreezePane(0, 1);

            workbook.write(out);
            out.flush();

            System.out.println("✅ Xuất Excel thương hiệu thành công! " + (rowNum - 1) + " dòng dữ liệu.");
            System.out.println("📊 Dung lượng file: " + out.size() + " bytes");

            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi xuất Excel thương hiệu: " + e.getMessage());
            return null;
        }
    }
}