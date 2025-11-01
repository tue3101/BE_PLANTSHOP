package com.example.backendplantshop.controller;

import com.example.backendplantshop.dto.request.users.UserDtoRequest;
import com.example.backendplantshop.dto.response.ApiResponse;
import com.example.backendplantshop.dto.response.user.LoginDtoResponse;
import com.example.backendplantshop.dto.response.user.UserDtoResponse;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.service.intf.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;


    @GetMapping({"/get-user", "/get-user/{id}"})
    ApiResponse<UserDtoResponse> getUser(@RequestHeader("Authorization") String authHeader,
                                       @PathVariable(value = "id", required = false) Integer id) {
        return ApiResponse.<UserDtoResponse>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(userService.getUser(authHeader, id))
                .build();
    }

    @GetMapping("/getall")
    ApiResponse<List<UserDtoResponse>> getAllUsers() {
        return ApiResponse.<List<UserDtoResponse>>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(userService.findAllUsers())
                .build();
    }

    @DeleteMapping("/delete/{id}")
    ApiResponse<Void> delete(@PathVariable("id") int id) {
        userService.delete(id);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.DELETE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.DELETE_SUCCESSFULL.getMessage())
                .build();
    }


    @PutMapping("/restore/{id}")
    ApiResponse<Void> restore(@PathVariable("id") int id) {
        userService.restoreUser(id);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.RESTORE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.RESTORE_SUCCESSFULL.getMessage())
                .build();
    }


    @PutMapping("/update/{id}")
    ApiResponse<LoginDtoResponse> update ( @PathVariable("id") int id,@Valid @RequestBody UserDtoRequest userDtoRequest){
        LoginDtoResponse tokens = userService.update(id, userDtoRequest);
        return ApiResponse.<LoginDtoResponse>builder()
                .statusCode(ErrorCode.UPDATE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.UPDATE_SUCCESSFULL.getMessage())
                .data(tokens) // null nếu không đổi role
                .build();
    }


}


