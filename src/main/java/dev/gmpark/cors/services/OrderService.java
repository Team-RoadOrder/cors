package dev.gmpark.cors.services;

import dev.gmpark.cors.dtos.PaymentItemDto;
import dev.gmpark.cors.dtos.SingleOrderDto;
import dev.gmpark.cors.entities.OrderEntity;
import dev.gmpark.cors.entities.OrderItemEntity;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.OrderMapper;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.vos.CartVo;
import dev.gmpark.cors.vos.ShopItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderMapper orderMapper;
    private final ItemService itemService;
    private final CartService cartService;

    private static final long FREE_DELIVERY_THRESHOLD = 70000;
    private static final long DELIVERY_FEE = 3000;

    @Transactional
    public CommonResult createOrder(OrderEntity order, List<OrderItemEntity> items) {
        if (this.orderMapper.insertOrder(order) == 0) {
            return CommonResult.FAILURE;
        }
        
        for (OrderItemEntity item : items) {
            item.setOrderId(order.getId());
        }
        
        return this.orderMapper.insertOrderItems(items) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }

    @Transactional
    public CommonResult processSingleOrder(RegisterEntity user, SingleOrderDto dto) {
        ShopItemVo item = this.itemService.getItemById(dto.getItemId());
        if (item == null) {
            return CommonResult.FAILURE;
        }

        int quantity = dto.getQuantity() > 0 ? dto.getQuantity() : 1; // 수량 확인 (기본 1)
        long totalProductPrice = item.getPrice() * quantity; // 총 상품 금액 계산
        
        List<OrderItemEntity> items = new ArrayList<>();
        items.add(OrderItemEntity.builder()
                .itemId(dto.getItemId())
                .shopId(item.getShopId())
                .size(dto.getSize())
                .quantity(quantity) // DTO에서 받은 수량 사용
                .price(item.getPrice())
                .build());

        // 새로 추가된 아이템 처리
        if (dto.getNewItems() != null) {
            for (Map<String, Object> newItem : dto.getNewItems()) {
                Long newItemId = ((Number) newItem.get("itemId")).longValue();
                String newSize = (String) newItem.get("size");
                int newQuantity = ((Number) newItem.get("quantity")).intValue();
                
                ShopItemVo newItemVo = this.itemService.getItemById(newItemId);
                if (newItemVo != null) {
                    totalProductPrice += newItemVo.getPrice() * newQuantity;
                    items.add(OrderItemEntity.builder()
                            .itemId(newItemId)
                            .shopId(newItemVo.getShopId())
                            .size(newSize)
                            .quantity(newQuantity)
                            .price(newItemVo.getPrice())
                            .build());
                }
            }
        }

        long deliveryFee = totalProductPrice >= FREE_DELIVERY_THRESHOLD ? 0 : DELIVERY_FEE;
        long totalPrice = totalProductPrice + deliveryFee;

        OrderEntity order = OrderEntity.builder()
                .userEmail(user.getEmail())
                .totalPrice(totalPrice)
                .status("PAID")
                .receiverName(dto.getReceiverName() != null ? dto.getReceiverName() : user.getName())
                .receiverPhone(dto.getReceiverPhone() != null ? dto.getReceiverPhone() : user.getPhone())
                .address(dto.getAddress() != null ? dto.getAddress() : user.getAddress())
                .addressDetail(dto.getAddressDetail() != null ? dto.getAddressDetail() : user.getAddressDetail())
                .request(dto.getRequest())
                .build();

        return this.createOrder(order, items);
    }

    @Transactional
    public CommonResult processCartOrder(RegisterEntity user, SingleOrderDto dto) {
        List<CartVo> cartItems = this.cartService.getCartItemsByIds(dto.getCartIds());
        if (cartItems.isEmpty()) {
            return CommonResult.FAILURE;
        }

        long totalProductPrice = 0;
        List<OrderItemEntity> orderItems = new ArrayList<>();
        Map<Long, Integer> cartQuantities = dto.getCartQuantities();

        for (CartVo cart : cartItems) {
            int quantity = cart.getQuantity();
            // 결제창에서 변경된 수량이 있으면 적용
            if (cartQuantities != null && cartQuantities.containsKey(cart.getId())) {
                quantity = cartQuantities.get(cart.getId());
            }
            
            totalProductPrice += cart.getItemPrice() * quantity;
            orderItems.add(OrderItemEntity.builder()
                    .itemId(cart.getItemId())
                    .shopId(cart.getShopId())
                    .size(cart.getSize())
                    .quantity(quantity)
                    .price(cart.getItemPrice())
                    .build());
        }

        // 새로 추가된 아이템 처리
        if (dto.getNewItems() != null) {
            for (Map<String, Object> newItem : dto.getNewItems()) {
                Long newItemId = ((Number) newItem.get("itemId")).longValue();
                String newSize = (String) newItem.get("size");
                int newQuantity = ((Number) newItem.get("quantity")).intValue();
                
                ShopItemVo newItemVo = this.itemService.getItemById(newItemId);
                if (newItemVo != null) {
                    totalProductPrice += newItemVo.getPrice() * newQuantity;
                    orderItems.add(OrderItemEntity.builder()
                            .itemId(newItemId)
                            .shopId(newItemVo.getShopId())
                            .size(newSize)
                            .quantity(newQuantity)
                            .price(newItemVo.getPrice())
                            .build());
                }
            }
        }

        long deliveryFee = (totalProductPrice >= FREE_DELIVERY_THRESHOLD) ? 0 : DELIVERY_FEE;
        if (totalProductPrice == 0) deliveryFee = 0; // 상품이 없으면 배송비도 0

        long totalPrice = totalProductPrice + deliveryFee;

        OrderEntity order = OrderEntity.builder()
                .userEmail(user.getEmail())
                .totalPrice(totalPrice)
                .status("PAID")
                .receiverName(dto.getReceiverName() != null ? dto.getReceiverName() : user.getName())
                .receiverPhone(dto.getReceiverPhone() != null ? dto.getReceiverPhone() : user.getPhone())
                .address(dto.getAddress() != null ? dto.getAddress() : user.getAddress())
                .addressDetail(dto.getAddressDetail() != null ? dto.getAddressDetail() : user.getAddressDetail())
                .request(dto.getRequest())
                .build();

        CommonResult orderResult = this.createOrder(order, orderItems);
        
        if (orderResult == CommonResult.SUCCESS) {
            // 수정된 부분: deleteCartItems 호출 시 user 파라미터 추가
            this.cartService.deleteCartItems(user, dto.getCartIds());
        }
        
        return orderResult;
    }

    public List<PaymentItemDto> getPaymentItemsForSingleOrder(Long itemId, String size) {
        List<PaymentItemDto> items = new ArrayList<>();
        ShopItemVo item = this.itemService.getItemById(itemId);
        if (item != null) {
            List<String> sizes = new ArrayList<>();
            if (item.getSize() != null) {
                sizes = Arrays.stream(item.getSize().split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
            }

            items.add(PaymentItemDto.builder()
                    .itemId(item.getId())
                    .itemName(item.getItemName())
                    .color(item.getColor())
                    .size(size)
                    .quantity(1)
                    .price(item.getPrice())
                    .imagePath(item.getImages().isEmpty() ? null : item.getImages().get(0).getImagePath())
                    .availableSizes(sizes)
                    .build());
        }
        return items;
    }

    public List<PaymentItemDto> getPaymentItemsForCartOrder(List<Long> cartIds) {
        List<PaymentItemDto> items = new ArrayList<>();
        List<CartVo> cartItems = this.cartService.getCartItemsByIds(cartIds);
        for (CartVo cart : cartItems) {
            // 장바구니 아이템의 경우 원본 상품 정보를 조회해서 가능한 사이즈 목록을 가져와야 함
            // CartVo에는 원본 상품의 전체 사이즈 정보가 없을 수 있음 (보통 선택된 사이즈만 저장됨)
            // 따라서 itemId로 다시 조회하거나 CartVo에 해당 정보가 있어야 함.
            // 여기서는 itemId로 다시 조회하는 방식을 사용 (성능상 이슈가 있을 수 있으나 정확성을 위해)
            
            List<String> sizes = new ArrayList<>();
            ShopItemVo originalItem = this.itemService.getItemById(cart.getItemId());
            if (originalItem != null && originalItem.getSize() != null) {
                sizes = Arrays.stream(originalItem.getSize().split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
            }

            items.add(PaymentItemDto.builder()
                    .itemId(cart.getItemId())
                    .itemName(cart.getItemName())
                    .color(cart.getItemColor())
                    .size(cart.getSize())
                    .quantity(cart.getQuantity())
                    .price(cart.getItemPrice())
                    .imagePath(cart.getItemImage())
                    .cartId(cart.getId())
                    .availableSizes(sizes)
                    .build());
        }
        return items;
    }

    public CommonResult updateOrderItem(long id, int status) {
        if (id < 1) {
            return CommonResult.FAILURE;
        }
        if (status == 0) {
            return CommonResult.FAILURE;
        }

        // Mapper 호출
        return this.orderMapper.updateOrderItemStatus(id, status) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }
}
