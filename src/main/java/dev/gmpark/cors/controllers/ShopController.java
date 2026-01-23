package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.LikeShopEntity;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.services.MyService;
import dev.gmpark.cors.services.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/shop")
@RequiredArgsConstructor
public class ShopController {
    private final ShopService shopService;
    private final MyService myService;

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getShop(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
                        ModelAndView modelAndView,
                       @RequestParam(value = "shopId", required = false, defaultValue = "0") int shopId) {

        if( sessionUser == null ) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }

        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/owner");
            return modelAndView;
        }
        ShopInfoEntity shopInfo = this.shopService.getShopInfo(shopId);

        ShopInfoEntity[] likeShops = this.myService.getLikeShops(sessionUser);
        boolean isLiked = false;
        if (sessionUser != null && likeShops != null) {
            final int currentShopId = shopId;
            isLiked = Arrays.stream(likeShops)
                    .anyMatch(shop -> shop.getShopId() == currentShopId);
        }

        modelAndView.addObject("shopInfo", shopInfo);
        modelAndView.addObject("isLiked", isLiked);
        modelAndView.setViewName("shop/shop");
        return modelAndView;
    }
    @RequestMapping(value = "items", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ShopItemEntity[] getShopItems(
            @RequestParam(value = "shopId", required = false, defaultValue = "0") int shopId,
            @RequestParam(value = "category", required = false) String categoryCode) {
        if (categoryCode == null || categoryCode.isEmpty() || categoryCode.equals("1")) {
            return this.shopService.getItemsByShopAndCategory(shopId, null);
        }
        String dbCategoryName = switch (categoryCode){
            case "2" -> "아우터";
            case "3" -> "상의" ;
            case "4" -> "바지" ;
            case "5" -> "신발" ;
            case "6" -> "악세사리";
            default -> null;
        };
        return this.shopService.getItemsByShopAndCategory(shopId, dbCategoryName);
    }
    @RequestMapping(value = "/like", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postLikeShop(LikeShopEntity likeShopEntity,
                                            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response= new HashMap<>();
        if (sessionUser == null) {
            response.put("result", "FAILURE_SESSION");
        }
        CommonResult result = this.shopService.toggleLikeInfo(likeShopEntity, sessionUser);
        response.put("result", result.name());
        return response;
    }
}
