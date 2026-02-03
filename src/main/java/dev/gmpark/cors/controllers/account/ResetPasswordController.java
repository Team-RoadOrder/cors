package dev.gmpark.cors.controllers.account;


import dev.gmpark.cors.results.register.ResetPasswordResult;
import dev.gmpark.cors.services.ResetPasswordService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ResetPasswordController {
    private final ResetPasswordService resetPasswordService;

    @RequestMapping(value = "/resetpw", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getRestPassword() {
            return "resetpw/resetpw";
    }
    @RequestMapping(value = "/resetpw", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> updatePassword(@RequestParam String email, 
                                              @RequestParam String password,
                                              @RequestParam String code,
                                              @RequestParam String salt) {
        ResetPasswordResult result = this.resetPasswordService.UpdatePassword(email, password, code, salt);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", result);

        return responseBody;
    }

}
