package com.example.th06876_java202.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageInfo {
    private String fileName;
    private String extension;
    private String fileSize;
    private String lastModified;

    public String getImageUrl() {
        return "/images/" + fileName;
    }
}