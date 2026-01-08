package dev.gmpark.cors.controllers;

import dev.gmpark.cors.dtos.PaymentItemDto;
import dev.gmpark.cors.dtos.SingleOrderDto;
import dev.gmpark.cors.entities.RegisterEntity;
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

        modelAndView.addObject("user", sessionUser);
        modelAndView.addObject("items", items);
        modelAndView.addObject("totalProductPrice", totalProductPrice);
        modelAndView.addObject("deliveryFee", deliveryFee);
        modelAndView.addObject("totalPrice", totalPrice);
        modelAndView.addObject("isCartOrder", (cartIds != null && !cartIds.isEmpty()));

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

        try {
            CommonResult result = this.orderService.processSingleOrder(sessionUser, dto.getItemId(), dto.getSize(), dto.getRequest());
            response.put("result", result.name());
        } catch (Exception e) {
            e.printStackTrace();
            response.put("result", CommonResult.FAILURE.name());
            response.put("message", "주문 처리 중 오류가 발생했습니다.");
        }
        return response;
    }
}
