package dev.gmpark.cors.controllers;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.services.ItemService;
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
public class ItemListController {
    private final ItemService itemService;

    @RequestMapping(value = "/itemList", method = RequestMethod.GET)
    public ModelAndView getItemList(
            ModelAndView modelAndView,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "sort", required = false, defaultValue = "popular") String sort,
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {

        if (sessionUser == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }
        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/owner");
            return modelAndView;
        }
        int totalCount = this.itemService.getCountAll();
        PageVo pageVo = new PageVo(page, totalCount);

        String userAddress = (sessionUser != null) ? sessionUser.getAddress() : "";
        ShopItemEntity[] items = this.itemService.getAllItemsByPage(pageVo, userAddress, sort);

        modelAndView.addObject("items", items);
        modelAndView.addObject("pageVo", pageVo);
        modelAndView.addObject("sort", sort); // HTML에서 버튼 활성화를 위해 전달
        modelAndView.addObject("sessionUser", sessionUser);
        modelAndView.setViewName("itemlist/itemlist");

        return modelAndView;
    }
}