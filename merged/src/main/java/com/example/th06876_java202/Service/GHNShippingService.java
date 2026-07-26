package com.example.th06876_java202.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class GHNShippingService {

    @Autowired
    private GHNLocationService ghnLocationService;

    @Value("${ghn.api.token}")
    private String apiToken;

    @Value("${ghn.shop.id}")
    private int shopId;

    @Value("${ghn.api.url}")
    private String apiUrl;

    // ===== ĐỊA CHỈ GỬI =====
    @Value("${ghn.shop.province.id:201}")
    private int fromProvinceId;

    @Value("${ghn.shop.district.id:1442}")
    private int fromDistrictId;

    @Value("${ghn.shop.ward.code:1A0401}")
    private String fromWardCode;

    @Value("${ghn.shop.address:123 Hoang Hoa Tham}")
    private String fromAddress;

    @Value("${ghn.shop.district.name:Ba Dinh}")
    private String fromDistrictName;

    @Value("${ghn.shop.ward.name:Ngoc Ha}")
    private String fromWardName;

    @Value("${ghn.shop.province.name:Ha Noi}")
    private String fromProvinceName;

    @Value("${ghn.service.type.id:2}")
    private int serviceTypeId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GHNShippingService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String getApiToken() {
        return apiToken;
    }

    public int getShopId() {
        return shopId;
    }

    /**
     * Lấy thông tin địa chỉ gửi
     */
    public Map<String, Object> getFromAddress() {
        Map<String, Object> address = new HashMap<>();
        address.put("address", fromAddress);
        address.put("ward", fromWardName);
        address.put("district", fromDistrictName);
        address.put("province", fromProvinceName);
        address.put("districtId", fromDistrictId);
        address.put("wardCode", fromWardCode);
        address.put("provinceId", fromProvinceId);
        address.put("fullAddress", fromAddress + ", " + fromWardName + ", " + fromDistrictName + ", " + fromProvinceName);
        return address;
    }



    public BigDecimal calculateShippingFee(
            String toDistrict,
            String toWardName,
            String toProvince,
            int weight,
            BigDecimal amount) {

        try {
            System.out.println("========== TÍNH PHÍ SHIP GHN ==========");
            System.out.println("📍 Địa chỉ gửi: " + fromAddress + ", " + fromWardName + ", " + fromDistrictName);
            System.out.println("📍 Địa chỉ nhận: " + toDistrict + " - " + toWardName);

            String wardCode = ghnLocationService.getWardCode(toWardName, toDistrict, toProvince);

            System.out.println("✅ Ward Code: " + wardCode);


            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", apiToken);
            headers.set("ShopId", String.valueOf(shopId));
            headers.set("Content-Type", "application/json");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("service_type_id", serviceTypeId);
            requestBody.put("insurance_value", amount != null ? amount.intValue() : 0);
            requestBody.put("from_district_id", fromDistrictId);
            requestBody.put("to_ward_code", wardCode);
            requestBody.put("weight", weight);
            requestBody.put("length", 20);
            requestBody.put("width", 20);
            requestBody.put("height", 20);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl + "/v2/shipping-order/fee",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("data") && root.get("data").has("total")) {
                    int totalFee = root.get("data").get("total").asInt();
                    BigDecimal fee = BigDecimal.valueOf(totalFee);
                    System.out.println("✅ Phí ship GHN: " + fee + "đ");
                    return fee;
                }
            }


        } catch (Exception e) {
            System.err.println("❌ Lỗi tính phí ship: " + e.getMessage());
            e.printStackTrace();

        }
        return amount;
    }

//    /**
//     * Tính phí ship fallback khi API lỗi
//     */
//    public BigDecimal calculateFallbackShippingFee(BigDecimal amount, int weight) {
//        BigDecimal baseFee = BigDecimal.valueOf(30000);
//
//        // Cộng thêm theo giá trị đơn hàng
//        if (amount != null) {
//            if (amount.compareTo(BigDecimal.valueOf(500000)) > 0) {
//                baseFee = baseFee.add(BigDecimal.valueOf(10000));
//            }
//            if (amount.compareTo(BigDecimal.valueOf(1000000)) > 0) {
//                baseFee = baseFee.add(BigDecimal.valueOf(15000));
//            }
//            if (amount.compareTo(BigDecimal.valueOf(2000000)) > 0) {
//                baseFee = baseFee.add(BigDecimal.valueOf(20000));
//            }
//        }
//
//        // Cộng thêm theo cân nặng
//        if (weight > 1000) {
//            int extraWeight = (weight - 1000) / 500;
//            baseFee = baseFee.add(BigDecimal.valueOf(extraWeight * 10000));
//        }
//
//        // Giới hạn tối đa
//        BigDecimal maxFee = BigDecimal.valueOf(150000);
//        BigDecimal minFee = BigDecimal.valueOf(20000);
//
//        if (baseFee.compareTo(maxFee) > 0) {
//            return maxFee;
//        }
//        if (baseFee.compareTo(minFee) < 0) {
//            return minFee;
//        }
//        return baseFee;
//    }
//
//    /**
//     * Lấy District ID từ tên quận/huyện
//     */
    private int getDistrictIdByName(String districtName) {
        if (districtName == null || districtName.trim().isEmpty()) {
            return 1442; // Mặc định Ba Đình
        }

        // Map các quận/huyện ở Hà Nội
        Map<String, Integer> districtMap = new HashMap<>();
        districtMap.put("Ba Đình", 1442);
        districtMap.put("Ba Dinh", 1442);
        districtMap.put("Hoàn Kiếm", 1444);
        districtMap.put("Hoan Kiem", 1444);
        districtMap.put("Tây Hồ", 1446);
        districtMap.put("Tay Ho", 1446);
        districtMap.put("Long Biên", 1448);
        districtMap.put("Long Bien", 1448);
        districtMap.put("Cầu Giấy", 1450);
        districtMap.put("Cau Giay", 1450);
        districtMap.put("Đống Đa", 1452);
        districtMap.put("Dong Da", 1452);
        districtMap.put("Hai Bà Trưng", 1454);
        districtMap.put("Hai Ba Trung", 1454);
        districtMap.put("Hoàng Mai", 1456);
        districtMap.put("Hoang Mai", 1456);
        districtMap.put("Thanh Xuân", 1458);
        districtMap.put("Thanh Xuan", 1458);
        districtMap.put("Hà Đông", 1460);
        districtMap.put("Ha Dong", 1460);
        districtMap.put("Bắc Từ Liêm", 1462);
        districtMap.put("Bac Tu Liem", 1462);
        districtMap.put("Nam Từ Liêm", 1464);
        districtMap.put("Nam Tu Liem", 1464);

        // Tìm kiếm không phân biệt hoa thường và dấu
        for (Map.Entry<String, Integer> entry : districtMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(districtName) ||
                    districtName.toLowerCase().contains(entry.getKey().toLowerCase()) ||
                    removeDiacritics(districtName).toLowerCase().contains(removeDiacritics(entry.getKey()).toLowerCase())) {
                return entry.getValue();
            }
        }

        // Mặc định trả về Ba Đình
        System.out.println("⚠️ Không tìm thấy District ID cho: " + districtName + ", sử dụng mặc định Ba Đình (1442)");
        return 1442;
    }

    /**
     * Bỏ dấu tiếng Việt
     */
    private String removeDiacritics(String str) {
        if (str == null) return "";
        String[] search = {"à","á","ạ","ả","ã","â","ầ","ấ","ậ","ẩ","ẫ","ă","ằ","ắ","ặ","ẳ","ẵ",
                "è","é","ẹ","ẻ","ẽ","ê","ề","ế","ệ","ể","ễ",
                "ì","í","ị","ỉ","ĩ",
                "ò","ó","ọ","ỏ","õ","ô","ồ","ố","ộ","ổ","ỗ","ơ","ờ","ớ","ợ","ở","ỡ",
                "ù","ú","ụ","ủ","ũ","ư","ừ","ứ","ự","ử","ữ",
                "ỳ","ý","ỵ","ỷ","ỹ",
                "đ",
                "À","Á","Ạ","Ả","Ã","Â","Ầ","Ấ","Ậ","Ẩ","Ẫ","Ă","Ằ","Ắ","Ặ","Ẳ","Ẵ",
                "È","É","Ẹ","Ẻ","Ẽ","Ê","Ề","Ế","Ệ","Ể","Ễ",
                "Ì","Í","Ị","Ỉ","Ĩ",
                "Ò","Ó","Ọ","Ỏ","Õ","Ô","Ồ","Ố","Ộ","Ổ","Ỗ","Ơ","Ờ","Ớ","Ợ","Ở","Ỡ",
                "Ù","Ú","Ụ","Ủ","Ũ","Ư","Ừ","Ứ","Ự","Ử","Ữ",
                "Ỳ","Ý","Ỵ","Ỷ","Ỹ",
                "Đ"};
        String[] replace = {"a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a",
                "e","e","e","e","e","e","e","e","e","e","e",
                "i","i","i","i","i",
                "o","o","o","o","o","o","o","o","o","o","o","o","o","o","o","o","o",
                "u","u","u","u","u","u","u","u","u","u","u",
                "y","y","y","y","y",
                "d",
                "A","A","A","A","A","A","A","A","A","A","A","A","A","A","A","A","A",
                "E","E","E","E","E","E","E","E","E","E","E",
                "I","I","I","I","I",
                "O","O","O","O","O","O","O","O","O","O","O","O","O","O","O","O","O",
                "U","U","U","U","U","U","U","U","U","U","U",
                "Y","Y","Y","Y","Y",
                "D"};
        String result = str;
        for (int i = 0; i < search.length; i++) {
            result = result.replaceAll(search[i], replace[i]);
        }
        return result;
    }
}