package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.dto.response.OrderStatisticsDtoResponse;
import com.example.backendplantshop.dto.response.ProductSalesResponse;
import com.example.backendplantshop.dto.response.StatisticsResponse;
import com.example.backendplantshop.dto.response.TopProductResponse;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.OrderDetailMapper;
import com.example.backendplantshop.mapper.OrderMapper;
import com.example.backendplantshop.service.intf.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    
    @Override
    public StatisticsResponse getStatisticsByDate(int year, int month, int day) {
        validateDate(year, month, day);
        
        OrderStatisticsDtoResponse result = orderMapper.getStatisticsByDate(year, month, day);
        return convertToStatisticsResponse(result);
    }
    
    @Override
    public StatisticsResponse getStatisticsByMonth(int year, int month) {
        validateMonth(year, month);
        
        OrderStatisticsDtoResponse result = orderMapper.getStatisticsByMonth(year, month);
        return convertToStatisticsResponse(result);
    }
    
    @Override
    public StatisticsResponse getStatisticsByYear(int year) {
        validateYear(year);
        
        OrderStatisticsDtoResponse result = orderMapper.getStatisticsByYear(year);
        return convertToStatisticsResponse(result);
    }
    
    @Override
    public ProductSalesResponse getTotalProductsSoldByMonth(int year, int month) {
        validateMonth(year, month);
        
        Integer totalQuantity = orderDetailMapper.getTotalProductsSoldByMonth(year, month);
        if (totalQuantity == null) {
            totalQuantity = 0;
        }
        
        return ProductSalesResponse.builder()
                .totalQuantitySold(totalQuantity)
                .year(year)
                .month(month)
                .build();
    }
    
    @Override
    public ProductSalesResponse getTotalProductsSoldByYear(int year) {
        validateYear(year);
        
        Integer totalQuantity = orderDetailMapper.getTotalProductsSoldByYear(year);
        if (totalQuantity == null) {
            totalQuantity = 0;
        }
        
        return ProductSalesResponse.builder()
                .totalQuantitySold(totalQuantity)
                .year(year)
                .month(null)
                .build();
    }
    
//    @Override
//    public List<TopProductResponse> getTopProductsByDate(int year, int month, int day, int limit) {
//        validateDate(year, month, day);
//        validateLimit(limit);
//
//        List<Map<String, Object>> results = orderDetailMapper.getTopProductsByDate(year, month, day, limit);
//        return convertToTopProductResponseList(results);
//    }
//
//    @Override
//    public List<TopProductResponse> getTopProductsByMonth(int year, int month, int limit) {
//        validateMonth(year, month);
//        validateLimit(limit);
//
//        List<Map<String, Object>> results = orderDetailMapper.getTopProductsByMonth(year, month, limit);
//        return convertToTopProductResponseList(results);
//    }
//
//    @Override
//    public List<TopProductResponse> getTopProductsByYear(int year, int limit) {
//        validateYear(year);
//        validateLimit(limit);
//
//        List<Map<String, Object>> results = orderDetailMapper.getTopProductsByYear(year, limit);
//        return convertToTopProductResponseList(results);
//    }
//
    private StatisticsResponse convertToStatisticsResponse(OrderStatisticsDtoResponse result) {
        if (result == null) {
            return StatisticsResponse.builder()
                    .totalRevenue(BigDecimal.ZERO)
                    .totalOrders(0)
                    .completedOrders(0)
                    .cancelledOrders(0)
                    .averageOrderValue(BigDecimal.ZERO)
                    .build();
        }
        
        return StatisticsResponse.builder()
                .totalRevenue(result.getTotalRevenue() != null ? result.getTotalRevenue() : BigDecimal.ZERO)
                .totalOrders(result.getTotalOrders() != null ? result.getTotalOrders() : 0)
                .completedOrders(result.getCompletedOrders() != null ? result.getCompletedOrders() : 0)
                .cancelledOrders(result.getCancelledOrders() != null ? result.getCancelledOrders() : 0)
                .averageOrderValue(result.getAverageOrderValue() != null ? result.getAverageOrderValue() : BigDecimal.ZERO)
                .build();
    }
    
    private List<TopProductResponse> convertToTopProductResponseList(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        
        return results.stream()
                .map(this::convertToTopProductResponse)
                .collect(Collectors.toList());
    }
    
    private TopProductResponse convertToTopProductResponse(Map<String, Object> result) {
        Integer productId = getIntegerValue(result, "product_id");
        String productName = getStringValue(result, "product_name");
        String imgUrl = getStringValue(result, "img_url");
        Integer totalQuantitySold = getIntegerValue(result, "totalQuantitySold");
        BigDecimal totalRevenue = getBigDecimalValue(result, "totalRevenue");
        Integer orderCount = getIntegerValue(result, "orderCount");
        
        return TopProductResponse.builder()
                .productId(productId)
                .productName(productName)
                .imgUrl(imgUrl)
                .totalQuantitySold(totalQuantitySold != null ? totalQuantitySold : 0)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .orderCount(orderCount != null ? orderCount : 0)
                .build();
    }
    
    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            log.warn("Không thể chuyển đổi giá trị {} thành BigDecimal: {}", value, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            log.warn("Không thể chuyển đổi giá trị {} thành Integer: {}", value, e.getMessage());
            return 0;
        }
    }
    
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    private void validateDate(int year, int month, int day) {
        validateYear(year);
        validateMonth(year, month);
        
        if (day < 1 || day > 31) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
    }
    
    private void validateMonth(int year, int month) {
        validateYear(year);
        
        if (month < 1 || month > 12) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
    }
    
    private void validateYear(int year) {
        if (year < 2000 || year > 2100) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
    }
    
    private void validateLimit(int limit) {
        if (limit < 1 || limit > 100) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
    }
}

