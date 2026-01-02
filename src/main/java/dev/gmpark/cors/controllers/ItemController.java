package dev.gmpark.cors.controllers;

import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.services.ItemService;
import dev.gmpark.cors.services.ShopService;
import dev.gmpark.cors.vos.ShopItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/item")
@RequiredArgsConstructor
public class ItemController {
    private final ShopService shopService;
    private final ItemService itemService;

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getItem (ModelAndView modelAndView, @RequestParam(value = "shopId", required = false, defaultValue = "0") int shopId,
                                 @RequestParam(value = "id") Long id){
        ShopInfoEntity shopInfo = this.shopService.getShopInfo(shopId);
        ShopItemVo item = this.itemService.getItemById(id);
        modelAndView.addObject("shopInfo", shopInfo);
        modelAndView.addObject("item", item);
        modelAndView.setViewName("item/item");
        return modelAndView;
    }
}
