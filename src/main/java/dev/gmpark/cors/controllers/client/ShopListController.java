package dev.gmpark.cors.controllers.client;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.services.MainService;
import dev.gmpark.cors.vos.PageVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class ShopListController {
    private final MainService mainService;

    @RequestMapping(value = "/shops", method = RequestMethod.GET)
    public ModelAndView getShopList(
            ModelAndView modelAndView,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "sort", required = false, defaultValue = "distance") String sort, // [추가] 정렬 기준 (기본값: distance)
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        if( sessionUser == null ) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }

        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/owner");
            return modelAndView;
        }
        int totalCount = this.mainService.getCountAll();
        PageVo pageVo = new PageVo(page, totalCount);

        String userAddress = (sessionUser != null) ? sessionUser.getAddress() : "";

        ShopInfoEntity[] shops = this.mainService.getAllShopByPage(pageVo, userAddress, sort);

        modelAndView.addObject("shops", shops);
        modelAndView.addObject("pageVo", pageVo);
        modelAndView.addObject("sessionUser", sessionUser);
        modelAndView.addObject("sort", sort); // [추가] HTML에서 버튼 활성화를 위해 전달
        modelAndView.setViewName("shoplist/shoplist");

        return modelAndView;
    }
}