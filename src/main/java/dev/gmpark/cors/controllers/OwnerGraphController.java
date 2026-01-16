package dev.gmpark.cors.controllers;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.services.OwnerGraphService;
import dev.gmpark.cors.services.OwnerMainService;
import dev.gmpark.cors.vos.PaymentListVo;
import dev.gmpark.cors.vos.SalesGraphVo;
import dev.gmpark.cors.vos.SalesStatusVo;
import dev.gmpark.cors.vos.TopProductVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
@RequestMapping(value = "/owner")
@RequiredArgsConstructor
public class OwnerGraphController {
    private final OwnerMainService ownerMainService;
    private final OwnerGraphService ownerGraphService;

    @RequestMapping(value = "/graph", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getOwnerGraph(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser, ModelAndView modelAndView) {
        if (sessionUser == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }

        ShopInfoEntity shopInfo = ownerMainService.getShopByEmail(sessionUser.getEmail());
        if (shopInfo == null) {
            // 샵 정보가 없으면 적절한 처리 (예: 등록 페이지로 리다이렉트)
            modelAndView.setViewName("redirect:/owner/shop"); // 예시
            return modelAndView;
        }

        modelAndView.setViewName("ownergraph/ownergraph");
        modelAndView.addObject("shop", shopInfo);

        // 초기 데이터 로드 (SSR)
        SalesStatusVo status = ownerGraphService.getWeeklyStatus(shopInfo.getShopId());
        List<PaymentListVo> payments = ownerGraphService.getRecentPayments(shopInfo.getShopId());
        List<TopProductVo> topProducts = ownerGraphService.getTopProducts(shopInfo.getShopId());

        modelAndView.addObject("status", status);
        modelAndView.addObject("payments", payments);
        modelAndView.addObject("topProducts", topProducts);

        return modelAndView;
    }

    // 그래프 데이터 API (AJAX 호출용)
    @RequestMapping(value = "/graph/data/daily", method = RequestMethod.GET)
    @ResponseBody
    public List<SalesGraphVo> getDailyGraphData(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser) {
        ShopInfoEntity shopInfo = ownerMainService.getShopByEmail(sessionUser.getEmail());
        return ownerGraphService.getDailySales(shopInfo.getShopId());
    }

    @RequestMapping(value = "/graph/data/monthly", method = RequestMethod.GET)
    @ResponseBody
    public List<SalesGraphVo> getMonthlyGraphData(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser) {
        ShopInfoEntity shopInfo = ownerMainService.getShopByEmail(sessionUser.getEmail());
        return ownerGraphService.getMonthlySales(shopInfo.getShopId());
    }
}
