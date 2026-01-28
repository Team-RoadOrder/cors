package dev.gmpark.cors.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gmpark.cors.dtos.CartOrderDto;
import dev.gmpark.cors.dtos.PaymentItemDto;
import dev.gmpark.cors.dtos.SingleOrderDto;
import dev.gmpark.cors.entities.CartEntity;
import dev.gmpark.cors.entities.OrderEntity;
import dev.gmpark.cors.entities.OrderItemEntity;
import dev.gmpark.cors.entities.PointHistoryEntity;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.CartMapper;
import dev.gmpark.cors.mappers.OrderMapper;
import dev.gmpark.cors.mappers.RegisterMapper;
import dev.gmpark.cors.results.CommonResult;
import dev.gmpark.cors.results.Result;
import dev.gmpark.cors.validators.PayValidator;
import dev.gmpark.cors.vos.CartVo;
import dev.gmpark.cors.vos.ShopItemVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayService {
    private final OrderService orderService;
    private final RegisterMapper registerMapper;
    private final OrderMapper orderMapper;
    private final CartService cartService;
    private final ItemService itemService;
    private final CartMapper cartMapper;

    // 개발자센터 시크릿 키 (테스트용)
    private final String SECRET_KEY = "test_sk_yZqmkKeP8gNGqeA05AvprbQRxB9l";

    public Map<String, Object> getPaymentInfo(RegisterEntity sessionUser, Long itemId, String size, List<Long> cartIds) {
        if (sessionUser == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> result = new HashMap<>();

        // 최신 유저 정보 조회
        RegisterEntity user = this.registerMapper.selectByEmail(sessionUser.getEmail());
        if (user == null) {
            user = sessionUser;
        }
        result.put("user", user);

        List<PaymentItemDto> items = new ArrayList<>();

        if (itemId != null && size != null) {
            items = this.orderService.getPaymentItemsForSingleOrder(itemId, size);
        } else if (cartIds != null && !cartIds.isEmpty()) {
            items = this.orderService.getPaymentItemsForCartOrder(cartIds);
        }
        result.put("items", items);

        long totalProductPrice = 0;
        for (PaymentItemDto item : items) {
            totalProductPrice += item.getPrice() * item.getQuantity();
        }
        result.put("totalProductPrice", totalProductPrice);

        long deliveryFee = 0;
        if (totalProductPrice > 0) {
            deliveryFee = (totalProductPrice >= 70000) ? 0 : 3000;
        }
        result.put("deliveryFee", deliveryFee);
        result.put("totalPrice", totalProductPrice + deliveryFee);
        result.put("isCartOrder", (cartIds != null && !cartIds.isEmpty()));
        result.put("cartIds", cartIds);

        return result;
    }

    public Pair<Result, String> processPayment(RegisterEntity sessionUser, SingleOrderDto dto) {
        if (sessionUser == null) {
            return Pair.of(CommonResult.FAILURE_SESSION, "로그인이 필요합니다.");
        }
        if (dto == null) {
            return Pair.of(CommonResult.FAILURE, "주문 정보가 없습니다.");
        }

        RegisterEntity user = this.registerMapper.selectByEmail(sessionUser.getEmail());
        if (user == null) {
            return Pair.of(CommonResult.FAILURE, "사용자 정보를 찾을 수 없습니다.");
        }

        // 유효성 검사 (Validator 사용)
        if (!PayValidator.validateReceiverName(dto.getReceiverName())) {
            return Pair.of(CommonResult.FAILURE, "수령인 이름은 한글 또는 영문 2~10자로 입력해주세요.");
        }
        if (!PayValidator.validateReceiverPhone(dto.getReceiverPhone())) {
            return Pair.of(CommonResult.FAILURE, "올바른 연락처 형식이 아닙니다.");
        }
        if (!PayValidator.validateAddress(dto.getAddress())) {
            return Pair.of(CommonResult.FAILURE, "배송지 주소를 입력해주세요.");
        }
        if (!PayValidator.validateAddressDetail(dto.getAddressDetail())) {
            return Pair.of(CommonResult.FAILURE, "상세 주소를 입력해주세요.");
        }
        
        // 포인트 사용량 검증
        if (dto.getUsedPoints() < 0) {
            return Pair.of(CommonResult.FAILURE, "포인트 사용량은 0 이상이어야 합니다.");
        }
        if (!PayValidator.validatePoints(dto.getUsedPoints(), user)) {
            return Pair.of(CommonResult.FAILURE, "보유 포인트보다 많이 사용할 수 없습니다.");
        }

        if (dto.getCartIds() != null && !dto.getCartIds().isEmpty()) {
            return Pair.of(this.orderService.processCartOrder(user, dto), null);
        } else if (dto.getItemId() != null && dto.getSize() != null) {
            return Pair.of(this.orderService.processSingleOrder(user, dto), null);
        } else {
            return Pair.of(CommonResult.FAILURE, "주문 정보가 올바르지 않습니다.");
        }
    }

    /**
     * [1단계] 결제 전 주문 데이터 생성 (가주문)
     * 주문 상태를 'PENDING'(대기) 등으로 저장하고 주문번호를 반환
     */
    @Transactional
    public String prepareOrder(SingleOrderDto dto, String userEmail) {
        // 0. 기존 PENDING 상태의 주문 삭제 (중복 방지 및 정리)
        orderMapper.deletePendingOrdersByUserEmail(userEmail);

        // 1. 유저 정보 조회 및 포인트 검증
        RegisterEntity user = registerMapper.selectByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");
        }
        if (dto.getUsedPoints() > user.getPoint()) {
            throw new RuntimeException("보유 포인트보다 많은 포인트를 사용할 수 없습니다.");
        }
        if (dto.getUsedPoints() < 0) {
            throw new RuntimeException("잘못된 포인트 금액입니다.");
        }

        // 2. 가격 계산 및 주문 아이템 구성 (DB 정보 기반)
        long totalProductPrice = 0;
        List<OrderItemEntity> orderItems = new ArrayList<>();
        
        // 2-1. 장바구니 아이템 처리
        if (dto.getCartIds() != null && !dto.getCartIds().isEmpty()) {
             List<CartVo> cartItems = this.cartService.getCartItemsByIds(dto.getCartIds());
             Map<Long, Integer> cartQuantities = dto.getCartQuantities();
             
             for (CartVo cart : cartItems) {
                // 본인 장바구니 확인
                if (!cart.getUserEmail().equals(userEmail)) {
                    continue; 
                }

                int quantity = cart.getQuantity();
                if (cartQuantities != null && cartQuantities.containsKey(cart.getId())) {
                    quantity = cartQuantities.get(cart.getId());
                }
                if (quantity < 1) quantity = 1;

                // DB에서 조회된 가격(cart.getItemPrice()) 사용
                totalProductPrice += cart.getItemPrice() * quantity;
                
                orderItems.add(OrderItemEntity.builder()
                        .itemId(cart.getItemId())
                        .shopId(cart.getShopId())
                        .size(cart.getSize())
                        .quantity(quantity)
                        .price(cart.getItemPrice())
                        .build());
            }
        } 
        // 2-2. 단일 상품 주문 처리
        else if (dto.getItemId() != null) {
             ShopItemVo item = this.itemService.getItemById(dto.getItemId());
             if (item != null) {
                 int quantity = dto.getQuantity();
                 if (quantity < 1) quantity = 1;

                 totalProductPrice = item.getPrice() * quantity;
                 
                 orderItems.add(OrderItemEntity.builder()
                        .itemId(dto.getItemId())
                        .shopId(item.getShopId())
                        .size(dto.getSize())
                        .quantity(quantity)
                        .price(item.getPrice())
                        .build());
             }
        }
        
        // 2-3. 추가 아이템 처리
        if (dto.getNewItems() != null) {
            for (Map<String, Object> newItem : dto.getNewItems()) {
                Long newItemId = ((Number) newItem.get("itemId")).longValue();
                String newSize = (String) newItem.get("size");
                int newQuantity = ((Number) newItem.get("quantity")).intValue();
                
                if (newQuantity < 1) newQuantity = 1;

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

        // 3. 배송비 및 최종 결제 금액 계산
        long deliveryFee = (totalProductPrice >= 70000 || totalProductPrice == 0) ? 0 : 3000;
        long totalPrice = totalProductPrice + deliveryFee - dto.getUsedPoints();
        
        if (totalPrice < 0) {
            throw new RuntimeException("결제 금액이 올바르지 않습니다.");
        }

        // 4. 주문 엔티티 생성 및 저장
        OrderEntity order = OrderEntity.builder()
                .userEmail(userEmail)
                .totalPrice(totalPrice)
                .status("PENDING") // 결제 대기 상태
                .createdAt(LocalDateTime.now())
                .receiverName(dto.getReceiverName())
                .receiverPhone(dto.getReceiverPhone())
                .address(dto.getAddress())
                .addressDetail(dto.getAddressDetail())
                .request(dto.getRequest())
                .build();
        
        orderMapper.insertOrder(order); 

        // 5. 주문 상세(OrderItem) 정보 저장
        for (OrderItemEntity item : orderItems) {
            item.setOrderId(order.getId());
        }
        if (!orderItems.isEmpty()) {
            orderMapper.insertOrderItems(orderItems);
        }

        return "ORD-" + String.format("%010d", order.getId()); 
    }

    /**
     * [2단계] 결제 승인 요청 (토스 API 호출) 및 DB 업데이트
     */
    @Transactional
    public void verifyAndCompletePayment(String paymentKey, String orderId, Long amount) throws Exception {
        
        // orderId 파싱 (ORD-0000000123 -> 123)
        long dbId;
        try {
            dbId = Long.parseLong(orderId.replace("ORD-", ""));
        } catch (NumberFormatException e) {
            throw new RuntimeException("잘못된 주문 번호입니다.");
        }

        // 1. DB에서 orderId로 주문 정보를 조회 (금액 위변조 체크용)
        OrderEntity order = orderMapper.selectOrderById(dbId);
        if (order == null) {
            throw new RuntimeException("주문 정보를 찾을 수 없습니다.");
        }
        // 금액 검증 (DB 금액 vs 결제 요청 금액)
        if (order.getTotalPrice() == null ||
                order.getTotalPrice().longValue() != amount.longValue()) {
            throw new RuntimeException("결제 금액이 일치하지 않습니다. (주문금액: " + order.getTotalPrice() + ", 결제금액: " + amount + ")");
        }

        // 2. 토스페이먼츠 승인 API 호출
        URL url = new URL("https://api.tosspayments.com/v1/payments/confirm");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 인증 헤더 설정 (시크릿 키 Base64 인코딩)
        String encodedKey = Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + encodedKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        // 요청 본문 전송
        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.createObjectNode()
                .put("paymentKey", paymentKey)
                .put("orderId", orderId)
                .put("amount", amount)
                .toString();

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        // 3. 응답 처리
        int code = connection.getResponseCode();
        boolean isSuccess = code == 200;

        InputStream responseStream = isSuccess ? connection.getInputStream() : connection.getErrorStream();
        JsonNode responseNode = mapper.readTree(responseStream);

        if (!isSuccess) {
            // 결제 승인 실패 시 에러 처리
            throw new RuntimeException(responseNode.path("message").asText());
        }

        // 4. [성공 시] DB 상태 업데이트 (PENDING -> PAID)
        order.setStatus("PAID"); 
        order.setPaymentKey(paymentKey); 
        order.setPaidAt(LocalDateTime.now());
        
        orderMapper.updateOrderStatus(order);

        // 5. 장바구니 비우기 및 포인트 차감 계산을 위한 아이템 조회
        List<OrderItemEntity> orderItems = orderMapper.selectOrderItemsByOrderId(order.getId());
        
        long totalProductPrice = 0;
        for (OrderItemEntity item : orderItems) {
            totalProductPrice += item.getPrice() * item.getQuantity();
            
            // 장바구니 삭제
            CartEntity cartItem = cartMapper.selectCartItem(order.getUserEmail(), item.getItemId(), item.getSize());
            if (cartItem != null) {
                cartMapper.deleteCartItem(cartItem.getId());
            }
        }

        // 6. 포인트 차감 (원래 금액 - 실 결제 금액 = 사용 포인트)
        long deliveryFee = (totalProductPrice >= 70000 || totalProductPrice == 0) ? 0 : 3000;
        long originalTotalPrice = totalProductPrice + deliveryFee;
        long usedPoints = originalTotalPrice - order.getTotalPrice();

        if (usedPoints > 0) {
            registerMapper.updatePoint(order.getUserEmail(), -(int)usedPoints);
            registerMapper.insertPointHistory(PointHistoryEntity.builder()
                    .userEmail(order.getUserEmail())
                    .amount(-(int)usedPoints)
                    .type("USE")
                    .orderId(String.valueOf(order.getId()))
                    .build());
        }

        // 7. 포인트 적립 (1%)
        int earnedPoints = (int) (order.getTotalPrice() * 0.01);
        if (earnedPoints > 0) {
            registerMapper.updatePoint(order.getUserEmail(), earnedPoints);
            registerMapper.insertPointHistory(PointHistoryEntity.builder()
                    .userEmail(order.getUserEmail())
                    .amount(earnedPoints)
                    .type("EARN")
                    .orderId(String.valueOf(order.getId()))
                    .build());
        }
    }

    /**
     * [환불] 결제 취소 요청
     */
    @Transactional
    public void cancelPayment(Long orderId, String userEmail, String cancelReason) {
        // 1. 주문 조회
        OrderEntity order = orderMapper.selectOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("주문 정보를 찾을 수 없습니다.");
        }

        // 2. 권한 및 상태 검증
        if (!order.getUserEmail().equals(userEmail)) {
            throw new RuntimeException("본인의 주문만 취소할 수 있습니다.");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new RuntimeException("결제 완료 상태의 주문만 취소할 수 있습니다.");
        }
        if (order.getPaymentKey() == null) {
            throw new RuntimeException("결제 키가 존재하지 않습니다.");
        }

        try {
            // 3. 토스페이먼츠 취소 API 호출
            URL url = new URL("https://api.tosspayments.com/v1/payments/" + order.getPaymentKey() + "/cancel");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            String encodedKey = Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + encodedKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.createObjectNode()
                    .put("cancelReason", cancelReason)
                    .toString();

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int code = connection.getResponseCode();
            boolean isSuccess = code == 200;

            if (!isSuccess) {
                InputStream errorStream = connection.getErrorStream();
                JsonNode errorNode = mapper.readTree(errorStream);
                throw new RuntimeException(errorNode.path("message").asText());
            }

            // 4. DB 상태 업데이트 (CANCELLED)
            order.setStatus("CANCELLED");
            orderMapper.updateOrderStatus(order);

            // 5. 포인트 복구 및 회수 로직
            // 5-1. 원 주문 금액 재계산 (포인트 사용량 역산)
            List<OrderItemEntity> orderItems = orderMapper.selectOrderItemsByOrderId(order.getId());
            long totalProductPrice = 0;
            for (OrderItemEntity item : orderItems) {
                totalProductPrice += item.getPrice() * item.getQuantity();
            }

            long deliveryFee = (totalProductPrice >= 70000 || totalProductPrice == 0) ? 0 : 3000;
            long originalTotalPrice = totalProductPrice + deliveryFee;
            long usedPoints = originalTotalPrice - order.getTotalPrice(); // 사용했던 포인트

            int earnedPoints = (int) (order.getTotalPrice() * 0.01); // 적립됐던 포인트

            // 5-2. 포인트 처리 (사용 포인트 반환 - 적립 포인트 회수)
            int pointChange = (int) usedPoints - earnedPoints;

            if (pointChange != 0) {
                registerMapper.updatePoint(order.getUserEmail(), pointChange);
            }
            
            // 히스토리 기록
            if (usedPoints > 0) {
                registerMapper.insertPointHistory(PointHistoryEntity.builder()
                        .userEmail(order.getUserEmail())
                        .amount((int) usedPoints)
                        .type("REFUND") // 환불로 인한 반환
                        .orderId(String.valueOf(order.getId()))
                        .build());
            }
            if (earnedPoints > 0) {
                registerMapper.insertPointHistory(PointHistoryEntity.builder()
                        .userEmail(order.getUserEmail())
                        .amount(-earnedPoints)
                        .type("R_EARN") // 적립 취소 (Refund Earn)
                        .orderId(String.valueOf(order.getId()))
                        .build());
            }

        } catch (Exception e) {
            throw new RuntimeException("환불 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
