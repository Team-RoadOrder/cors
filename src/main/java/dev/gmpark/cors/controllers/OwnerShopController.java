package dev.gmpark.cors.controllers;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.results.CommonResult;
import dev.gmpark.cors.services.OwnerMainService;
import dev.gmpark.cors.services.OwnerShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/owner")
@RequiredArgsConstructor
public class OwnerShopController {

    private final OwnerShopService ownerShopService; // 서비스 연결
    private final OwnerMainService ownerMainService;

    @RequestMapping(value = "/shop", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getOwnerShop(
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
            Model model
    ) throws IOException {


        if (sessionUser == null) {
            return "redirect:/login";
        }
        if (!"owner".equalsIgnoreCase(sessionUser.getUsertype())) {
            return "redirect:/main";
        }
        ShopInfoEntity shopInfo = ownerMainService.getShopByEmail(sessionUser.getEmail());
        if (shopInfo == null) {

            return "redirect:/owner?alert=noshop";
        }
        if ( sessionUser.getLevel()==2 || sessionUser.getLevel() == 1) {
            return "redirect:/owner?alert=noauth";
        }

        model.addAttribute("shop", shopInfo);
        return "ownershop/ownershop";


    }

    @RequestMapping(value = "/shop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public  Map<String, Object>  postOwnerShop(
            ShopItemEntity shopItem,
            @RequestParam(value = "images", required = false) MultipartFile[] images, // 이미지 배열 받기,
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser
    ) {
        ShopInfoEntity shopInfo = ownerMainService.getShopByEmail(sessionUser.getEmail());
        shopItem.setShopId(shopInfo.getShopId());

        CommonResult result = ownerShopService.registerShopItem(shopItem, images);
        Map<String, Object> response = new HashMap<>();
        response.put("result", result.name());
        return response;
    }
    @RequestMapping(value = "/owner/login-status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getLoginStatus(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        response.put("isLoggedIn", sessionUser != null);
        return response;
    }
}