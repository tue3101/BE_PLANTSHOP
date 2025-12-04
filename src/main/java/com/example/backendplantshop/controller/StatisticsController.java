package com.example.backendplantshop.controller;

import com.example.backendplantshop.dto.request.StatisticsRequest;
import com.example.backendplantshop.dto.response.ApiResponse;
import com.example.backendplantshop.dto.response.ProductSalesResponse;
import com.example.backendplantshop.dto.response.StatisticsResponse;
import com.example.backendplantshop.dto.response.TopProductResponse;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.service.intf.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    /**
     * Lấy thống kê theo ngày
     * GET /api/statistics/by-date?year=2024&month=12&day=25
     */
    @GetMapping("/by-date")
    public ApiResponse<StatisticsResponse> getStatisticsByDate(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            @RequestParam("day") int day) {
        StatisticsResponse statistics = statisticsService.getStatisticsByDate(year, month, day);
        return ApiResponse.<StatisticsResponse>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(statistics)
                .build();
    }
    
    /**
     * Lấy thống kê theo tháng
     * GET /api/statistics/by-month?year=2024&month=12
     */
    @GetMapping("/by-month")
    public ApiResponse<StatisticsResponse> getStatisticsByMonth(
            @RequestParam("year") int year,
            @RequestParam("month") int month) {
        StatisticsResponse statistics = statisticsService.getStatisticsByMonth(year, month);
        return ApiResponse.<StatisticsResponse>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(statistics)
                .build();
    }
    
    /**
     * Lấy thống kê theo năm
     * GET /api/statistics/by-year?year=2024
     */
    @GetMapping("/by-year")
    public ApiResponse<StatisticsResponse> getStatisticsByYear(@RequestParam("year") int year) {
        StatisticsResponse statistics = statisticsService.getStatisticsByYear(year);
        return ApiResponse.<StatisticsResponse>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(statistics)
                .build();
    }
    
    /**
     * Lấy tổng số sản phẩm bán được theo tháng
     * GET /api/statistics/products-sold/by-month?year=2024&month=12
     */
    @GetMapping("/products-sold/by-month")
    public ApiResponse<ProductSalesResponse> getTotalProductsSoldByMonth(
            @RequestParam("year") int year,
            @RequestParam("month") int month) {
        ProductSalesResponse productSales = statisticsService.getTotalProductsSoldByMonth(year, month);
        return ApiResponse.<ProductSalesResponse>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(productSales)
                .build();
    }
    
    /**
     * Lấy tổng số sản phẩm bán được theo năm
     * GET /api/statistics/products-sold/by-year?year=2024
     */
    @GetMapping("/products-sold/by-year")
    public ApiResponse<ProductSalesResponse> getTotalProductsSoldByYear(@RequestParam("year") int year) {
        ProductSalesResponse productSales = statisticsService.getTotalProductsSoldByYear(year);
        return ApiResponse.<ProductSalesResponse>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(productSales)
                .build();
    }
    
//    /**
//     * Lấy danh sách sản phẩm bán chạy theo ngày
//     * GET /api/statistics/top-products/by-date?year=2024&month=12&day=25&limit=10
//     */
//    @GetMapping("/top-products/by-date")
//    public ApiResponse<List<TopProductResponse>> getTopProductsByDate(
//            @RequestParam("year") int year,
//            @RequestParam("month") int month,
//            @RequestParam("day") int day,
//            @RequestParam(value = "limit", defaultValue = "10") int limit) {
//        List<TopProductResponse> topProducts = statisticsService.getTopProductsByDate(year, month, day, limit);
//        return ApiResponse.<List<TopProductResponse>>builder()
//                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
//                .success(Boolean.TRUE)
//                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
//                .data(topProducts)
//                .build();
//    }
//
//    /**
//     * Lấy danh sách sản phẩm bán chạy theo tháng
//     * GET /api/statistics/top-products/by-month?year=2024&month=12&limit=10
//     */
//    @GetMapping("/top-products/by-month")
//    public ApiResponse<List<TopProductResponse>> getTopProductsByMonth(
//            @RequestParam("year") int year,
//            @RequestParam("month") int month,
//            @RequestParam(value = "limit", defaultValue = "10") int limit) {
//        List<TopProductResponse> topProducts = statisticsService.getTopProductsByMonth(year, month, limit);
//        return ApiResponse.<List<TopProductResponse>>builder()
//                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
//                .success(Boolean.TRUE)
//                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
//                .data(topProducts)
//                .build();
//    }
//
//    /**
//     * Lấy danh sách sản phẩm bán chạy theo năm
//     * GET /api/statistics/top-products/by-year?year=2024&limit=10
//     */
//    @GetMapping("/top-products/by-year")
//    public ApiResponse<List<TopProductResponse>> getTopProductsByYear(
//            @RequestParam("year") int year,
//            @RequestParam(value = "limit", defaultValue = "10") int limit) {
//        List<TopProductResponse> topProducts = statisticsService.getTopProductsByYear(year, limit);
//        return ApiResponse.<List<TopProductResponse>>builder()
//                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
//                .success(Boolean.TRUE)
//                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
//                .data(topProducts)
//                .build();
//    }
}

