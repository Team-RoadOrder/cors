package dev.gmpark.cors.controllers;


import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value = "/")
public class ShopController {
    @RequestMapping(value = "/shop", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String shop() {
        return "shop/shop";
    }
}
