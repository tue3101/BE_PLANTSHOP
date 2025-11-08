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

        // Kiểm tra email
        Users existingUserByEmail = userMapper.findByEmailIgnoreDeleted(registerDtoRequest.getEmail());
        if (existingUserByEmail != null) {
            // Nếu user đã tồn tại và chưa bị xóa 
            if (existingUserByEmail.getIs_deleted() == null || !existingUserByEmail.getIs_deleted()) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            // Nếu user đã tồn tại nhưng đã bị xóa mềm 
            if (existingUserByEmail.getIs_deleted()) {
                throw new AppException(ErrorCode.EMAIL_DISABLED_NEED_RESTORE);
            }
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
        log.info("=== BẮT ĐẦU GOOGLE LOGIN ===");
        log.info("Request: {}", googleLoginDtoRequest != null ? "not null" : "null");
        
        if (googleLoginDtoRequest == null || googleLoginDtoRequest.getCode() == null || googleLoginDtoRequest.getCode().trim().isEmpty()) {
            log.error("Google login request không hợp lệ: request={}, code={}", 
                googleLoginDtoRequest != null ? "not null" : "null",
                googleLoginDtoRequest != null ? googleLoginDtoRequest.getCode() : "null");
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        try {
            String codePreview = googleLoginDtoRequest.getCode().length() > 10 
                ? googleLoginDtoRequest.getCode().substring(0, 10) + "..." 
                : googleLoginDtoRequest.getCode();
            log.info("Bắt đầu đăng nhập Google với code: {}, redirectUri: {}", 
                codePreview, 
                googleLoginDtoRequest.getRedirectUri());
            
            // Bước 1: Exchange code → access token
            // Truyền redirectUri từ request, nếu không có thì service sẽ dùng từ config
            String accessToken;
            try {
                accessToken = googleAuthService.exchangeCodeForAccessToken(
                    googleLoginDtoRequest.getCode(), 
                    googleLoginDtoRequest.getRedirectUri()
                );
            } catch (Exception e) {
                log.error("Lỗi khi exchange code với Google: {}", e.getMessage());
                throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
            }
            
            if (accessToken == null || accessToken.trim().isEmpty()) {
                log.error("Access token từ Google là null hoặc rỗng");
                throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
            }
            
            log.info("Đã lấy được access token từ Google thành công");

            // Bước 2: Lấy thông tin user từ Google
            Map<String, String> googleUserInfo;
            try {
                googleUserInfo = googleAuthService.getUserInfoFromGoogle(accessToken);
            } catch (Exception e) {
                log.error("Lỗi khi lấy thông tin user từ Google: {}", e.getMessage(), e);
                throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
            }
            
            String googleId = googleAuthService.getGoogleId(googleUserInfo);
            String email = googleAuthService.getEmail(googleUserInfo);
            String name = googleAuthService.getName(googleUserInfo);
            
            log.info("Thông tin user từ Google: email={}, googleId={}, name={}", email, googleId, name);

            if (email == null || email.trim().isEmpty() || googleId == null || googleId.trim().isEmpty()) {
                log.error("Email hoặc GoogleId không hợp lệ: email={}, googleId={}", email, googleId);
                throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
            }

            // Bước 3: Kiểm tra user đã tồn tại chưa (kể cả is_deleted = 1)
            log.info("Bắt đầu kiểm tra user theo googleId (không quan tâm is_deleted): {}", googleId);
            Users existingUser = null;
            try {
                existingUser = userMapper.findByGoogleIdIgnoreDeleted(googleId);
                log.info("Kết quả findByGoogleIdIgnoreDeleted: {}", existingUser != null ? "found" : "not found");
            } catch (Exception e) {
                log.error("Lỗi khi tìm user theo googleId (ignore deleted): {}", e.getMessage(), e);
                // Tiếp tục tìm theo email thay vì throw exception ngay
                existingUser = null;
            }
            
            // Nếu không tìm thấy theo google_id, thử tìm theo email (không quan tâm is_deleted)
            if (existingUser == null) {
                log.info("Không tìm thấy user theo googleId, thử tìm theo email (không quan tâm is_deleted): {}", email);
                try {
                    existingUser = userMapper.findByEmailIgnoreDeleted(email);
                    log.info("Kết quả findByEmailIgnoreDeleted: {}", existingUser != null ? "found" : "not found");
                } catch (Exception e) {
                    log.error("Lỗi khi tìm user theo email (ignore deleted): {}", e.getMessage(), e);
                    throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
                }
            }
            
            // Kiểm tra nếu user đã tồn tại
            if (existingUser != null) {
                // Kiểm tra xem user có bị xóa mềm không (is_deleted = 1)
                if (existingUser.getIs_deleted() != null && existingUser.getIs_deleted()) {
                    log.warn("User đã tồn tại nhưng bị vô hiệu hóa: email={}, google_id={}", email, googleId);
                    throw new AppException(ErrorCode.ACCOUNT_DISABLED);
                }
                
                // User tồn tại và chưa bị xóa, sử dụng user này
                log.info("User đã tồn tại và chưa bị xóa: email={}, google_id={}", email, googleId);
                Users user = existingUser;
                
                // Nếu user có email nhưng chưa có google_id, cập nhật google_id
                if (user.getGoogle_id() == null || user.getGoogle_id().trim().isEmpty()) {
                    log.info("Cập nhật google_id cho user hiện có: {}", email);
                    try {
                        user.setGoogle_id(googleId);
                        userMapper.update(user);
                        log.info("Đã cập nhật google_id cho user: {}", email);
                    } catch (Exception e) {
                        log.error("Lỗi khi cập nhật google_id: {}", e.getMessage(), e);
                        throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
                    }
                }
                
                // Tiếp tục với việc tạo JWT tokens (nhảy xuống Bước 5)
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
            }

            // Bước 4: Nếu user chưa tồn tại, tạo mới
            log.info("User chưa tồn tại, bắt đầu tạo user mới: email={}, google_id={}", email, googleId);
            
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
            log.info("Username được tạo: {}", username);

            // Tạo user mới với password ngẫu nhiên (user Google không cần password)
            String randomPassword = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
            Users user = Users.builder()
                    .email(email)
                    .password(randomPassword)
                    .username(username)
                    .google_id(googleId)
                    .role("USER")
                    .is_deleted(false)
                    .build();

            log.info("Bắt đầu insert user mới vào database: email={}, google_id={}", email, googleId);
            try {
                userMapper.insert(user);
                log.info("Đã tạo user mới từ Google thành công: {} với google_id: {}", email, googleId);
            } catch (Exception e) {
                log.error("Lỗi khi insert user mới: {}", e.getMessage(), e);
                log.error("SQL exception details: ", e);
                throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
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
            log.error("Stack trace AppException: ", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception không mong muốn khi đăng nhập với Google: {}", e.getMessage(), e);
            log.error("Exception class: {}, cause: {}", e.getClass().getName(), e.getCause());
            
            // Kiểm tra xem có phải lỗi từ Google không
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Google")) {
                log.error("Lỗi từ Google OAuth: {}", errorMessage);
            }
            
            // Log toàn bộ stack trace
            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace.length > 0) {
                log.error("Lỗi tại: {} - {}", stackTrace[0].getClassName(), stackTrace[0].getMethodName());
            }
            
            // Throw exception với message chi tiết hơn để frontend biết lỗi gì
            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
        }
    }

}
