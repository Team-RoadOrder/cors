package dev.gmpark.cors.controllers;

import dev.gmpark.cors.entities.EmailTokenEntity;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.exceptions.TransactionalException;
import dev.gmpark.cors.results.register.SendEmailResult;
import dev.gmpark.cors.results.register.VerifyEmailResult;
import dev.gmpark.cors.services.RegisterService;
import jakarta.mail.MessagingException;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.*;

@Controller
@RequestMapping(value = "/")
public class RegisterController  extends AbstractGeneralController {
    private final RegisterService registerService;
    @Autowired
    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @RequestMapping(value = "/register", method = RequestMethod.GET, produces =
            MediaType.APPLICATION_JSON_VALUE)
    public String getRegister() {
        return "register/register";
    }
    @RequestMapping(value = "/register", method = RequestMethod.POST, produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String,Object> postRegister(RegisterEntity register, EmailTokenEntity emailToken) {
        Enum<?> result;
        try {
            result = this.registerService.register(register, emailToken);
        } catch (TransactionalException e) {
            result = e.result;
        }
/*
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("result", result.name());
        return responseBody;*/
        return prepareJsonResponse(result);


    }
    @RequestMapping(value = "/register/email" , method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postEmail(@RequestParam(value = "email", required = false ) String email,
                                         @RequestParam(value = "type" , required = false ) int type) throws MessagingException {
        Pair<SendEmailResult, EmailTokenEntity> result = this.registerService.sendEmail(email,type);
        Map<String,Object>response = new HashMap<>();
        response.put("result",result.getLeft().name());
        if ( result.getLeft() == SendEmailResult.SUCCESS) {
            response.put("salt", result.getRight().getSalt());
        }
        return response;
    }
    @RequestMapping(value = "/register/email", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String,Object> patchEmail(EmailTokenEntity emailToken) {
        VerifyEmailResult result = this.registerService.verifyEmail(emailToken);
        Map<String,Object> response = new HashMap<>();
        response.put("result", result.name());
        return response;
    }
}

