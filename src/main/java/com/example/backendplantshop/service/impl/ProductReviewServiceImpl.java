package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.convert.ProductReviewConvert;
import com.example.backendplantshop.convert.UserConvert;
import com.example.backendplantshop.dto.request.ProductReviewDtoRequest;
import com.example.backendplantshop.dto.response.ProductReviewDtoResponse;
import com.example.backendplantshop.entity.OrderDetails;
import com.example.backendplantshop.entity.Orders;
import com.example.backendplantshop.entity.ProductReview;
import com.example.backendplantshop.entity.Products;
import com.example.backendplantshop.entity.Users;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.enums.OrderSatus;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.OrderDetailMapper;
import com.example.backendplantshop.mapper.OrderMapper;
import com.example.backendplantshop.mapper.ProductMapper;
import com.example.backendplantshop.mapper.ProductReviewMapper;
import com.example.backendplantshop.mapper.UserMapper;
import com.example.backendplantshop.service.intf.ProductReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {
    
    private final ProductReviewMapper productReviewMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final AuthServiceImpl authService;
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    
    @Override
    @Transactional
    public ProductReviewDtoResponse createReview(ProductReviewDtoRequest request) {
        // Lấy user hiện tại
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        
        // Kiểm tra quyền: chỉ USER mới được tạo review
        if (!authService.isUser(role) && !authService.isAdmin(role)) {
            log.warn("User không có quyền tạo review. Role: {}", role);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        // Kiểm tra sản phẩm có tồn tại không
        Products product = productMapper.findById(request.getProduct_id());
        if (product == null) {
            log.error("Sản phẩm không tồn tại: product_id={}", request.getProduct_id());
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        
        // Kiểm tra user đã có đơn hàng thành công (DELIVERED) chứa sản phẩm này chưa
        boolean hasPurchasedProduct = checkUserHasPurchasedProduct(currentUserId, request.getProduct_id());
        if (!hasPurchasedProduct) {
            log.warn("User chưa mua sản phẩm này hoặc đơn hàng chưa thành công: user_id={}, product_id={}", 
                    currentUserId, request.getProduct_id());
            throw new AppException(ErrorCode.ACCESS_DENIED); // Có thể tạo ErrorCode mới cho trường hợp này
        }
        
        // Kiểm tra user đã review sản phẩm này chưa
        // Nếu đã review, cập nhật review cũ thay vì tạo mới
        List<ProductReview> existingReviews = productReviewMapper.findByProductIdAndUserId(
                request.getProduct_id(), currentUserId);
        if (!existingReviews.isEmpty()) {
            // User đã review, cập nhật review cũ
            ProductReview existingReview = existingReviews.get(0);
            log.info("User đã review sản phẩm này, cập nhật review cũ: review_id={}, user_id={}, product_id={}", 
                    existingReview.getReview_id(), currentUserId, request.getProduct_id());
            
            LocalDateTime now = LocalDateTime.now();
            ProductReview updatedReview = ProductReviewConvert.convertToUpdatedProductReview(existingReview, request, now);
            productReviewMapper.update(updatedReview);
            
            Users user = userMapper.findById(currentUserId);
            return ProductReviewConvert.convertToProductReviewDtoResponseWithUser(updatedReview, user, product);
        }
        
        // Tạo review
        LocalDateTime now = LocalDateTime.now();
        ProductReview review = ProductReviewConvert.convertToProductReview(request, currentUserId, now);
        productReviewMapper.insert(review);
        
        log.info("Đã tạo review: review_id={}, user_id={}, product_id={}, rating={}", 
                review.getReview_id(), currentUserId, request.getProduct_id(), request.getRating());
        
        // Lấy thông tin user để trả về
        Users user = userMapper.findById(currentUserId);
        return ProductReviewConvert.convertToProductReviewDtoResponseWithUser(review, user, product);
    }
    
    @Override
    public ProductReviewDtoResponse getReviewById(int reviewId) {
        ProductReview review = productReviewMapper.findById(reviewId);
        if (review == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        
        // Lấy thông tin user và product
        Users user = userMapper.findById(review.getUser_id());
        Products product = productMapper.findById(review.getProduct_id());
        return ProductReviewConvert.convertToProductReviewDtoResponseWithUser(review, user, product);
    }
    
    @Override
    public List<ProductReviewDtoResponse> getReviewsByProductId(int productId) {
        // Kiểm tra sản phẩm có tồn tại không
        Products product = productMapper.findById(productId);
        if (product == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        
        List<ProductReview> reviews = productReviewMapper.findByProductId(productId);
        
        // Convert và thêm thông tin user và product
        return reviews.stream()
                .map(review -> {
                    Users user = userMapper.findById(review.getUser_id());
                    Products productForReview = productMapper.findById(review.getProduct_id());
                    return ProductReviewConvert.convertToProductReviewDtoResponseWithUser(review, user, productForReview);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ProductReviewDtoResponse> getReviewsByUserId(int userId) {
        // Kiểm tra quyền: user chỉ xem được review của chính mình, admin xem được tất cả
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        
        if (!authService.isAdmin(role) && currentUserId != userId) {
            log.warn("User không có quyền xem review của user khác. Current user: {}, Requested user: {}", 
                    currentUserId, userId);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        List<ProductReview> reviews = productReviewMapper.findByUserId(userId);
        
        // Convert và thêm thông tin user và product
        return reviews.stream()
                .map(review -> {
                    Users user = userMapper.findById(review.getUser_id());
                    Products productForReview = productMapper.findById(review.getProduct_id());
                    return ProductReviewConvert.convertToProductReviewDtoResponseWithUser(review, user, productForReview);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public ProductReviewDtoResponse updateReview(int reviewId, ProductReviewDtoRequest request) {
        // Lấy review hiện tại
        ProductReview existingReview = productReviewMapper.findById(reviewId);
        if (existingReview == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        
        // Kiểm tra quyền: chỉ user tạo review hoặc admin mới được sửa
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        
        if (!authService.isAdmin(role) && existingReview.getUser_id() != currentUserId) {
            log.warn("User không có quyền sửa review này. Review user: {}, Current user: {}", 
                    existingReview.getUser_id(), currentUserId);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        // Cập nhật review
        LocalDateTime now = LocalDateTime.now();
        ProductReview updatedReview = ProductReviewConvert.convertToUpdatedProductReview(existingReview, request, now);
        productReviewMapper.update(updatedReview);
        
        log.info("Đã cập nhật review: review_id={}", reviewId);
        
        // Lấy thông tin user và product để trả về
        Users user = userMapper.findById(existingReview.getUser_id());
        Products product = productMapper.findById(existingReview.getProduct_id());
        return ProductReviewConvert.convertToProductReviewDtoResponseWithUser(updatedReview, user, product);
    }
    
    @Override
    @Transactional
    public void deleteReview(int reviewId) {
        // Lấy review hiện tại
        ProductReview review = productReviewMapper.findById(reviewId);
        if (review == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        
        // Kiểm tra quyền: chỉ user tạo review hoặc admin mới được xóa
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        
        if (!authService.isAdmin(role) && review.getUser_id() != currentUserId) {
            log.warn("User không có quyền xóa review này. Review user: {}, Current user: {}", 
                    review.getUser_id(), currentUserId);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        // Soft delete
        productReviewMapper.delete(reviewId);
        log.info("Đã xóa review: review_id={}", reviewId);
    }
    
    @Override
    public List<ProductReviewDtoResponse> getAllReviews() {
        // Chỉ admin mới được xem tất cả review
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role)) {
            log.warn("User không có quyền xem tất cả review. Role: {}", role);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        List<ProductReview> reviews = productReviewMapper.getAll();
        
        // Convert và thêm thông tin user và product
        return reviews.stream()
                .map(review -> {
                    Users user = userMapper.findById(review.getUser_id());
                    Products product = productMapper.findById(review.getProduct_id());
                    return ProductReviewConvert.convertToProductReviewDtoResponseWithUser(review, user, product);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ProductReviewDtoResponse> getAllDeletedReviews() {
        // Chỉ admin mới được xem các review đã bị xóa
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role)) {
            log.warn("User không có quyền xem các review đã bị xóa. Role: {}", role);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        List<ProductReview> deletedReviews = productReviewMapper.getAllDeleted();
        
        if (deletedReviews == null || deletedReviews.isEmpty()) {
            log.info("Không có review nào đã bị xóa");
            return new ArrayList<>();
        }
        
        log.info("Lấy tất cả review đã bị xóa: {} review", deletedReviews.size());
        
        // Convert và thêm thông tin user và product
        return deletedReviews.stream()
                .map(review -> {
                    Users user = userMapper.findById(review.getUser_id());
                    Products product = productMapper.findById(review.getProduct_id());
                    return ProductReviewConvert.convertToProductReviewDtoResponseWithUser(review, user, product);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public ProductReviewDtoResponse restoreReview(int reviewId) {
        // Chỉ admin mới được khôi phục review
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role)) {
            log.warn("User không có quyền khôi phục review. Role: {}", role);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        // Kiểm tra review có tồn tại và đã bị xóa không
        ProductReview deletedReview = productReviewMapper.findByIdDeleted(reviewId);
        if (deletedReview == null) {
            log.warn("Review không tồn tại hoặc chưa bị xóa: review_id={}", reviewId);
            throw new AppException(ErrorCode.NOT_DELETE); // Sử dụng ErrorCode tương tự như restore user
        }
        
        // Khôi phục review
        productReviewMapper.restoreReview(reviewId);
        log.info("Đã khôi phục review: review_id={}", reviewId);
        
        // Lấy lại review sau khi restore (lúc này is_deleted = 0)
        ProductReview restoredReview = productReviewMapper.findById(reviewId);
        if (restoredReview == null) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        
        // Lấy thông tin user và product để trả về
        Users user = userMapper.findById(restoredReview.getUser_id());
        Products product = productMapper.findById(restoredReview.getProduct_id());
        return ProductReviewConvert.convertToProductReviewDtoResponseWithUser(restoredReview, user, product);
    }
  
    private boolean checkUserHasPurchasedProduct(int userId, int productId) {
        // Lấy tất cả đơn hàng của user
        List<Orders> userOrders = orderMapper.findByUserId(userId);
        
        if (userOrders == null || userOrders.isEmpty()) {
            log.debug("User chưa có đơn hàng nào: user_id={}", userId);
            return false;
        }
        
        // Lọc các đơn hàng có status DELIVERED (thành công)
        List<Orders> deliveredOrders = userOrders.stream()
                .filter(order -> order.getStatus() == OrderSatus.DELIVERED)
                .collect(Collectors.toList());
        
        if (deliveredOrders.isEmpty()) {
            log.debug("User chưa có đơn hàng thành công: user_id={}", userId);
            return false;
        }
        
        // Kiểm tra xem có đơn hàng nào chứa sản phẩm này không
        for (Orders order : deliveredOrders) {
            List<OrderDetails> orderDetails = orderDetailMapper.findByOrderId(order.getOrder_id());
            
            // Kiểm tra xem đơn hàng có chứa sản phẩm này không
            boolean containsProduct = orderDetails.stream()
                    .anyMatch(detail -> detail.getProduct_id() == productId);
            
            if (containsProduct) {
                log.info("User đã mua sản phẩm này trong đơn hàng thành công: user_id={}, product_id={}, order_id={}", 
                        userId, productId, order.getOrder_id());
                return true;
            }
        }
        
        log.debug("User chưa mua sản phẩm này trong đơn hàng thành công: user_id={}, product_id={}", 
                userId, productId);
        return false;
    }
}

