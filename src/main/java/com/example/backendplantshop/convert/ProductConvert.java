package com.example.backendplantshop.convert;

import com.example.backendplantshop.dto.request.products.ProductDtoRequest;
import com.example.backendplantshop.dto.response.ProductDtoResponse;
import com.example.backendplantshop.entity.Products;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ProductConvert {
    // Phương thức chuyển đổi từ Products sang ProductDtoResponse sử dụng Builder
    public static ProductDtoResponse convertToProductDtoResponse(Products product) {
        return ProductDtoResponse.builder()
                .product_id(product.getProduct_id())
                .product_name(product.getProduct_name())
                .description(product.getDescription())
                .img_url(product.getImg_url())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .size(product.getSize())
                .out_of_stock(product.isOut_of_stock())
                .category_id(product.getCategory_id())
                .build();
    }

    //phương thức chuyển đổi từ ProductDtoRequest sang Products
    public static Products toProducts(ProductDtoRequest request, String imgUrl) {
        return Products.builder()
                .product_name(request.getProduct_name())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .size(request.getSize())
                .out_of_stock(request.getOut_of_stock())
                .img_url(imgUrl)
                .category_id(request.getCategory_id())
                .build();
    }

    public static List<ProductDtoResponse> convertListProductToListProductDtoResponse(List<Products> productList) {
        return productList.stream() //lấy danh sách productList (các entity từ DB) biến nó thành luồng dữ liệu (stream) để xử lý tuần tự từng phần tử
                .map(ProductConvert::convertToProductDtoResponse) //dùng map để biến đổi từng phần tử trong luồng stream (Lấy từng phần tử Products trong stream → truyền vào hàm)
                .collect(Collectors.toList()); //gom các kết quả sau khi map thành 1 list dtoResponse
    }

    public static Products toUpdatedProducts(int id, ProductDtoRequest request, Products existingProduct) {
        return Products.builder()
                .product_id(id)
                .product_name(request.getProduct_name()  != null ? request.getProduct_name() :existingProduct.getProduct_name())
                .description(request.getDescription() != null ? request.getDescription() :existingProduct.getDescription() )
                .price(request.getPrice() != null ? request.getPrice() :existingProduct.getPrice())
                .quantity(request.getQuantity() != null ? request.getQuantity() : existingProduct.getQuantity())
                .size(request.getSize()  != null ? request.getSize() : existingProduct.getSize())
                .out_of_stock(request.getOut_of_stock() != null ? request.getOut_of_stock() : existingProduct.isOut_of_stock())
                .img_url(existingProduct.getImg_url() != null ? request.getImg_url() : existingProduct.getImg_url()) // Giữ nguyên ảnh cũ
                .category_id(request.getCategory_id() != null ? request.getCategory_id() : existingProduct.getCategory_id())
                .created_at(existingProduct.getCreated_at()) // Giữ nguyên ngày tạo
                .updated_at(LocalDateTime.now()) // Cập nhật ngày sửa
                .is_deleted(existingProduct.is_deleted())
                .build();
    }

    // Phương thức restore và update product với dữ liệu mới
    public static Products toRestoreProduct(Products existingProduct, ProductDtoRequest request, String imgUrl) {
        return Products.builder()
                .product_id(existingProduct.getProduct_id())
                .product_name(request.getProduct_name())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .size(request.getSize())
                .out_of_stock(request.getOut_of_stock())
                .img_url(imgUrl)
                .category_id(request.getCategory_id())
                .created_at(existingProduct.getCreated_at()) // Giữ nguyên ngày tạo
                .is_deleted(false) // Restore product
                .build();
    }

}
