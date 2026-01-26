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

    /**
     * [유지] 정규화 기준: '1구매-1리뷰' 정책의 데이터 무결성 보장
     * 사유: 재구매 시 리뷰 작성이 가능하려면 '리뷰가 작성되지 않은 가장 최신의 예약/주문 건'을 찾아야 함.
     * 이를 통해 중복 리뷰 작성을 방지하고 비즈니스 규칙을 데이터 레벨에서 강제함.
     */
    Integer selectAvailableReservationId(@Param("email") String email, @Param("itemId") Long itemId);

    /**
     * [유지] 정규화 기준: '도메인 분리 및 책임 명확화'
     * 사유: 리뷰의 핵심 본문 데이터를 처리하는 메서드.
     * insertReservationForOrder 등으로 준비된 예약 식별자를 사용하여 최종적인 리뷰 엔터티를 완성함.
     */
    int insertReview(ItemReviewEntity review);

    /**
     *     int insertReservationForOrder(ReservationEntity reservation);
     *     xml:
     *         <insert id="insertReservationForOrder" useGeneratedKeys="true" keyProperty="id">
     *         INSERT INTO `cors`.`reservations` (`user_email`,`shop_id`,`visit_date`,`status`,`created_at`)
     *         VALUES (#{userEmail},#{shopId},NOW(), 'ORDER_REVIEW', NOW())
     *     </insert>
     * [삭제] 정규화 기준: '논리적 오류 수정' 및 '참조 무결성 유지'
     * 사유: DB 설계상 리뷰는 예약 ID를 외래키(FK)로 참조함.
     * 현 정책상 예약 없이 구매한 '주문 완료' 고객도 리뷰를 써야 하므로, 논리적 모순 해결을 위해 더미 예약 레코드를 생성함.
     *
     */


    /**
     * [유지] 정규화 기준: '단일 책임 원칙(SRP)' 준수
     * 사유: 본문과 이미지는 데이터 저장 주기와 방식이 다름.
     * 이미지 다중 업로드 대응을 위해 리뷰 본문 저장 로직과 분리하여 중복 없는 이미지 데이터 처리를 보장함.
     */
    int insertReviewImage(ItemReviewImageEntity image);

    /**
     * [유지] 정규화 기준: '중복되는 쿼리 삭제 및 로직 통합'
     * 사유: MyBatis의 동적 쿼리(<choose>)를 활용하여 최신순/도움순 정렬 요청을 하나의 메서드로 통합 관리함.
     * 서비스 레이어에서 별도의 정렬 로직을 짤 필요가 없게 하여 복잡도를 낮춤.f
     * # 전체리뷰조회
     */
    List<ReviewVo> selectReviewsByItemId(@Param("itemId") Long itemId, @Param("sortType") String sortType);

    /**
     * [유지] 정규화 기준: '필요 데이터의 최소 조회'
     * 사유: 리뷰 목록 조회 시 무거운 이미지 데이터를 매번 Join 하지 않고, 필요한 상세 시점에만 조회하여 쿼리 성능 최적화.
     */
    List<String> selectImagesByReviewId(Long id);

    /**
     * [유지] 정규화 기준: '같은 역할을 하는 로직 통합'
     * 사유: 상품 상세 페이지 상단의 '이미지 그리드' 전용 쿼리.
     * 본문 내용 없이 경로만 빠르게 추출하여 페이지 로딩 속도를 향상시킴.
     */
    List<String> selectAllReviewImages(Long itemId);

    /**
     * [유지] 정규화 기준: '권한 검증의 로직화'
     * 사유: 상점 주인이 자신의 상품 리뷰에 댓글(답글)을 다는 권한을 확인하는 절차.
     * 서비스단에서 하드코딩 대신 DB를 통해 소유권을 검증하여 보안 무결성 확보.
     */
    int isShopOwnerOfReview(@Param("shopId") int shopId, @Param("email") String email);

    /**
     * [유지] 정규화 기준: '상세 데이터 조회 표준화'
     * 사유: 수정 및 삭제 전 데이터 존재 여부와 작성자 일치 여부를 확인하기 위한 표준 "단건 조회 메서드".
     */
    ReviewVo selectReviewById(Long id);

    /**
     * [유지] 정규화 기준: '데이터 수정 정규화'
     * 사유: 본문 내용과 별점만 선택적으로 업데이트하여 업데이트 시 발생할 수 있는 데이터 오염을 방지함.
     */
    int updateReview(ItemReviewEntity review);

    /**
     * [유지] 정규화 기준: '물리적 삭제 정책 준수'
     * 사유: 이미지 삭제 전 본문을 먼저 삭제하거나, 상태 기반 삭제가 아닐 경우 명확한 물리 삭제를 수행함.
     */
    int deleteReview(Long id);

    /*>>>>>>>>>> 대댓글, 도움돼요 <<<<<<<<<<*/

    /**
     * [유지] 정규화 기준: '다대다 관계의 교차 테이블 관리'
     * 사유: 사용자와 리뷰 간의 '도움돼요' 관계를 기록.
     * 중복 클릭 방지를 위한 유니크 제약 조건을 보조함.
     */
    int insertReviewLike(@Param("reviewId") Long reviewId, @Param("email") String email);

    int deleteReviewLike(@Param("reviewId") Long reviewId, @Param("email") String email);

    /**
     * [유지] 정규화 기준: '데이터 정합성 및 원자성'
     * 사유: Like 테이블의 변화에 따라 실제 리뷰 테이블의 카운트를 동기화함.
     * 실시간 정렬(도움순)을 위해 캐싱된 카운트 값을 관리함.
     */
    int increaseUsefulCount(@Param("reviewId") Long reviewId);

    /**
     * [유지] 정규화 기준: '논리적 오류 수정'
     * 사유: 카운트 감소 시 SQL의 GREATEST(0, ...) 등을 활용하여 음수가 되는 데이터 오류를 원천 차단함.
     */
    int decreaseUsefulCount(@Param("reviewId") Long reviewId);

    /**
     * [유지] 정규화 기준: '중복 로직 통합'
     * 사유: 사용자가 해당 리뷰에 이미 '도움돼요'를 눌렀는지 확인하여 토글(Toggle) 기능을 구현함.
     */
    int checkReviewLikeExists(@Param("reviewId") Long reviewId, @Param("email") String email);

    /**
     * [유지] 정규화 기준: '실시간 데이터 반영'
     * 사유: 토글 완료 후 화면에 즉시 갱신된 카운트를 전달하기 위해 사용함.
     */
    int selectReviewLikeCount(Long reviewId);

    /*>>>>>>>>>> 리뷰 댓글 관련 <<<<<<<<<<<*/

    /**
     * [유지] 정규화 기준: '도메인 분리' 및 '권한 정책 차등'
     * 사유: 리뷰는 구매자만 쓰지만, 댓글은 누구나 참여 가능한 소통 도메인임.
     * 목적이 다르므로 독립된 테이블과 메서드로 관리하여 유지보수성을 높임.
     */
    int insertComment(ItemReviewCommentEntity comment);

    /**
     * [유지] 정규화 기준: '업데이트 범위 제한'
     * 사유: 댓글 본문 외 생성일 등은 변하지 않도록 관리함.
     */
    int updateComment(ItemReviewCommentEntity comment);

    /**
     * [유지] 정규화 기준: '비즈니스 정책 준수'
     * 사유: 대댓글이 있는 경우 삭제를 제한하는 로직의 최종 수행 단계.
     */
    int deleteComment(Long id);

    /**
     * [유지] 정규화 기준: '논리적 오류 수정 - 삭제 방어'
     * 사유: "대댓글이 달린 원본 삭제 불가" 정책을 위해 하위 자식 레코드 유무를 확인하는 필수 검증 쿼리.
     */
    int selectChildCountByParentId(Long parentId);

    /**
     * [유지] 정규화 기준: '수정 권한 검증'
     * 사유: 댓글 수정 전 본인 확인을 위해 데이터 원본을 조회함.
     */
    ItemReviewCommentEntity selectCommentById(Long id);

    /**
     * [유지] 정규화 기준: '계층형 데이터 정렬 통합'
     * 사유: SQL 레벨에서 부모-자식 순서(IFNULL 정렬)를 맞춰 가져옴으로써, 서비스단의 재귀 로직 중복을 제거함.
     */
    List<ItemReviewCommentEntity> selectCommentsByReviewId(Long reviewId);
}