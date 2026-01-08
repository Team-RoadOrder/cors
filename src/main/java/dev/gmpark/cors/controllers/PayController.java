package dev.gmpark.cors.controllers;

import dev.gmpark.cors.dtos.SingleOrderDto;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.services.ItemService;
import dev.gmpark.cors.services.OrderService;
import dev.gmpark.cors.vos.ShopItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class PayController {
    private final ItemService itemService;
    private final OrderService orderService;

    @RequestMapping(value = "/pay", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getPay(ModelAndView modelAndView,
                               @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
                               @RequestParam(value = "itemId") Long itemId,
                               @RequestParam(value = "size") String size) {
        if (sessionUser == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }
        ShopItemVo item = this.itemService.getItemById(itemId);
        modelAndView.addObject("user", sessionUser);
        modelAndView.addObject("item", item);
        modelAndView.addObject("size", size);
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
