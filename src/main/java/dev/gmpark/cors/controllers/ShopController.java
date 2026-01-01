package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.services.OwnerShopService;
import dev.gmpark.cors.services.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/shop")
@RequiredArgsConstructor
public class ShopController {
    private final ShopService shopService;

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView shop(ModelAndView modelAndView,
                       @RequestParam(value = "shopId", required = false, defaultValue = "0") int shopId) {
        ShopInfoEntity shopInfo = this.shopService.getShopInfo(shopId);
        modelAndView.addObject("shopInfo", shopInfo);
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
            case "4" -> "하의" ;
            case "5" -> "신발" ;
            case "6" -> "악세사리";
            default -> null;
        };
        return this.shopService.getItemsByShopAndCategory(shopId, dbCategoryName);
    }

}
