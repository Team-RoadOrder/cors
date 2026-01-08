package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.CartEntity;
import dev.gmpark.cors.mappers.CartMapper;
import dev.gmpark.cors.vos.CartVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional
    public boolean deleteCartItems(List<Long> ids) {
        int deletedCount = 0;
        for (Long id : ids) {
            deletedCount += this.cartMapper.deleteCartItem(id);
        }
        return deletedCount > 0;
    }

    public List<CartVo> getCartItemsByIds(List<Long> ids) {
        return this.cartMapper.selectCartItemsByIds(ids);
    }
}
