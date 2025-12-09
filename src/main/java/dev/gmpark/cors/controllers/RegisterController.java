package dev.gmpark.cors.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/")
public class RegisterController {
    @RequestMapping(value = "/register")
    public String getRegister(){
        return "register/register";
    }
}
