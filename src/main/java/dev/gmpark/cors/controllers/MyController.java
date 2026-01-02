package dev.gmpark.cors.controllers;


import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/")
public class MyController {

    @RequestMapping(value = "/my", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getMy(ModelAndView modelAndView) {
        modelAndView.setViewName("my/my");
        return modelAndView;
    }
}
