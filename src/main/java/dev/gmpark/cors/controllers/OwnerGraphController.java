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
        if (!"owner".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/main");
            return modelAndView;
        }
        if ( sessionUser.getLevel()==2 || sessionUser.getLevel() == 1) {
            modelAndView.setViewName("redirect:/owner?alert=noauth"); // 예시
            return modelAndView;
        }

        ShopInfoEntity shopInfo = ownerMainService.getShopByEmail(sessionUser.getEmail());
        if (shopInfo == null) {
            // 샵 정보가 없으면 적절한 처리 (예: 등록 페이지로 리다이렉트)
            modelAndView.setViewName("redirect:/owner?alert=nograph");
            return modelAndView;
        }

        modelAndView.setViewName("ownergraph/ownergraph");
        modelAndView.addObject("shop", shopInfo);


        // authorizedId: 현재 로그인한 사용자가 정당하게 접근 가능한 매장의 식별자
        int requestedId = shopInfo.getShopId();//실제로 DB에서 조회하고자 하는 매장의 식별자
        Integer authorizedId = shopInfo.getShopId();//현재 로그인한 사용자가 정당하게 접근 가능한 매장의 식별자



        //금주 매출 현황 및 증감 지표 분석
        SalesStatusVo status = ownerGraphService.getWeeklyStatus(requestedId, authorizedId);

        //해당 매장의 최근 결제 내역 (최신순)
        List<PaymentListVo> payments = ownerGraphService.getRecentPayments(requestedId, authorizedId);

        // 매출 상위 5개 제품 리스트 및 순위
        List<TopProductVo> topProducts = ownerGraphService.getTopProducts(requestedId, authorizedId);

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
        int targetId = shopInfo.getShopId();//조회 대상(targetId)
        Integer authId = shopInfo.getShopId();//보안 검증용(authId) ID

        // 3. 서비스 호출 시 두 식별자를 함께 전달하여 '본인 매장 여부' 검증한 후 데이터를 반환
        return ownerGraphService.getDailySales(authId,targetId);
    }

    @RequestMapping(value = "/graph/data/monthly", method = RequestMethod.GET)
    @ResponseBody
    public List<SalesGraphVo> getMonthlyGraphData(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser) {
        ShopInfoEntity shopInfo = ownerMainService.getShopByEmail(sessionUser.getEmail());
        int targetShopId = shopInfo.getShopId();          // SQL 조회용 ID
        Integer authorizedShopId = shopInfo.getShopId(); // 보안 검증용 ID

        // 3. 두 인자를 모두 전달하여 서비스 레이어의 보안 로직 통과
        return ownerGraphService.getMonthlySales(targetShopId, authorizedShopId);
    }
}
