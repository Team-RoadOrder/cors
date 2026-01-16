package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.services.OwnerMainService;
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
public class OwnerGraphController {
    private final OwnerMainService ownerMainService;
    @RequestMapping(value = "/graph", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getOwnerGraph(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser, ModelAndView modelAndView) {
        modelAndView.setViewName("ownergraph/ownergraph");
        ShopInfoEntity shopInfo = ownerMainService.getShopByEmail(sessionUser.getEmail());
        modelAndView.addObject("shop", shopInfo);
        /*레벨이 1인경우에만 권한없음으로  ownermain.js에 로직짜기 */
        return modelAndView;
    }

}
