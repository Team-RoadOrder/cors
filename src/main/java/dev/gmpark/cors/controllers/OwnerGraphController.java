package dev.gmpark.cors.controllers;


import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/owner")
@RequiredArgsConstructor
public class OwnerGraphController {
    @RequestMapping(value = "/graph", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getOwnerGraph(ModelAndView modelAndView) {
        modelAndView.setViewName("ownergraph/ownergraph");
        return modelAndView;
    }

}
