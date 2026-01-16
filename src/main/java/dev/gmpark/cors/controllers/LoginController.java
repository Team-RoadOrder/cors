package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.results.register.LoginResult;
import dev.gmpark.cors.services.LoginService;
import dev.gmpark.cors.services.OwnerMemberService;
import dev.gmpark.cors.services.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;


@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;
    private final RegisterService registerService;
    private final OwnerMemberService ownerMemberService;
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

        RegisterEntity loginUser = this.loginService.CheckLogin(email, password);

        Map<String, Object> responseBody = new HashMap<>();

        if( loginUser != null ) {
            if (loginUser.getOwnerEmail() != null && !loginUser.getOwnerEmail().isEmpty() && !loginUser.getEmail().equals(loginUser.getOwnerEmail())) {

                RegisterEntity ownerEntity = this.loginService.getUserByEmail(loginUser.getOwnerEmail());

                if (ownerEntity != null) {
                    ownerEntity.setLevel(loginUser.getLevel());
                    ownerEntity.setName(loginUser.getName());
                    ownerEntity.setUsertype("owner");
                    session.setAttribute("realEmployeeEmail", loginUser.getEmail());
                    session.setAttribute("sessionUser", ownerEntity);

                    responseBody.put("status", LoginResult.SUCCESS);
                    responseBody.put("usertype", ownerEntity.getUsertype());
                    responseBody.put("user", ownerEntity.getEmail());
                } else {
                    // 사장 정보를 못 찾음 (예외 처리)
                    responseBody.put("status", LoginResult.FAILURE);
                }

            } else {
                session.setAttribute("sessionUser", loginUser);

                responseBody.put("status", LoginResult.SUCCESS);
                responseBody.put("usertype", loginUser.getUsertype());
                responseBody.put("user", loginUser.getEmail());
            }

        } else {
            responseBody.put("status", LoginResult.FAILURE);
        }

        return responseBody;
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,  HttpSession session) {
        if (sessionUser != null) {
            String targetEmail = sessionUser.getEmail();
            String realEmployeeEmail = (String) session.getAttribute("realEmployeeEmail");
            if (realEmployeeEmail != null) {
                targetEmail = realEmployeeEmail;
            }
            if (sessionUser.getLevel() == 1 || sessionUser.getLevel() == 2) {
                this.ownerMemberService.updateLogoutTime(targetEmail);
            }

            session.removeAttribute("sessionUser");
            session.removeAttribute("realEmployeeEmail");
            session.invalidate();
        }

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

