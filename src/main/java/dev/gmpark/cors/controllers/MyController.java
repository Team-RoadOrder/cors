package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.services.MyService;
import dev.gmpark.cors.vos.OrderHistoryVo;
import dev.gmpark.cors.vos.ReservationItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class MyController {
    private final MyService myService;
    @RequestMapping(value = "/my", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getMy(ModelAndView modelAndView,@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        modelAndView.setViewName("my/my");
        if(sessionUser == null) {
            modelAndView.setViewName("redirect:/login");
        }

        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/owner");
            return modelAndView;
        }
        modelAndView.addObject("sessionUser", sessionUser);
        return modelAndView;
    }
    @RequestMapping(value = "/my/tab", method = RequestMethod.GET)
    public ModelAndView getTabContent(ModelAndView modelAndView,
                                      @RequestParam(value = "menu") String menu,
                                      @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {

        // 1. 공통 데이터 (유저 정보 등)
        modelAndView.addObject("sessionUser", sessionUser);

        // 2. 탭 이름(menu)에 따라 서로 다른 데이터를 DB에서 조회
        if (sessionUser != null) {
            switch (menu) {
                case "home":
                    List<ReservationItemVo> allItems = myService.getAllReservations(sessionUser);
                    List<ReservationItemVo> homeItems = allItems.stream()
                            .filter(item -> !"완료".equals(item.getStatus()) && !"취소".equals(item.getStatus())) // 완료도 아니고, 취소도 아닌 것
                            .limit(4)
                            .collect(Collectors.toList());
                    OrderHistoryVo[] homeOrders = this.myService.getOrderHistory(sessionUser); // 변수명 변경
                    modelAndView.addObject("homeOrders", homeOrders);
                    modelAndView.addObject("items", homeItems);
                    break;

                case "orders":
                    OrderHistoryVo[] orders = this.myService.getOrderHistory(sessionUser);
                    modelAndView.addObject("orders", orders);
                    break;

                case "reservation":
                    List<ReservationItemVo> items = this.myService.getAllReservations(sessionUser);
                    modelAndView.addObject("items",items );
                    long waitCount = 0;
                    long confirmCount = 0;
                    long endCount = 0;

                    if (items != null) {
                        waitCount = items.stream().filter(i -> "대기".equals(i.getStatus())).count();
                        confirmCount = items.stream().filter(i -> "확정".equals(i.getStatus())).count();
                        // 종료는 '완료' 또는 '취소'인 경우
                        endCount = items.stream().filter(i -> "완료".equals(i.getStatus()) || "취소".equals(i.getStatus())).count();
                    }
                    modelAndView.addObject("waitCount", waitCount);
                    modelAndView.addObject("confirmCount", confirmCount);
                    modelAndView.addObject("endCount", endCount);
                    break;

                case "likes-shop":
                    modelAndView.addObject("likeShops", this.myService.getLikeShops(sessionUser));
                    break;

                case "likes-item":
                    modelAndView.addObject("likeItems", this.myService.getLikeItems(sessionUser));
                    break;

                case "profile":
                    // [프로필] 탭일 때 -> 상세 회원 정보 (필요하다면)
                    // RegisterEntity userDetail = userService.getUserDetail(sessionUser.getEmail());
                    // modelAndView.addObject("userDetail", userDetail);
                    break;

                case "address":
                    // [배송지] 탭일 때 -> 주소 목록 조회
                    // List<AddressDTO> addressList = userService.getAddressList(sessionUser.getEmail());
                    // modelAndView.addObject("addressList", addressList);
                    break;

                // custom 등 나머지 케이스...
            }
        }

        // 3. 해당 프래그먼트만 반환
        modelAndView.setViewName("fragments/myfragments :: " + menu);

        return modelAndView;
    }

    @RequestMapping(value = "/my/name", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> patchMyName(@RequestParam("name") String newName,
                           @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.myService.updateUserName(sessionUser, newName);
        response.put("result", result.name());
        return response;
    }
    @RequestMapping(value = "/my/phone", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> patchMyPhone(@RequestParam("phone") String newPhone,
                                           @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.myService.updateUserPhone(sessionUser, newPhone);
        response.put("result", result.name());
        return response;
    }
    @RequestMapping(value = "/my/address", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> patchMyAddress(@RequestParam("address") String newAddress,
                                            @RequestParam("addressDetail") String newAddressDetail,
                                            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.myService.updateUserAddress(sessionUser, newAddress, newAddressDetail);
        response.put("result", result.name());
        return response;
    }
    @RequestMapping(value = "/my/delete-user", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> deleteMyUser(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.myService.deleteUser(sessionUser);
        response.put("result", result.name());
        return response;
    }
}
