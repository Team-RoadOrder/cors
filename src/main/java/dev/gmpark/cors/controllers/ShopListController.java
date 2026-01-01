package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.RegisterEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/")
public class ShopListController {
    @RequestMapping(value = "/shops", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getShopList(ModelAndView modelAndView,
                                    @SessionAttribute(value = "sessionUser",required = false) RegisterEntity sessionUser) {
        if (sessionUser == null) {
            // "redirect:/login" 문자열을 리턴하는 게 아니라 뷰 이름으로 설정
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }

        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/owner");
            return modelAndView;
        }
        modelAndView.setViewName("shoplist/shoplist");
        return modelAndView;
    }
}
