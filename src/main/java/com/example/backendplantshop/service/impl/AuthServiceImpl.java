package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.convert.UserConvert;
import com.example.backendplantshop.dto.request.users.ChangePasswordDtoRequest;
import com.example.backendplantshop.dto.request.users.GoogleLoginDtoRequest;
import com.example.backendplantshop.dto.request.users.LoginDtoRequest;
import com.example.backendplantshop.dto.request.users.RegisterDtoRequest;
import com.example.backendplantshop.dto.response.user.LoginDtoResponse;
import com.example.backendplantshop.dto.response.user.RegisterDtoResponse;
import com.example.backendplantshop.entity.UserTokens;
import com.example.backendplantshop.entity.Users;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.UserMapper;
import com.example.backendplantshop.security.JwtUtil;
import com.example.backendplantshop.service.impl.GoogleAuthService;
import com.example.backendplantshop.service.intf.AuthenticationService;
import com.example.backendplantshop.service.intf.UserTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthenticationService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserTokenService userTokenService;
    private final GoogleAuthService googleAuthService;

    public String clean(String input) {
        return (input != null && !input.trim().isEmpty()) ? input : null;
    }
    // ===== Helpers: lấy thông tin từ token trong SecurityContext =====
    public int getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
        }
        try {
            return Integer.parseInt(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
        }
    }

    public String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
        }
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            String authority = auth.getAuthority(); // dạng ROLE_USER / ROLE_ADMIN
            if (authority != null && authority.startsWith("ROLE_")) {
                return authority.substring(5);
            }
        }
        throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
    }
    public boolean isUser(String role) {
        return "USER".equalsIgnoreCase(role);
    }

    public boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role);
    }


    public RegisterDtoResponse register(RegisterDtoRequest registerDtoRequest) {
        if (registerDtoRequest == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        // Làm sạch dữ liệu: nếu email hoặc phone_number là chuỗi rỗng hoặc chỉ chứa khoảng trắng thì gán null
        registerDtoRequest.setEmail(clean(registerDtoRequest.getEmail()));


        // Kiểm tra trùng lặp - chỉ kiểm tra khi có giá trị
        if (userMapper.findByEmail(registerDtoRequest.getEmail()) != null) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }


        if (userMapper.findByUsername(registerDtoRequest.getUsername()) != null) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        Users users = UserConvert.convertResigterDtoRequestToUsers(registerDtoRequest, passwordEncoder);
        userMapper.insert(users);

        // Trả về thông tin user đã đăng ký
        return UserConvert.convertUsersToRegisterDtoResponse(users);
    }


    public LoginDtoResponse login(LoginDtoRequest loginDtoRequest) {
        if (loginDtoRequest == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }


        // Tìm user theo email hoặc phone
        Users users = null;
        if (loginDtoRequest.getEmail() != null && !loginDtoRequest.getEmail().trim().isEmpty()) {
            users = userMapper.findByEmail(loginDtoRequest.getEmail());
        }

        if (users == null) {
            throw new AppException(ErrorCode.USER_NOT_EXISTS);
        }

        // Kiểm tra password
        if (!passwordEncoder.matches(loginDtoRequest.getPassword(), users.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Cả access và refresh đều không dùng được → cấp cặp token mới
        String accessToken = jwtUtil.generateAccessToken(users.getUser_id(), users.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(users.getUser_id(), users.getRole());

        // Lưu token vào DB
        userTokenService.saveToken(UserTokens.builder()
                .user_id(users.getUser_id())
                .token(refreshToken) // chỉ lưu refreshToken trong DB
                .expires_at(LocalDateTime.now().plusDays(7)) // hạn refresh token
                .revoked(false)
                .build());
        // Trả về accessToken + refreshToken
        return LoginDtoResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public LoginDtoResponse refresh(String authHeader) {
        // Bóc token từ header
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;

        if (token == null) {
            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
        }

        // Validate refresh token
//        if (!jwtUtil.validateToken(token) || !jwtUtil.isRefreshToken(token)) {
//            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
//        }

        int id = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        // Lấy token cũ từ DB (chưa revoke)
        UserTokens existing = userTokenService.findTokenByUser(id);
        if (existing == null || Boolean.TRUE.equals(existing.getRevoked())) {
            throw new AppException(ErrorCode.TOKEN_REVOKED);
        }
        // revoke token cũ
        if (existing.getExpires_at().isBefore(LocalDateTime.now())) {
            userTokenService.revokeTokensByUser(id);
            throw new AppException(ErrorCode.TOKEN_HAS_EXPIRED);
        }

        // Nếu refresh token chưa hết hạn thì trả về lại refresh token cũ
        String newAccessToken = jwtUtil.generateAccessToken(id, role);
        return LoginDtoResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(existing.getToken())
                .build();

    }

    //    @Override
//    public void changePassword(ChangePasswordDtoRequest changePasswordDtoRequest, String authHeader) {
//        try{
//            // Lấy token từ Authorization header
//            String token = (authHeader != null && authHeader.startsWith("Bearer "))
//                    ? authHeader.substring(7)
//                    : null;
//
//            if (token == null || !jwtUtil.validateToken(token) || !jwtUtil.isAccessToken(token)) {
//                throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
//            }
//
//            int currentUserId = jwtUtil.extractUserId(token);
//            Users user = userMapper.findById(currentUserId);
//            if (user == null) {
//                throw new AppException(ErrorCode.USER_NOT_EXISTS);
//            }
//
//            // Kiểm tra mật khẩu cũ
//            if (!passwordEncoder.matches(changePasswordDtoRequest.getOldPassword(), user.getPassword())) {
//                log.error("Wrong old password for user {}", currentUserId);
//                throw new AppException(ErrorCode.INVALID_CREDENTIALS);
//            }
//
//            // Mã hóa mật khẩu mới
//            String encodedNewPassword = passwordEncoder.encode(changePasswordDtoRequest.getNewPassword());
//            userMapper.changePassword(currentUserId, encodedNewPassword);
//            log.info("Password changed successfully for user {}", currentUserId);
//        }catch (AppException e) {
//            log.error("AppException occurred while changing password for user {}: {}", e.getMessage());
//            throw e; // ném lại để ControllerAdvice xử lý trả response lỗi
//        } catch (Exception e) {
//            log.error("Unexpected error while changing password for user {}: {}", e.getMessage(), e);
//            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
//        }
//
//    }
    @Override
    public void changePassword(ChangePasswordDtoRequest changePasswordDtoRequest, String authHeader) {
        // Lấy token từ Authorization header
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;

        if (token == null ) {
            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
        }
        // Lấy thông tin user từ token
        int currentUserId = jwtUtil.extractUserId(token);

        //check tồn tại user
        Users user =userMapper.findById(currentUserId);
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_EXISTS);
        }

        //check old pass
        if(!passwordEncoder.matches(changePasswordDtoRequest.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        String enCodeNewPassword = passwordEncoder.encode(changePasswordDtoRequest.getNewPassword());
        int rows = userMapper.changePassword(currentUserId,enCodeNewPassword);
        if (rows == 0) {
            throw new AppException(ErrorCode.CHANGEPASSWORD_FAILED);
        }


    }


//    @Override
//    public String findRoleByUserId(int id) {
//        Users users = userMapper.findById(id);
//        if (users == null) {
//            throw new AppException(ErrorCode.USER_NOT_EXISTS);
//        }
//        return users.getRole();
//    }


    @Override
    public void logout(String authHeader) {
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;

        if (token == null) {
            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
        }

        if(jwtUtil.isAccessToken(token)) {
            int id = jwtUtil.extractUserId(token);
            userTokenService.revokeTokensByUser(id);
        }
        else {
            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
        }
    }

    @Override
    public LoginDtoResponse loginWithGoogle(GoogleLoginDtoRequest googleLoginDtoRequest) {
        if (googleLoginDtoRequest == null || googleLoginDtoRequest.getCode() == null || googleLoginDtoRequest.getCode().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        try {
            // Bước 1: Exchange code → access token
            // Truyền redirectUri từ request, nếu không có thì service sẽ dùng từ config
            String accessToken = googleAuthService.exchangeCodeForAccessToken(
                googleLoginDtoRequest.getCode(), 
                googleLoginDtoRequest.getRedirectUri()
            );
            
            if (accessToken == null || accessToken.trim().isEmpty()) {
                throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
            }

            // Bước 2: Lấy thông tin user từ Google
            Map<String, String> googleUserInfo = googleAuthService.getUserInfoFromGoogle(accessToken);
            String googleId = googleAuthService.getGoogleId(googleUserInfo);
            String email = googleAuthService.getEmail(googleUserInfo);
            String name = googleAuthService.getName(googleUserInfo);

            if (email == null || email.trim().isEmpty() || googleId == null || googleId.trim().isEmpty()) {
                throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
            }

            // Bước 3: Tìm user theo google_id hoặc email
            Users user = userMapper.findByGoogleId(googleId);
            
            // Nếu không tìm thấy theo google_id, thử tìm theo email
            if (user == null) {
                user = userMapper.findByEmail(email);
                
                // Nếu tìm thấy user theo email nhưng chưa có google_id, cập nhật google_id
                if (user != null && (user.getGoogle_id() == null || user.getGoogle_id().trim().isEmpty())) {
                    user.setGoogle_id(googleId);
                    userMapper.update(user);
                    log.info("Đã cập nhật google_id cho user: {}", email);
                }
            }

            // Bước 4: Nếu user chưa tồn tại, tạo mới
            if (user == null) {
                // Tạo username từ name hoặc email nếu name không có
                String username = (name != null && !name.trim().isEmpty()) 
                    ? name.trim().replaceAll("\\s+", "") // Xóa khoảng trắng
                    : email.substring(0, email.indexOf("@")); // Dùng phần trước @ của email

                // Đảm bảo username duy nhất
                String baseUsername = username;
                int counter = 1;
                while (userMapper.findByUsername(username) != null) {
                    username = baseUsername + counter;
                    counter++;
                }

                // Tạo user mới với password ngẫu nhiên (user Google không cần password)
                String randomPassword = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
                user = Users.builder()
                        .email(email)
                        .password(randomPassword)
                        .username(username)
                        .google_id(googleId)
                        .role("USER")
                        .is_deleted(false)
                        .build();

                userMapper.insert(user);
                log.info("Đã tạo user mới từ Google: {} với google_id: {}", email, googleId);
            }

            // Bước 5: Tạo JWT tokens (giống login thường)
            String jwtAccessToken = jwtUtil.generateAccessToken(user.getUser_id(), user.getRole());
            String jwtRefreshToken = jwtUtil.generateRefreshToken(user.getUser_id(), user.getRole());

            // Lưu refresh token vào DB
            userTokenService.saveToken(UserTokens.builder()
                    .user_id(user.getUser_id())
                    .token(jwtRefreshToken)
                    .expires_at(LocalDateTime.now().plusDays(7))
                    .revoked(false)
                    .build());

            return LoginDtoResponse.builder()
                    .accessToken(jwtAccessToken)
                    .refreshToken(jwtRefreshToken)
                    .build();

        } catch (AppException e) {
            log.error("AppException khi đăng nhập Google: code={}, message={}", 
                e.getErrorCode().getCode(), e.getErrorCode().getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi đăng nhập với Google: {}", e.getMessage(), e);
            // Throw exception với message chi tiết hơn để frontend biết lỗi gì
            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
        }
    }

}
