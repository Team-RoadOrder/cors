package dev.gmpark.cors.services;

import dev.gmpark.cors.dtos.ReviewStatsDto;
import dev.gmpark.cors.entities.*;
import dev.gmpark.cors.mappers.OrderMapper;
import dev.gmpark.cors.mappers.ReviewMapper;
import dev.gmpark.cors.results.CommonResult;
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
    private final OrderMapper orderMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * [정규화: 입력 데이터 오염 및 인젝션 방어]
     * 사유: 악의적인 사용자가 스크립트를 삽입(XSS)하거나 비정상적인 공백으로 레이아웃을 깨뜨리는 행위를 방어함.
     * 1. HtmlUtils.htmlEscape: 모든 HTML 특수문자를 이스케이프 처리하여 브라우저 내 악성 스크립트 실행을 원천 차단함.
     * 2. 정규식 활용: trim()과 replaceAll을 통해 연속된 공백을 1칸으로 정규화하여 데이터 가독성 및 UI 정합성을 확보함.
     */
    private String normalizeText(String text, int min, int max) {
        if (text == null) return "";
        String cleaned = HtmlUtils.htmlEscape(text.trim()).replaceAll("\\s{2,}", " ");
        return (cleaned.length() < min || cleaned.length() > max) ? "" : cleaned;
    }

    /**
     * [리뷰 작성: FBI 보안 정규화 및 구매 무결성 강제]
     * 정규화 기준: '실제 구매 데이터 기반 권한 할당' 및 '60일 이내 작성 정책'
     * 사유:
     * 1. 물건을 사지 않은 유저의 유령 리뷰 차단.
     * 2. 구매 후 60일이 지난 건에 대한 리뷰 작성 권한 만료 강제.
     * 3. 기한 내 삭제 시 재작성은 허용하되, 기한 초과 시 재작성 불가 정책 실현.
     */
    @Transactional
    public CommonResult writeReview(ItemReviewEntity review, MultipartFile[] images, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION; // 세션 유효성 선행 검증

        // [보안: 입력 검증] 서버 사이드에서 내용 길이와 이미지 개수(최대 3장)를 최종 검증함.
        String content = normalizeText(review.getContent(), 1, 100);
        if (content.isEmpty() || (images != null && images.length > 3)) return CommonResult.FAILURE;
        review.setContent(content);

        /**
         * [보안 포인트 1: 구매 이력 실시간 대조]
         * 사유: 서버 DB의 결제 완료 이력을 기준으로 작성 권한을 재검증함.
         */
        int orderHistory = this.orderMapper.selectCompleteOrderCount(review.getItemId(), user.getEmail());
        if (orderHistory <= 0) return CommonResult.FAILURE;

        /**
         * [보안 포인트 2: 60일 기한 및 미작성 슬롯(Reservation ID) 탐색]
         * 사유: selectAvailableReservationId는 SQL 레벨에서 다음을 수행함:
         * - 해당 아이템의 리뷰가 없는(또는 삭제된) 구매 건 탐색 (LEFT JOIN ... IS NULL)
         * - 구매일로부터 60일 이내인 건만 필터링 (DATE_SUB(NOW(), INTERVAL 60 DAY))
         * [결과 처리] resId가 null이면 '이미 작성함' 또는 '60일 경과'로 간주하여 FAILURE 반환.
         */
        Integer resId = this.reviewMapper.selectAvailableReservationId(user.getEmail(), review.getItemId());

        // [정규화] 실패 원인을 FAILURE로 통합하여 프론트엔드에서 60일 정책 알림 모달을 띄우도록 유도함.
        if (resId == null) return CommonResult.FAILURE;

        // [Identity Pinning] 작성자 정보를 세션의 실제 이메일로 강제 고정하여 신원 위조 방지.
        review.setReservationId(resId);
        review.setUserEmail(user.getEmail());

        // 리뷰 본문 저장
        if (this.reviewMapper.insertReview(review) <= 0) return CommonResult.FAILURE;

        // [원자적 트랜잭션] 이미지 저장 실패 시 DB 레코드까지 롤백 유도.
        return (images != null && images.length > 0) ? saveImages(review.getId(), images) : CommonResult.SUCCESS;
    }

    /**
     * [이미지 저장: 트랜잭션 무결성 및 파일 보안 보장]
     *  추가: UUID : 네트워크상 고유성 보장하는 ID
     *  경로 유추를 통한 무단 열람 공격에서는 매우 안전
     * 사유: I/O 오류 또는 DB 인서트 오류 발생 시 데이터 정합성을 위해 전체 프로세스를 취소함.
     */
    private CommonResult saveImages(Long reviewId, MultipartFile[] images) {
        // [보안 정규화] OS 구분자 혼선을 방지하기 위해 웹 표준인 '/'로 통일
        // 타당성: WebMvcConfig의 ResourceLocation은 내부적으로 '/'를 기준으로 경로를 해석함.
        String basePath = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        String reviewPath = basePath + "review/";

        try {
            // [방어적 폴더 생성]
            // 타당성: 상위 디렉토리가 읽기 전용이거나 권한이 없는 경우 mkdirs()가 false를 반환함.
            File dir = new File(reviewPath);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    // 폴더 생성 실패 시 로그를 남겨 즉각적인 원인 파악 유도
                    System.err.println("디렉토리 생성 실패: " + reviewPath);
                    throw new IOException("Critical: Cannot create review directory.");
                }
            }

            for (MultipartFile file : images) {
                if (file.isEmpty()) continue;

                String originalName = file.getOriginalFilename();
                String ext = (originalName != null && originalName.contains("."))
                        ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase() : ".jpg";

                // [네트워크 고유성 보장] UUID 사용 이유:
                // 타당성: 동일 파일명 업로드 시 기존 데이터 덮어쓰기(Overwrite) 공격 및 파일명 추측을 통한 IDOR 공격 차단.
                String uuidName = UUID.randomUUID().toString() + ext;

                // [물리 저장 수행]
                file.transferTo(new File(reviewPath + uuidName));

                ItemReviewImageEntity imgEntity = ItemReviewImageEntity.builder()
                        .reviewId(reviewId)
                        .imageData(uuidName)
                        .build();

                if (this.reviewMapper.insertReviewImage(imgEntity) <= 0) throw new RuntimeException();
            }
            return CommonResult.SUCCESS;
        } catch (IOException | RuntimeException e) {
            // [원자적 트랜잭션] 물리 파일 저장 실패 시 DB 레코드까지 롤백 유도
            e.printStackTrace();
            throw new RuntimeException("Image storage failed - triggering rollback", e);
        }
    }


    /**
     * [리뷰 목록 조회: 데이터 무결성 유지]
     * 사유: Mapper의 JOIN을 통해 리뷰 본문, 작성자의 실명(userName), 매핑된 이미지 목록을 통합 조회함.
     */
    public List<ReviewVo> getReviews(Long itemId, String sortType) {
        List<ReviewVo> reviews = this.reviewMapper.selectReviewsByItemId(itemId, sortType);
        if (reviews != null) {
            // 각 리뷰에 속한 이미지 리스트를 개별적으로 세팅하여 반환
            reviews.forEach(r -> r.setImages(this.reviewMapper.selectImagesByReviewId(r.getId())));
        }
        return reviews;
    }

    /**
     * [리뷰 상세 조회: 훼손 없이 유지]
     * 사유: 특정 리뷰의 모든 세부 정보와 이미지를 단일 조회함.
     */
    public ReviewVo getReviewById(Long reviewId) {
        ReviewVo review = this.reviewMapper.selectReviewById(reviewId);
        if (review != null) {
            review.setImages(this.reviewMapper.selectImagesByReviewId(reviewId));
        }
        return review;
    }

    /**
     * [리뷰 수정: 소유권 철저 검증 및 데이터 변조 방어]
     * 사유: DB에서 조회한 원본 작성자 이메일과 요청자의 세션 정보를 1:1 대조하여 무단 수정을 원천 차단함.
     */
    @Transactional
    public CommonResult modifyReview(Long reviewId, String content, int rating, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;

        ReviewVo review = this.reviewMapper.selectReviewById(reviewId);
        // [보안] 세션 유저와 원본 작성자가 불일치할 경우 수정을 즉시 차단하여 권한 남용 방지.
        if (review == null || !review.getUserEmail().equals(user.getEmail())) return CommonResult.FAILURE;

        String normalized = normalizeText(content, 1, 100);
        if (normalized.isEmpty()) return CommonResult.FAILURE;

        ItemReviewEntity entity = ItemReviewEntity.builder()
                .id(reviewId)
                .content(normalized)
                .rating(rating)
                .build();
        return this.reviewMapper.updateReview(entity) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }

    /**
     * [리뷰 통계 계산: 연산 로직 효율화]
     * 사유: 별점 분포(1~5점)를 인덱스로 관리하고 평균 평점을 소수점 첫째 자리까지 정규화함.
     */
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

    /**
     * [전체 이미지 조회]
     * 특정 아이템에 달린 모든 리뷰 이미지 경로를 리스트로 반환.
     */
    public List<String> getAllImages(Long itemId) {
        return this.reviewMapper.selectAllReviewImages(itemId);
    }

    /**
     * [리뷰 삭제: 다중 권한 매트릭스 및 물리 무결성 정규화]
     * 사유: 작성자 본인, 매장 점주, 시스템 관리자 각각의 삭제 권한을 계층적으로 검증하여 수행함.
     */
    @Transactional
    public CommonResult deleteReview(Long reviewId, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;

        ReviewVo review = this.reviewMapper.selectReviewById(reviewId);
        if (review == null) return CommonResult.FAILURE;

        // [FBI 정규화] 권한 매트릭스: 작성자 OR 점주 OR 관리자만 접근 허용
        boolean isOwner = review.getUserEmail().equals(user.getEmail());
        boolean isShopOwner = this.reviewMapper.isShopOwnerOfReview(review.getShopId(), user.getEmail()) > 0;
        boolean isAdmin = user.getLevel() == 1;

        if (isOwner || isShopOwner || isAdmin) {
            List<String> files = this.reviewMapper.selectImagesByReviewId(reviewId);
            if (this.reviewMapper.deleteReview(reviewId) > 0) {
                deletePhysicalFiles(files); // DB 레코드 삭제 성공 시에만 물리 파일을 제거하여 서버 자원 보호.
                return CommonResult.SUCCESS;
            }
        }
        return CommonResult.FAILURE;
    }

    /**
     * [물리 파일 삭제 자동화]
     * 사유: DB 삭제와 동기화하여 서버 내 쓰레기 파일(Orphan Files)이 남지 않도록 자동 관리함.
     */
    private void deletePhysicalFiles(List<String> files) {
        String path = uploadDir.endsWith(File.separator) ? uploadDir : uploadDir + File.separator;
        files.forEach(f -> {
            File file = new File(path + f);
            if (file.exists()) file.delete();
        });
    }

    /**
     * [유효 예약 ID 조회]
     * 특정 아이템에 대해 리뷰 작성이 가능한 구매 건이 있는지 확인.
     */
    public Integer getAvailableReservationId(String email, Long itemId) {
        return this.reviewMapper.selectAvailableReservationId(email, itemId);
    }

    /**
     * [도움돼요: 어뷰징 및 중복 추천 방어]
     * 사유: DB 유니크 제약 조건을 활용하여 1인 1리뷰당 1회 추천 정책을 물리적으로 강제함.
     */
    @Transactional
    public CommonResult toggleReviewLike(Long reviewId, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;
        // 이미 추천한 이력이 있는지 확인하여 추가(insert) 또는 삭제(delete) 수행
        return (this.reviewMapper.checkReviewLikeExists(reviewId, user.getEmail()) > 0)
                ? removeLike(reviewId, user.getEmail())
                : addLike(reviewId, user.getEmail());
    }

    private CommonResult addLike(Long reviewId, String email) {
        this.reviewMapper.insertReviewLike(reviewId, email);
        this.reviewMapper.increaseUsefulCount(reviewId);
        return CommonResult.SUCCESS;
    }

    private CommonResult removeLike(Long reviewId, String email) {
        this.reviewMapper.deleteReviewLike(reviewId, email);
        this.reviewMapper.decreaseUsefulCount(reviewId);
        return CommonResult.FAILURE;
    }

    /*도움돼요 개수 조회*/
    public int getReviewLikeCount(Long reviewId) {
        return this.reviewMapper.selectReviewLikeCount(reviewId);
    }

    /**
     * [댓글 작성: 인젝션 방어 및 작성자 신원 고정]
     * 정규화 기준: '익명 처리 원천 차단'
     * 사유: 댓글은 누구나 소통 가능하나 작성자 신원은 무조건 세션 유저의 이메일로 강제 고정하여 투명성 확보.
     */
    @Transactional
    public CommonResult writeComment(ItemReviewCommentEntity comment, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;

        String content = normalizeText(comment.getContent(), 1, 100);
        if (content.isEmpty()) return CommonResult.FAILURE;

        comment.setContent(content);
        comment.setUserEmail(user.getEmail()); // [Identity Pinning] 강제 적용

        return this.reviewMapper.insertComment(comment) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }

    /**
     * [댓글 목록 조회]
     * 특정 리뷰에 달린 모든 댓글 및 대댓글 리스트를 실시간 조회.
     */
    public List<ItemReviewCommentEntity> getComments(Long reviewId) {
        return this.reviewMapper.selectCommentsByReviewId(reviewId);
    }

    /**
     * [댓글 수정: 무단 변조 및 소유권 검증]
     * 사유: selectCommentById를 통해 원본 작성자의 이메일을 실시간 대조하여 무단 변조를 차단함.
     */
    @Transactional
    public CommonResult modifyComment(Long commentId, String content, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;

        ItemReviewCommentEntity comment = this.reviewMapper.selectCommentById(commentId);
        // 작성자 본인 확인 절차 필수 이행
        if (comment == null || !comment.getUserEmail().equals(user.getEmail())) return CommonResult.FAILURE;

        String normalized = normalizeText(content, 1, 100);
        if (normalized.isEmpty()) return CommonResult.FAILURE;

        comment.setContent(normalized);
        return this.reviewMapper.updateComment(comment) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }

    /**
     * [댓글 삭제: 데이터 고아 현상 및 계층 파괴 방어]
     * 정규화 기준: '참조 무결성 유지 정책'
     * 사유: 하위 답글이 존재하는 원본 댓글 삭제를 차단하여 데이터 계층 구조의 안정성을 확보함.
     */
    @Transactional
    public CommonResult deleteComment(Long commentId, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;

        ItemReviewCommentEntity comment = this.reviewMapper.selectCommentById(commentId);
        if (comment == null) return CommonResult.FAILURE;

        // [정책] 자식 레코드(대댓글) 존재 여부를 선행 확인하여 논리적 고립 방지.
        int childCount = this.reviewMapper.selectChildCountByParentId(commentId);
        if (childCount > 0) return CommonResult.FAILURE;

        boolean canDelete = comment.getUserEmail().equals(user.getEmail()) || user.getLevel() == 1;
        return canDelete && this.reviewMapper.deleteComment(commentId) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }
}