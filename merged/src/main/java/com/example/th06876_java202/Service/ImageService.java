    package com.example.th06876_java202.Service;

    import com.example.th06876_java202.Entity.ImageInfo;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Service;
    import java.io.IOException;
    import java.nio.file.*;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.stream.Collectors;

    @Service
    public class ImageService {




        @Value("${upload.path:D:/AnhSP/}")
        private String uploadPath;

        public List<String> getAllImages() throws IOException {
            Path uploadPath = Paths.get(this.uploadPath);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                return new ArrayList<>();
            }

            try (var stream = Files.list(uploadPath)) {
                return stream
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(fileName -> {
                            // Chỉ lấy file ảnh
                            String lower = fileName.toLowerCase();
                            return lower.endsWith(".jpg") ||
                                    lower.endsWith(".jpeg") ||
                                    lower.endsWith(".png") ||
                                    lower.endsWith(".gif") ||
                                    lower.endsWith(".webp");
                        })
                        .collect(Collectors.toList());
            }
        }

        public List<ImageInfo> getImageInfoList() throws IOException {
            Path uploadPath = Paths.get(this.uploadPath);
            List<ImageInfo> imageInfos = new ArrayList<>();

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                return imageInfos;
            }

            try (var stream = Files.list(uploadPath)) {
                stream.filter(Files::isRegularFile)
                        .forEach(path -> {
                            String fileName = path.getFileName().toString();
                            try {
                                long fileSize = Files.size(path);
                                String fileSizeStr = formatFileSize(fileSize);
                                String extension = getFileExtension(fileName);

                                imageInfos.add(new ImageInfo(
                                        fileName,
                                        extension,
                                        fileSizeStr,
                                        Files.getLastModifiedTime(path).toString()
                                ));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }

            return imageInfos;
        }

        private String formatFileSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return (size / 1024) + " KB";
            if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)) + " MB";
            return (size / (1024 * 1024 * 1024)) + " GB";
        }

        private String getFileExtension(String fileName) {
            int lastDot = fileName.lastIndexOf(".");
            if (lastDot > 0) {
                return fileName.substring(lastDot + 1).toUpperCase();
            }
            return "";
        }


        public boolean deleteImage(String fileName) {
            if (fileName == null || fileName.isEmpty()) return false;
            try {
                Path filePath = Paths.get(this.uploadPath, fileName);
                return Files.deleteIfExists(filePath);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        public String getImagePath(String fileName) {
            if (fileName == null || fileName.isEmpty()) return "";
            return "/images/" + fileName;
        }

        public boolean imageExists(String fileName) {
            if (fileName == null || fileName.isEmpty()) {
                return false;
            }
            Path path = Paths.get(uploadPath, fileName);
            return Files.exists(path) && Files.isRegularFile(path);
        }

        public List<String> filterExistingImages(List<String> imageNames) {
            if (imageNames == null || imageNames.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> existing = new ArrayList<>();
            for (String fileName : imageNames) {
                if (imageExists(fileName)) {
                    existing.add(fileName);
                }
            }
            return existing;
        }
    }