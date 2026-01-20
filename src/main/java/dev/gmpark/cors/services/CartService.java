package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.CartEntity;
import dev.gmpark.cors.mappers.CartMapper;
import dev.gmpark.cors.vos.CartVo;
import dev.gmpark.cors.vos.ShopItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartMapper cartMapper;
    private final ItemService itemService;

    public Long addCart(String userEmail, Long itemId, String size, int quantity) {
        CartEntity existingCart = this.cartMapper.selectCartItem(userEmail, itemId, size);

        if (existingCart != null) {
            if (this.cartMapper.updateCartQuantity(existingCart.getId(), quantity) > 0) {
                return existingCart.getId();
            }
        } else {
            CartEntity cart = CartEntity.builder()
                    .userEmail(userEmail)
                    .itemId(itemId)
                    .size(size)
                    .quantity(quantity)
                    .build();
            if (this.cartMapper.insertCart(cart) > 0) {
                return cart.getId();
            }
        }
        return -1L;
    }

    public CartVo[] getCartList(String userEmail) {
        CartVo[] carts = this.cartMapper.selectCartByUserEmail(userEmail);
        
        for (CartVo cart : carts) {
            ShopItemVo item = this.itemService.getItemById(cart.getItemId());
            if (item != null && item.getSize() != null) {
                List<String> sizes = Arrays.stream(item.getSize().split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
                cart.setAvailableSizes(sizes);
            }
        }
        
        return carts;
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
    
    public int getCartCount(String userEmail) {
        return this.cartMapper.selectCartCount(userEmail);
    }
}
