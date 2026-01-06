package dev.gmpark.cors.mappers;

import dev.gmpark.cors.entities.CartEntity;
import dev.gmpark.cors.vos.CartVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CartMapper {
    int insertCart(CartEntity cart);
    CartVo[] selectCartByUserEmail(@Param("userEmail") String userEmail);
    int deleteCartItem(@Param("id") Long id);
}
