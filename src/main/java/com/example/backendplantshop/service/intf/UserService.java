package com.example.backendplantshop.service.intf;


import com.example.backendplantshop.dto.request.users.UserDtoRequest;
import com.example.backendplantshop.dto.response.user.LoginDtoResponse;
import com.example.backendplantshop.dto.response.user.UserDtoResponse;

import java.util.List;

public interface UserService {
    UserDtoResponse findById(int id);
    List<UserDtoResponse> findAllUsers();
    LoginDtoResponse update(int id,UserDtoRequest userDtoRequest);
    void delete(int id);
    UserDtoResponse getUser(String authHeader, Integer id);
    void restoreUser(int id);

}
