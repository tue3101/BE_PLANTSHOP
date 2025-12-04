package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.dto.request.products.ProductDtoRequest;
import com.example.backendplantshop.dto.response.ProductDtoResponse;
import com.example.backendplantshop.entity.Products;
import com.example.backendplantshop.entity.Users;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.ProductMapper;
import com.example.backendplantshop.mapper.CategoryMapper;
import com.example.backendplantshop.mapper.OrderDetailMapper;
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
    private final CategoryMapper categoryMapper;
    private final CategoryServiceImpl categoryServiceImpl;
    private final OrderDetailMapper orderDetailMapper;


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
        Products existingProduct = productMapper.findByProductName_Size_Category(
            productRequest.getProduct_name(), 
            productRequest.getSize(),
            productRequest.getCategory_id()
        );

        if (existingProduct != null) {
            // Nếu sản phẩm đã tồn tại và chưa bị xóa
            if (!existingProduct.is_deleted()) {
                throw new AppException(ErrorCode.PRODUCT_ALREADY_EXISTS);
            }
            // Nếu sản phẩm đã tồn tại nhưng đã bị xóa mềm 
            if (existingProduct.is_deleted()) {
                String imgUrl = processImage(image, productRequest.getImg_url());
                Products productToUpdate = ProductConvert.toRestoreProduct(existingProduct, productRequest, imgUrl);
                productMapper.update(productToUpdate);
                return;
            }
        }
        
        // Nếu không tìm thấy sản phẩm nào → tạo mới
        String imgUrl = processImage(image, productRequest.getImg_url());
        Products product = ProductConvert.toProducts(productRequest, imgUrl);
        productMapper.insert(product);

    }

    private String clean(String input) {
        return (input != null && !input.trim().isEmpty()) ? input : null;
    }

    public void update(int id, ProductDtoRequest productRequest, MultipartFile image) throws IOException {
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role) && currentUserId != id) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        Products existingProduct = productMapper.findById(id);
        if (existingProduct == null) {
            throw new AppException(ErrorCode.PRODUCT_NOT_EXISTS);
        }

        productRequest.setProduct_name(clean(productRequest.getProduct_name()));
        productRequest.setDescription(clean(productRequest.getDescription()));
        productRequest.setImg_url(clean(productRequest.getImg_url()));
        productRequest.setPrice(productRequest.getPrice());
        productRequest.setQuantity(productRequest.getQuantity());
        productRequest.setSize(productRequest.getSize());
        productRequest.setOut_of_stock(productRequest.getOut_of_stock());
        productRequest.setCategory_id(productRequest.getCategory_id());


        // Kiểm tra danh mục tồn tại
        if (categoryServiceImpl.findById(productRequest.getCategory_id()) == null) {
            throw new AppException(ErrorCode.CATEGORY_NOT_EXISTS);
        }


        // Kiểm tra trùng tên + size + danh mục
        Products duplicate = productMapper.findByProductName_Size_Category(productRequest.getProduct_name(), productRequest.getSize(), productRequest.getCategory_id());
        if (duplicate != null && duplicate.getProduct_id() != id) {
            throw new AppException(ErrorCode.PRODUCT_ALREADY_EXISTS);
        }

        // Xử lý ảnh
        String imgUrl = existingProduct.getImg_url();
        if (image != null && !image.isEmpty()) {
            imgUrl = processImage(image, existingProduct.getImg_url());
        }

        Products updatedProduct = ProductConvert.toUpdatedProducts(id, productRequest, existingProduct);
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

//        kiểm tra sp có đang tồn tại trong đơn hàng nào ko
        int activeOrderCount = orderDetailMapper.countActiveOrderDetailsByProductId(id);
        if (activeOrderCount > 0) {
            throw new AppException(ErrorCode.PRODUCT_IN_ORDER_NOT_DELETABLE);
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
        
        // Kiểm tra sản phẩm có bị xóa mềm không
        Products deletedProduct = productMapper.findByIdDeleted(id);
        if(deletedProduct == null){
            throw new AppException(ErrorCode.NOT_DELETE);
        }
        
        // Kiểm tra danh mục của sản phẩm có bị xóa mềm không
        var categoryDeleted = categoryMapper.findByIdDeleted(deletedProduct.getCategory_id());
        if(categoryDeleted != null) {
            throw new AppException(ErrorCode.CANNOT_RESTORE_PRODUCT_CATEGORY_DELETED);
        }
        
        productMapper.restoreProduct(id);
    }

    @Override
    public List<ProductDtoResponse> getAllProductDeleted() {
        var products = ProductConvert.convertListProductToListProductDtoResponse(productMapper.getAllProductDeleted());
        if(products.isEmpty()){
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        return products;
    }

}
