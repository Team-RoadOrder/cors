package dev.gmpark.cors.controllers.client;


import dev.gmpark.cors.dtos.ReservationDto;
import dev.gmpark.cors.entities.*;
import dev.gmpark.cors.results.CommonResult;
import dev.gmpark.cors.results.ReservationResult;
import dev.gmpark.cors.services.ReservationService;
import dev.gmpark.cors.services.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/reservation")
@RequiredArgsConstructor
public class ReservationController {
    private final ShopService shopService;
    private final ReservationService reservationService;

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getReservation(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,ModelAndView modelAndView, @RequestParam(value = "shopId", required = false, defaultValue = "0") int shopId) {
        if( sessionUser == null ) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }

        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/owner");
            return modelAndView;
        }
        ShopInfoEntity shopInfo = this.shopService.getShopInfo(shopId);
        modelAndView.setViewName("reservation/reservation");
        modelAndView.addObject("shopInfo", shopInfo);
          return modelAndView;
        };
    @RequestMapping(value = "/all-items", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getAllItems(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,ShopInfoEntity shopInfo) {
        Map<String, Object> response = new HashMap<>();
        if (sessionUser == null) {
            response.put("result", "FAILURE_SESSION");
            return response;
        }
        ShopItemEntity[] items = this.reservationService.getItemsByShopId(shopInfo);

        response.put("result", "SUCCESS");
        response.put("items", items);
        return response;
    }
    @RequestMapping(value = "/post-items", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postItems (
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
            @RequestBody ReservationDto reservationDto) { // DTO로 한 번에 받음

        Map<String, Object> response = new HashMap<>();
        if (sessionUser == null) {
            response.put("result", "FAILURE_SESSION");
            return response;
        }
        ReservationResult result = this.reservationService.registerReservation(reservationDto, sessionUser.getEmail());

        response.put("result", result.name());
        return response;
    }
    @RequestMapping(value = "/delete", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String,Object> deleteReservation(
            @RequestParam(value = "reservationId") int reservationId){
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.reservationService.deleteReservation(reservationId);
        response.put("result", result.name());
        return response;
    }

}
