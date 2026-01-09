package dev.gmpark.cors.controllers;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.services.ItemService;
import dev.gmpark.cors.services.MyService;
import dev.gmpark.cors.services.ShopService;
import dev.gmpark.cors.vos.ShopItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/item")
@RequiredArgsConstructor
public class ItemController {
    private final ShopService shopService;
    private final ItemService itemService;
    private final MyService myService;
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getItem (ModelAndView modelAndView, @RequestParam(value = "shopId", required = false, defaultValue = "0") int shopId,
                                 @RequestParam(value = "id") Long id,
                                 @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser){

        if (sessionUser == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }
        ShopItemVo item = this.itemService.getItemById(id);

        if (shopId == 0 && item != null) {
            shopId = item.getShopId();
        }

        ShopInfoEntity shopInfo = this.shopService.getShopInfo(shopId);
        int likeCount = this.shopService.getShopLikeCount(shopId);
        ShopInfoEntity[] likeShops = this.myService.getLikeShops(sessionUser);

        boolean isLiked = false;
        if (sessionUser != null && likeShops != null) {
            final int currentShopId = shopId;
            isLiked = Arrays.stream(likeShops)
                    .anyMatch(shop -> shop.getShopId() == currentShopId);
        }

        // [AI 추가] 추천 상품 조회
        List<ShopItemVo> relatedItems = this.itemService.getRelatedItems(id);

        modelAndView.addObject("shopInfo", shopInfo);
        modelAndView.addObject("item", item);
        modelAndView.addObject("likeShops", this.myService.getLikeShops(sessionUser));
        modelAndView.addObject("isLiked", isLiked); // boolean
        modelAndView.addObject("likeCount", likeCount);
        modelAndView.addObject("relatedItems", relatedItems); // AI 추천 상품 추가
        modelAndView.setViewName("item/item");
        return modelAndView;
    }
    @RequestMapping(value = "/like" , method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postLikeItem(
            @RequestParam(value = "shopId") int shopId,
            @RequestParam(value = "itemId") Long itemId,
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {

        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.itemService.toggleLikeItem(shopId, itemId, sessionUser);
        response.put("result",result.name() );
        return response;
    }


}