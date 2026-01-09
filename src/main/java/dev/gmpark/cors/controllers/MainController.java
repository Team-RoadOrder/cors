package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.services.MainService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/")
public class MainController {
    private final MainService mainService;

    @Autowired
    public MainController(MainService mainService) {
        this.mainService = mainService;
    }

    @RequestMapping(value = "/main")
    public String getMain(
            @SessionAttribute(value = "sessionUser",required = false)RegisterEntity sessionUser,
            Model model
            ) {
        if (sessionUser == null) {
            return "redirect:/login";
        }
        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            return "redirect:/owner";
        }
        return "main/main";
    }
    @RequestMapping(value = "/login-status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String,Object> loginStatus(@SessionAttribute(value = "sessionUser",required = false)RegisterEntity sessionUser) {
        Map<String,Object> response = new HashMap<>();
        response.put("isLoggedIn",sessionUser != null);
        return response;
    }
    @RequestMapping(value = "/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
    @RequestMapping(value = "/items", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ShopItemEntity[] getItemsByStyle(@RequestParam(value = "style") String style) {
        String dbStyleName = switch (style) {
            case "1" -> "미니멀";
            case "2" -> "캐주얼";
            case "3" -> "스트릿";
            default -> "미니멀"; // 기본값
        };
        // 이제 "미니멀"이라는 문자열이 서비스 -> 매퍼 -> SQL로 전달됩니다.
        return this.mainService.getAllByStyle(dbStyleName);
    }
    @RequestMapping(value = "/six-shops", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ShopInfoEntity[] getSixShops() {
        /*return this.*/
        return this.mainService.getSixShop();
    }
    @RequestMapping(value = "/all-shops", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ShopInfoEntity[] getAllShops() {
        /*return this.*/
        return this.mainService.getAllShop();
    }
}
