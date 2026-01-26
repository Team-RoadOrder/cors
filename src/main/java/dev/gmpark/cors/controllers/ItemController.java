package dev.gmpark.cors.controllers;

import dev.gmpark.cors.dtos.ReviewStatsDto;
import dev.gmpark.cors.entities.*;
import dev.gmpark.cors.results.CommonResult;
import dev.gmpark.cors.services.*;
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

        if (sessionUser == null) {
            modelAndView.setViewName("redirect:/login");
            return modelAndView;
        }
        if (!"customer".equalsIgnoreCase(sessionUser.getUsertype())) {
            modelAndView.setViewName("redirect:/main");
            return modelAndView;
        }

        ShopItemVo item = this.itemService.getItemById(id);

        if (shopId == 0 && item != null) {
            shopId = item.getShopId();
        }

        ShopInfoEntity shopInfo = this.shopService.getShopInfo(shopId);
        int likeCount = this.shopService.getShopLikeCount(shopId);
        ShopInfoEntity[] likeShops = this.myService.getLikeShops(sessionUser);

        boolean isLiked = false;
        if (sessionUser != null && likeShops != null) {
            final int currentShopId = shopId;
            isLiked = Arrays.stream(likeShops)
                    .anyMatch(shop -> shop.getShopId() == currentShopId);
        }

        // [AI 추가] 추천 상품 조회 로직
        List<ShopItemVo> relatedItems = this.itemService.getRelatedItems(id);

        // 리뷰 목록 조회
        List<ReviewVo> reviews = this.reviewService.getReviews(id, sort);

        // 별점 평균 및 막대 그래프용 데이터 계산
        ReviewStatsDto stats = this.reviewService.getReviewStats(id);

        // 스타일 그리드 이미지 조회
        List<String> reviewImages = this.reviewService.getAllImages(id);

        // 예약 ID 조회 (리뷰 작성 권한 확인용)
        Integer availableReservationId = this.reviewService.getAvailableReservationId(sessionUser.getEmail(), id);

        // #region 고객문의 주석처리됨
        /* // [CustomerCare 추가] 상품 문의 목록 조회
        List<CustomerCareVo> inquiries = this.customerCareService.getCustomerCareList(id);
        */
        // #endregion

        modelAndView.addObject("sessionUser", sessionUser);
        modelAndView.addObject("shopInfo", shopInfo);
        modelAndView.addObject("item", item);
        modelAndView.addObject("likeShops", this.myService.getLikeShops(sessionUser));
        modelAndView.addObject("isLiked", isLiked);
        modelAndView.addObject("likeCount", likeCount);

        // [AI 추천] 데이터 전달
        modelAndView.addObject("relatedItems", relatedItems);

        // 리뷰 :데이터 전달
        modelAndView.addObject("reviews", reviews);
        modelAndView.addObject("reviewImages", reviewImages);
        modelAndView.addObject("availableReservationId", availableReservationId);

        // 리뷰 별점 계산된 통계 데이터를 HTML로 전달
        modelAndView.addObject("stats", stats);

        // #region 고객문의 주석처리됨
        /*
        // [CustomerCare] 데이터 전달
        modelAndView.addObject("inquiries", inquiries);
        */
        // #endregion

        modelAndView.setViewName("item/item");
        return modelAndView;
    }


    // ABOUT 리뷰
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
                                           @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.reviewService.modifyReview(id, content, rating, sessionUser);
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
}