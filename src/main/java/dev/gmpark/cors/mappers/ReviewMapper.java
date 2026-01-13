package dev.gmpark.cors.mappers;

import dev.gmpark.cors.entities.ItemReviewCommentEntity;
import dev.gmpark.cors.entities.ItemReviewEntity;
import dev.gmpark.cors.entities.ItemReviewImageEntity;
import dev.gmpark.cors.entities.ReservationEntity;
import dev.gmpark.cors.vos.ReviewVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface ReviewMapper {

    // 구매 횟수 = 리뷰 작성 가능 횟수
    Integer selectReviewCountByEmail(@Param("email") String email, @Param("itemId") Long itemId);

    // 확정된 예약 ID 하나 가져오기
    Integer selectAvailableReservationId(@Param("email") String email, @Param("itemId") Long itemId);

    // 리뷰 본문 저장 (삽입 후 생성된 id가 entity에 담김)
    int insertReview(ItemReviewEntity review);

    //주문완료시 -> 리뷰 본문 저장 :"주문 리뷰"일 때는 reservation_id가 아예 필요 없는 상태여야함.
    int insertReservationForOrder(ReservationEntity reservation);

    // 리뷰 이미지 저장
    int insertReviewImage(ItemReviewImageEntity image);

    // 상품별 리뷰 목록 조회 (sortType: 'useful' 또는 'latest')
    List<ReviewVo> selectReviewsByItemId(@Param("itemId") Long itemId, @Param("sortType") String sortType);

    //개별 리뷰용 :특정 리뷰 ID 하나에 달린 이미지들만 조회
    List<String> selectImagesByReviewId(Long id);

    // 해당 상품의 모든 리뷰 이미지 경로만 추출 (상단 사진 그리드용)
    List<String> selectAllReviewImages(Long itemId);

    // 권한 및 구매 체크
    int checkPurchaseHistory(@Param("email") String email, @Param("itemId") Long itemId);
    int isShopOwnerOfReview(@Param("shopId") int shopId, @Param("email") String email);

    // 삭제 및 상세조회
    ReviewVo selectReviewById(Long id);

    // 리뷰 수정: 리뷰 본문 및 별점 업데이트
    int updateReview(ItemReviewEntity review);

    // 리뷰 삭제
    int deleteReview(Long id);

    /*>>>>>>>>>>대댓글, 도움돼요<<<<<<<<<<*/

    // 1. 좋아요 테이블에 INSERT (카운트 증가 X)
    int insertReviewLike(@Param("reviewId") Long reviewId, @Param("email") String email);

    // 2. 좋아요 테이블에서 DELETE (카운트 감소 X)
    int deleteReviewLike(@Param("reviewId") Long reviewId, @Param("email") String email);

    //리뷰 테이블의 useful_count + 1
    int increaseUsefulCount(@Param("reviewId") Long reviewId);

    // 리뷰 테이블의 useful_count - 1 (0 이하 방지 포함)
    int decreaseUsefulCount(@Param("reviewId") Long reviewId);

    // 좋아요 여부 확인
    int checkReviewLikeExists(@Param("reviewId") Long reviewId, @Param("email") String email);

    // 현재 좋아요 수 조회
    int selectReviewLikeCount(Long reviewId);

    /*>>>>>>>>>> [리뷰 댓글 관련]<<<<<<<<<<<*/

    int insertComment(ItemReviewCommentEntity comment);

    //댓글/대댓글 수정 (기본 updateReview와 구분)
    int updateComment(ItemReviewCommentEntity comment);

    //댓글/대댓글 삭제 (기본 deleteReview와 구분)
    int deleteComment(Long id);

    // 글 단일 조회 (수정/삭제 전 본인 확인용)
    ItemReviewCommentEntity selectCommentById(Long id);

    // 특정 리뷰의 댓글 목록 조회 (부모-자식 정렬 포함)
    List<ItemReviewCommentEntity> selectCommentsByReviewId(Long reviewId);
}