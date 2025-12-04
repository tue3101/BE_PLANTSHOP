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
import com.example.backendplantshop.enums.ShippingStatus;
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
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        // Kiểm tra sản phẩm có tồn tại không
        Products product = productMapper.findById(request.getProduct_id());
        if (product == null) {
            log.error("Sản phẩm không tồn tại: product_id={}", request.getProduct_id());
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        
        // Kiểm tra order_detail hợp lệ và thuộc user hiện tại
        OrderDetails orderDetail = validateOrderDetailForReview(currentUserId, request);
        
        // Mỗi order_detail chỉ được đánh giá một lần
        ProductReview existingReview = productReviewMapper.findByOrderDetailId(orderDetail.getOrder_detail_id());
        if (existingReview != null) {
            log.warn("Chi tiết đơn hàng {} đã có review {}", orderDetail.getOrder_detail_id(), existingReview.getReview_id());
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
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


    //hàm lấy review thuộc sản phẩm của user (cho admin xem chi tiết)
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


    //hàm lấy các review của sản phẩm đó (để xem tất cả review của sp khi vào trang chi tiết)
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
                    return ProductReviewConvert.convertToProductReviewDtoResponseWithUser(review, user, product);
                })
                .collect(Collectors.toList()); ////gom các kết quả sau khi map thành 1 list
    }


    //hàm lấy review của user (khi user xem lại đánh giá của mình trong đơn hàng)
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
        
        // Kiểm tra quyền: chỉ user tạo review
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        
        if (authService.isAdmin(role) && existingReview.getUser_id() != currentUserId) {
            log.warn("User không có quyền sửa review này. Review user: {}, Current user: {}", 
                    existingReview.getUser_id(), currentUserId);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        if (request.getProduct_id() == null || request.getOrder_detail_id() == null ||
                existingReview.getProduct_id() != request.getProduct_id() ||
                existingReview.getOrder_detail_id() != request.getOrder_detail_id()) {
            log.warn("Yêu cầu cập nhật review không khớp order detail hoặc sản phẩm: review_id={}, user_id={}",
                    reviewId, currentUserId);
            throw new AppException(ErrorCode.ORDER_DETAIL_PRODUCT_MISMATCH);
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


    //lấy tất cả các đánh giá (admin)
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

    //kiểm tra các điều kiện để được đánh giá
    private OrderDetails validateOrderDetailForReview(int userId, ProductReviewDtoRequest request) {
        if (request.getOrder_detail_id() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        OrderDetails orderDetail = orderDetailMapper.findById(request.getOrder_detail_id());
        if (orderDetail == null) {
            log.warn("Không tìm thấy order_detail_id={} để đánh giá", request.getOrder_detail_id());
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        //kiểm tra chi tiết đơn có khớp với sp ko
        if (orderDetail.getProduct_id() != request.getProduct_id()) {
            log.warn("order_detail_id={} không thuộc product_id={} (thực tế product_id={})",
                    request.getOrder_detail_id(), request.getProduct_id(), orderDetail.getProduct_id());
            throw new AppException(ErrorCode.ORDER_DETAIL_PRODUCT_MISMATCH);
        }

        Orders order = orderMapper.findById(orderDetail.getOrder_id());
        if (order == null) {
            log.warn("Không tìm thấy order_id={} cho order_detail_id={}", orderDetail.getOrder_id(), orderDetail.getOrder_detail_id());
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        //kiểm tra user đang đánh giá có phải user đang login
        if (order.getUser_id() != userId) {
            log.warn("User {} cố gắng đánh giá order_detail {} không thuộc sở hữu", userId, orderDetail.getOrder_detail_id());
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        //kiểm tra đơn giao thaành công chưa
        if (order.getShipping_status() != ShippingStatus.DELIVERED) {
            log.warn("Đơn hàng {} chưa giao thành công nên chưa thể đánh giá", order.getOrder_id());
            throw new AppException(ErrorCode.ORDER_NOT_DELIVERED_FOR_REVIEW);
        }

        return orderDetail;
    }
}

