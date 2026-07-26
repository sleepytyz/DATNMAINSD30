package com.example.th06876_java202.Entity;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

public class FileUploadUtil {

    private static final String UPLOAD_DIR = "D:/AnhSP/";

    public static String saveFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new IOException("Tên file không hợp lệ!");
        }

        Path filePath = uploadPath.resolve(originalFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println(" Đã lưu ảnh với tên gốc: " + originalFileName);
        return originalFileName;
    }

    public static boolean deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        try {
            Path filePath = Paths.get(UPLOAD_DIR, fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("🗑️ Đã xóa file: " + fileName);
                return true;
            }
        } catch (IOException e) {
            System.err.println("⚠️ Không xóa được file: " + fileName + " - " + e.getMessage());
        }
        return false;
    }

    public static boolean fileExists(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        Path filePath = Paths.get(UPLOAD_DIR, fileName);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }
}