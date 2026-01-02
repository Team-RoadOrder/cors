package dev.gmpark.cors.controllers;


import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class SearchResultController {
    @RequestMapping(value = "/search-result", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getSearchResult( ModelAndView modelAndView ) {
        modelAndView.setViewName("searchresult/searchresult");
        return modelAndView;
    }
}
