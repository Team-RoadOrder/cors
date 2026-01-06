package dev.gmpark.cors.controllers;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.services.CartService;
import dev.gmpark.cors.vos.CartVo;
import jakarta.servlet.http.HttpSession;
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
            response.put("result", "failure");
            response.put("message", "로그인이 필요합니다.");
            return response;
        }
        boolean result = this.cartService.addCart(sessionUser.getEmail(), itemId, size, quantity);
        if (result) {
            response.put("result", "success");
        } else {
            response.put("result", "failure");
        }
        return response;
    }

    @RequestMapping(value = "/cart", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> deleteCart(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
                                          @RequestParam(value = "ids") List<Long> ids) {
        Map<String, Object> response = new HashMap<>();
        if (sessionUser == null) {
            response.put("result", "failure");
            response.put("message", "로그인이 필요합니다.");
            return response;
        }
        
        boolean result = true;
        for (Long id : ids) {
            if (!this.cartService.deleteCartItem(id)) {
                result = false;
            }
        }

        if (result) {
            response.put("result", "success");
        } else {
            response.put("result", "failure");
        }
        return response;
    }
}
