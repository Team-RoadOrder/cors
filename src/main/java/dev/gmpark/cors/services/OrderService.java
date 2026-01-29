package dev.gmpark.cors.services;

import com.fasterxml.jackson.databind.JsonNode;
import dev.gmpark.cors.dtos.PaymentItemDto;
import dev.gmpark.cors.dtos.SingleOrderDto;
import dev.gmpark.cors.entities.OrderEntity;
import dev.gmpark.cors.entities.OrderItemEntity;
import dev.gmpark.cors.entities.PointHistoryEntity;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.OrderMapper;
import dev.gmpark.cors.mappers.RegisterMapper;
import dev.gmpark.cors.results.CommonResult;
import dev.gmpark.cors.vos.CartVo;
import dev.gmpark.cors.vos.ShopItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderMapper orderMapper;
    private final ItemService itemService;
    private final CartService cartService;
    private final RegisterMapper registerMapper;
    private final TossApiService tossApiService;

    private static final long FREE_DELIVERY_THRESHOLD = 70000;
    private static final long DELIVERY_FEE = 3000;
    // private static final double POINT_EARN_RATE = 0.02; // Removed: PayService handles point earning

    /**
     * 주문 데이터 정규화 (Normalization)
     * - 입력값이 없으면 유저 정보로 채움
     * - 수량이 범위를 벗어나면 조정 (Clamping)
     * - 문자열 앞뒤 공백 제거 (Trimming)
     */
    private void normalizeOrderData(RegisterEntity user, SingleOrderDto dto) {
        // 1. 수신자 정보가 비어있으면 주문자(User) 정보로 대체 및 공백 제거
        if (dto.getReceiverName() == null || dto.getReceiverName().isBlank()) {
            dto.setReceiverName(user.getName());
        } else {
            dto.setReceiverName(dto.getReceiverName().trim());
        }

        if (dto.getReceiverPhone() == null || dto.getReceiverPhone().isBlank()) {
            // 전화번호에서 숫자만 남기고 저장 (Format Normalization)
            String cleanPhone = user.getPhone().replaceAll("[^0-9]", "");
            dto.setReceiverPhone(cleanPhone);
        } else {
            dto.setReceiverPhone(dto.getReceiverPhone().replaceAll("[^0-9]", ""));
        }

        if (dto.getAddress() == null || dto.getAddress().isBlank()) {
            dto.setAddress(user.getAddress());
            dto.setAddressDetail(user.getAddressDetail());
        } else {
            dto.setAddress(dto.getAddress().trim());
            dto.setAddressDetail(dto.getAddressDetail() != null ? dto.getAddressDetail().trim() : "");
        }

        // 2. 요청사항 공백 제거
        if (dto.getRequest() != null) {
            dto.setRequest(dto.getRequest().trim());
        }

        // 3. 수량 정규화 (1 ~ 99)
        dto.setQuantity(this.normalizeQuantity(dto.getQuantity()));
    }

    /**
     * 수량 범위 보정 (1 ~ 99)
     */
    private int normalizeQuantity(int quantity) {
        if (quantity < 1) return 1;
        if (quantity > 99) return 99;
        return quantity;
    }

    @Transactional
    public CommonResult processSingleOrder(RegisterEntity user, SingleOrderDto dto) {
        // 1. 기본 유효성 검사 (Validation)
        if (user == null || dto == null || dto.getItemId() == null || dto.getSize() == null) {
            return CommonResult.FAILURE;
        }

        // 2. 데이터 정규화 (Normalization) - 로직 수행 전 데이터 정제
        this.normalizeOrderData(user, dto);

        ShopItemVo item = this.itemService.getItemById(dto.getItemId());
        if (item == null) {
            return CommonResult.FAILURE;
        }

        long totalProductPrice = item.getPrice() * dto.getQuantity();
        
        List<OrderItemEntity> items = new ArrayList<>();
        items.add(OrderItemEntity.builder()
                .itemId(dto.getItemId())
                .shopId(item.getShopId())
                .size(dto.getSize())
                .quantity(dto.getQuantity()) // 정규화된 수량 사용
                .price(item.getPrice())
                .build());

        // 추가 아이템 처리 (Option Items)
        if (dto.getNewItems() != null) {
            for (Map<String, Object> newItem : dto.getNewItems()) {
                Long newItemId = ((Number) newItem.get("itemId")).longValue();
                String newSize = (String) newItem.get("size");
                int newQuantity = this.normalizeQuantity(((Number) newItem.get("quantity")).intValue());
                
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

        return this.finalizeOrder(user, dto, totalProductPrice, items);
    }

    @Transactional
    public CommonResult processCartOrder(RegisterEntity user, SingleOrderDto dto) {
        // 1. 기본 유효성 검사
        if (user == null || dto == null || dto.getCartIds() == null || dto.getCartIds().isEmpty()) {
            return CommonResult.FAILURE;
        }

        // 2. 데이터 정규화
        this.normalizeOrderData(user, dto);

        List<CartVo> cartItems = this.cartService.getCartItemsByIds(dto.getCartIds());
        if (cartItems.isEmpty()) {
            return CommonResult.FAILURE;
        }

        long totalProductPrice = 0;
        List<OrderItemEntity> orderItems = new ArrayList<>();
        Map<Long, Integer> cartQuantities = dto.getCartQuantities();

        for (CartVo cart : cartItems) {
            int quantity = cart.getQuantity();
            if (cartQuantities != null && cartQuantities.containsKey(cart.getId())) {
                quantity = cartQuantities.get(cart.getId());
            }
            quantity = this.normalizeQuantity(quantity); // 수량 정규화

            totalProductPrice += cart.getItemPrice() * quantity;
            orderItems.add(OrderItemEntity.builder()
                    .itemId(cart.getItemId())
                    .shopId(cart.getShopId())
                    .size(cart.getSize())
                    .quantity(quantity)
                    .price(cart.getItemPrice())
                    .build());
        }

        // 추가 아이템 처리 (위와 동일 로직)
        if (dto.getNewItems() != null) {
            for (Map<String, Object> newItem : dto.getNewItems()) {
                Long newItemId = ((Number) newItem.get("itemId")).longValue();
                String newSize = (String) newItem.get("size");
                int newQuantity = this.normalizeQuantity(((Number) newItem.get("quantity")).intValue());

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

        CommonResult result = this.finalizeOrder(user, dto, totalProductPrice, orderItems);

        if (result == CommonResult.SUCCESS) {
            this.cartService.deleteCartItems(user, dto.getCartIds());
        }
        
        return result;
    }

    // 공통 주문 처리 로직 (결제, 포인트, DB저장)
    private CommonResult finalizeOrder(RegisterEntity user, SingleOrderDto dto, long totalProductPrice, List<OrderItemEntity> items) {
        long deliveryFee = (totalProductPrice >= FREE_DELIVERY_THRESHOLD || totalProductPrice == 0) ? 0 : DELIVERY_FEE;
        long totalPrice = totalProductPrice + deliveryFee;

        // 포인트 사용 검증
        int usedPoints = dto.getUsedPoints();
        if (usedPoints > 0) {
            if (user.getPoint() < usedPoints) return CommonResult.FAILURE;
            if (usedPoints > totalPrice) return CommonResult.FAILURE;
            
            totalPrice -= usedPoints;
            this.registerMapper.updatePoint(user.getEmail(), -usedPoints);
        }

        OrderEntity order = OrderEntity.builder()
                .userEmail(user.getEmail())
                .totalPrice(totalPrice)
                .status("PAID")
                .receiverName(dto.getReceiverName())
                .receiverPhone(dto.getReceiverPhone())
                .address(dto.getAddress())
                .addressDetail(dto.getAddressDetail())
                .request(dto.getRequest())
                .build();

        if (this.orderMapper.insertOrder(order) == 0) {
            return CommonResult.FAILURE;
        }

        for (OrderItemEntity item : items) {
            item.setOrderId(order.getId());
        }

        if (this.orderMapper.insertOrderItems(items) > 0) {
            // 포인트 이력 저장 (사용)
            if (usedPoints > 0) {
                this.registerMapper.insertPointHistory(PointHistoryEntity.builder()
                        .userEmail(user.getEmail())
                        .amount(-usedPoints)
                        .type("USE")
                        .orderId(String.valueOf(order.getId()))
                        .build());
            }

            // 포인트 적립 로직 제거 (PayService에서 처리)
            /*
            int earnedPoints = (int) (totalPrice * POINT_EARN_RATE);
            if (earnedPoints > 0) {
                this.registerMapper.updatePoint(user.getEmail(), earnedPoints);
                this.registerMapper.insertPointHistory(PointHistoryEntity.builder()
                        .userEmail(user.getEmail())
                        .amount(earnedPoints)
                        .type("EARN")
                        .orderId(String.valueOf(order.getId()))
                        .build());
            }
            */
            return CommonResult.SUCCESS;
        }

        return CommonResult.FAILURE;
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

    public Map<String, Object> getPaymentInfo(Long itemId, String size, List<Long> cartIds) {
        List<PaymentItemDto> items = new ArrayList<>();

        if (itemId != null && size != null) {
            items = this.getPaymentItemsForSingleOrder(itemId, size);
        } else if (cartIds != null && !cartIds.isEmpty()) {
            items = this.getPaymentItemsForCartOrder(cartIds);
        }

        long totalProductPrice = 0;
        for (PaymentItemDto item : items) {
            totalProductPrice += item.getPrice() * item.getQuantity();
        }

        long deliveryFee = 0;
        if (totalProductPrice > 0) {
            deliveryFee = (totalProductPrice >= FREE_DELIVERY_THRESHOLD)
                    ? 0
                    : DELIVERY_FEE;
        }

        long totalPrice = totalProductPrice + deliveryFee;

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("totalProductPrice", totalProductPrice);
        result.put("deliveryFee", deliveryFee);
        result.put("totalPrice", totalPrice);
        
        return result;
    }

    public Map<String, Object> getPaymentPageData(RegisterEntity sessionUser, Long itemId, String size, List<Long> cartIds) {
        Map<String, Object> result = new HashMap<>();
        
        RegisterEntity user = this.registerMapper.selectByEmail(sessionUser.getEmail());
        if (user == null) {
            user = sessionUser;
        }
        
        Map<String, Object> paymentInfo = this.getPaymentInfo(itemId, size, cartIds);
        result.putAll(paymentInfo);
        result.put("user", user);
        result.put("isCartOrder", (cartIds != null && !cartIds.isEmpty()));
        result.put("cartIds", cartIds);
        
        return result;
    }

    public Map<String, Object> processOrder(String userEmail, SingleOrderDto dto) {
        Map<String, Object> response = new HashMap<>();
        
        if (userEmail == null || dto == null) {
            response.put("result", CommonResult.FAILURE.name());
            response.put("message", "잘못된 요청입니다.");
            return response;
        }

        RegisterEntity user = this.registerMapper.selectByEmail(userEmail);
        if (user == null) {
            response.put("result", CommonResult.FAILURE.name());
            response.put("message", "사용자 정보를 찾을 수 없습니다.");
            return response;
        }

        try {
            CommonResult result;
            if (dto.getCartIds() != null && !dto.getCartIds().isEmpty()) {
                result = this.processCartOrder(user, dto);
            } else if (dto.getItemId() != null && dto.getSize() != null) {
                result = this.processSingleOrder(user, dto);
            } else {
                result = CommonResult.FAILURE;
                response.put("message", "주문 정보가 올바르지 않습니다.");
                response.put("result", result.name());
                return response;
            }
            response.put("result", result.name());
        } catch (Exception e) {
            e.printStackTrace();
            response.put("result", CommonResult.FAILURE.name());
            response.put("message", "주문 처리 중 오류가 발생했습니다.");
        }
        return response;
    }

    // 환불 로직 분리
    private boolean processRefund(long orderItemId, String manualReason) {
        try {
            OrderItemEntity orderItem = this.orderMapper.selectOrderItemById(orderItemId);
            if (orderItem == null) return false;

            OrderEntity order = this.orderMapper.selectOrderById(orderItem.getOrderId());
            if (order == null) return false;

            // paymentKey가 있는 경우에만 환불 API 호출 (테스트 데이터 등 예외 처리)
            if (order.getPaymentKey() != null && !order.getPaymentKey().isBlank()) {
                long cancelAmount = orderItem.getPrice() * orderItem.getQuantity();
                
                // ★ 환불 가능 금액 조회 (토스 API 호출)
                JsonNode paymentInfo = this.tossApiService.getPayment(order.getPaymentKey());
                long balanceAmount = paymentInfo.path("balanceAmount").asLong(); // 남은 환불 가능 금액

                // ★ [수정] 잔액이 0이어도 포인트 환불은 진행해야 함
                // 기존: if (balanceAmount <= 0) return true; -> 포인트 환불 로직 실행 안됨
                // 수정: 잔액이 0이면 토스 환불은 건너뛰고 포인트 환불 로직으로 바로 이동

                long refundAmount = 0;
                
                if (balanceAmount > 0) {
                    refundAmount = cancelAmount;
                    if (cancelAmount > balanceAmount) {
                        refundAmount = balanceAmount;
                    }

                    String cancelReason = manualReason != null ? manualReason : 
                                         (orderItem.getRefundReason() != null ? orderItem.getRefundReason() : "관리자 취소");
                    
                    // ★ [수정] 토스 API 호출 시 refundAmount가 0보다 클 때만 호출
                    if (refundAmount > 0) {
                        this.tossApiService.cancelPayment(order.getPaymentKey(), cancelReason, refundAmount);
                    }
                }
                
                // ★ 포인트 환불 로직 (사용한 포인트 돌려주기)
                // 토스에서 환불받지 못한 금액(cancelAmount - refundAmount)은 전액 포인트로 환불
                long pointRefundAmount = cancelAmount - refundAmount;
                if (pointRefundAmount > 0) {
                    this.registerMapper.updatePoint(order.getUserEmail(), (int) pointRefundAmount);
                    this.registerMapper.insertPointHistory(PointHistoryEntity.builder()
                            .userEmail(order.getUserEmail())
                            .amount((int) pointRefundAmount)
                            .type("REFUND") // 환불 타입
                            .orderId(String.valueOf(order.getId()))
                            .build());
                }

                // ★ 적립된 포인트 회수 로직
                int earnedPointsToRevoke = (int) (refundAmount * 0.01); 
                if (earnedPointsToRevoke > 0) {
                    this.registerMapper.updatePoint(order.getUserEmail(), -earnedPointsToRevoke);
                    this.registerMapper.insertPointHistory(PointHistoryEntity.builder()
                            .userEmail(order.getUserEmail())
                            .amount(-earnedPointsToRevoke)
                            .type("REVOKE") // 회수 타입
                            .orderId(String.valueOf(order.getId()))
                            .build());
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public CommonResult updateOrderItem(long id, int status) {
        if (id < 1 || status == 0) {
            return CommonResult.FAILURE;
        }

        if (status == 3) {
            // 환불 로직 수행
            if (!this.processRefund(id, null)) {
                return CommonResult.FAILURE;
            }
        }

        return this.orderMapper.updateOrderItemStatus(id, status) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }

    public CommonResult updateOrderItemAndRefundReason(long id, int status, String refundReason) {
        // status가 2(환불요청) 또는 3(환불완료)일 때만 허용
        if( id < 1 || (status != 2 && status != 3) || refundReason == null || refundReason.isEmpty()) {
            return CommonResult.FAILURE;
        }
        
        if (status == 3) {
            // 환불 로직 수행
            if (!this.processRefund(id, refundReason)) {
                return CommonResult.FAILURE;
            }
        }

        return this.orderMapper.updateOrderItemStatusAndRefundReason(id, status, refundReason) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }

}
