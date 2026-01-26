package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.CartEntity;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.CartMapper;
import dev.gmpark.cors.results.Result;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.vos.CartVo;
import dev.gmpark.cors.vos.ShopItemVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
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

    public Pair<Result, Long> addCart(RegisterEntity sessionUser, Long itemId, String size, int quantity) {
        if (sessionUser == null) {
            return Pair.of(CommonResult.FAILURE_SESSION, -1L);
        }
        if (itemId == null || itemId < 1 || size == null || size.isEmpty() || quantity < 1) {
            return Pair.of(CommonResult.FAILURE, -1L);
        }
        
        // 수량 제한 (최대 99개)
        if (quantity > 99) {
            return Pair.of(CommonResult.FAILURE, -1L);
        }

        // 상품 존재 여부 확인
        ShopItemVo item = this.itemService.getItemById(itemId);
        if (item == null) {
            return Pair.of(CommonResult.FAILURE, -1L);
        }

        // 사이즈 유효성 검사
        if (item.getSize() != null) {
            List<String> availableSizes = Arrays.stream(item.getSize().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
            if (!availableSizes.contains(size)) {
                return Pair.of(CommonResult.FAILURE, -1L);
            }
        }

        CartEntity existingCart = this.cartMapper.selectCartItem(sessionUser.getEmail(), itemId, size);

        if (existingCart != null) {
            // 기존 수량 + 추가 수량이 99개를 넘지 않도록 체크
            if (existingCart.getQuantity() + quantity > 99) {
                return Pair.of(CommonResult.FAILURE, -1L);
            }
            if (this.cartMapper.updateCartQuantity(existingCart.getId(), quantity) > 0) {
                return Pair.of(CommonResult.SUCCESS, existingCart.getId());
            }
        } else {
            CartEntity cart = CartEntity.builder()
                    .userEmail(sessionUser.getEmail())
                    .itemId(itemId)
                    .size(size)
                    .quantity(quantity)
                    .build();
            if (this.cartMapper.insertCart(cart) > 0) {
                return Pair.of(CommonResult.SUCCESS, cart.getId());
            }
        }
        return Pair.of(CommonResult.FAILURE, -1L);
    }

    public CartVo[] getCartList(RegisterEntity sessionUser) {
        if (sessionUser == null) {
            return new CartVo[0];
        }
        CartVo[] carts = this.cartMapper.selectCartByUserEmail(sessionUser.getEmail());
        
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

    @Transactional
    public Result deleteCartItems(RegisterEntity sessionUser, List<Long> ids) {
        if (sessionUser == null) {
            return CommonResult.FAILURE_SESSION;
        }
        if (ids == null || ids.isEmpty()) {
            return CommonResult.FAILURE;
        }

        // 본인의 장바구니 아이템인지 확인
        List<CartVo> cartItems = this.cartMapper.selectCartItemsByIds(ids);
        for (CartVo cart : cartItems) {
            if (!cart.getUserEmail().equals(sessionUser.getEmail())) {
                return CommonResult.FAILURE; // 권한 없음
            }
        }

        int deletedCount = 0;
        for (Long id : ids) {
            deletedCount += this.cartMapper.deleteCartItem(id);
        }
        return deletedCount > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }

    public List<CartVo> getCartItemsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return this.cartMapper.selectCartItemsByIds(ids);
    }
    
    public int getCartCount(RegisterEntity sessionUser) {
        if (sessionUser == null) {
            return 0;
        }
        return this.cartMapper.selectCartCount(sessionUser.getEmail());
    }

    public Result updateCartQuantity(RegisterEntity sessionUser, Long cartId, int quantity) {
        if (sessionUser == null) {
            return CommonResult.FAILURE_SESSION;
        }
        if (cartId == null || quantity < 1) {
            return CommonResult.FAILURE;
        }
        
        // 수량 제한 (최대 99개)
        if (quantity > 99) {
            return CommonResult.FAILURE;
        }

        // 본인의 장바구니 아이템인지 확인
        List<CartVo> cartItems = this.cartMapper.selectCartItemsByIds(Arrays.asList(cartId));
        if (cartItems.isEmpty() || !cartItems.get(0).getUserEmail().equals(sessionUser.getEmail())) {
            return CommonResult.FAILURE;
        }

        if (this.cartMapper.updateCartQuantityById(cartId, quantity) > 0) {
            return CommonResult.SUCCESS;
        }
        return CommonResult.FAILURE;
    }
}
