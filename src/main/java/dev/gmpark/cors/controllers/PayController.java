package dev.gmpark.cors.controllers;

import dev.gmpark.cors.dtos.PaymentItemDto;
import dev.gmpark.cors.dtos.SingleOrderDto;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.RegisterMapper;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class PayController {
    private final OrderService orderService;
    private final RegisterMapper registerMapper;

    @RequestMapping(value = "/pay", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getPay(ModelAndView modelAndView,
                               @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
                               @RequestParam(value = "itemId", required = false) Long itemId,
                               @RequestParam(value = "size", required = false) String size,
                               @RequestParam(value = "cartIds", required = false) List<Long> cartIds) {
        if (sessionUser == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }
        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/owner");
            return modelAndView;
        }

        // 최신 유저 정보 조회 (포인트 등)
        RegisterEntity user = this.registerMapper.selectByEmail(sessionUser.getEmail());
        if (user == null) {
            user = sessionUser; // 조회 실패 시 세션 정보 사용 (비상)
        }

        List<PaymentItemDto> items = new ArrayList<>();

        // 1. 단일 상품 구매
        if (itemId != null && size != null) {
            items = this.orderService.getPaymentItemsForSingleOrder(itemId, size);
        } 
        // 2. 장바구니 구매
        else if (cartIds != null && !cartIds.isEmpty()) {
            items = this.orderService.getPaymentItemsForCartOrder(cartIds);
        }

        long totalProductPrice = 0;
        for (PaymentItemDto item : items) {
            totalProductPrice += item.getPrice() * item.getQuantity();
        }

        // 배송비 계산 로직 수정 (7만원 이상 무료)
        long deliveryFee = 0;
        if (totalProductPrice > 0) {
            deliveryFee = (totalProductPrice >= 70000) ? 0 : 3000;
        }

        long totalPrice = totalProductPrice + deliveryFee;

        modelAndView.addObject("user", user); // 최신 정보가 담긴 user 객체 전달
        modelAndView.addObject("items", items);
        modelAndView.addObject("totalProductPrice", totalProductPrice);
        modelAndView.addObject("deliveryFee", deliveryFee);
        modelAndView.addObject("totalPrice", totalPrice);
        modelAndView.addObject("isCartOrder", (cartIds != null && !cartIds.isEmpty()));
        modelAndView.addObject("cartIds", cartIds); // 장바구니 ID 목록도 뷰로 전달

        modelAndView.setViewName("payment/payment");
        return modelAndView;
    }

    @RequestMapping(value = "/pay", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postPay(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
                                       @RequestBody SingleOrderDto dto) {
        Map<String, Object> response = new HashMap<>();
        if (sessionUser == null) {
            response.put("result", CommonResult.FAILURE.name());
            response.put("message", "로그인이 필요합니다.");
            return response;
        }
        
        // 최신 유저 정보 조회 (포인트 확인을 위해)
        RegisterEntity user = this.registerMapper.selectByEmail(sessionUser.getEmail());
        if (user == null) {
            response.put("result", CommonResult.FAILURE.name());
            response.put("message", "사용자 정보를 찾을 수 없습니다.");
            return response;
        }

        try {
            CommonResult result;
            // 장바구니 주문인 경우
            if (dto.getCartIds() != null && !dto.getCartIds().isEmpty()) {
                result = this.orderService.processCartOrder(user, dto);
            } 
            // 단일 상품 주문인 경우
            else if (dto.getItemId() != null && dto.getSize() != null) {
                result = this.orderService.processSingleOrder(user, dto);
            } else {
                result = CommonResult.FAILURE;
                response.put("message", "주문 정보가 올바르지 않습니다.");
            }
            response.put("result", result.name());
        } catch (Exception e) {
            e.printStackTrace();
            response.put("result", CommonResult.FAILURE.name());
            response.put("message", "주문 처리 중 오류가 발생했습니다.");
        }
        return response;
    }
}
