package dev.gmpark.cors.controllers;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.services.OwnerMainService; // OwnerMainService 사용
import dev.gmpark.cors.vos.ReservationItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Model 추가
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/")
public class OwnerMainController {

    private final OwnerMainService ownerMainService; // 서비스 이름 변경

    @Autowired
    public OwnerMainController(OwnerMainService ownerMainService) {
        this.ownerMainService = ownerMainService;
    }

    @RequestMapping(value = "/owner", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getOwnerMain(
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
            Model model
    ) {
        // 1. 로그인 검증
        if (sessionUser == null) {
            return "redirect:/login";
        }
        if (!"owner".equalsIgnoreCase(sessionUser.getUsertype())) {
            return "redirect:/main";
        }

        // 2. 서비스 호출: DB에 저장된 매장 정보가 있는지 확인
        ShopInfoEntity shopInfo = ownerMainService.getShopByEmail(sessionUser.getEmail());

        // 3. [핵심] 등록된 매장이 없으면(null), 회원가입 정보(User)를 기본값으로 채워줌
        if (shopInfo == null) {
            shopInfo = new ShopInfoEntity();

            // 가게 이름 -> '박규민님의 매장' (가입 이름 + 텍스트)
            shopInfo.setShopName(sessionUser.getStoreName());

            // 주소 -> 회원가입 시 입력한 주소
            shopInfo.setShopAddress(sessionUser.getAddress());

            // 나머지는 빈 값이나 안내 문구 채우기
            shopInfo.setShopTime("09:00 ~ 22:00");
            shopInfo.setShopCategory("카테고리 선택 ex: 미니멀");
            shopInfo.setShopTel(sessionUser.getPhone());

            // 나중에 저장할 때를 대비해 FK(userEmail) 미리 세팅
            shopInfo.setUserEmail(sessionUser.getEmail());
        }

        // 4. HTML로 데이터 전달 (이름은 'shop')
        model.addAttribute("shop", shopInfo);
        if (shopInfo.getShopId() > 0) {
            List<ReservationItemVo> reservations = ownerMainService.getReservationsByShopId(shopInfo.getShopId());
            model.addAttribute("reservations", reservations);
        } else {
            model.addAttribute("reservations", List.of()); // 빈 리스트 전달
        }
        model.addAttribute("orders", this.ownerMainService.getOrdersByShopId(shopInfo.getShopId()));

        return "ownermain/ownermain";
    }

    // ... (로그인 상태 확인 API 등 나머지 메서드는 그대로 유지) ...
    @RequestMapping(value = "/owner/login-status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getLoginStatus(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        response.put("isLoggedIn", sessionUser != null);
        return response;
    }
    // [추가] 정보 수정/등록 요청 처리 (POST)
    @RequestMapping(value = "/owner/post-info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postInfo(
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
            ShopInfoEntity shopInfo
    ) {
        Map<String, Object> response = new HashMap<>();

        // 1. 로그인 체크
        if (sessionUser == null || !"owner".equalsIgnoreCase(sessionUser.getUsertype())) {
            response.put("result", "FAILURE");
            return response;
        }
        if( sessionUser.getLevel() == 2 || sessionUser.getLevel() == 1) {
            response.put("result", "NO_AUTH");
            return response;
        }
        // 2. 서비스 호출 (저장 로직)
        String result = ownerMainService.saveShopInfo(sessionUser, shopInfo);

        response.put("result", result);
        return response;
    }
    @RequestMapping(value = "/owner/all-items", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getAllItems(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {

        Map<String, Object> response = new HashMap<>();

        // 1. 로그인 체크
        if (sessionUser == null) {
            response.put("result", "FAILURE");
            return response;
        }

        // 2. 서비스 호출 (User 객체를 넘김)
        // Service가 알아서 매장 ID 찾고 상품 가져옴
        ShopItemEntity[] items = this.ownerMainService.getItemsByUser(sessionUser);

        // 3. 결과 리턴
        response.put("result", "SUCCESS");
        response.put("items", items);

        return response;
    }
    @RequestMapping(value = "/owner/patch-item", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> patchItem( ShopItemEntity shopItem,
                                          @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser){
        Map<String, Object> response = new HashMap<>();
        if( sessionUser.getLevel() == 1) {
            response.put("result", "NO_AUTH");
            return response;
        }
        CommonResult result = this.ownerMainService.modify(shopItem);
        response.put("result", result.name());
        return response;

        /*레벨이 2거나1인경우에는 권한없음으로  ownermain.js에 로직짜기 */
    }
    @RequestMapping(value = "/owner/delete-item", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String,Object> deleteItem(ShopItemEntity shopItem,
                                         @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser){
        Map<String,Object> response = new HashMap<>();
        if( sessionUser.getLevel() == 2 || sessionUser.getLevel() == 1) {
            response.put("result", "NO_AUTH");
            return response;
        }
        CommonResult result = this.ownerMainService.delete(shopItem.getId());

        response.put("result",result.name() );
        return response;
        /*레벨이 2거나1인경우에는 권한없음으로  ownermain.js에 로직짜기 */
    }
    @RequestMapping(value = "/owner/patch-reservation", method = RequestMethod.PATCH,produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String,Object> patchReservation(@RequestParam(value = "reservationId", required = false, defaultValue = "0") int reservationId,
                                               @RequestParam(value = "status", required = false) String status,
                                               @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser){
        Map<String,Object> response = new HashMap<>();
        if( sessionUser.getLevel() == 1) {
            response.put("result", "NO_AUTH");
            return response;
        }
        CommonResult result = this.ownerMainService.updateReservationStatus(reservationId, status);

        response.put("result", result.name());
        return response;
        /*레벨이 1인경우에만 권한없음으로  ownermain.js에 로직짜기 */

    }



}