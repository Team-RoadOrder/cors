package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.CartEntity;
import dev.gmpark.cors.mappers.CartMapper;
import dev.gmpark.cors.vos.CartVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartMapper cartMapper;

    public boolean addCart(String userEmail, Long itemId, String size, int quantity) {
        CartEntity cart = CartEntity.builder()
                .userEmail(userEmail)
                .itemId(itemId)
                .size(size)
                .quantity(quantity)
                .build();
        return this.cartMapper.insertCart(cart) > 0;
    }

    public CartVo[] getCartList(String userEmail) {
        return this.cartMapper.selectCartByUserEmail(userEmail);
    }
    
    public boolean deleteCartItem(Long id) {
        return this.cartMapper.deleteCartItem(id) > 0;
    }
}
