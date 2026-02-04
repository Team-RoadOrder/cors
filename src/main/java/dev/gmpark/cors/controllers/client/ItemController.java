package dev.gmpark.cors.controllers.client;

import dev.gmpark.cors.dtos.ReviewStatsDto;
import dev.gmpark.cors.entities.*;
import dev.gmpark.cors.results.CommonResult;
import dev.gmpark.cors.services.*;
import dev.gmpark.cors.vos.ReviewReportVo;
import dev.gmpark.cors.vos.LikeItemVo;
import dev.gmpark.cors.vos.ReviewVo;
import dev.gmpark.cors.vos.ShopItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/item")
@RequiredArgsConstructor
public class ItemController {
    private final ShopService shopService;
    private final ItemService itemService;
    private final MyService myService;
    private final ReviewService reviewService;

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getItem (ModelAndView modelAndView,
                                 @RequestParam(value = "shopId", required = false, defaultValue = "0") int shopId,
                                 @RequestParam(value = "id") Long id,
                                 @RequestParam(value = "sort", required = false, defaultValue = "latest") String sort,
                                 @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser){

        // [보안 정규화] 세션 만료 및 로그인 유무 선행 확인
        if (sessionUser == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }

        // [보안 정규화] 고객(customer) 전용 페이지 접근 제어
        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/main");
            return modelAndView;
        }
        LikeItemVo[] likeItems = this.myService.getLikeItems(sessionUser);
        boolean isLikeItem = false;

        if (likeItems != null && likeItems.length > 0) {
            isLikeItem = Arrays.stream(likeItems)
                    .anyMatch(vo -> vo.getId() == id); // .getId()로 변경
        }
        ShopItemVo item = this.itemService.getItemById(id);
        if (shopId == 0 && item != null) {
            shopId = item.getShopId();
        }

        ShopInfoEntity shopInfo = this.shopService.getShopInfo(shopId);
        int likeCount = this.shopService.getShopLikeCount(shopId);
        ShopInfoEntity[] likeShops = this.myService.getLikeShops(sessionUser);

        boolean isLiked = false;
        if (likeShops != null) {
            final int currentShopId = shopId;
            isLiked = Arrays.stream(likeShops).anyMatch(shop -> shop.getShopId() == currentShopId);
        }

        // [데이터 정규화] 관련 상품, 리뷰 리스트, 평점 통계, 이미지 그리드 통합 조회
        List<ShopItemVo> relatedItems = this.itemService.getRelatedItems(id);
        List<ReviewVo> reviews = this.reviewService.getReviews(id, sort);
        ReviewStatsDto stats = this.reviewService.getReviewStats(id);
        List<String> reviewImages = this.reviewService.getAllImages(id);


        Integer availableReservationId = this.reviewService.getAvailableReservationId(sessionUser.getEmail(), id);
        modelAndView.addObject("isLikeItem", isLikeItem);
        modelAndView.addObject("sessionUser", sessionUser);
        modelAndView.addObject("shopInfo", shopInfo);
        modelAndView.addObject("item", item);
        modelAndView.addObject("likeShops", likeShops);
        modelAndView.addObject("isLiked", isLiked);
        modelAndView.addObject("likeCount", likeCount);
        modelAndView.addObject("relatedItems", relatedItems);
        modelAndView.addObject("reviews", reviews);
        modelAndView.addObject("reviewImages", reviewImages);
        modelAndView.addObject("availableReservationId", availableReservationId);
        modelAndView.addObject("stats", stats);

        modelAndView.setViewName("item/item");
        return modelAndView;
    }


    @RequestMapping(value = "/reviews", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ReviewVo> getReviewsApi(@RequestParam(value = "itemId") Long itemId,
                                        @RequestParam(value = "sort", defaultValue = "latest") String sort) {
        return this.reviewService.getReviews(itemId, sort);
    }


    @RequestMapping(value = "/review", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postReview(ItemReviewEntity review,
                                          @RequestParam(value = "images", required = false) MultipartFile[] images,
                                          @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        // 서비스 레이어에서 실구매 여부 및 1구매-1리뷰 정책을 최종 검증함
        CommonResult result = this.reviewService.writeReview(review, images, sessionUser);
        response.put("result", result.name());
        return response;
    }


    @RequestMapping(value = "/review/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ReviewVo getReview(@PathVariable(value = "id") Long id) {
        return this.reviewService.getReviewById(id);
    }


    @RequestMapping(value = "/review/{id}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> patchReview(@PathVariable(value = "id") Long id,
                                           @RequestParam(value = "content") String content,
                                           @RequestParam(value = "rating") int rating,
                                           @RequestParam(value = "images", required = false) MultipartFile[] images,
                                           @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();

        CommonResult result = this.reviewService.modifyReview(id, content, rating, images, sessionUser);

        response.put("result", result.name());
        return response;
    }


    @RequestMapping(value = "/review/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> deleteReview(@PathVariable(value = "id") Long id,
                                            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.reviewService.deleteReview(id, sessionUser);
        response.put("result", result.name());
        return response;
    }


    @RequestMapping(value = "/review/like", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postReviewLike(@RequestParam(value = "reviewId") Long reviewId,
                                              @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.reviewService.toggleReviewLike(reviewId, sessionUser);
        response.put("result", result.name());
        response.put("count", this.reviewService.getReviewLikeCount(reviewId));
        return response;
    }


    @RequestMapping(value = "/review/comment", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postComment(ItemReviewCommentEntity comment,
                                           @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.reviewService.writeComment(comment, sessionUser);
        response.put("result", result.name());
        return response;
    }


    @RequestMapping(value = "/review/comments/{reviewId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ItemReviewCommentEntity> getComments(@PathVariable(value = "reviewId") Long reviewId) {
        return this.reviewService.getComments(reviewId);
    }


    @RequestMapping(value = "/review/comment/{id}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> patchComment(@PathVariable(value = "id") Long id,
                                            @RequestParam(value = "content") String content,
                                            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.reviewService.modifyComment(id, content, sessionUser);
        response.put("result", result.name());
        return response;
    }


    @RequestMapping(value = "/review/comment/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> deleteComment(@PathVariable(value = "id") Long id,
                                             @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.reviewService.deleteComment(id, sessionUser);
        response.put("result", result.name());
        return response;
    }


    @RequestMapping(value = "/like" , method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postLikeItem(
            @RequestParam(value = "shopId") int shopId,
            @RequestParam(value = "itemId") Long itemId,
            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {

        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.itemService.toggleLikeItem(shopId, itemId, sessionUser);
        response.put("result",result.name() );
        return response;
    }

    @RequestMapping(value = "/admin", method = RequestMethod.GET)
    public ModelAndView getAdminReports(ModelAndView modelAndView,
                                        @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {

        if (sessionUser == null || !"admin".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }


        List<ReviewReportVo> reports = this.reviewService.getReportSummaryList(50, 0);


        modelAndView.addObject("reports", reports);
        modelAndView.addObject("totalCount", reports != null ? reports.size() : 0);


        modelAndView.setViewName("admin/admin");
        return modelAndView;
    }
    @RequestMapping(value = "/review/report", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postReviewReport(@RequestParam(value = "type") String type,
                                                @RequestParam(value = "id") Long id,
                                                @RequestParam(value = "reason") String reason,
                                                @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();

        CommonResult result = this.reviewService.reportReviewOrComment(type, id, reason, sessionUser);
        response.put("result", result.name());
        return response;
    }


    @RequestMapping(value = "/admin/report/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ReviewReportVo> getReportList(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity admin) {
        if (admin == null || !"admin".equalsIgnoreCase(admin.getUsertype())) return null;
        return this.reviewService.getReportSummaryList(50, 0);
    }


    @RequestMapping(value = "/review/report/process", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postReportProcess(@RequestParam(value = "targetType") String targetType,
                                                 @RequestParam(value = "targetId") Long targetId,
                                                 @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.reviewService.processReport(targetType, targetId, sessionUser);
        response.put("result", result.name());
        return response;
    }

    @RequestMapping(value = "/review/report/keep", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postReportKeep(@RequestParam(value = "targetType") String targetType,
                                              @RequestParam(value = "targetId") Long targetId,
                                              @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();


        if (sessionUser == null || !"admin".equalsIgnoreCase(sessionUser.getUsertype())) {
            response.put("result", CommonResult.FAILURE.name());
            return response;
        }


        CommonResult result = this.reviewService.keepReport(targetType, targetId);
        response.put("result", result.name());
        return response;
    }
}