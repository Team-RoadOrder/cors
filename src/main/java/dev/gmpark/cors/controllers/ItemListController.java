package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.services.ItemService;
import dev.gmpark.cors.vos.PageVo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
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
    @RequestMapping(value = "/items", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getItemsList(ModelAndView modelAndView,
                                     @SessionAttribute(value = "sessionUser",required = false) RegisterEntity sessionUser,
                                     @RequestParam(value = "page", required = false, defaultValue = "1") int requestPage) {
        if(sessionUser == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }
        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/owner");
            return modelAndView;
        }
        int totalCount = this.itemService.getCountAll();
        PageVo pageVo = new PageVo(requestPage, totalCount);
        ShopItemEntity[] items  = this.itemService.getAllItemsByPage(pageVo);
        modelAndView.addObject("pageVo", pageVo);
        modelAndView.addObject("items", items);
        modelAndView.setViewName("itemlist/itemlist");
        return modelAndView;

    }
}
