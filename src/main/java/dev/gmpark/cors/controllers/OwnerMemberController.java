package dev.gmpark.cors.controllers;

import dev.gmpark.cors.entities.OwnerMemberEntity;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.mappers.OwnerMemberMapper;
import dev.gmpark.cors.results.register.RegisterResult;
import dev.gmpark.cors.services.OwnerMainService;
import dev.gmpark.cors.services.OwnerMemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/owner")
@RequiredArgsConstructor
public class OwnerMemberController {
    private final OwnerMainService ownerMainService;
    private final OwnerMemberService ownerMemberService;
    private final OwnerMemberMapper ownerMemberMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 사원 관리 페이지 (목록 조회)
     */
//    @RequestMapping(value = "/member", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    public ModelAndView getMember(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
//                                  @RequestParam(value = "level", required = false, defaultValue = "0") int filterLevel,
//                                  @RequestParam(value = "keyword", required = false) String keyword,
//                                  ModelAndView modelAndView) {
//        //TODO
//        // 1. 이메일이 아니라 세션 유저가 가진 shopId를 먼저 확보
//        int currentShopId = (int) sessionUser.;
//
//        // TODO 2. 이메일 대신 shopId로 매장 정보를 조회하도록 변경
//        // (ownerMainService에 해당 메서드가 없다면 추가해야 합니다)
//        ShopInfoEntity shopInfo = this.ownerMainService.;
//
//        if (shopInfo != null) {
//            // 3. 확보된 shopId를 사용하여 임직원 목록 조회
//            OwnerMemberEntity loginUser = OwnerMemberEntity.builder()
//                    .email(sessionUser.getEmail())
//                    .level(sessionUser.getLevel())
//                    .build();
//
//            List<OwnerMemberEntity> members = this.ownerMemberService.getMembers(loginUser, filterLevel, keyword);
//            modelAndView.addObject("shop", shopInfo);
//            modelAndView.addObject("members", members);
//        }
//
//        modelAndView.setViewName("ownermember/ownermember");
//        return modelAndView;
//    }


    /**
     * 신규 사원 등록 API
     */
    @RequestMapping(value = "/member", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postMember(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser,
                                          OwnerMemberEntity newMember) {

        ShopInfoEntity shop = this.ownerMainService.getShopByEmail(sessionUser.getEmail());

        // [해결] sessionUser.getShopId() 대신 조회된 shop 객체에서 ID 추출
        Long currentShopId = (shop != null) ? (long) shop.getShopId() : null;

        OwnerMemberEntity loginUser = OwnerMemberEntity.builder()
                .email(sessionUser.getEmail())
                .shopId(currentShopId)
                .storeName(sessionUser.getStoreName())
                .level(sessionUser.getLevel())
                .build();

        RegisterResult result = this.ownerMemberService.addMember(loginUser, newMember);

        Map<String, Object> response = new HashMap<>();
        response.put("result", result.name().toLowerCase());
        response.put("status", result.name().toUpperCase());
        return response;
    }

    /**
     * 사원 정보 수정 및 권한 위임 API
     */
    @RequestMapping(value = "/member", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> patchMember(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser,
                                           OwnerMemberEntity targetMember,
                                           @RequestParam(value = "currentPassword", required = false) String currentPassword) {

        ShopInfoEntity shop = this.ownerMainService.getShopByEmail(sessionUser.getEmail());
        Long currentShopId = (shop != null) ? (long) shop.getShopId() : null;

        OwnerMemberEntity loginUser = OwnerMemberEntity.builder()
                .email(sessionUser.getEmail())
                .shopId(currentShopId)
                .storeName(sessionUser.getStoreName())
                .level(sessionUser.getLevel())
                .build();

        RegisterResult result = this.ownerMemberService.modifyMember(loginUser, targetMember, currentPassword);

        Map<String, Object> response = new HashMap<>();
        response.put("result", result.name().toLowerCase());
        response.put("status", result.name().toUpperCase());
        return response;
    }

    /**
     * 사원 삭제 API
     */
    @RequestMapping(value = "/member", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> deleteMember(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser,
                                            @RequestParam(value = "email") String targetEmail) {

        ShopInfoEntity shop = this.ownerMainService.getShopByEmail(sessionUser.getEmail());
        Long currentShopId = (shop != null) ? (long) shop.getShopId() : null;

        OwnerMemberEntity loginUser = OwnerMemberEntity.builder()
                .email(sessionUser.getEmail())
                .shopId(currentShopId)
                .storeName(sessionUser.getStoreName())
                .level(sessionUser.getLevel())
                .build();

        RegisterResult result = this.ownerMemberService.removeMember(loginUser, targetEmail);

        Map<String, Object> response = new HashMap<>();
        response.put("result", result.name().toLowerCase());
        response.put("status", result.name().toUpperCase());
        return response;
    }

    /**
     * 로그아웃 처리
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String getLogout(HttpSession session) {
        RegisterEntity sessionUser = (RegisterEntity) session.getAttribute("sessionUser");
        if (sessionUser != null) {
            this.ownerMemberService.updateLogoutTime(sessionUser.getEmail());
        }
        session.invalidate();
        return "redirect:/login";
    }
}