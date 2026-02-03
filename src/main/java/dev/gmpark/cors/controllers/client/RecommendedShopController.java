package dev.gmpark.cors.controllers.client;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.services.RecommendedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class RecommendedShopController {
    private final RecommendedService recommendedService;

    @RequestMapping(value = "/recommended-shops", method = RequestMethod.GET)
    public ModelAndView getRecommendedShops(ModelAndView modelAndView,
                                            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        if (sessionUser == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }
        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/owner");
            return modelAndView;
        }
        
        // [추가] 선호 스타일이 없는 경우 마이페이지로 리다이렉트하며 오류 메시지 전달
        if (sessionUser.getStyle() == null || sessionUser.getStyle().trim().isEmpty()) {
            modelAndView.addObject("errorMessage", "선호 스타일을 먼저 설정해주세요.");
            modelAndView.setViewName("redirect:/my"); // 또는 스타일 설정 페이지로 이동
            return modelAndView;
        }
        
        // 서비스에서 데이터 가져오기
        Map<String, Object> recommendedData = this.recommendedService.getRecommendedData(sessionUser);
        
        modelAndView.addAllObjects(recommendedData);
        modelAndView.setViewName("recommendedshop/recommendedshop");
        return modelAndView;
    }
}
