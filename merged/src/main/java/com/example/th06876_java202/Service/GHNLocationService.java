package com.example.th06876_java202.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GHNLocationService {

    @Value("${ghn.api.token}")
    private String apiToken;

    @Value("${ghn.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ⭐ CACHE TRONG RAM - LƯU WARD CODE
    private final Map<String, String> wardCodeCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> districtIdCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> provinceIdCache = new ConcurrentHashMap<>();

    public GHNLocationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Lấy Ward Code từ GHN API (có cache)
     */
    public String getWardCode(String wardName, String districtName, String provinceName) {
        if (wardName == null || wardName.trim().isEmpty()) {
            return null;
        }

        // ⭐ TẠO KEY CHO CACHE
        String cacheKey = provinceName + "|" + districtName + "|" + wardName;

        // ⭐ KIỂM TRA CACHE
        if (wardCodeCache.containsKey(cacheKey)) {
            System.out.println("📦 Lấy Ward Code từ cache: " + wardCodeCache.get(cacheKey));
            return wardCodeCache.get(cacheKey);
        }

        try {
            // Lấy Province ID
            Integer provinceId = getProvinceId(provinceName);
            if (provinceId == null) {
                return null;
            }

            // Lấy District ID
            Integer districtId = getDistrictId(districtName, provinceId);
            if (districtId == null) {
                return null;
            }

            // ⭐ GỌI API GHN LẤY DANH SÁCH PHƯỜNG/XÃ
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", apiToken);
            headers.set("Content-Type", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl + "/master-data/ward?district_id=" + districtId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");

                for (JsonNode item : data) {
                    String name = item.path("WardName").asText();
                    String wardCode = item.path("WardCode").asText();

                    // ⭐ LƯU VÀO CACHE THEO TÊN ĐẦY ĐỦ
                    wardCodeCache.put(cacheKey, wardCode);

                    // ⭐ LƯU THÊM CACHE THEO TÊN NGẮN
                    wardCodeCache.put(districtId + "|" + name, wardCode);

                    if (name.equalsIgnoreCase(wardName) ||
                            name.toLowerCase().contains(wardName.toLowerCase()) ||
                            wardName.toLowerCase().contains(name.toLowerCase())) {
                        System.out.println("✅ Tìm thấy Ward Code: " + wardCode + " cho " + wardName);
                        return wardCode;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi lấy Ward Code: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy District ID từ GHN API (có cache)
     */
    public Integer getDistrictId(String districtName, Integer provinceId) {
        if (districtName == null || districtName.trim().isEmpty() || provinceId == null) {
            return null;
        }

        String cacheKey = provinceId + "|" + districtName;

        if (districtIdCache.containsKey(cacheKey)) {
            return districtIdCache.get(cacheKey);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", apiToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl + "/master-data/district?province_id=" + provinceId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");

                for (JsonNode item : data) {
                    String name = item.path("DistrictName").asText();
                    Integer id = item.path("DistrictID").asInt();

                    districtIdCache.put(provinceId + "|" + name, id);

                    if (name.equalsIgnoreCase(districtName) ||
                            name.toLowerCase().contains(districtName.toLowerCase()) ||
                            districtName.toLowerCase().contains(name.toLowerCase())) {
                        return id;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi lấy District ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy Province ID từ GHN API (có cache)
     */
    public Integer getProvinceId(String provinceName) {
        if (provinceName == null || provinceName.trim().isEmpty()) {
            return null;
        }

        if (provinceIdCache.containsKey(provinceName)) {
            return provinceIdCache.get(provinceName);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", apiToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl + "/master-data/province",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");

                for (JsonNode item : data) {
                    String name = item.path("ProvinceName").asText();
                    Integer id = item.path("ProvinceID").asInt();
                    provinceIdCache.put(name, id);

                    if (name.equalsIgnoreCase(provinceName) ||
                            name.toLowerCase().contains(provinceName.toLowerCase()) ||
                            provinceName.toLowerCase().contains(name.toLowerCase())) {
                        return id;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi lấy Province ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy tất cả thông tin vị trí
     */
    public Map<String, Object> getLocationInfo(String wardName, String districtName, String provinceName) {
        Map<String, Object> result = new java.util.HashMap<>();

        try {
            Integer provinceId = getProvinceId(provinceName);
            if (provinceId != null) {
                result.put("provinceId", provinceId);

                Integer districtId = getDistrictId(districtName, provinceId);
                if (districtId != null) {
                    result.put("districtId", districtId);

                    String wardCode = getWardCode(wardName, districtName, provinceName);
                    if (wardCode != null) {
                        result.put("wardCode", wardCode);
                    }
                }
            }

            result.put("success", !result.isEmpty());

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Xóa cache (gọi khi cần refresh)
     */
    public void clearCache() {
        wardCodeCache.clear();
        districtIdCache.clear();
        provinceIdCache.clear();
        System.out.println("🗑️ Đã xóa cache");
    }
}