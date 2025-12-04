package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.convert.UserConvert;
import com.example.backendplantshop.dto.request.users.UserDtoRequest;
import com.example.backendplantshop.dto.response.user.LoginDtoResponse;
import com.example.backendplantshop.dto.response.user.UserDtoResponse;
import com.example.backendplantshop.entity.Orders;
import com.example.backendplantshop.entity.Users;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.enums.OrderSatus;
import com.example.backendplantshop.enums.ShippingStatus;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.*;
import com.example.backendplantshop.service.intf.UserService;
import com.example.backendplantshop.service.intf.UserTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final AuthServiceImpl authService;
    private final UserTokenService userTokenService;
    private final CartDetailMapper cartDetailMapper;
    private final CartMapper cartMapper;
    private final UserTokenMapper userTokenMapper;
    private final OrderMapper orderMapper;


    private String clean(String input) {
        if (input == null) return null;
        input = input.trim();
        return input.isEmpty() ? null : input;
    }


    public UserDtoResponse findById(int id) {
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role) && currentUserId != id) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        var users = userMapper.findById(id);
        if (users == null) {
            throw  new AppException(ErrorCode.USER_NOT_EXISTS);
        }
        return UserConvert.convertUsersToUserDtoResponse(users);
    }


    public List<UserDtoResponse> findAllUsers() {
        if (!authService.isAdmin(authService.getCurrentRole())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        var users = UserConvert.convertListUserToListUserDtoResponse(userMapper.findAll());
        if (users == null) {
            throw  new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        return users;
    }

public LoginDtoResponse update(int id, UserDtoRequest userDtoRequest) {
    int currentUserId = authService.getCurrentUserId();
    String role = authService.getCurrentRole();
    if (!authService.isAdmin(role) && currentUserId != id) {
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    Users existingUser = userMapper.findById(id);
    if (existingUser == null) {
        throw new AppException(ErrorCode.USER_NOT_EXISTS);
    }

    userDtoRequest.setEmail(clean(userDtoRequest.getEmail()));
    userDtoRequest.setPhone_number(clean(userDtoRequest.getPhone_number()));
    userDtoRequest.setRole(clean(userDtoRequest.getRole()));
    userDtoRequest.setAddress(clean(userDtoRequest.getAddress()));

    // Kiểm tra: Nếu request có trường role (không null và không rỗng), chỉ admin mới được gửi
    if (userDtoRequest.getRole() != null && !userDtoRequest.getRole().trim().isEmpty()) {
        if (!authService.isAdmin(role)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    // Kiểm tra trùng lặp - chỉ kiểm tra khi có giá trị
    Users duplicateEmail = userMapper.findByEmail(userDtoRequest.getEmail());
    if (duplicateEmail != null && duplicateEmail.getUser_id() != id) {
        throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    Users duplicatePhone = userMapper.findByPhoneNumber(userDtoRequest.getPhone_number());
    if (duplicatePhone != null && duplicatePhone.getUser_id() != id) {
        throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
    }
    Users duplicateUsername = userMapper.findByUsername(userDtoRequest.getUsername());
    if (duplicateUsername != null && duplicateUsername.getUser_id() != id) {
        throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
    }

    // Kiểm tra role có toon tại và so sánh có bằng với role  trong db
    boolean roleChanged = userDtoRequest.getRole() != null && 
                         !userDtoRequest.getRole().equals(existingUser.getRole());
    if (roleChanged) {
        if (!authService.isAdmin(role)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }
    
    
    try {
        userMapper.update(UserConvert.toUpdatedUser(id, userDtoRequest, existingUser));
    } catch (DataIntegrityViolationException e) {
        // Bắt lỗi duplicate từ database constraint (race condition hoặc kiểm tra bị bỏ sót)
        Throwable rootCause = e.getRootCause();
        if (rootCause instanceof SQLException) {
            SQLException sqlException = (SQLException) rootCause;
            String errorMessage = sqlException.getMessage();
            // Kiểm tra nếu là lỗi duplicate key trên username
            if (errorMessage != null && (errorMessage.toLowerCase().contains("username") 
                    || errorMessage.toLowerCase().contains("duplicate") 
                    || errorMessage.toLowerCase().contains("unique"))) {
                throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
            }
        }
        throw e;
    }

    if (roleChanged) {
        userTokenService.revokeTokensByUser(id);
    }

    return null;
}

    public void delete(int id) {
        if (!authService.isAdmin(authService.getCurrentRole())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if(userMapper.findById(id) == null){
            throw new AppException(ErrorCode.USER_NOT_EXISTS);
        }
        
        // Kiểm tra user có đơn hàng chưa giao thành công không
        boolean hasPendingOrders = checkUserHasPendingOrders(id);
        if (hasPendingOrders) {
            throw new AppException(ErrorCode.USER_HAS_PENDING_ORDERS);
        }
        
        cartDetailMapper.deleteByUserId(id);
        cartMapper.deleteByUserId(id);
        userTokenMapper.revokeTokensByUser(id);
        userMapper.delete(id);
    }


    //kiểm tra user có đơn hàng chưa xửa lý hay ko
    private boolean checkUserHasPendingOrders(int userId) {
        // Lấy tất cả đơn hàng của user
        List<Orders> userOrders = orderMapper.findByUserId(userId);
        
        if (userOrders == null || userOrders.isEmpty()) {
            log.debug("User chưa có đơn hàng nào: user_id={}", userId);
            return false;
        }
        
        // Kiểm tra xem có đơn hàng nào chưa DELIVERED không
        // Các trạng thái chưa giao thành công: PENDING_CONFIRMATION, CONFIRMED, SHIPPING
        // CANCELLED không tính vì đã hủy
        //anyMatch dùng để kiểm tra có ít nhất một ptu trong stream thỏa đk ko
        boolean hasPendingOrders = userOrders.stream()
                .anyMatch(order -> order.getShipping_status()!= ShippingStatus.DELIVERED
                        && order.getStatus() != OrderSatus.CANCELLED);
        
        if (hasPendingOrders) {
            log.info("User có đơn hàng chưa giao thành công: user_id={}, số đơn hàng={}", 
                    userId, userOrders.size());
        }
        
        return hasPendingOrders;
    }

    public UserDtoResponse getUser(String authHeader, Integer id) {
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();

        int targetUserId = (id != null) ? id : currentUserId;

        if (id != null && !authService.isAdmin(role) && currentUserId != targetUserId) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        return findById(targetUserId);
    }

    @Override
    public void restoreUser(int id) {
        if (!authService.isAdmin(authService.getCurrentRole())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if(userMapper.findByIdDeleted(id) == null){
            throw new AppException(ErrorCode.NOT_DELETE);
        }
        cartMapper.restoreByUserId(id);
        cartDetailMapper.restoreByUserId(id);
        userMapper.restoreUser(id);
    }

    public List<UserDtoResponse> findAllUserDeleted() {
        if (!authService.isAdmin(authService.getCurrentRole())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        var users = UserConvert.convertListUserToListUserDtoResponse(userMapper.findAllUserDeleted());
        if (users == null) {
            throw  new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        return users;
    }


}
