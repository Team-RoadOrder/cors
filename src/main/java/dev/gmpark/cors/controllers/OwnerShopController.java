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
@RequiredArgsConstructor // [중요] final이 붙은 필드(Service)를 자동으로 주입해줍니다.
public class OwnerShopController {

    private final OwnerShopService ownerShopService; // 서비스 연결
    private final OwnerMainService ownerMainService;

    @RequestMapping(value = "/shop", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getOwnerShop(
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
            Model model
    ) throws IOException {

        // 1. 로그인 검증
        if (sessionUser == null) {
            return "redirect:/login";
        }
        if (!"owner".equalsIgnoreCase(sessionUser.getUsertype())) {
            return "redirect:/main";
        }
        ShopInfoEntity shopInfo = ownerMainService.getShopByEmail(sessionUser.getEmail());
        if (shopInfo == null) {
            // URL 뒤에 파라미터를 붙여서 리다이렉트
            return "redirect:/owner?alert=noshop";
        }
        if ( sessionUser.getLevel()==2 || sessionUser.getLevel() == 1) {
            return "redirect:/owner?alert=noauth";
        }
        /*레벨이 2거나1인경우에는 권한없음으로  ownermain.js에 로직짜기 */
        model.addAttribute("shop", shopInfo);
        return "ownershop/ownershop";


    }

    // [수정] POST 요청 처리 부분
    @RequestMapping(value = "/shop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody // [중요] HTML 페이지 이동이 아니라, 결과 데이터(JSON 문자열)만 리턴한다는 뜻
    public  Map<String, Object>  postOwnerShop(
            ShopItemEntity shopItem, // HTML form의 name 속성과 Entity 필드명이 같으면 자동 매핑
            @RequestParam(value = "images", required = false) MultipartFile[] images, // 이미지 배열 받기,
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser
    ) {
        ShopInfoEntity shopInfo = ownerMainService.getShopByEmail(sessionUser.getEmail());
        shopItem.setShopId(shopInfo.getShopId());
        // 서비스에게 일 시키기 (DB 저장 + 파일 저장)
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