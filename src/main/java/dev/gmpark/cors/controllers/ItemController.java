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

    /**
     * [상품 상세 페이지 조회]
     * 정규화 기준: '접근 권한 보안' 및 '실시간 데이터 통합 렌더링'
     * 사유: 비로그인 사용자 및 권한 외 사용자의 접근을 입구에서 차단하고, 리뷰/통계/작성권한 데이터를 한 번에 모델링함.
     */
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

        /**
         * [FBI 보안 포인트] 리뷰 작성 권한 확인
         * 사유: '1구매-1리뷰' 정책에 따라 현재 유저가 리뷰를 쓸 수 있는 빈 구매 슬롯(Reservation ID)이 있는지 확인하여 버튼 활성화 여부 결정.
         */
        Integer availableReservationId = this.reviewService.getAvailableReservationId(sessionUser.getEmail(), id);

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

    /**
     * [리뷰 API: 목록 조회]
     */
    @RequestMapping(value = "/reviews", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ReviewVo> getReviewsApi(@RequestParam(value = "itemId") Long itemId,
                                        @RequestParam(value = "sort", defaultValue = "latest") String sort) {
        return this.reviewService.getReviews(itemId, sort);
    }

    /**
     * [리뷰 API: 신규 작성]
     * 보안 포인트: Identity Pinning (세션 유저 정보만 사용)
     */
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

    /**
     * [리뷰 API: 상세 단건 조회]
     */
    @RequestMapping(value = "/review/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ReviewVo getReview(@PathVariable(value = "id") Long id) {
        return this.reviewService.getReviewById(id);
    }

    /**
     * [리뷰 API: 내용 수정]
     */
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

    /**
     * [리뷰 API: 삭제 처리]
     */
    @RequestMapping(value = "/review/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> deleteReview(@PathVariable(value = "id") Long id,
                                            @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.reviewService.deleteReview(id, sessionUser);
        response.put("result", result.name());
        return response;
    }

    /**
     * [리뷰 API: 도움돼요 토글]
     */
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

    /**
     * [댓글 API: 신규 작성]
     * 정규화 기준: '익명성 배제'
     * 사유: 서비스 세션의 실명 정보를 기반으로 댓글이 작성되도록 강제함.
     */
    @RequestMapping(value = "/review/comment", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> postComment(ItemReviewCommentEntity comment,
                                           @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.reviewService.writeComment(comment, sessionUser);
        response.put("result", result.name());
        return response;
    }

    /**
     * [댓글 API: 목록 조회]
     */
    @RequestMapping(value = "/review/comments/{reviewId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ItemReviewCommentEntity> getComments(@PathVariable(value = "reviewId") Long reviewId) {
        return this.reviewService.getComments(reviewId);
    }

    /**
     * [댓글 API: 내용 수정]
     * 보안 포인트: 데이터 변조 방어
     */
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

    /**
     * [댓글 API: 삭제 처리]
     * 정규화 기준: '데이터 고아 현상 방지'
     * 사유: 서비스단에서 하위 답글 유무를 판별한 뒤 안전하게 삭제 프로세스 수행.
     */
    @RequestMapping(value = "/review/comment/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> deleteComment(@PathVariable(value = "id") Long id,
                                             @SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser) {
        Map<String, Object> response = new HashMap<>();
        CommonResult result = this.reviewService.deleteComment(id, sessionUser);
        response.put("result", result.name());
        return response;
    }

    /**
     * [관심 상품 API: 토글]
     */
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