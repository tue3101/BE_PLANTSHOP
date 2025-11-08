package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.CartDetails;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartDetailMapper {
    // Lấy số lượng tồn kho của sản phẩm
    Integer findProductQuantityInStock(@Param("productID") int productId);
    // Lấy danh sách chi tiết giỏ hàng theo cartId
    List<CartDetails> findCartDetailByCartId(@Param("cartID") int cartId);

    // Thêm sản phẩm vào giỏ (nếu đã tồn tại thì tăng số lượng)
    void insetProductToCart(@Param("cartID") int cartId,
                            @Param("productID") int productId,
                            @Param("quantity") int quantity);

    // Cập nhật số lượng, selected trực tiếp
    void updateQuantity(@Param("cartID") int cartId,
                        @Param("productID") int productId,
                        @Param("quantity") int quantity,
                        @Param("selected") Boolean selected);

    // Tăng số lượng lên 1
//    void increaseQuantity(@Param("cartID") int cartId,
//                         @Param("productID") int productId);
//
//    // Giảm số lượng xuống 1
//    void decreaseQuantity(@Param("cartID") int cartId,
//                         @Param("productID") int productId);

    void deleteByUserId(@Param("userID") int userId);
    void restoreByUserId(@Param("userID") int userId);
    // Xóa mềm các sản phẩm đã selected (selected = true) trong giỏ hàng của user
    void deleteSelectedProductsByUserId(@Param("userID") int userId);

    // Xóa sản phẩm khỏi giỏ hàng (soft delete)
    void removeProductFromCart(@Param("cartID") int cartId,
                               @Param("productID") int productId);

    // Kiểm tra sản phẩm có bị soft delete không
    Boolean checkDeletedProduct(@Param("cartID") int cartId,
                               @Param("productID") int productId);

    // Restore sản phẩm đã bị soft delete và set quantity theo giá trị được truyền vào
    void restoreProductToCart(@Param("cartID") int cartId,
                             @Param("productID") int productId,
                              @Param("quantity") int quantity);


    Integer findQuantityInCart(@Param("cartID") Integer cartId, @Param("productID") int productId);

}
