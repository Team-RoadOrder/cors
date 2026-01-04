package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.services.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class SearchResultController {

    // [추가] ItemService 주입
    private final ItemService itemService;

    @RequestMapping(value = "/search-result", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getSearchResult(
            @RequestParam(value = "keyword", required = false) String keyword, // [추가] keyword 파라미터 받기
            ModelAndView modelAndView) {

        // [추가] 검색 로직 수행
        ShopItemEntity[] searchResults = this.itemService.searchItems(keyword);

        // [추가] 모델에 데이터 담기
        modelAndView.addObject("keyword", keyword);
        modelAndView.addObject("searchResults", searchResults);

        modelAndView.setViewName("searchresult/searchresult");
        return modelAndView;
    }
}