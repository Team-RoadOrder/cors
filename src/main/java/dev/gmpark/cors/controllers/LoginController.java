package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.results.register.LoginResult;
import dev.gmpark.cors.services.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;


@Controller
@RequestMapping(value = "/")
public class LoginController {
    private final LoginService loginService;

    @Autowired
    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @RequestMapping(value = "/login",method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getLogin() {
        return "login/login";
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postLogin(@RequestParam String email, @RequestParam String password) {

        RegisterEntity user = this.loginService.CheckLogin(email, password);
        Map<String, Object> responseBody = new HashMap<>();

        if( user != null ) {
            responseBody.put("status", LoginResult.SUCCESS);
            responseBody.put("usertype", user.getUsertype());
        } else {
            responseBody.put("status", LoginResult.FAILURE);
        }
        System.out.println(responseBody);
        return responseBody;
    }
}

