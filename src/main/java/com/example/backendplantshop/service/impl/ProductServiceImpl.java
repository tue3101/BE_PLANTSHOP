package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.dto.request.products.ProductDtoRequest;
import com.example.backendplantshop.dto.response.ProductDtoResponse;
import com.example.backendplantshop.entity.Products;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.ProductMapper;
import com.example.backendplantshop.convert.ProductConvert;
import com.example.backendplantshop.service.intf.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor

public class ProductServiceImpl implements ProductService {
    private final ProductMapper productMapper;
    private final AuthServiceImpl authService;
    private final CloudinaryServiceImpl cloudinaryService;


    private final CategoryServiceImpl categoryServiceImpl;


    public ProductDtoResponse findProductById(int id){
        var product = productMapper.findById(id);
        if (product == null){
            throw new AppException(ErrorCode.PRODUCT_NOT_EXISTS);
        }
        return ProductConvert.convertToProductDtoResponse(product);
    }

    public List<ProductDtoResponse> getAllProducts(){
        var products = ProductConvert.convertListProductToListProductDtoResponse(productMapper.getAll());
        if(products.isEmpty()){
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        return products;
    }

    public void insert(ProductDtoRequest productRequest, MultipartFile image) throws IOException {
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role) ) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        // Validate input - kiểm tra các trường bắt buộc
        if (productRequest.getProduct_name() == null || productRequest.getProduct_name().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        
        if (productRequest.getSize() == null || productRequest.getSize().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        
        if (productRequest.getPrice() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        
        if (productRequest.getQuantity() < 0) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        
        // Kiểm tra danh mục có được điền không
        if (productRequest.getCategory_id() < 0) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        
        // Kiểm tra danh mục tồn tại
        if (categoryServiceImpl.findById(productRequest.getCategory_id()) == null) {
            throw new AppException(ErrorCode.CATEGORY_NOT_EXISTS);
        }

        // Kiểm tra sản phẩm trùng tên, size và danh mục
        Products existingProduct = productMapper.findByProductNameAndSize(
            productRequest.getProduct_name(), 
            productRequest.getSize(),
            productRequest.getCategory_id()
        );

        if (existingProduct != null) {
            throw new AppException(ErrorCode.PRODUCT_ALREADY_EXISTS);
        }
        

        // Xử lý ảnh
        String imgUrl = processImage(image, productRequest.getImg_url());

        Products product = ProductConvert.toProducts(productRequest, imgUrl);

        productMapper.insert(product);

    }
//
//    public void update(int id, ProductDtoRequest productRequest, MultipartFile image) throws IOException {
//        // Kiểm tra danh mục tồn tại
//        if (categoryServiceImpl.findById(productRequest.getCategory_id()) == null) {
//            throw new AppException(ErrorCode.CATEGORY_NOT_EXISTS);
//        }
//
//
//        // Kiểm tra tên + size có bị trùng với sản phẩm khác không
//        Products existing = productMapper.findByProductNameAndSize(
//                productRequest.getProduct_name(),
//                productRequest.getSize()
//        );
//        if (existing != null && existing.getProduct_id() != id) {
//            throw new AppException(ErrorCode.PRODUCT_ALREADY_EXISTS);
//        }
//
//        // Lấy sản phẩm hiện tại
//        Products product = productMapper.findById(id);
//        if (product == null) {
//            throw new AppException(ErrorCode.PRODUCT_NOT_EXISTS);
//        }
//
//        Products updatedProduct = ProductConvert.toUpdatedProducts(id, productRequest, product);
//
//        productMapper.update(updatedProduct);
//    }
//
//    public void updateWithImage(int id, ProductDtoRequest productRequest, MultipartFile image) throws IOException {
//        // Kiểm tra danh mục tồn tại
//        if (categoryServiceImpl.findById(productRequest.getCategory_id()) == null) {
//            throw new AppException(ErrorCode.CATEGORY_NOT_EXISTS);
//        }
//
//        // Lấy sản phẩm hiện tại
//        Products existingProduct = productMapper.findById(id);
//        if (existingProduct == null) {
//            throw new AppException(ErrorCode.PRODUCT_NOT_EXISTS);
//        }
//
//        // Kiểm tra sản phẩm trùng tên và size (loại trừ sản phẩm hiện tại)
//        Products duplicateProduct = productMapper.findByProductNameAndSize(
//            productRequest.getProduct_name(),
//            productRequest.getSize()
//        );
//        if (duplicateProduct != null && duplicateProduct.getProduct_id() != id) {
//            throw new AppException(ErrorCode.PRODUCT_ALREADY_EXISTS);
//        }
//
//        // Xử lý ảnh mới nếu có
//        String imgUrl = existingProduct.getImg_url(); // Giữ nguyên ảnh cũ
//        if (image != null && !image.isEmpty()) {
//            imgUrl = processImage(image, existingProduct.getImg_url());
//        }
//
//        // Tạo sản phẩm cập nhật với ảnh mới
//        Products updatedProduct = ProductConvert.toUpdatedProducts(id, productRequest, existingProduct);
//        updatedProduct.setImg_url(imgUrl); // Set ảnh mới
//
//
//        productMapper.update(updatedProduct);
//    }
    public void update(int id, ProductDtoRequest productRequest, MultipartFile image) throws IOException {
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role) ) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        // Validate input - kiểm tra các trường bắt buộc
        if (productRequest.getProduct_name() == null || productRequest.getProduct_name().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        
        if (productRequest.getSize() == null || productRequest.getSize().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        
        if (productRequest.getPrice() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        
        if (productRequest.getQuantity() <0) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        
        // Kiểm tra danh mục có được điền không
        if (productRequest.getCategory_id() <0) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        
        // Kiểm tra danh mục tồn tại
        if (categoryServiceImpl.findById(productRequest.getCategory_id()) == null) {
            throw new AppException(ErrorCode.CATEGORY_NOT_EXISTS);
        }

        // Kiểm tra sản phẩm hiện tại
        Products product = productMapper.findById(id);
        if (product == null) {
            throw new AppException(ErrorCode.PRODUCT_NOT_EXISTS);
        }

        // Kiểm tra trùng tên + size + danh mục
        Products duplicate = productMapper.findByProductNameAndSize(productRequest.getProduct_name(), productRequest.getSize(), productRequest.getCategory_id());
        if (duplicate != null && duplicate.getProduct_id() != id) {
            throw new AppException(ErrorCode.PRODUCT_ALREADY_EXISTS);
        }

        // Xử lý ảnh nếu có
        String imgUrl = product.getImg_url();
        if (image != null && !image.isEmpty()) {
            imgUrl = processImage(image, product.getImg_url());
        }

        // Cập nhật sản phẩm
        Products updatedProduct = ProductConvert.toUpdatedProducts(id, productRequest, product);
        updatedProduct.setImg_url(imgUrl);
        productMapper.update(updatedProduct);
    }


    public void delete(int id){
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role) ) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if(productMapper.findById(id)==null){
            throw new AppException(ErrorCode.PRODUCT_NOT_EXISTS);
        }
        productMapper.delete(id);
    }

    private String processImage(MultipartFile image, String defaultImgUrl) {
        if (image != null && !image.isEmpty()) {
            try {
                return cloudinaryService.uploadImage(image, "plantshop/products");
            } catch (Exception e) {
                log.error("Error processing image", e);
                throw new RuntimeException("Failed to process image", e);
            }
        } else {
            // Nếu không có ảnh, sử dụng ảnh mặc định hoặc để trống
            return defaultImgUrl != null ? defaultImgUrl : "";
        }
    }

    // Phương thức xử lý ảnh
//    private String processImage(MultipartFile image, String defaultImgUrl) throws IOException {
//        if (image != null && !image.isEmpty()) {
//            // Thư mục lưu ảnh runtime
//            //user.dir là một thuộc tính của hệ thống Java dùng lấy đường dẫn gốc thư mục làm việc hiện tại và thêm images tạo đường dẫn
//            String uploadDir = System.getProperty("user.dir") + "/images/";
//            File uploadFolder = new File(uploadDir); //khởi tạo đối tượng đại diện đường dẫn
//
//            //nếu chưa tồn tại thì tạo mới thư mục
//            if (!uploadFolder.exists()) {
//                uploadFolder.mkdirs();
//            }
//
//            //tạo tên file mới để lưu ảnh (thời gian hiện tại _ tên file gốc)
//            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
//            File dest = new File(uploadDir + fileName); //tạo đối tượng đại diện vị trí upload
//            image.transferTo(dest); //copy dữ liệu từ MultipartFile sang file vật lý
//
//            return "/images/" + fileName;
//        } else {
//            // Nếu không có ảnh, sử dụng ảnh mặc định hoặc để trống
//            return defaultImgUrl != null ? defaultImgUrl : "";
//        }
//    }

    @Override
    public void restoreProduct(int id) {
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role) ) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if(productMapper.findByIdDeleted(id) == null){
            throw new AppException(ErrorCode.NOT_DELETE);
        }
        productMapper.restoreProduct(id);
    }

}
