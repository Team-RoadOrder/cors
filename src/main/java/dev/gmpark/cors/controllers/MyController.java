package dev.gmpark.cors.controllers;


import dev.gmpark.cors.entities.RegisterEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/")
public class MyController {

    @RequestMapping(value = "/my", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getMy(ModelAndView modelAndView,@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        modelAndView.setViewName("my/my");
        if(sessionUser == null) {
            modelAndView.setViewName("redirect:/login");
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
                    // 홈 탭을 다시 눌렀을 때 갱신이 필요하다면 여기서 로직 수행
                    // modelAndView.addObject("summary", myPageService.getSummary(sessionUser));
                    break;

                case "orders":
                    // [구매 내역] 탭일 때 -> 구매 리스트 조회
                    // List<OrderDTO> orderList = orderService.findOrdersByUser(sessionUser.getEmail());
                    // modelAndView.addObject("orderList", orderList);
                    break;

                case "reservation":
                    // [예약/픽업] 탭일 때 -> 예약 리스트 조회
                    // List<ReservationDTO> resList = orderService.findReservations(sessionUser.getEmail());
                    // modelAndView.addObject("resList", resList);
                    break;

                case "likes-shop":
                    // [관심 매장] 탭일 때 -> 매장 리스트 조회
                    // List<ShopDTO> likeShops = shopService.findLikeShops(sessionUser.getEmail());
                    // modelAndView.addObject("likeShops", likeShops);
                    break;

                case "likes-item":
                    // [관심 상품] 탭일 때 -> 상품 리스트 조회
                    // List<ItemDTO> likeItems = itemService.findLikeItems(sessionUser.getEmail());
                    // modelAndView.addObject("likeItems", likeItems);
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
}
