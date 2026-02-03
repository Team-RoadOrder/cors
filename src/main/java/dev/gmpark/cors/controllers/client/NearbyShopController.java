package dev.gmpark.cors.controllers.client;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.services.MainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class NearbyShopController {
    private final MainService mainService;
    @RequestMapping(value = "/nearby-shop", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getNearbyShop(ModelAndView modelAndView,
                                      @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        if( sessionUser == null ) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }

        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/owner");
            return modelAndView;
        }

        ShopInfoEntity[] shops = this.mainService.getAllShop(sessionUser.getAddress());
        modelAndView.addObject("sessionUser", sessionUser);
        modelAndView.addObject("shops", shops);
        modelAndView.setViewName("nearbyshop/nearbyshop");
        return modelAndView;
    }
}
