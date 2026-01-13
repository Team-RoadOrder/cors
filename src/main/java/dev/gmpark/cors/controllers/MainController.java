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
    public ShopItemEntity[] getItemsByStyle(
            @RequestParam(value = "style") String style,
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {

        String dbStyleName = switch (style) {
            case "1" -> "미니멀";
            case "2" -> "캐주얼";
            case "3" -> "스트릿";
            case "4" -> "댄디";
            case "5" -> "빈티지";
            case "6" -> "모던";
            case "7" -> "스포티";
            case "8" -> "페미닌";
            default -> "미니멀";
        };
        return this.mainService.getAllByStyle(dbStyleName, sessionUser.getAddress());
    }
    @RequestMapping(value = "/six-shops", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ShopInfoEntity[] getSixShops(@SessionAttribute(value = "sessionUser",required = false)RegisterEntity sessionUser) {
        return this.mainService.getSixShop(sessionUser.getAddress());
    }
    @RequestMapping(value = "/all-shops", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ShopInfoEntity[] getAllShops(@SessionAttribute(value = "sessionUser",required = false)RegisterEntity sessionUser) {
        /*return this.*/
        return this.mainService.getAllShop(sessionUser.getAddress());
    }
}
