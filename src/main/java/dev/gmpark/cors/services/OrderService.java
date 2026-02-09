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

import java.util.*;
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
    private static final double POINT_EARN_RATE = 0.01; // 적립률 1%로 통일

    /**
     * 주문 데이터 정규화 (Normalization)
     */
    private void normalizeOrderData(RegisterEntity user, SingleOrderDto dto) {
        if (dto.getReceiverName() == null || dto.getReceiverName().isBlank()) {
            dto.setReceiverName(user.getName());
        } else {
            dto.setReceiverName(dto.getReceiverName().trim());
        }

        if (dto.getReceiverPhone() == null || dto.getReceiverPhone().isBlank()) {
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

        if (dto.getRequest() != null) {
            dto.setRequest(dto.getRequest().trim());
        }

        dto.setQuantity(this.normalizeQuantity(dto.getQuantity()));
    }

    private int normalizeQuantity(int quantity) {
        if (quantity < 1) return 1;
        if (quantity > 99) return 99;
        return quantity;
    }

    @Transactional
    public CommonResult processSingleOrder(RegisterEntity user, SingleOrderDto dto) {
        if (user == null || dto == null || dto.getItemId() == null || dto.getSize() == null) {
            return CommonResult.FAILURE;
        }

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
                .quantity(dto.getQuantity())
                .price(item.getPrice())
                .build());

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
        if (user == null || dto == null || dto.getCartIds() == null || dto.getCartIds().isEmpty()) {
            return CommonResult.FAILURE;
        }

        this.normalizeOrderData(user, dto);

        List<CartVo> cartItems = this.cartService.getCartItemsByIds(dto.getCartIds());
        if (cartItems.isEmpty()) {
            return CommonResult.FAILURE;
        }

        long totalProductPrice = 0;
        List<OrderItemEntity> orderItems = new ArrayList<>();
        Map<Long, Integer> cartQuantities = dto.getCartQuantities();

        for (CartVo cart : cartItems) {
            // [추가] 결제 처리 시에도 본인 장바구니인지 재검증 (보안 강화)
            if (!cart.getUserEmail().equals(user.getEmail())) {
                return CommonResult.FAILURE;
            }

            int quantity = cart.getQuantity();
            if (cartQuantities != null && cartQuantities.containsKey(cart.getId())) {
                quantity = cartQuantities.get(cart.getId());
            }
            quantity = this.normalizeQuantity(quantity);

            totalProductPrice += cart.getItemPrice() * quantity;
            orderItems.add(OrderItemEntity.builder()
                    .itemId(cart.getItemId())
                    .shopId(cart.getShopId())
                    .size(cart.getSize())
                    .quantity(quantity)
                    .price(cart.getItemPrice())
                    .build());
        }

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

    private CommonResult finalizeOrder(RegisterEntity user, SingleOrderDto dto, long totalProductPrice, List<OrderItemEntity> items) {
        Map<Integer, Long> shopTotalMap = new HashMap<>();
        for (OrderItemEntity item : items) {
            shopTotalMap.put(item.getShopId(), shopTotalMap.getOrDefault(item.getShopId(), 0L) + (item.getPrice() * item.getQuantity()));
        }

        long deliveryFee = 0;
        for (Long shopTotal : shopTotalMap.values()) {
            if (shopTotal < FREE_DELIVERY_THRESHOLD) {
                deliveryFee += DELIVERY_FEE;
            }
        }

        long totalPrice = totalProductPrice + deliveryFee;

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
            if (usedPoints > 0) {
                this.registerMapper.insertPointHistory(PointHistoryEntity.builder()
                        .userEmail(user.getEmail())
                        .amount(-usedPoints)
                        .type("USE")
                        .orderId(String.valueOf(order.getId()))
                        .build());
            }
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
                    .shopId(item.getShopId())
                    .build());
        }
        return items;
    }

    // [수정] userEmail 파라미터 추가 및 본인 확인 로직 적용
    public List<PaymentItemDto> getPaymentItemsForCartOrder(String userEmail, List<Long> cartIds) {
        List<PaymentItemDto> items = new ArrayList<>();
        List<CartVo> cartItems = this.cartService.getCartItemsByIds(cartIds);

        for (CartVo cart : cartItems) {
            // [중요] 내 장바구니가 아니면 예외 발생 -> Controller에서 리다이렉트 처리
            if (!cart.getUserEmail().equals(userEmail)) {
                throw new IllegalArgumentException("잘못된 접근입니다.");
            }

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
                    .shopId(cart.getShopId())
                    .build());
        }
        return items;
    }

    // [수정] userEmail 파라미터 추가
    public Map<String, Object> getPaymentInfo(String userEmail, Long itemId, String size, List<Long> cartIds) {
        List<PaymentItemDto> items = new ArrayList<>();

        if (itemId != null && size != null) {
            items = this.getPaymentItemsForSingleOrder(itemId, size);
        } else if (cartIds != null && !cartIds.isEmpty()) {
            // [수정] userEmail 전달하여 검증 수행
            items = this.getPaymentItemsForCartOrder(userEmail, cartIds);
        }

        long totalProductPrice = 0;
        Map<Integer, Long> shopTotalMap = new HashMap<>();

        for (PaymentItemDto item : items) {
            long itemTotal = item.getPrice() * item.getQuantity();
            totalProductPrice += itemTotal;
            shopTotalMap.put(item.getShopId(), shopTotalMap.getOrDefault(item.getShopId(), 0L) + itemTotal);
        }

        long deliveryFee = 0;
        if (totalProductPrice > 0) {
            for (Long shopTotal : shopTotalMap.values()) {
                if (shopTotal < FREE_DELIVERY_THRESHOLD) {
                    deliveryFee += DELIVERY_FEE;
                }
            }
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

        // [수정] user.getEmail()을 전달
        Map<String, Object> paymentInfo = this.getPaymentInfo(user.getEmail(), itemId, size, cartIds);
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

    public int calculateEarnedPoints(long orderItemId) {
        OrderItemEntity targetItem = this.orderMapper.selectOrderItemById(orderItemId);
        if (targetItem == null) return 0;

        OrderEntity order = this.orderMapper.selectOrderById(targetItem.getOrderId());
        if (order == null) return 0;

        List<OrderItemEntity> allItems = this.orderMapper.selectOrderItemsByOrderId(order.getId());

        long originalTotalOrderPrice = 0;
        Map<Integer, Long> shopTotalMap = new HashMap<>();

        for (OrderItemEntity item : allItems) {
            long p = item.getPrice() * item.getQuantity();
            originalTotalOrderPrice += p;
            shopTotalMap.put(item.getShopId(), shopTotalMap.getOrDefault(item.getShopId(), 0L) + p);
        }

        long originalDeliveryFee = 0;
        for (Long shopTotal : shopTotalMap.values()) {
            if (shopTotal < 70000) originalDeliveryFee += 3000;
        }
        originalTotalOrderPrice += originalDeliveryFee;

        long totalPointsToDeduct = originalTotalOrderPrice - order.getTotalPrice();


        List<OrderItemEntity> validItems = allItems.stream()
                .filter(item -> item.getStatus() != 3 && item.getStatus() != 5 && item.getStatus() != 7)
                .sorted(Comparator.comparing(OrderItemEntity::getId))
                .collect(Collectors.toList());

        long remainingPoints = totalPointsToDeduct;
        long effectivePrice = 0;

        for (OrderItemEntity item : validItems) {
            long itemOriginalPrice = item.getPrice() * item.getQuantity();
            long deductAmount = 0;

            if (remainingPoints > 0) {
                deductAmount = Math.min(itemOriginalPrice, remainingPoints);
                remainingPoints -= deductAmount;
            }

            if (item.getId().equals(orderItemId)) {
                effectivePrice = itemOriginalPrice - deductAmount;
                break;
            }
        }

        if (effectivePrice < 0) effectivePrice = 0;

        return (int) (effectivePrice * 0.01);
    }

    private boolean processRefund(long orderItemId, String manualReason) {
        try {
            OrderItemEntity orderItem = this.orderMapper.selectOrderItemById(orderItemId);
            if (orderItem == null) return false;

            OrderEntity order = this.orderMapper.selectOrderById(orderItem.getOrderId());
            if (order == null) return false;

            if (order.getPaymentKey() != null && !order.getPaymentKey().isBlank()) {
                long cancelAmount = orderItem.getPrice() * orderItem.getQuantity();

                // [수정] 배송비 환불 로직 수정
                // 1. 주문 취소(status 3)인 경우: 배송 전이므로 배송비 환불 가능성 체크
                // 2. 환불 완료(status 3)인데 이전 상태가 환불 요청(2)인 경우: 배송 후 환불이므로 배송비 환불 X
                // 하지만 현재 메서드에서는 이전 상태를 알 수 없음.
                // 따라서, 현재 아이템의 상태가 이미 '배송중(1)' 또는 '배송완료(6)' 등을 거쳤는지 확인하거나,
                // 단순히 '환불 요청(2)' 상태에서 넘어온 것인지를 판단해야 함.
                // updateOrderItemAndRefundReason 메서드에서 status 3으로 변경 시 호출되므로,
                // DB상의 현재 상태(변경 전 상태)를 확인하면 됨.
                
                // orderItem은 DB에서 조회한 상태이므로 변경 전 상태임.
                boolean isReturnRequest = (orderItem.getStatus() == 2 || orderItem.getStatus() == 7); // 환불 요청 or 환불 거절 상태
                
                // 주문 취소(배송 전)일 때만 배송비 환불 로직 수행
                if (!isReturnRequest) {
                    List<OrderItemEntity> allItems = this.orderMapper.selectOrderItemsByOrderId(order.getId());

                    long shopTotalPrice = 0;
                    int activeShopItemCount = 0;
                    int targetShopId = orderItem.getShopId();

                    for (OrderItemEntity item : allItems) {
                        if (item.getShopId() == targetShopId) {
                            shopTotalPrice += item.getPrice() * item.getQuantity();
                            // 현재 취소하려는 아이템을 제외하고, 취소되지 않은(status != 3) 아이템이 있는지 확인
                            if (item.getId() != null && !item.getId().equals(orderItemId) && item.getStatus() != 3) {
                                activeShopItemCount++;
                            }
                        }
                    }

                    // 해당 샵의 모든 아이템이 취소되는 경우에만 배송비 환불
                    if (activeShopItemCount == 0) {
                        long deliveryFee = (shopTotalPrice >= FREE_DELIVERY_THRESHOLD || shopTotalPrice == 0) ? 0 : DELIVERY_FEE;
                        cancelAmount += deliveryFee;
                    }
                }

                JsonNode paymentInfo = this.tossApiService.getPayment(order.getPaymentKey());
                long balanceAmount = paymentInfo.path("balanceAmount").asLong();

                long refundAmount = 0;

                if (balanceAmount > 0) {
                    // [수정] 환불 로직 개선: 기본적으로 현금 우선 환불, 마지막 상품일 때 포인트 우선 환불
                    
                    // 1. 남은 아이템 확인 (마지막 상품인지 체크)
                    List<OrderItemEntity> allItems = this.orderMapper.selectOrderItemsByOrderId(order.getId());
                    boolean isLastItem = true;
                    for (OrderItemEntity item : allItems) {
                        // 현재 환불하려는 아이템이 아니고, 아직 환불되지 않은(status != 3) 아이템이 있다면 마지막이 아님
                        if (item.getId() != null && !item.getId().equals(orderItemId) && item.getStatus() != 3) {
                            isLastItem = false;
                            break;
                        }
                    }
                    
                    long refundPoint = 0;
                    long refundCash = 0;
                    
                    // 취소 사유 정의
                    String cancelReason = manualReason != null ? manualReason :
                            (orderItem.getRefundReason() != null ? orderItem.getRefundReason() : "관리자 취소");

                    if (isLastItem) {
                        // [마지막 상품] 포인트 우선 환불 (남은 포인트 전액 환불)
                        
                        // 주문 당시 총 금액 및 사용 포인트 계산
                        long originalTotalOrderPrice = 0;
                        Map<Integer, Long> shopTotalMap = new HashMap<>();
                        for (OrderItemEntity item : allItems) {
                            long p = item.getPrice() * item.getQuantity();
                            originalTotalOrderPrice += p;
                            shopTotalMap.put(item.getShopId(), shopTotalMap.getOrDefault(item.getShopId(), 0L) + p);
                        }
                        long originalDeliveryFee = 0;
                        for (Long shopTotal : shopTotalMap.values()) {
                            if (shopTotal < FREE_DELIVERY_THRESHOLD) originalDeliveryFee += DELIVERY_FEE;
                        }
                        originalTotalOrderPrice += originalDeliveryFee;
                        
                        long totalUsedPoints = originalTotalOrderPrice - order.getTotalPrice();
                        
                        // 잔여 사용 포인트 계산
                        int refundedPoints = this.registerMapper.selectTotalRefundedPointsByOrderId(String.valueOf(order.getId()));
                        long remainingUsedPoints = totalUsedPoints - refundedPoints;
                        
                        if (remainingUsedPoints < 0) remainingUsedPoints = 0;
                        
                        // 포인트 우선 환불
                        if (remainingUsedPoints > 0) {
                            if (cancelAmount >= remainingUsedPoints) {
                                refundPoint = remainingUsedPoints;
                                refundCash = cancelAmount - remainingUsedPoints;
                            } else {
                                refundPoint = cancelAmount;
                                refundCash = 0;
                            }
                        } else {
                            refundPoint = 0;
                            refundCash = cancelAmount;
                        }
                        
                    } else {
                        // [부분 환불] 현금 우선 환불
                        if (cancelAmount <= balanceAmount) {
                            refundCash = cancelAmount;
                            refundPoint = 0;
                        } else {
                            refundCash = balanceAmount;
                            refundPoint = cancelAmount - balanceAmount;
                        }
                    }
                    
                    // 토스 환불 (현금 부분)
                    if (refundCash > 0) {
                        if (refundCash > balanceAmount) {
                            refundCash = balanceAmount; // 예외 상황 방지
                        }
                        this.tossApiService.cancelPayment(order.getPaymentKey(), cancelReason, refundCash);
                    }
                    
                    // 포인트 환불 (포인트 부분)
                    if (refundPoint > 0) {
                        this.registerMapper.updatePoint(order.getUserEmail(), (int) refundPoint);
                        this.registerMapper.insertPointHistory(PointHistoryEntity.builder()
                                .userEmail(order.getUserEmail())
                                .amount((int) refundPoint)
                                .type("REFUND")
                                .orderId(String.valueOf(order.getId()))
                                .build());
                    }
                }

                // 포인트 회수 로직 (구매 확정 시 적립된 포인트가 있다면 회수)
                int earnedHistoryCount = this.registerMapper.countEarnedPointHistory(order.getUserEmail(), String.valueOf(order.getId()));
                
                // [수정] 해당 아이템이 구매 확정 상태(4)였을 때만 포인트 회수
                if (earnedHistoryCount > 0 && orderItem.getStatus() == 4) {
                    int earnedPointsToRevoke = (int) (cancelAmount * POINT_EARN_RATE); // 상품 가격 기준
                    
                    int maxRevokePoints = (int) (cancelAmount * 0.01);
                    if (earnedPointsToRevoke > maxRevokePoints) {
                        earnedPointsToRevoke = maxRevokePoints;
                    }

                    if (earnedPointsToRevoke > 0) {
                        this.registerMapper.updatePoint(order.getUserEmail(), -earnedPointsToRevoke);
                        this.registerMapper.insertPointHistory(PointHistoryEntity.builder()
                                .userEmail(order.getUserEmail())
                                .amount(-earnedPointsToRevoke)
                                .type("REVOKE")
                                .orderId(String.valueOf(order.getId()))
                                .build());
                    }
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
            if (!this.processRefund(id, null)) {
                return CommonResult.FAILURE;
            }
        }

        return this.orderMapper.updateOrderItemStatus(id, status) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }

    public CommonResult updateOrderItemAndRefundReason(long id, int status, String refundReason) {
        if( id < 1 ||
                (status != 2 && status != 3 && status != 0 && status != 5 && status != 7) ||
                refundReason == null ||
                refundReason.isEmpty() ||
                refundReason.length() > 200) {

            return CommonResult.FAILURE;
        }

        if (status == 3) {
            if (!this.processRefund(id, refundReason)) {
                return CommonResult.FAILURE;
            }
        }

        return this.orderMapper.updateOrderItemStatusAndRefundReason(id, status, refundReason) > 0
                ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }
    private static final Set<String> ALLOWED_COURIERS = Set.of(
            "CJ대한통운",
            "우체국택배",
            "한진택배",
            "롯데택배",
            "로젠택배",
            "CU편의점택배",
            "GSPostbox택배"
    );
    public CommonResult updateDelivery(long id, String courier, String trackingNumber, int ownerShopId ) {

        if (id < 1 || courier == null || courier.isEmpty() || trackingNumber == null || trackingNumber.isEmpty()) {
            return CommonResult.FAILURE;
        }
        if (!ALLOWED_COURIERS.contains(courier)) {
            return CommonResult.FAILURE;
        }
        if(trackingNumber.length() < 10 || trackingNumber.length() > 14) {
            return CommonResult.FAILURE;
        }
        OrderItemEntity orderItem = this.orderMapper.selectOrderItemById(id);
        if (orderItem == null) {
            return CommonResult.FAILURE;
        }
        if (orderItem.getShopId() != ownerShopId) {
            // 내 가게 물건이 아니면 접근 거부
            return CommonResult.NO_AUTH;
        }
        int update = this.orderMapper.updateDelivery(id, courier, trackingNumber, 6);
        return update > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }
}