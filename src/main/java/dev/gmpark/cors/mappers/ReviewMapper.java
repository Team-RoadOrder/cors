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
}