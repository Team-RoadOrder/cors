package dev.gmpark.cors.controllers;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.results.register.RegisterResult;
import dev.gmpark.cors.services.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping(value = "/register")
public class RegisterController {
    private final RegisterService registerService;
    @Autowired
    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET, produces =
            MediaType.APPLICATION_JSON_VALUE)
    public String getRegister() {
        return "register/register";
    }
    @RequestMapping(value = "/", method = RequestMethod.POST, produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String,Object> postRegister(RegisterEntity register) {
        RegisterResult result = this.registerService.register(register);
        if(result == RegisterResult.SUCCESS) {
            System.out.println(register.getId());
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("result", result.name());
        return responseBody;

    }

}
