package dev.gmpark.cors.controllers;

import dev.gmpark.cors.dtos.CartOrderDto;
import dev.gmpark.cors.dtos.SingleOrderDto;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.results.Result;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.services.CartService;
import dev.gmpark.cors.services.OrderService;
import dev.gmpark.cors.vos.CartVo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
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
public class CartController {
    private final CartService cartService;
    private final OrderService orderService;

    @RequestMapping(value = "/cart", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getCart(ModelAndView modelAndView,
                                @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        if (sessionUser == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }
        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/owner");
            return modelAndView;
        }

        CartVo[] cartList = this.cartService.getCartList(sessionUser);
        modelAndView.addObject("cartList", cartList);
        modelAndView.setViewName("cart/cart");
        return modelAndView;
    }



    @RequestMapping(value = "/cart", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> addCart(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser,
                                       @RequestParam(value = "itemId") Long itemId,
                                       @RequestParam(value = "size") String size,
                                       @RequestParam(value = "quantity", defaultValue = "1") int quantity) {
        Map<String, Object> response = new HashMap<>();
        Pair<Result, Long> result = this.cartService.addCart(sessionUser, itemId, size, quantity);
        
        response.put("result", result.getLeft().name());
        if (result.getLeft() == CommonResult.SUCCESS) {
            response.put("cartId", result.getRight());
        }
        return response;
    }
    @Data
    public static class CartItemRequest {
        private Long itemId;
        private String size;
        private int quantity; // 수량 추가
    }

    @RequestMapping(value = "/cart/batch-add", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> addBatchCart(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser,
                                            @RequestBody List<CartItemRequest> items) {

        Map<String, Object> response = new HashMap<>();
        if (sessionUser == null) {
            response.put("result", "FAILURE_SESSION");
            return response;
        }
        if (items == null || items.isEmpty()) {
            response.put("result", CommonResult.FAILURE.name());
            return response;
        }

        int successCount = 0;

        for (CartItemRequest item : items) {
            int qty = item.getQuantity() > 0 ? item.getQuantity() : 1;

            Pair<Result, Long> result = this.cartService.addCart(sessionUser, item.getItemId(), item.getSize(), qty);

            if (result.getLeft() == CommonResult.SUCCESS) {
                successCount++;
            }
        }

        if (successCount > 0) {
            response.put("result", CommonResult.SUCCESS.name());
        } else {
            response.put("result", CommonResult.FAILURE.name());
        }

        return response;
    }
    @RequestMapping(value = "/cart", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> deleteCart(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser,
                                          @RequestParam(value = "ids") List<Long> ids) {
        Map<String, Object> response = new HashMap<>();
        Result result = this.cartService.deleteCartItems(sessionUser, ids);
        response.put("result", result.name());
        return response;
    }

    @RequestMapping(value = "/cart/order", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> orderCart(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser,
                                         @RequestBody CartOrderDto dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (dto.getCartIds() == null || dto.getCartIds().isEmpty()) {
                response.put("result", CommonResult.FAILURE.name());
                response.put("message", "주문할 상품이 없습니다.");
                return response;
            }
            
            SingleOrderDto orderDto = new SingleOrderDto();
            orderDto.setCartIds(dto.getCartIds());
            orderDto.setRequest("요청사항 없음");
            
            CommonResult result = this.orderService.processCartOrder(sessionUser, orderDto);
            response.put("result", result.name());
        } catch (Exception e) {
            e.printStackTrace();
            response.put("result", CommonResult.FAILURE.name());
            response.put("message", "주문 처리 중 오류가 발생했습니다.");
        }
        return response;
    }
    
    @RequestMapping(value = "/cart/count", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getCartCount(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        int count = this.cartService.getCartCount(sessionUser);
        response.put("count", count);
        return response;
    }

    @RequestMapping(value = "/cart", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> updateCartQuantity(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser,
                                                  @RequestParam(value = "cartId") Long cartId,
                                                  @RequestParam(value = "quantity") int quantity) {
        Map<String, Object> response = new HashMap<>();
        if (sessionUser == null) {
            response.put("result", CommonResult.FAILURE.name());
            return response;
        }

        Result result = this.cartService.updateCartQuantity(sessionUser, cartId, quantity);
        
        response.put("result", result.name());
        return response;
    }
}
