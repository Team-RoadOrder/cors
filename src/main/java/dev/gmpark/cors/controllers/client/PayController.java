package dev.gmpark.cors.controllers.client;

import dev.gmpark.cors.dtos.SingleOrderDto;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.results.CommonResult;
import dev.gmpark.cors.services.OrderService;
import dev.gmpark.cors.services.PayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class PayController {
    private final OrderService orderService;
    private final PayService payService;

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

        try {
            // [수정] 서비스 호출: 장바구니 소유권 검증 로직이 포함된 메서드 실행
            Map<String, Object> pageData = this.orderService.getPaymentPageData(sessionUser, itemId, size, cartIds);

            // [안전장치] 장바구니 결제인데 상품이 하나도 없다면(유효하지 않은 접근 등) 리다이렉트
            if (cartIds != null && !cartIds.isEmpty()) {
                List<?> items = (List<?>) pageData.get("items");
                if (items == null || items.isEmpty()) {
                    modelAndView.setViewName("redirect:/cart");
                    return modelAndView;
                }
            }

            modelAndView.addAllObjects(pageData);
            modelAndView.setViewName("payment/payment");
        } catch (IllegalArgumentException e) {
            // [핵심] 남의 장바구니 접근 시 예외를 잡아 장바구니로 튕겨내기
            modelAndView.setViewName("redirect:/cart");
        } catch (Exception e) {
            e.printStackTrace();
            modelAndView.setViewName("redirect:/cart");
        }

        return modelAndView;
    }

    @RequestMapping(value = "/pay", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postPay(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
                                       @RequestBody SingleOrderDto dto) {
        if (sessionUser == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("result", CommonResult.FAILURE.name());
            response.put("message", "로그인이 필요합니다.");
            return response;
        }

        return this.orderService.processOrder(sessionUser.getEmail(), dto);
    }

    @PostMapping("/pay/prepare")
    @ResponseBody
    public String prepareOrder(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
                               @RequestBody SingleOrderDto dto) {
        if (sessionUser == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        // 서비스에서 주문을 생성하고 주문번호(OrderId)를 받아옵니다.
        return payService.prepareOrder(dto, sessionUser.getEmail());
    }

    @PostMapping("/pay/complete-point")
    @ResponseBody
    public String completePointPayment(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
                                       @RequestBody Map<String, String> payload) {
        if (sessionUser == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        String orderId = payload.get("orderId");
        payService.completePointPayment(orderId, sessionUser.getEmail());
        return "SUCCESS";
    }

    @GetMapping("/payment/success")
    public String paymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            Model model
    ) {
        try {
            // 전액 포인트 결제인 경우 검증 로직 스킵 (이미 처리됨)
            if (!"POINT_PAYMENT".equals(paymentKey)) {
                // 서비스 로직 호출 (토스 승인 요청 -> DB 업데이트)
                payService.verifyAndCompletePayment(paymentKey, orderId, amount);
            }

            // 성공 페이지로 데이터 전달
            model.addAttribute("orderId", orderId);
            model.addAttribute("amount", amount);
            return "payment/success";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "결제 승인 중 오류가 발생했습니다: " + e.getMessage());
            return "payment/fail";
        }
    }

    @GetMapping("/payment/fail")
    public String paymentFail(
            @RequestParam String message,
            @RequestParam String code,
            Model model
    ) {
        model.addAttribute("message", message);
        model.addAttribute("code", code);
        return "payment/fail";
    }
}