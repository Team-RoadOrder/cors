package dev.gmpark.cors.controllers;

import dev.gmpark.cors.dtos.SingleOrderDto;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.results.CommonResult;
import dev.gmpark.cors.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
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

        Map<String, Object> pageData = this.orderService.getPaymentPageData(sessionUser, itemId, size, cartIds);
        modelAndView.addAllObjects(pageData);
        modelAndView.setViewName("payment/payment");
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
}
