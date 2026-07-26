package com.example.th06876_java202.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:///D:/AnhSP/");
    }
    @PostConstruct
    public void initDirectories() {
        try {
            Path qrPath = Paths.get("D:\\QRSanPham");
            if (!Files.exists(qrPath)) {
                Files.createDirectories(qrPath);
                System.out.println("✅ Đã tạo thư mục: " + qrPath.toAbsolutePath());
            } else {
                System.out.println("✅ Thư mục đã tồn tại: " + qrPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("❌ Không thể tạo thư mục D:\\QRSanPham: " + e.getMessage());
        }
    }
}