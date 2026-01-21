package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.services.ItemService;
import lombok.RequiredArgsConstructor;
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
public class SearchResultController {

    // [추가] ItemService 주입
    private final ItemService itemService;

    @RequestMapping(value = "/search-result", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getSearchResult(
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
            @RequestParam(value = "keyword", required = false) String keyword, // [추가] keyword 파라미터 받기
            ModelAndView modelAndView) {
        if( sessionUser == null ) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }

        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/owner");
            return modelAndView;
        }
        // [추가] 검색 로직 수행
        ShopItemEntity[] searchResults = this.itemService.searchItems(keyword);

        // [추가] 모델에 데이터 담기
        modelAndView.addObject("keyword", keyword);
        modelAndView.addObject("searchResults", searchResults);

        modelAndView.setViewName("searchresult/searchresult");
        return modelAndView;
    }
}