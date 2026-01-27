package dev.gmpark.cors.controllers;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.services.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class SearchResultController {

    private final SearchService searchService;

    @RequestMapping(value = "/search-result", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getSearchResult(
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
            @RequestParam(value = "keyword", required = false) String keyword,
            ModelAndView modelAndView) {
        if( sessionUser == null ) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }

        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/owner");
            return modelAndView;
        }
        
        // SearchService를 통해 검색 결과 가져오기
        Map<String, Object> searchData = this.searchService.search(keyword);

        modelAndView.addObject("keyword", keyword);
        modelAndView.addAllObjects(searchData);

        modelAndView.setViewName("searchresult/searchresult");
        return modelAndView;
    }
}
