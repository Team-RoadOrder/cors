package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.services.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/reservation")
@RequiredArgsConstructor
public class ReservationController {
    private final ShopService shopService;

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getReservation(ModelAndView modelAndView, @RequestParam(value = "shopId", required = false, defaultValue = "0") int shopId) {
        ShopInfoEntity shopInfo = this.shopService.getShopInfo(shopId);
        modelAndView.setViewName("reservation/reservation");
        modelAndView.addObject("shopInfo", shopInfo);
          return modelAndView;
        };
}
