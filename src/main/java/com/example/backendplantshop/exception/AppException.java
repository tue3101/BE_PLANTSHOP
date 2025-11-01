package com.example.backendplantshop.exception;

import com.example.backendplantshop.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppException extends RuntimeException{
    ErrorCode errorCode;
}
