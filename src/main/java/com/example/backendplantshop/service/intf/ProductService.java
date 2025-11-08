package com.example.backendplantshop.service.intf;

import com.example.backendplantshop.dto.request.products.ProductDtoRequest;
import com.example.backendplantshop.dto.response.ProductDtoResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductService {
    ProductDtoResponse findProductById(int id);
    List<ProductDtoResponse> getAllProducts();
    void insert(ProductDtoRequest productRequest, MultipartFile image) throws IOException;
    void update(int id, ProductDtoRequest productRequest, MultipartFile image) throws IOException;
    void delete(int id);
    void restoreProduct(int id);
    List<ProductDtoResponse> getAllProductDeleted();

}
