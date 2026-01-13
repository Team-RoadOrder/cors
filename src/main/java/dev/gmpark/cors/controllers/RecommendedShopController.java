package dev.gmpark.cors.controllers;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.services.RecommendedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class RecommendedShopController {
    private final RecommendedService recommendedService;

    @RequestMapping(value = "/recommended-shops", method = RequestMethod.GET)
    public ModelAndView getRecommendedShops(ModelAndView modelAndView,
                                            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        if (sessionUser == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }
        
        String style = sessionUser.getStyle();
        List<String> categories = new ArrayList<>();
        
        if (style != null && !style.isEmpty()) {
            String[] styles = style.split(",");
            for (String s : styles) {
                String trimmedStyle = s.trim();
                switch (trimmedStyle) {
                    case "1":
                        categories.add("미니멀");
                        break;
                    case "2":
                        categories.add("캐주얼");
                        break;
                    case "3":
                        categories.add("스트릿");
                        break;
                    default:
                        categories.add(trimmedStyle);
                        break;
                }
            }
        }
        
        if (categories.isEmpty()) {
            categories.add("미니멀"); // 기본값
        }
        
        String[] categoriesArray = categories.toArray(new String[0]);
        String styleText = String.join(", ", categories);
        
        ShopInfoEntity[] recommendedShops = this.recommendedService.getShopsByCategories(categoriesArray);
        ShopItemEntity[] popularItems = this.recommendedService.getItemsByCategoriesOrderByLikes(categoriesArray);
        ShopItemEntity[] allItems = this.recommendedService.getItemsByCategories(categoriesArray);
        
        modelAndView.addObject("userStyle", styleText);
        modelAndView.addObject("recommendedShops", recommendedShops);
        modelAndView.addObject("popularItems", popularItems);
        modelAndView.addObject("allItems", allItems);
        modelAndView.setViewName("recommendedshop/recommendedshop");
        return modelAndView;
    }
}
