package dev.gmpark.cors.controllers;

import dev.gmpark.cors.dtos.CartOrderDto;
import dev.gmpark.cors.dtos.SingleOrderDto;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.services.CartService;
import dev.gmpark.cors.services.OrderService;
import dev.gmpark.cors.vos.CartVo;
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
        CartVo[] cartList = this.cartService.getCartList(sessionUser.getEmail());
        modelAndView.addObject("cartList", cartList);
        modelAndView.setViewName("cart/cart");
        return modelAndView;
    }

    @RequestMapping(value = "/cart", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> addCart(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
                                       @RequestParam(value = "itemId") Long itemId,
                                       @RequestParam(value = "size") String size,
                                       @RequestParam(value = "quantity", defaultValue = "1") int quantity) {
        Map<String, Object> response = new HashMap<>();
        if (sessionUser == null) {
            response.put("result", CommonResult.FAILURE.name());
            response.put("message", "로그인이 필요합니다.");
            return response;
        }
        Long cartId = this.cartService.addCart(sessionUser.getEmail(), itemId, size, quantity);
        if (cartId > 0) {
            response.put("result", CommonResult.SUCCESS.name());
            response.put("cartId", cartId); // 생성된 ID 반환
        } else {
            response.put("result", CommonResult.FAILURE.name());
        }
        return response;
    }

    @RequestMapping(value = "/cart", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> deleteCart(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
                                          @RequestParam(value = "ids") List<Long> ids) {
        Map<String, Object> response = new HashMap<>();
        if (sessionUser == null) {
            response.put("result", CommonResult.FAILURE.name());
            response.put("message", "로그인이 필요합니다.");
            return response;
        }
        
        boolean result = this.cartService.deleteCartItems(ids);
        response.put("result", result ? CommonResult.SUCCESS.name() : CommonResult.FAILURE.name());
        return response;
    }

    @RequestMapping(value = "/cart/order", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> orderCart(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
                                         @RequestBody CartOrderDto dto) {
        Map<String, Object> response = new HashMap<>();
        if (sessionUser == null) {
            response.put("result", CommonResult.FAILURE.name());
            response.put("message", "로그인이 필요합니다.");
            return response;
        }

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
    public Map<String, Object> getCartCount(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        if (sessionUser == null) {
            response.put("count", 0);
            return response;
        }
        int count = this.cartService.getCartCount(sessionUser.getEmail());
        response.put("count", count);
        return response;
    }
}
