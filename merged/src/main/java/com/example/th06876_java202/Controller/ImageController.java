package com.example.th06876_java202.Controller;

import com.example.th06876_java202.Entity.ImageInfo;
import com.example.th06876_java202.Service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getImageList() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ImageInfo> images = imageService.getImageInfoList();
            response.put("success", true);
            response.put("images", images);
            response.put("count", images.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkImageExists(
            @RequestParam String fileName) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean exists = imageService.imageExists(fileName);
            response.put("success", true);
            response.put("exists", exists);
            response.put("fileName", fileName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


}