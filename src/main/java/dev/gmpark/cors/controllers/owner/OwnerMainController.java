package dev.gmpark.cors.controllers.owner;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.results.CommonResult;
import dev.gmpark.cors.results.ReservationResult;
import dev.gmpark.cors.services.MyService;
import dev.gmpark.cors.services.OrderService;
import dev.gmpark.cors.services.OwnerMainService;
import dev.gmpark.cors.vos.OrderHistoryVo;
import dev.gmpark.cors.vos.ReservationItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class OwnerMainController {

    private final OwnerMainService ownerMainService; // 서비스 이름 변경
    private final MyService myService;
    private final OrderService orderService;


    @RequestMapping(value = "/owner", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getOwnerMain(
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
            Model model
    ) {
        if (sessionUser == null) {
            return "redirect:/login";
        }
        if (!"owner".equalsIgnoreCase(sessionUser.getUsertype())) {
            return "redirect:/main";
        }
        ShopInfoEntity shopInfo = ownerMainService.getShopByEmail(sessionUser.getEmail());

        if (shopInfo == null) {
            shopInfo = new ShopInfoEntity();

            shopInfo.setShopName(sessionUser.getStoreName());

            shopInfo.setShopAddress(sessionUser.getAddress());

            shopInfo.setShopTime("09:00 ~ 22:00");

            shopInfo.setShopCategory("");

            shopInfo.setShopTel(sessionUser.getPhone());

            shopInfo.setUserEmail(sessionUser.getEmail());
        }

        model.addAttribute("shop", shopInfo);
        if (shopInfo.getShopId() > 0) {
            List<ReservationItemVo> reservations = ownerMainService.getReservationsByShopId(shopInfo.getShopId());
            model.addAttribute("reservations", reservations);
        } else {
            model.addAttribute("reservations", List.of()); // 빈 리스트 전달
        }
        OrderHistoryVo[] allOrders = ownerMainService.getOrdersByShopId(shopInfo.getShopId());

        List<OrderHistoryVo> pendingOrders = new ArrayList<>();
        if (allOrders != null) {
            for (OrderHistoryVo order : allOrders) {
                if (order.getStatus() == 0 || order.getStatus() == 2 || order.getStatus() == 6 ) {
                    pendingOrders.add(order);
                }
            }
        }

        model.addAttribute("orders", pendingOrders);
        return "ownermain/ownermain";
    }

    @RequestMapping(value = "/owner/login-status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getLoginStatus(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        response.put("isLoggedIn", sessionUser != null);
        return response;
    }
    @RequestMapping(value = "/owner/post-info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postInfo(
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
            ShopInfoEntity shopInfo
    ) {
        Map<String, Object> response = new HashMap<>();

        if (sessionUser == null || !"owner".equalsIgnoreCase(sessionUser.getUsertype())) {
            response.put("result", "FAILURE");
            return response;
        }
        if( sessionUser.getLevel() == 2 || sessionUser.getLevel() == 1) {
            response.put("result", "NO_AUTH");
            return response;
        }
        String result = ownerMainService.saveShopInfo(sessionUser, shopInfo);

        response.put("result", result);
        return response;
    }
    @RequestMapping(value = "/owner/all-items", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getAllItems(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {

        Map<String, Object> response = new HashMap<>();

        if (sessionUser == null) {
            response.put("result", "FAILURE");
            return response;
        }

        ShopItemEntity[] items = this.ownerMainService.getItemsByUser(sessionUser);

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
        CommonResult result = this.ownerMainService.modify(sessionUser,shopItem);
        response.put("result", result.name());
        return response;

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
        CommonResult result = this.ownerMainService.delete(sessionUser,shopItem.getId());

        response.put("result",result.name() );
        return response;
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
        ReservationResult result = this.ownerMainService.updateReservationStatus(sessionUser,reservationId, status);

        response.put("result", result.name());
        return response;

    }
    @RequestMapping(value = "/owner/delete-shop", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> deleteShop(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        if (sessionUser == null || !"owner".equalsIgnoreCase(sessionUser.getUsertype())) {
            response.put("result", "FAILURE");
            return response;
        }
        if( sessionUser.getLevel() == 2 || sessionUser.getLevel() == 1) {
            response.put("result", "NO_AUTH");
            return response;
        }
        CommonResult result = this.myService.deleteUser(sessionUser);
        response.put("result", result.name());

        return response;
    }
    @RequestMapping(value = "/owner/patch-order-status", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> patchOrderItemStatus(@RequestParam(value = "id") Long id, @RequestParam(value = "status") Integer status, @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        if( (sessionUser.getLevel() == 1 && status == 3) ||(sessionUser.getLevel() == 2 && status == 3) ) {
            response.put("result", "NO_AUTH");
            return response;
        }
        CommonResult result = this.orderService.updateOrderItem(id, status);
        response.put("result", result.name());
        return response;
    }


    @RequestMapping(value = "/owner/refund", method = RequestMethod.PATCH, produces =  MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String,Object> refundItem(@RequestParam(value = "id") Long id,
                                         @RequestParam(value = "status") Integer status,
                                         @RequestParam(value = "refundReason") String refundReason) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.orderService.updateOrderItemAndRefundReason(id, status, refundReason);
        response.put("result", result.name());
        return response;
    }
    @RequestMapping(value = "/delivery", method = RequestMethod.PATCH , produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String,Object> updateDelivery(@RequestParam(value = "id") Long id,
                                          @RequestParam(value = "courier") String courier,
                                          @RequestParam(value = "trackingNumber") String trackingNumber,
                                          @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        ShopInfoEntity shopInfo = ownerMainService.getShopByEmail(sessionUser.getEmail());
        if (shopInfo == null) {
            response.put("result", "FAILURE");
            return response;
        }
        CommonResult result = this.orderService.updateDelivery(id, courier, trackingNumber,shopInfo.getShopId());
        response.put("result", result.name());
        return response;

    }


}