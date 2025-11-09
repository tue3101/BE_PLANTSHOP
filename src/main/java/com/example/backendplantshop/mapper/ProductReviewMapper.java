package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductReviewMapper {
    int insert(ProductReview review);

    ProductReview findById(@Param("reviewID") int reviewId);

    List<ProductReview> findByProductId(@Param("productID") int productId);

    List<ProductReview> findByUserId(@Param("userID") int userId);

    List<ProductReview> findByProductIdAndUserId(@Param("productID") int productId, @Param("userID") int userId);

    void update(ProductReview review);

    void delete(@Param("reviewID") int reviewId);

    List<ProductReview> getAll();

    List<ProductReview> getAllDeleted();

    ProductReview findByIdDeleted(@Param("reviewID") int reviewId);

    void restoreReview(@Param("reviewID") int reviewId);
}
