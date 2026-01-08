package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.results.register.LoginResult;
import dev.gmpark.cors.services.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;


@Controller
@RequestMapping(value = "/")
public class LoginController {
    private final LoginService loginService;

    @Autowired
    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }
    @RequestMapping(value = "/login", method = RequestMethod.GET) // 페이지 이동이므로 produces 생략 권장
    public String getLogin(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {

        if (sessionUser != null) {
            String userType = sessionUser.getUsertype();

            if ("customer".equalsIgnoreCase(userType)) {
                return "redirect:/main";
            } else if ("owner".equalsIgnoreCase(userType)) {
                return "redirect:/owner";
            } else {
                return "redirect:/login"; // 기본 경로 나중에 수정하삼
            }
        }
        return "login/login";
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postLogin(@RequestParam String email, @RequestParam String password , HttpSession session) {

        RegisterEntity user = this.loginService.CheckLogin(email, password);
        Map<String, Object> responseBody = new HashMap<>();
        if( user != null ) {
            session.setAttribute("sessionUser", user);
            /*session.setAttribute("loggedInUserUsertype", user.getUsertype());*/
            responseBody.put("status", LoginResult.SUCCESS);
            responseBody.put("usertype", user.getUsertype());
            responseBody.put("user", user.getEmail());
        } else {
            responseBody.put("status", LoginResult.FAILURE);
        }


        System.out.println(responseBody);
        System.out.println(session.getAttribute("sessionUser"));
        return responseBody;
        /*Pair<LoginResult, UserEntity> result = this.userService.login(email, password);
        if ( result.getLeft() == LoginResult.SUCCESS) {
            session.setAttribute("sessionUser", result.getRight());
        }*/
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,  HttpSession session) {
        // 세션을 무효화(Invalidate)하여 세션에 저장된 모든 사용자 정보를 제거합니다.
        if (sessionUser != null) {
            session.removeAttribute("sessionUser");
        }
        // 로그아웃 후 원하는 페이지로 리다이렉트합니다.
        return "redirect:/login";
    }
    @RequestMapping(value = "/api/auth/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getLoginStatus(HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        String userEmail = (String) session.getAttribute("loggedInUserEmail");
        if (userEmail != null) {
            response.put("isLoggedIn", true);
        } else {
            response.put("isLoggedIn", false);
        }

        return response;
    }
}

