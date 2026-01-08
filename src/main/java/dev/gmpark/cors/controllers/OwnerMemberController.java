package dev.gmpark.cors.controllers;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.services.OwnerMainService;
import dev.gmpark.cors.services.OwnerShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/owner")
@RequiredArgsConstructor
public class OwnerMemberController {
    private final OwnerShopService ownerShopService; // 서비스 연결
    private final OwnerMainService ownerMainService;
    @RequestMapping(value = "/member", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getMember(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser, ModelAndView modelAndView) {
        modelAndView.setViewName("ownermember/ownermember");

        ShopInfoEntity shopInfo = ownerMainService.getShopByEmail(sessionUser.getEmail());
        modelAndView.addObject("shop", shopInfo);
        return modelAndView;
    }
}
