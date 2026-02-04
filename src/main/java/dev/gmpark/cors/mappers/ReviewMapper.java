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

    Integer selectAvailableReservationId(@Param("email") String email, @Param("itemId") Long itemId);


    int insertReview(ItemReviewEntity review);

    int insertReviewImage(ItemReviewImageEntity image);

    List<ReviewVo> selectReviewsByItemId(@Param("itemId") Long itemId, @Param("sortType") String sortType);

    List<String> selectImagesByReviewId(Long id);

    List<String> selectAllReviewImages(Long itemId);

    int isShopOwnerOfReview(@Param("shopId") int shopId, @Param("email") String email);

    ReviewVo selectReviewById(Long id);

    int updateReview(ItemReviewEntity review);

    int deleteReview(Long id);

    // [추가] 리뷰 본체는 건드리지 않고, 연결된 이미지 레코드만 삭제
    int deleteImagesByReviewId(@Param("reviewId") Long reviewId);

    int insertReviewLike(@Param("reviewId") Long reviewId, @Param("email") String email);

    int deleteReviewLike(@Param("reviewId") Long reviewId, @Param("email") String email);

    int increaseUsefulCount(@Param("reviewId") Long reviewId);

    int decreaseUsefulCount(@Param("reviewId") Long reviewId);

    int checkReviewLikeExists(@Param("reviewId") Long reviewId, @Param("email") String email);

    int selectReviewLikeCount(Long reviewId);

    int insertComment(ItemReviewCommentEntity comment);

    int updateComment(ItemReviewCommentEntity comment);

    int deleteComment(Long id);

    int selectChildCountByParentId(Long parentId);

    ItemReviewCommentEntity selectCommentById(Long id);

    List<ItemReviewCommentEntity> selectCommentsByReviewId(Long reviewId);


    // [신규 추가] 리뷰 삭제 시 외래키 오류 방지를 위한 일괄 삭제 메서드
    int deleteCommentsByReviewId(@Param("reviewId") Long reviewId); // 리뷰 하위 댓글 전체 삭제
    int deleteLikesByReviewId(@Param("reviewId") Long reviewId);    // 리뷰 좋아요 전체 삭제


}