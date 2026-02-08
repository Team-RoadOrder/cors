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
        long deliveryFee = (totalProductPrice >= FREE_DELIVERY_THRESHOLD || totalProductPrice == 0) ? 0 : DELIVERY_FEE;
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

    private boolean processRefund(long orderItemId, String manualReason) {
        try {
            OrderItemEntity orderItem = this.orderMapper.selectOrderItemById(orderItemId);
            if (orderItem == null) return false;

            OrderEntity order = this.orderMapper.selectOrderById(orderItem.getOrderId());
            if (order == null) return false;

            if (order.getPaymentKey() != null && !order.getPaymentKey().isBlank()) {
                long cancelAmount = orderItem.getPrice() * orderItem.getQuantity();

                JsonNode paymentInfo = this.tossApiService.getPayment(order.getPaymentKey());
                long balanceAmount = paymentInfo.path("balanceAmount").asLong();

                long refundAmount = 0;

                if (balanceAmount > 0) {
                    refundAmount = cancelAmount;
                    if (cancelAmount > balanceAmount) {
                        refundAmount = balanceAmount;
                    }

                    String cancelReason = manualReason != null ? manualReason :
                            (orderItem.getRefundReason() != null ? orderItem.getRefundReason() : "관리자 취소");

                    if (refundAmount > 0) {
                        this.tossApiService.cancelPayment(order.getPaymentKey(), cancelReason, refundAmount);
                    }
                }

                long pointRefundAmount = cancelAmount - refundAmount;
                if (pointRefundAmount > 0) {
                    this.registerMapper.updatePoint(order.getUserEmail(), (int) pointRefundAmount);
                    this.registerMapper.insertPointHistory(PointHistoryEntity.builder()
                            .userEmail(order.getUserEmail())
                            .amount((int) pointRefundAmount)
                            .type("REFUND")
                            .orderId(String.valueOf(order.getId()))
                            .build());
                }

                // [수정] 포인트 회수 로직 (최대 1% 제한 적용 및 구매확정 여부 확인)
                // 구매 확정 시에만 포인트가 적립되므로, 적립된 내역이 있는지 확인 후 회수해야 함
                int earnedHistoryCount = this.registerMapper.countEarnedPointHistory(order.getUserEmail(), String.valueOf(order.getId()));
                
                if (earnedHistoryCount > 0) {
                    int earnedPointsToRevoke = (int) (refundAmount * POINT_EARN_RATE);
                    
                    // 유효성 검사: 회수할 포인트가 환불 금액의 1%를 초과하지 않도록 제한
                    int maxRevokePoints = (int) (refundAmount * 0.01);
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
        // [수정됨] status 검사에 5와 7을 추가했습니다.
        if( id < 1 ||
                (status != 2 && status != 3 && status != 0 && status != 5 && status != 7) ||
                refundReason == null ||
                refundReason.isEmpty() ||
                refundReason.length() > 200) {

            return CommonResult.FAILURE;
        }

        // (기존 로직 유지) 환불 승인(3)일 때만 환불 처리 로직 실행
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