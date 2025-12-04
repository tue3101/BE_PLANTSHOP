package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.convert.UserConvert;
import com.example.backendplantshop.dto.request.users.ChangePasswordDtoRequest;
import com.example.backendplantshop.dto.request.users.GoogleLoginDtoRequest;
import com.example.backendplantshop.dto.request.users.LoginDtoRequest;
import com.example.backendplantshop.dto.request.users.RegisterDtoRequest;
import com.example.backendplantshop.dto.request.users.SendOtpDtoRequest;
import com.example.backendplantshop.dto.request.users.SendOtpRegisterDtoRequest;
import com.example.backendplantshop.dto.request.users.VerifyOtpDtoRequest;
import com.example.backendplantshop.dto.request.users.ForgotPasswordDtoRequest;
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
import com.example.backendplantshop.service.intf.OtpService;
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
    private final OtpService otpService;

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

        // Xác thực OTP trước khi đăng ký
        if (registerDtoRequest.getOtpCode() == null || registerDtoRequest.getOtpCode().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        
        boolean isOtpValid = otpService.verifyOtp(registerDtoRequest.getEmail(), registerDtoRequest.getOtpCode());
        if (!isOtpValid) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

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

        // Đánh dấu OTP đã sử dụng và cập nhật user_id sau khi đăng ký thành công
        try {
            otpService.markOtpAsUsed(registerDtoRequest.getEmail(), registerDtoRequest.getOtpCode());
            otpService.updateUserIdForOtp(registerDtoRequest.getEmail(), registerDtoRequest.getOtpCode(), users.getUser_id());
        } catch (Exception e) {
        }
        // Trả về thông tin user đã đăng ký
        return UserConvert.convertUsersToRegisterDtoResponse(users);
    }

    @Override
    public void sendOtpForRegister(SendOtpRegisterDtoRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        // Validate các trường bắt buộc
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        // Validate độ dài password
        if (request.getPassword().length() < 8 || request.getPassword().length() > 20) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        String email = clean(request.getEmail());
        String username = clean(request.getUsername());

        // Kiểm tra email đã được đăng ký chưa
        Users existingUserByEmail = userMapper.findByEmailIgnoreDeleted(email);
        if (existingUserByEmail != null && (existingUserByEmail.getIs_deleted() == null || !existingUserByEmail.getIs_deleted())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Kiểm tra username đã tồn tại chưa
        Users existingUserByUsername = userMapper.findByUsername(username);
        if (existingUserByUsername != null) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // Gửi OTP
        otpService.generateAndSendOtp(email);
    }

    public boolean verifyOtp(VerifyOtpDtoRequest request) {
        if (request == null || request.getEmail() == null || request.getOtpCode() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        String email = clean(request.getEmail());
        return otpService.verifyOtp(email, request.getOtpCode());
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
//        userTokenService.saveToken(UserTokens.builder()
//                .user_id(users.getUser_id())
//                .token(refreshToken) // chỉ lưu refreshToken trong DB
//                .expires_at(LocalDateTime.now().plusDays(7)) // hạn refresh token
//                .revoked(false)
//                .build());
//        // Trả về accessToken + refreshToken
//        return LoginDtoResponse.builder()
//                .accessToken(accessToken)
//                .refreshToken(refreshToken)
//                .build();

        userTokenService.saveToken(UserConvert.toUserToken(users, refreshToken));
        return UserConvert.toLoginDtoResponse(accessToken, refreshToken);
    }

    @Override
    public LoginDtoResponse refresh(String authHeader) {
        // Bóc token từ header
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;

        if (token == null || token.trim().isEmpty()) {
            log.warn("Refresh token không tồn tại hoặc rỗng");
            throw new AppException(ErrorCode.TOKEN_NOT_EXISTS);
        }

        // Validate refresh token: phải hợp lệ và phải là refresh token
        if (!jwtUtil.validateToken(token)) {
            log.warn("Refresh token không hợp lệ hoặc đã hết hạn");
            throw new AppException(ErrorCode.TOKEN_HAS_EXPIRED);
        }

        if (!jwtUtil.isRefreshToken(token)) {
            log.warn("Token không phải là refresh token");
            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
        }

        // Extract thông tin từ token (sau khi đã validate)
        int id;
        String role;
        try {
            id = jwtUtil.extractUserId(token);
            role = jwtUtil.extractRole(token);
        } catch (Exception e) {
            log.error("Lỗi khi extract thông tin từ refresh token: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
        }

        // Lấy token cũ từ DB (chưa revoke)
        UserTokens existing = userTokenService.findTokenByUser(id);
        if (existing == null) {
            log.warn("Không tìm thấy refresh token trong DB cho user ID: {}", id);
            throw new AppException(ErrorCode.TOKEN_NOT_EXISTS);
        }

        if (Boolean.TRUE.equals(existing.getRevoked())) {
            log.warn("Refresh token đã bị revoke cho user ID: {}", id);
            throw new AppException(ErrorCode.TOKEN_REVOKED);
        }

        // Kiểm tra token trong DB đã hết hạn chưa
        if (existing.getExpires_at() != null && existing.getExpires_at().isBefore(LocalDateTime.now())) {
            log.warn("Refresh token trong DB đã hết hạn cho user ID: {}", id);
            userTokenService.revokeTokensByUser(id);
            throw new AppException(ErrorCode.TOKEN_HAS_EXPIRED);
        }

        // Kiểm tra token từ header có khớp với token trong DB không
        if (!token.equals(existing.getToken())) {
            log.warn("Refresh token không khớp với token trong DB cho user ID: {}", id);
            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
        }

        // Nếu refresh token chưa hết hạn thì tạo access token mới và trả về lại refresh token cũ
        String newAccessToken = jwtUtil.generateAccessToken(id, role);
        log.info("Đã làm mới access token cho user ID: {}", id);
//        return LoginDtoResponse.builder()
//                .accessToken(newAccessToken)
//                .refreshToken(existing.getToken())
//                .build();

        return UserConvert.fromTokens(newAccessToken, existing);
    }


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

        // Thu hồi tất cả các token của user sau khi đổi mật khẩu
        userTokenService.revokeTokensByUser(currentUserId);
        log.info("User ID: {} đã đổi mật khẩu thành công và tất cả token đã bị thu hồi", currentUserId);
    }

    @Override
    public void sendOtpForgotPassword(SendOtpDtoRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        String email = clean(request.getEmail());
        
        // Kiểm tra email PHẢI TỒN TẠI
        Users existingUser = userMapper.findByEmailIgnoreDeleted(email);
        if (existingUser == null || (existingUser.getIs_deleted() != null && existingUser.getIs_deleted())) {
            throw new AppException(ErrorCode.USER_NOT_EXISTS);
        }

        // Gửi OTP với user_id (vì user đã tồn tại)
        otpService.generateAndSendOtp(email, existingUser.getUser_id());
    }

    @Override
    public void resetPassword(ForgotPasswordDtoRequest request) {
        // Trường hợp 1: Chưa đăng nhập - Reset password bằng OTP
        if (request == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        String email = clean(request.getEmail());
        
        if (email == null || request.getOtpCode() == null || request.getNewPassword() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        // Kiểm tra email tồn tại
        Users user = userMapper.findByEmailIgnoreDeleted(email);
        if (user == null || (user.getIs_deleted() != null && user.getIs_deleted())) {
            throw new AppException(ErrorCode.USER_NOT_EXISTS);
        }

        // Xác thực OTP
        boolean isOtpValid = otpService.verifyOtp(email, request.getOtpCode());
        if (!isOtpValid) {
            log.warn("OTP không hợp lệ cho email: {} với OTP: {}", email, request.getOtpCode());
            throw new AppException(ErrorCode.INVALID_OTP);
        }
        
        log.info("OTP đã được verify thành công cho email: {}, đang tiến hành reset password", email);

        // Cập nhật mật khẩu
        updateUserPassword(user.getUser_id(), request.getNewPassword());
        
        // Đánh dấu OTP đã sử dụng sau khi reset password thành công
        try {
            otpService.markOtpAsUsed(email, request.getOtpCode());
            log.info("Đã mark OTP đã sử dụng cho email: {}", email);
        } catch (Exception e) {
            log.warn("Không thể mark OTP cho email {}: {}", email, e.getMessage());
            // Không throw exception vì password đã được reset thành công
        }
        
        log.info("User ID: {} đã reset mật khẩu bằng OTP thành công", user.getUser_id());
    }

    @Override
    public void resetPassword(String authHeader, ForgotPasswordDtoRequest request) {
        // Trường hợp 2: Đã đăng nhập - Reset password bằng token (không cần OTP)
        if (request == null || request.getNewPassword() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        // Lấy token từ Authorization header
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;

        if (token == null) {
            throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
        }

        // Lấy thông tin user từ token
        int currentUserId = jwtUtil.extractUserId(token);

        // Kiểm tra user tồn tại
        Users user = userMapper.findById(currentUserId);
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_EXISTS);
        }

        // Cập nhật mật khẩu (không cần OTP vì đã có token xác thực)
        updateUserPassword(currentUserId, request.getNewPassword());
    }


    private void updateUserPassword(int userId, String newPassword) {
        // Mã hóa mật khẩu mới và cập nhật
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        int rows = userMapper.changePassword(userId, encodedNewPassword);
        if (rows == 0) {
            throw new AppException(ErrorCode.CHANGEPASSWORD_FAILED);
        }
        userTokenService.revokeTokensByUser(userId);
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
            
            // Exchange code → access token
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

            // Lấy thông tin user từ Google
            Map<String, String> googleUserInfo;
            try {
                googleUserInfo = googleAuthService.getUserInfoFromGoogle(accessToken);
            } catch (Exception e) {
                log.error("Lỗi khi lấy thông tin user từ Google: {}", e.getMessage(), e);
                throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
            }
            
            String email = googleAuthService.getEmail(googleUserInfo);
            String name = googleAuthService.getName(googleUserInfo);
            
            log.info("Thông tin user từ Google: email={}, name={}", email, name);

            if (email == null || email.trim().isEmpty()) {
                log.error("Email không hợp lệ: email={}", email);
                throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
            }

            //Kiểm tra user đã tồn tại chưa (tìm theo email)
            log.info("Bắt đầu kiểm tra user theo email (không quan tâm is_deleted): {}", email);
            Users existingUser = null;
            try {
                existingUser = userMapper.findByEmailIgnoreDeleted(email);
                log.info("Kết quả findByEmailIgnoreDeleted: {}", existingUser != null ? "found" : "not found");
            } catch (Exception e) {
                log.error("Lỗi khi tìm user theo email (ignore deleted): {}", e.getMessage(), e);
                throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
            }
            
            // Kiểm tra nếu user đã tồn tại
            if (existingUser != null) {
                // Kiểm tra xem user có bị xóa mềm không (is_deleted = 1)
                if (existingUser.getIs_deleted() != null && existingUser.getIs_deleted()) {
                    log.warn("User đã tồn tại nhưng bị vô hiệu hóa: email={}", email);
                    throw new AppException(ErrorCode.ACCOUNT_DISABLED);
                }
                
                // User tồn tại và chưa bị xóa, sử dụng user này
                log.info("User đã tồn tại và chưa bị xóa: email={}", email);
                Users user = existingUser;
                
                // Tạo JWT tokens
                String jwtAccessToken = jwtUtil.generateAccessToken(user.getUser_id(), user.getRole());
                String jwtRefreshToken = jwtUtil.generateRefreshToken(user.getUser_id(), user.getRole());

                // Lưu refresh token vào DB
//                userTokenService.saveToken(UserTokens.builder()
//                        .user_id(user.getUser_id())
//                        .token(jwtRefreshToken)
//                        .expires_at(LocalDateTime.now().plusDays(7))
//                        .revoked(false)
//                        .build());
//
//                return LoginDtoResponse.builder()
//                        .accessToken(jwtAccessToken)
//                        .refreshToken(jwtRefreshToken)
//                        .build();
                userTokenService.saveToken(UserConvert.toUserToken(user, jwtRefreshToken));

                return UserConvert.toLoginDtoResponse(jwtAccessToken,jwtRefreshToken);


            }

            // Bước 4: Nếu user chưa tồn tại, tạo mới
            log.info("User chưa tồn tại, bắt đầu tạo user mới: email={}", email);
            
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
                    .role("USER")
                    .is_deleted(false)
                    .build();

            log.info("Bắt đầu insert user mới vào database: email={}", email);
            try {
                userMapper.insert(user);
                log.info("Đã tạo user mới từ Google thành công: {}", email);
            } catch (Exception e) {
                log.error("Lỗi khi insert user mới: {}", e.getMessage(), e);
                log.error("SQL exception details: ", e);
                throw new AppException(ErrorCode.AUTHENTICATION_ERROR);
            }

            // Bước 5: Tạo JWT tokens (giống login thường)
            String jwtAccessToken = jwtUtil.generateAccessToken(user.getUser_id(), user.getRole());
            String jwtRefreshToken = jwtUtil.generateRefreshToken(user.getUser_id(), user.getRole());

            // Lưu refresh token vào DB
//            userTokenService.saveToken(UserTokens.builder()
//                    .user_id(user.getUser_id())
//                    .token(jwtRefreshToken)
//                    .expires_at(LocalDateTime.now().plusDays(7))
//                    .revoked(false)
//                    .build());
//
//            return LoginDtoResponse.builder()
//                    .accessToken(jwtAccessToken)
//                    .refreshToken(jwtRefreshToken)
//                    .build();

            userTokenService.saveToken(UserConvert.toUserToken(user, jwtRefreshToken));

            return UserConvert.toLoginDtoResponse(jwtAccessToken,jwtRefreshToken);

        } catch (AppException e) {
            log.error("AppException khi đăng nhập Google: code={}, message={}", 
                e.getErrorCode().getCode(), e.getErrorCode().getMessage());
            log.error("Stack trace AppException: ", e);
            throw e;
        } catch (Exception e) {
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
