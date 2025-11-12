package com.example.backendplantshop.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsRequest {
    private Integer year;  // Năm (bắt buộc cho thống kê theo năm)
    private Integer month; // Tháng (1-12, bắt buộc cho thống kê theo tháng)
    private Integer day;   // Ngày (1-31, bắt buộc cho thống kê theo ngày)
}

