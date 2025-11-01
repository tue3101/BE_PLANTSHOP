package com.example.backendplantshop.config;

import com.example.backendplantshop.security.JwtUtil;
import com.example.backendplantshop.mapper.UserMapper;
import com.example.backendplantshop.entity.Users;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization"); //lấy giá trị header

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token) && jwtUtil.isAccessToken(token)) {
                int userId = jwtUtil.extractUserId(token);
                String tokenRole = jwtUtil.extractRole(token);

                // Cross-check role hiện tại trong DB để bắt đăng nhập lại nếu role đã đổi
                Users current = userMapper.findById(userId);
                if (current == null || tokenRole == null || !tokenRole.equalsIgnoreCase(current.getRole())) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return; // buộc client đăng nhập lại vì token không còn phù hợp với role hiện tại
                }

                // Tạo quyền hạn cho user từ role
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + current.getRole()); //dùng GrantedAuthority để kiểm tra quyền truy cập các API
                //đại diện cho user đã xác thực
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        String.valueOf(userId),
                        null, //pass null vì JWT đã xác thực
                        Collections.singletonList(authority) //quyền hạn user
                );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); //lấy thông tin chi tiết từ requets vd IP, Session...

                SecurityContextHolder.getContext().setAuthentication(authenticationToken); //chức thông tin user được xác thực
            }
        }
        filterChain.doFilter(request, response); //đi tới controller
    }
}



