package com.example.backendplantshop.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Component
public class JwtUtil {
//    private static final String SECRET_KEY="nguyenngocthanhtuedh52112019nguyenngocthanhtuedh52112019"; // HS256 phải trên 32 ký tự
//    private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 60; // 1h
//    private static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24 * 7; // 7 ngày

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }



    //tạo token truy cập
    public String generateAccessToken(int user_id, String role) {
        //tạo claims chứa thông tin đóng gói vào payload của JWT
        Map<String, Object> claims = new HashMap<>(); //dùng hashMap để lưu key-value
        claims.put("role", role);
        claims.put("type", "access");
        //xây dụng token
        return Jwts.builder()
                .setClaims(claims) //thêm payload
                .setSubject(String.valueOf(user_id)) //định danh tính token
                .setIssuedAt(new Date()) //ghi nhớ tgian tạo
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration)) //hết hạn
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) //ký token với thuật toán
                .compact(); //đóng gói thành chuỗi JWT
    }

    public String generateRefreshToken(int user_id, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("type", "refresh");
        // claims.put("ver", getCurrentRefreshVersion(user_id));
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(user_id))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() +refreshTokenExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Lấy tất cả claims từ token của client
    private Claims extractAllClaims(String token) {
        //tạo JwtParserBuilder để giải mã và xác minh JWT
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) //chỉ định để kiểm tra chữ ký
                .build() //Xây dựng JwtParser từ builder.
                .parseClaimsJws(token)//Giải mã và phân tích chuỗi token , ktr hạn
                .getBody(); //lấy ra phần payload
    }

    // Lấy username từ token
    public int extractUserId(String token) {
        return Integer.parseInt(extractAllClaims(token).getSubject());
    }

    //lấy role từ token
    public String extractRole(String token) {

        return extractAllClaims(token).get("role", String.class);
    }

    // Kiểm tra token có hợp lệ không
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        }catch (ExpiredJwtException e) {
            System.out.println("Token expired!");
            return false;
        } catch (UnsupportedJwtException e) {
            System.out.println("Unsupported JWT!");
            return false;
        } catch (MalformedJwtException e) {
            System.out.println("Malformed JWT!");
            return false;
        } catch (SignatureException e) {
            System.out.println("Invalid signature!");
            return false;
        } catch (IllegalArgumentException e) {
            System.out.println("Token is null or empty!");
            return false;
        }
    }

//    // Kiểm tra token có phải access token không
    public boolean isAccessToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "access".equals(claims.get("type", String.class)); //so sánh chuỗi
        } catch (Exception e) {
            return false;
        }
    }
//
//    // Kiểm tra token có phải refresh token không
//    public boolean isRefreshToken(String token) {
//        try {
//            Claims claims = extractAllClaims(token);
//            return "refresh".equals(claims.get("type", String.class));
//        } catch (Exception e) {
//            return false;
//        }
//    }


}
