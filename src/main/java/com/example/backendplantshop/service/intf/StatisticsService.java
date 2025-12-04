package com.example.backendplantshop.service.intf;

import com.example.backendplantshop.dto.request.StatisticsRequest;
import com.example.backendplantshop.dto.response.ProductSalesResponse;
import com.example.backendplantshop.dto.response.StatisticsResponse;
import com.example.backendplantshop.dto.response.TopProductResponse;

import java.util.List;

public interface StatisticsService {
    /**
     * Lấy thống kê theo ngày
     */
    StatisticsResponse getStatisticsByDate(int year, int month, int day);
    
    /**
     * Lấy thống kê theo tháng
     */
    StatisticsResponse getStatisticsByMonth(int year, int month);
    
    /**
     * Lấy thống kê theo năm
     */
    StatisticsResponse getStatisticsByYear(int year);
    
    /**
     * Lấy tổng số sản phẩm bán được theo tháng
     */
    ProductSalesResponse getTotalProductsSoldByMonth(int year, int month);
    
    /**
     * Lấy tổng số sản phẩm bán được theo năm
     */
    ProductSalesResponse getTotalProductsSoldByYear(int year);
    
//    /**
//     * Lấy danh sách sản phẩm bán chạy theo ngày
//     */
//    List<TopProductResponse> getTopProductsByDate(int year, int month, int day, int limit);
//
//    /**
//     * Lấy danh sách sản phẩm bán chạy theo tháng
//     */
//    List<TopProductResponse> getTopProductsByMonth(int year, int month, int limit);
//
//    /**
//     * Lấy danh sách sản phẩm bán chạy theo năm
//     */
//    List<TopProductResponse> getTopProductsByYear(int year, int limit);
}

