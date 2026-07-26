package com.example.th06876_java202.config;

import com.example.th06876_java202.Service.TaiKhoanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableAsync
public class SecurityConfig {

    @Value("${free.ship.threshold:2000000}")
    private BigDecimal freeShipThreshold;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.setMaxPostSize(100 * 1024 * 1024);
        });
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // [SỬA] Bắt buộc phải có bean này khi dùng .maximumSessions(...) trong sessionManagement,
    // nếu không Spring Security sẽ không nhận được sự kiện session bị hủy (logout, hết hạn,...)
    // -> SessionRegistry bị sai lệch -> có thể coi phiên đang hợp lệ là "đã hết hạn" và đá
    // người dùng về lại trang đăng nhập một cách ngẫu nhiên, dù họ vừa mới đăng nhập xong.
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }



    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(TaiKhoanService taiKhoanService) {
        return new CustomUserDetailsService(taiKhoanService);
    }

    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return daoAuthenticationProvider;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        http.authorizeHttpRequests((requests) -> requests
                // ai cũng vào đc, ko cần đăng nhập
                .requestMatchers("/", "/login", "/register", "/forgot", "/accessDenied").permitAll()

                // ✅ Cho phép truy cập static resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/storefront/**").permitAll()

                // ✅ QUAN TRỌNG: Cho phép truy cập ảnh từ thư mục uploads
                .requestMatchers("/uploads/**", "/images/sanpham/**").permitAll()

                // === Kênh WebSocket thời gian thực (SockJS cần cả GET/POST fallback) ===
                .requestMatchers("/ws/**").permitAll()

                // === Website bán hàng FS Shoes: xem sản phẩm & giỏ hàng không cần đăng nhập ===
                .requestMatchers("/cua-hang/**").permitAll()
                .requestMatchers("/gio-hang", "/gio-hang/**").permitAll()
                // API địa giới + tính cước GHN (dữ liệu công khai, chỉ đọc)
                .requestMatchers("/api/giao-hang/**").permitAll()
                .requestMatchers("/api/cua-hang/**").permitAll()

                // === Trang nội dung công khai: giới thiệu, liên hệ, theo dõi đơn, xem đánh giá ===
                .requestMatchers("/gioi-thieu", "/lien-he", "/theo-doi-don-hang", "/theo-doi-don-hang/**", "/danh-gia").permitAll()

                // === Gửi đánh giá cần đăng nhập khách hàng ===
                .requestMatchers("/danh-gia/gui").hasRole("USER")

                // === Cần đăng nhập bằng tài khoản khách hàng (USER) ===
                .requestMatchers("/thanh-toan", "/thanh-toan/**").hasRole("USER")
                .requestMatchers("/ca-nhan", "/ca-nhan/**").hasRole("USER")
                .requestMatchers("/user/**").hasRole("USER")

                // === Chatbot & Hỗ trợ trực tuyến: khách (kể cả vãng lai) được chat ===
                .requestMatchers("/api/ho-tro/**").permitAll()

                // === API realtime cho khu quản lý (ADMIN/STAFF) ===
                .requestMatchers("/api/quan-ly/**").hasAnyRole("ADMIN", "STAFF")

                // === Khu vực quản lý bán hàng (ADMIN/STAFF) ===
                // Gộp: danh sách bản fixed + các mục dùng chung của bản team
                // (/cham-cong, /nv-ca-nhan, /staff) để STAFF cũng truy cập được.
                .requestMatchers(
                        "/banhang/**", "/chatlieu/**", "/danhgia/**", "/hotro/**", "/lienhe/**", "/chi-tiet-dot-giam-gia/**", "/danhmucsp/**",
                        "/detail/**", "/donhang/**", "/giamgia/**", "/hoa-don/**", "/khach-hang/**",
                        "/kichthuoc/**", "/kieugiay/**", "/mausac/**", "/nhan-vien/**", "/nhap-hang/**",
                        "/sanphamct/**", "/sanpham/**", "/tai-khoan/**", "/thong-ke/**", "/thuonghieu/**",
                        "/cham-cong/**", "/nv-ca-nhan/**", "/staff/**",
                        // [MERGE] Ca ban hang CUA CHINH NHAN VIEN (phai dat TRUOC rule /giao-ca/** = ADMIN)
                        "/giao-ca/phien", "/giao-ca/mo-ca", "/giao-ca/dong-ca",
                        "/giao-ca/tong-ket", "/giao-ca/lich-su", "/giao-ca/lich-cua-toi"
                ).hasAnyRole("ADMIN", "STAFF")

                // === Khu vực quản trị: sản phẩm, thuộc tính sản phẩm, nhân viên,
                // nhập hàng, giảm giá, giao ca & thống kê (chỉ ADMIN) ===
                .requestMatchers(
                        "/chatlieu/**", "/chi-tiet-dot-giam-gia/**", "/danhmucsp/**",
                        "/detail/**", "/giamgia/**", "/kichthuoc/**", "/kieugiay/**",
                        "/mausac/**", "/nhan-vien/**", "/nhap-hang/**", "/sanphamct/**",
                        "/sanphamha/**", "/sanpham/**", "/tai-khoan/**", "/thong-ke/**",
                        "/thuonghieu/**", "/giao-ca/**", "/admin/**"
                ).hasRole("ADMIN")

                .anyRequest().authenticated());

        http.formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll());

        // Cấu hình LOGOUT
        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
        );

        http.exceptionHandling(exception -> exception.accessDeniedPage("/accessDenied"));

        http.sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?expired"));

        http.rememberMe(r -> r
                .key("mySecretKey_12345")
                .rememberMeParameter("remember-me")
                .tokenValiditySeconds(30 * 24 * 60 * 60)
                .userDetailsService(userDetailsService)
                .useSecureCookie(false)
        );

        // ✅ SỬA LẠI: Cấu hình CSRF - Bỏ qua cho tất cả API /banhang/**
        http.csrf(csrf -> csrf
                // Gộp CSRF-ignore của cả hai bản (đều là exempt, an toàn cộng dồn)
                .ignoringRequestMatchers("/banhang/**")             // toàn bộ API bán hàng tại quầy (team)
                .ignoringRequestMatchers("/sanpham/add", "/sanpham/add/**")
                .ignoringRequestMatchers("/uploads/**")
                .ignoringRequestMatchers("/khach-hang/xoa-dia-chi/**")
                // SockJS fallback (xhr_send) dùng POST nội bộ, không phải form người dùng
                .ignoringRequestMatchers("/api/ho-tro/**")          // chat khách/vãng lai gửi bằng fetch JSON
                .ignoringRequestMatchers("/api/quan-ly/ho-tro/**")  // nhân viên trả lời bằng fetch JSON
                .ignoringRequestMatchers("/ws/**")
        );

        return http.build();
    }
}