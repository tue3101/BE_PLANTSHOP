package com.example.backendplantshop.dto.request.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDtoRequest {
    private int product_id;
//    @NotBlank(message = "tên sản phẩm không được bỏ trống")
    private String product_name;
    private String description;
    private String img_url;
//    @NotBlank(message = "giá sản phẩm không được bỏ trống")
    private BigDecimal price;
//    @NotBlank(message = "số lượng sản phẩm không được bỏ trống")
    private Integer quantity;
//    @NotBlank(message = "kích cỡ không được bỏ trống")
    private String size;
    private Boolean out_of_stock;
    private boolean is_deleted;
//    @NotBlank(message = "vui lòng chọn danh mục")
    private Integer category_id;
}
