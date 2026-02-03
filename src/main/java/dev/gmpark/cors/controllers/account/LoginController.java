package dev.gmpark.cors.controllers.account;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.results.register.LoginResult;
import dev.gmpark.cors.services.LoginService;
import dev.gmpark.cors.services.OwnerMemberService;
import dev.gmpark.cors.services.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;


@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;
    private final RegisterService registerService;
    private final OwnerMemberService ownerMemberService;
    @Value("${custom.property.kakao-client-id}")
    private String kakaoClientId;
    @Value("${custom.property.kakao-redirect-uri}")
    private String kakaoRedirectUri;
    @Value("${custom.property.naver-client-id}")
    private String naverClientId;
    @Value("${custom.property.naver-redirect-uri}")
    private String naverRedirectUri;


    @RequestMapping(value = "/")
    public String getRoot(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        if (sessionUser != null) {
            String userType = sessionUser.getUsertype();
            if ("customer".equalsIgnoreCase(userType)) {
                return "redirect:/main";
            } else if ("owner".equalsIgnoreCase(userType)) {
                return "redirect:/owner";
            }
        }
        return "redirect:/login";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET) // 페이지 이동이므로 produces 생략 권장
    public String getLogin(Model model, @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {

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
        model.addAttribute("kakaoClientId", kakaoClientId);
        model.addAttribute("kakaoRedirectUri", kakaoRedirectUri);
        model.addAttribute("naverClientId", naverClientId);
        model.addAttribute("naverRedirectUri", naverRedirectUri);
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
    @RequestMapping(value = "/login/kakao", method = RequestMethod.GET)
    public ModelAndView getLoginKakao(HttpSession session,
                                      @RequestParam(value = "code", required = false) String code) {

        ModelAndView modelAndView = new ModelAndView();

        if (code == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }

        // 1. [Service 호출] 실제 카카오 ID 받아오기
        String socialId = this.loginService.getKakaoSocialId(code);
        String socialTypeCode = "KAKAO";

        // 통신 실패 시 로그인 페이지로
        if (socialId == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }

        // 2. DB 조회
        RegisterEntity user = this.loginService.checkSocialUser(socialId, socialTypeCode);

        if (user == null) {
            // [비회원] -> 회원가입 페이지 (Forward)
            modelAndView.addObject("socialTypeCode", socialTypeCode);
            modelAndView.addObject("socialId", socialId);
            modelAndView.addObject("isSocialRegister", true);
            modelAndView.setViewName("register/register");
        } else {
            // [회원] -> 로그인 처리
            session.setAttribute("sessionUser", user);
            if ("owner".equalsIgnoreCase(user.getUsertype())) {
                modelAndView.setViewName("redirect:/owner");
            } else {
                modelAndView.setViewName("redirect:/main");
            }
        }
        return modelAndView;
    }
    @RequestMapping(value = "/login/naver", method = RequestMethod.GET)
    public ModelAndView getLoginNaver(HttpSession session,
                                      @RequestParam(value = "code", required = false) String code) {

        ModelAndView modelAndView = new ModelAndView();

        if (code == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }

        // 1. [Service 호출] 실제 네이버 ID 받아오기
        String socialId = this.loginService.getNaverSocialId(code);
        String socialTypeCode = "NAVER";

        // 통신 실패 시 로그인 페이지로
        if (socialId == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }

        // 2. DB 조회
        RegisterEntity user = this.loginService.checkSocialUser(socialId, socialTypeCode);

        if (user == null) {
            // [비회원] -> 회원가입 페이지 (Forward)
            modelAndView.addObject("socialTypeCode", socialTypeCode);
            modelAndView.addObject("socialId", socialId);
            modelAndView.addObject("isSocialRegister", true);
            modelAndView.setViewName("register/register");
        } else {
            // [회원] -> 로그인 처리
            session.setAttribute("sessionUser", user);
            if ("owner".equalsIgnoreCase(user.getUsertype())) {
                modelAndView.setViewName("redirect:/owner");
            } else {
                modelAndView.setViewName("redirect:/main");
            }
        }
        return modelAndView;
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

