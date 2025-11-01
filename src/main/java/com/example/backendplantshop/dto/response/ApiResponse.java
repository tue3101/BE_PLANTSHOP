package com.example.backendplantshop.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) //chỉ include những field khác null vào Json còn null thì bỏ qua ko xuất hiện trong response
@Builder
public class ApiResponse <T>{ //class generic dùng T (tham số kiểu) cho data có kiểu linh hoạt
    private Integer statusCode;
    private Boolean success;
    private String message;
    private T data;
}
