package dev.gmpark.cors.services;

import dev.gmpark.cors.dtos.ReviewStatsDto;
import dev.gmpark.cors.entities.*;
import dev.gmpark.cors.mappers.OrderMapper;
import dev.gmpark.cors.mappers.ReviewMapper;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.vos.ReviewVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewMapper reviewMapper;
    /*TODO : oderMapper 추가*/
    private final OrderMapper orderMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // 텍스트 정규화 및 글자 수 검증 (1~100자)
    private String normalizeText(String text, int min, int max) {
        if (text == null) return "";
        String cleaned = HtmlUtils.htmlEscape(text.trim()).replaceAll("\\s{2,}", " ");
        return (cleaned.length() < min || cleaned.length() > max) ? "" : cleaned;
    }

    /*리뷰 작성*/
    @Transactional
    public CommonResult writeReview(ItemReviewEntity review, MultipartFile[] images, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;

        // 본문 정규화 및 이미지 개수(최대 3장) 체크
        String content = normalizeText(review.getContent(), 1, 100);
        if (content.isEmpty() || (images != null && images.length > 3)) return CommonResult.FAILURE;
        review.setContent(content);

        // 예약완료 기반 완료수 : history ->reservationHistory변경
        int reservationHistory = this.reviewMapper.checkPurchaseHistory(user.getEmail(), review.getItemId());
        // : 주문완료 기반 완료수
        int orderHistory = this.orderMapper.selectCompleteOrderCount(review.getItemId(),user.getEmail());
        // :총 권한 합산 (예약 + 주문)
        int totalHistory = orderHistory + reservationHistory;

        //: 이미 작성된 리뷰의 수
        Integer written = this.reviewMapper.selectReviewCountByEmail(user.getEmail(), review.getItemId());
        int actualWritten = (written != null) ? written : 0;

        //:검증 : 합산권한이 0 이거나 이미 작성되어 권한이없은 경우는 실패
        if(totalHistory == 0 ||actualWritten>= totalHistory){
            return CommonResult.FAILURE;
        }
        //기존 예약 테이블에서 사용가능한 ID 조회
        Integer resId = this.reviewMapper.selectAvailableReservationId(user.getEmail(), review.getItemId());
        // 예약 ID 없는데 주문이력으로 작성하는 경우 -> 더미 예약 처리
        if (resId == null && orderHistory > 0) {
            ReservationEntity dummyRes = new ReservationEntity();
            dummyRes.setUserEmail(user.getEmail());
            dummyRes.setShopId(review.getShopId());
            //ReviewMapper에 추가할 메서드 호출
            if (this.reviewMapper.insertReservationForOrder(dummyRes) > 0) {
                resId = dummyRes.getId(); // 생성된 PK(id)를 받아옴
            } else {
                return CommonResult.FAILURE;
            }
        }
        review.setReservationId(resId); // 이제 null이 아니므로 FK 에러가 나지 않음
        review.setUserEmail(user.getEmail());
        review.setUserEmail(user.getEmail());
//        if (this.reviewMapper.insertReview(review) <= 0) return CommonResult.FAILURE;






        if (this.reviewMapper.insertReview(review) <= 0) return CommonResult.FAILURE;
        // 이미지 서버 저장 및 DB 기록
        if (images != null) {
            String path = uploadDir.endsWith(File.separator) ? uploadDir : uploadDir + File.separator;
            for (MultipartFile file : images) {
                if (file.isEmpty()) continue;
                String originalName = file.getOriginalFilename();
                String ext = originalName != null && originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : ".jpg";
                String uuidName = UUID.randomUUID() + ext;
                try {
                    file.transferTo(new File(path + uuidName));
                    ItemReviewImageEntity imgEntity = ItemReviewImageEntity.builder()
                            .reviewId(review.getId())
                            .imageData(uuidName)
                            .build();
                    if (this.reviewMapper.insertReviewImage(imgEntity) <= 0) throw new RuntimeException("이미지 저장 실패");
                } catch (IOException e) {
                    throw new RuntimeException("파일 저장 실패");
                }
            }
        }
        return CommonResult.SUCCESS;
    }

    /*[TODO:리뷰테이블에서 가져오는거라 - 수정 필요 x]리뷰 목록 조회 (이미지 포함)*/
    public List<ReviewVo> getReviews(Long itemId, String sortType) {
        List<ReviewVo> reviews = this.reviewMapper.selectReviewsByItemId(itemId, sortType);
        if (reviews != null) {
            reviews.forEach(r -> r.setImages(this.reviewMapper.selectImagesByReviewId(r.getId())));
        }
        return reviews;
    }
    /*[TODO:리뷰테이블에서 가져오는거라 - 수정 필요 x]단일 리뷰 조회*/
    public ReviewVo getReviewById(Long reviewId) {
        ReviewVo review = this.reviewMapper.selectReviewById(reviewId);
        if (review != null) {
            review.setImages(this.reviewMapper.selectImagesByReviewId(reviewId));
        }
        return review;
    }

    /*뷰 수정*/
    @Transactional
    public CommonResult modifyReview(Long reviewId, String content, int rating, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;
        String normalized = normalizeText(content, 1, 100);
        ReviewVo review = this.reviewMapper.selectReviewById(reviewId);

        //본인확인 ( 작성이메일 : 로그인 유저 이메일)
        if (normalized.isEmpty() || review == null || !review.getUserEmail().equals(user.getEmail()))
            return CommonResult.FAILURE;

        ItemReviewEntity entity = new ItemReviewEntity();
        entity.setId(reviewId);
        entity.setContent(normalized);
        entity.setRating(rating);
        return this.reviewMapper.updateReview(entity) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }

    /*리뷰 통계 계산*/
    public ReviewStatsDto getReviewStats(Long itemId) {
        List<ReviewVo> reviews = this.reviewMapper.selectReviewsByItemId(itemId, "latest");
        int[] counts = new int[6];
        double sum = 0;
        for (ReviewVo r : reviews) {
            if (r.getRating() >= 1 && r.getRating() <= 5) {
                counts[r.getRating()]++;
                sum += r.getRating();
            }
        }
        double avg = reviews.isEmpty() ? 0.0 : Math.round((sum / reviews.size()) * 10) / 10.0;
        return new ReviewStatsDto(avg, reviews.size(), counts);
    }

    /*전체 리뷰 이미지 조회*/
    public List<String> getAllImages(Long itemId) {
        return this.reviewMapper.selectAllReviewImages(itemId);
    }

    /*리뷰 삭제*/
    @Transactional
    /*TODO :userEmail 비교 기반*/
    public CommonResult deleteReview(Long reviewId, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;
        ReviewVo review = this.reviewMapper.selectReviewById(reviewId);
        if (review == null) return CommonResult.FAILURE;

        if (review.getUserEmail().equals(user.getEmail()) || this.reviewMapper.isShopOwnerOfReview(review.getShopId(), user.getEmail()) > 0 || user.getLevel() == 1) {
            List<String> files = this.reviewMapper.selectImagesByReviewId(reviewId);
            if (this.reviewMapper.deleteReview(reviewId) > 0) {
                String path = uploadDir.endsWith(File.separator) ? uploadDir : uploadDir + File.separator;
                files.forEach(f -> {
                    File file = new File(path + f);
                    if (file.exists()) file.delete();
                });
                return CommonResult.SUCCESS;
            }
        }
        return CommonResult.FAILURE;
    }

    /*작성 가능한 예약 ID 조회*/
    public Integer getAvailableReservationId(String email, Long itemId) {
        return this.reviewMapper.selectAvailableReservationId(email, itemId);
    }



    /*도움돼요 토글*/
    @Transactional
    public CommonResult toggleReviewLike(Long reviewId, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;
        if (this.reviewMapper.checkReviewLikeExists(reviewId, user.getEmail()) > 0) {
            this.reviewMapper.deleteReviewLike(reviewId, user.getEmail());
            this.reviewMapper.decreaseUsefulCount(reviewId);
            return CommonResult.FAILURE; // 토글 해제 상태를 FAILURE로 반환(기존 로직 준수)
        }
        this.reviewMapper.insertReviewLike(reviewId, user.getEmail());
        this.reviewMapper.increaseUsefulCount(reviewId);
        return CommonResult.SUCCESS; // 토글 설정 상태를 SUCCESS로 반환
    }

    /*도움돼요 개수 조회*/
    public int getReviewLikeCount(Long reviewId) {
        return this.reviewMapper.selectReviewLikeCount(reviewId);
    }

    /*댓글 작성*/
    @Transactional
    public CommonResult writeComment(ItemReviewCommentEntity comment, RegisterEntity user) {
        /*history와 purchaseHistory를 합산하여 사용자가 가진 전체 리뷰 작성 권한을 계산*/
        if (user == null) return CommonResult.FAILURE_SESSION;
        String content = normalizeText(comment.getContent(), 1, 100);
        if (content.isEmpty()) return CommonResult.FAILURE;
        comment.setContent(content);
        comment.setUserEmail(user.getEmail());

        return this.reviewMapper.insertComment(comment) > 0 ?
                CommonResult.SUCCESS : CommonResult.FAILURE;
    }

    /*댓글 목록 조회*/
    public List<ItemReviewCommentEntity> getComments(Long reviewId) {
        return this.reviewMapper.selectCommentsByReviewId(reviewId);
    }

    /*댓글 수정*/
    @Transactional
    public CommonResult modifyComment(Long commentId, String content, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;
        String normalized = normalizeText(content, 1, 100);
        ItemReviewCommentEntity comment = this.reviewMapper.selectCommentById(commentId);
        if (normalized.isEmpty() || comment == null || !comment.getUserEmail().equals(user.getEmail())) return CommonResult.FAILURE;
        comment.setContent(normalized);
        return this.reviewMapper.updateComment(comment) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }

    /* 댓글 삭제*/
    @Transactional
    public CommonResult deleteComment(Long commentId, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;
        ItemReviewCommentEntity comment = this.reviewMapper.selectCommentById(commentId);
        if (comment != null && (comment.getUserEmail().equals(user.getEmail()) || user.getLevel() == 1)) {
            return this.reviewMapper.deleteComment(commentId) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
        }
        return CommonResult.FAILURE;
    }
}