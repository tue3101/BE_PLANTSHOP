package com.example.backendplantshop.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration //báo đây là class cấu hình
@RequiredArgsConstructor
public class SecurityConfig {
    //inject filter kiểm tra JWT
    private final JwtAuthenticationFilter jwtAuthenticationFilter;


    //mã hóa mật khẩu và so sánh khi login
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();

    }


    //xác thực thông tin login
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
         return config.getAuthenticationManager();
    }

    //cấu hình bảo mật HTTP
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // tắt CSRF vì dùng jwt và api stateless thì session ko tồn tại
                .cors(cors -> cors.configurationSource(corsConfigurationSource)) // Bật CORS
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) //dùng lambda (biến -> bthuc/khối lệnh) , ko tạo session và ko dùng
                .authorizeHttpRequests(auth -> auth //cấu hình phân quyền bằng lambda
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh", "/api/auth/google", "/api/auth/send-otp-register", "/api/auth/verify-otp", "/api/auth/forgot-password/**").permitAll() //cho phép truy cập tự do ko cần JWT
                        .requestMatchers("/api/payments/momo/callback", "/api/payments/momo/return").permitAll() //cho phép MoMo callback công khai
                        .anyRequest().authenticated() //mọi request ngoài ds trên phải được xác thực JWT
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
