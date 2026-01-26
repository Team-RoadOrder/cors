package dev.gmpark.cors.services;

import dev.gmpark.cors.dtos.PaymentItemDto;
import dev.gmpark.cors.dtos.SingleOrderDto;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.RegisterMapper;
import dev.gmpark.cors.results.Result;
import dev.gmpark.cors.results.CommonResult;
import dev.gmpark.cors.validators.PayValidator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayService {
    private final OrderService orderService;
    private final RegisterMapper registerMapper;

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
}
