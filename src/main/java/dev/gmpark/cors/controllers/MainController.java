package dev.gmpark.cors.controllers;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/")
public class MainController {
    @RequestMapping(value = "/")
    public String getMain() {
        return "main/main";
    }
}
