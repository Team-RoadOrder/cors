package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.services.ItemService;
import dev.gmpark.cors.vos.ShopItemVo;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class PayController {
    private final ItemService itemService;

    @RequestMapping(value = "/pay" , method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getPay( ModelAndView modelAndView,
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

}
