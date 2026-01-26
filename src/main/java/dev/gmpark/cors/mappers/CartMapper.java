package dev.gmpark.cors.mappers;

import dev.gmpark.cors.entities.CartEntity;
import dev.gmpark.cors.vos.CartVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartMapper {
    int insertCart(CartEntity cart);
    CartVo[] selectCartByUserEmail(@Param("userEmail") String userEmail);
    int deleteCartItem(@Param("id") Long id);
    int deleteCartItems(@Param("ids") List<Long> ids); // 추가: 일괄 삭제
    List<CartVo> selectCartItemsByIds(@Param("ids") List<Long> ids);
    int selectCartCount(@Param("userEmail") String userEmail);
    
    // 추가된 메서드
    CartEntity selectCartItem(@Param("userEmail") String userEmail, @Param("itemId") Long itemId, @Param("size") String size);
    int updateCartQuantity(@Param("id") Long id, @Param("quantity") int quantity);
    int updateCartQuantityById(@Param("id") Long id, @Param("quantity") int quantity);
}
