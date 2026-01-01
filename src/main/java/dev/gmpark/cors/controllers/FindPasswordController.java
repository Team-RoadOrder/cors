package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.results.register.FindPasswordResult;
import dev.gmpark.cors.services.FindPasswordService;
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
public class FindPasswordController {

    private final FindPasswordService findPasswordService;
    @Autowired
    public FindPasswordController(FindPasswordService findPasswordService) {
        this.findPasswordService = findPasswordService;
    }

    @RequestMapping(value = "/findpw", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getFindPassword() {
            return "findpw/findpw";
    }
    @RequestMapping(value = "/findpw", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> findPassword(@RequestParam String email) {
        RegisterEntity user = this.findPasswordService.FindPassword(email);
        Map<String, Object> responseBody = new HashMap<>();
        if (user != null) {
            responseBody.put("status", FindPasswordResult.SUCCESS);
            responseBody.put("password", user.getPassword());
        } else {
            responseBody.put("status",FindPasswordResult.FAILURE);
        }

        return responseBody;
    }

}
