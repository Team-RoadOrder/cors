package dev.gmpark.cors.services;

import dev.gmpark.cors.dtos.ReviewStatsDto;
import dev.gmpark.cors.entities.*;
import dev.gmpark.cors.mappers.OrderMapper;
import dev.gmpark.cors.mappers.ReportMapper;
import dev.gmpark.cors.mappers.ReviewMapper;
import dev.gmpark.cors.results.CommonResult;
import dev.gmpark.cors.vos.ReviewReportVo;
import dev.gmpark.cors.vos.ReviewVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;
import dev.gmpark.cors.validators.BadWordValidator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final BadWordValidator badWordValidator; //비속어/유해정보차단
    private final ReportMapper reportMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;


    private String normalizeText(String text, int min, int max) {
        if (text == null) return "";
        String cleaned = HtmlUtils.htmlEscape(text.trim()).replaceAll("\\s{2,}", " ");
        return (cleaned.length() < min || cleaned.length() > max) ? "" : cleaned;
    }



    @Transactional
    public CommonResult writeReview(ItemReviewEntity review, MultipartFile[] images, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;


        String content = normalizeText(review.getContent(), 1, 100);
        if (content.isEmpty() || badWordValidator.isBad(content)) return CommonResult.FAILURE;


        if (images != null && images.length > 3) return CommonResult.FAILURE;


        Integer validReservationId = this.reviewMapper.selectAvailableReservationId(user.getEmail(), review.getItemId());

        if (validReservationId == null || validReservationId <= 0) {
            return CommonResult.FAILURE;
        }


        review.setContent(content);
        review.setReservationId(validReservationId);
        review.setUserEmail(user.getEmail());


        if (this.reviewMapper.insertReview(review) <= 0) return CommonResult.FAILURE;


        if (images != null && images.length > 0) {
            return saveImages(review.getId(), images);
        }

        return CommonResult.SUCCESS;
    }


    private CommonResult saveImages(Long reviewId, MultipartFile[] images) {

//        String basePath = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        String reviewPath = getReviewPath();

        try {
            File dir = new File(reviewPath);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    System.err.println("디렉토리 생성 실패: " + reviewPath);
                    throw new IOException("Critical: Cannot create review directory.");
                }
            }

            for (MultipartFile file : images) {
                if (file.isEmpty()) continue;

                String originalName = file.getOriginalFilename();
                String ext = (originalName != null && originalName.contains("."))
                        ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase() : ".jpg";


                String uuidName = UUID.randomUUID().toString() + ext;

                file.transferTo(new File(reviewPath + uuidName));

                ItemReviewImageEntity imgEntity = ItemReviewImageEntity.builder()
                        .reviewId(reviewId)
                        .imageData(uuidName)
                        .build();

                if (this.reviewMapper.insertReviewImage(imgEntity) <= 0) throw new RuntimeException();
            }
            return CommonResult.SUCCESS;
        } catch (IOException | RuntimeException e) {

            e.printStackTrace();
            throw new RuntimeException("Image storage failed - triggering rollback", e);
        }
    }



    public List<ReviewVo> getReviews(Long itemId, String sortType) {
        List<ReviewVo> reviews = this.reviewMapper.selectReviewsByItemId(itemId, sortType);
        if (reviews != null) {

            reviews.forEach(r -> r.setImages(this.reviewMapper.selectImagesByReviewId(r.getId())));
        }
        return reviews;
    }

    public ReviewVo getReviewById(Long reviewId) {
        ReviewVo review = this.reviewMapper.selectReviewById(reviewId);
        if (review != null) {
            review.setImages(this.reviewMapper.selectImagesByReviewId(reviewId));
        }
        return review;
    }

    @Transactional
    public CommonResult modifyReview(Long reviewId, String content, int rating, MultipartFile[] images, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;

        ReviewVo review = this.reviewMapper.selectReviewById(reviewId);
        if (review == null || !review.getUserEmail().equals(user.getEmail())) return CommonResult.FAILURE;

        // [보안 추가] 관리자가 문구 치환 방식으로 처리했을 경우를 대비한 2차 검증
        if (review.getContent() != null && review.getContent().contains("운영 정책 위반")) {
            return CommonResult.FAILURE;
        }

        String normalized = normalizeText(content, 1, 100);
        if (normalized.isEmpty() || badWordValidator.isBad(normalized)) {
            return CommonResult.FAILURE;
        }

        // 이하 기존 수정 로직 유지...
        ItemReviewEntity entity = ItemReviewEntity.builder().id(reviewId).content(normalized).rating(rating).build();
        if (this.reviewMapper.updateReview(entity) <= 0) return CommonResult.FAILURE;
        // 이미지 처리 로직 생략...
        return CommonResult.SUCCESS;
    }


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


    public List<String> getAllImages(Long itemId) {
        return this.reviewMapper.selectAllReviewImages(itemId);
    }



    @Transactional
    public CommonResult deleteReview(Long reviewId, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;

        ReviewVo review = this.reviewMapper.selectReviewById(reviewId);
        if (review == null) return CommonResult.FAILURE;

        // [정규화] 관리자 판별 조건을 processReport와 동일하게 맞춤
        boolean isOwner = review.getUserEmail().equals(user.getEmail());
        boolean isShopOwner = this.reviewMapper.isShopOwnerOfReview(review.getShopId(), user.getEmail()) > 0;
        boolean isAdmin = user.getLevel() == 1 || "admin".equalsIgnoreCase(user.getUsertype());

        if (isOwner || isShopOwner || isAdmin) {
            // [중요] 리뷰 하위 자식 데이터(댓글, 좋아요)를 여기서도 한 번 더 체크하여 삭제
            // 일반 사용자가 자기 글을 지울 때도 외래키 오류가 나지 않도록 방어 로직 추가
            this.reviewMapper.deleteCommentsByReviewId(reviewId);
            this.reviewMapper.deleteLikesByReviewId(reviewId);

            List<String> files = this.reviewMapper.selectImagesByReviewId(reviewId);
            if (this.reviewMapper.deleteReview(reviewId) > 0) {
                deletePhysicalFiles(files);
                return CommonResult.SUCCESS;
            }
        }
        return CommonResult.FAILURE;
    }

   //이미지 경로 공통
    private String getReviewPath() {
        String dir = uploadDir.endsWith(File.separator) ? uploadDir : uploadDir + File.separator;
        return dir + "review" + File.separator;
    }


    private void deletePhysicalFiles(List<String> files) {
//        String dir = uploadDir.endsWith(File.separator) ? uploadDir : uploadDir + File.separator;
        String reviewPath = getReviewPath();

        files.forEach(f -> {
            File file = new File(reviewPath + f);
            if (file.exists()) {
                if (!file.delete()) {
                    System.err.println("파일 삭제 실패 (경로/권한 확인 필요): " + f);
                }
            }
        });
    }


    public Integer getAvailableReservationId(String email, Long itemId) {
        return this.reviewMapper.selectAvailableReservationId(email, itemId);
    }


    @Transactional
    public CommonResult toggleReviewLike(Long reviewId, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;

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


    public int getReviewLikeCount(Long reviewId) {
        return this.reviewMapper.selectReviewLikeCount(reviewId);
    }


    @Transactional
    public CommonResult writeComment(ItemReviewCommentEntity comment, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;

        String content = normalizeText(comment.getContent(), 1, 100);
        if (content.isEmpty() || badWordValidator.isBad(content)) {
            return CommonResult.FAILURE;
        }

        comment.setContent(content);
        comment.setUserEmail(user.getEmail()); // [Identity Pinning] 강제 적용

        return this.reviewMapper.insertComment(comment) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }


    public List<ItemReviewCommentEntity> getComments(Long reviewId) {
        return this.reviewMapper.selectCommentsByReviewId(reviewId);
    }


    @Transactional
    public CommonResult modifyComment(Long commentId, String content, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;

        ItemReviewCommentEntity comment = this.reviewMapper.selectCommentById(commentId);
        if (comment == null) return CommonResult.FAILURE;


        if (!comment.getUserEmail().equals(user.getEmail())) return CommonResult.FAILURE;

        if (comment.getContent().contains("운영 정책 위반으로 인해 블라인드")) {
            return CommonResult.FAILURE;
        }

        String normalized = normalizeText(content, 1, 100);
        if (normalized.isEmpty() || badWordValidator.isBad(normalized)) {
            return CommonResult.FAILURE;
        }

        comment.setContent(normalized);
        return this.reviewMapper.updateComment(comment) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }


    @Transactional
    public CommonResult deleteComment(Long commentId, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;

        ItemReviewCommentEntity comment = this.reviewMapper.selectCommentById(commentId);
        if (comment == null) return CommonResult.FAILURE;


        int childCount = this.reviewMapper.selectChildCountByParentId(commentId);
        if (childCount > 0) return CommonResult.FAILURE;

        boolean canDelete = comment.getUserEmail().equals(user.getEmail()) || user.getLevel() == 1;
        return canDelete && this.reviewMapper.deleteComment(commentId) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }


    @Transactional
    public CommonResult reportReviewOrComment(String type, Long id, String reason, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;

        ReviewReportEntity report = ReviewReportEntity.builder()
                .targetType(type)
                .targetId(id)
                .reporterEmail(user.getEmail())
                .reporterName(user.getName())
                .reasonCode(reason)
                .build();

        try {
            return this.reportMapper.insertReport(report) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
        } catch (Exception e) {

            return CommonResult.FAILURE;
        }
    }

    public List<ReviewReportVo> getReportSummaryList(int limit, int offset) {
        return this.reportMapper.selectReportSummaryList(limit, offset);
    }

    @Transactional
    public CommonResult processReport(String targetType, Long targetId, RegisterEntity sessionUser) {
        if (sessionUser == null || !"admin".equalsIgnoreCase(sessionUser.getUsertype())) {
            return CommonResult.FAILURE;
        }

        CommonResult result = CommonResult.FAILURE;

        if ("REVIEW".equals(targetType)) {

            this.reportMapper.deleteReportsByTarget("REVIEW", targetId);

            this.reportMapper.deleteChildReportsByReviewId(targetId);


            this.reviewMapper.deleteCommentsByReviewId(targetId);
            this.reviewMapper.deleteLikesByReviewId(targetId);


            result = this.deleteReview(targetId, sessionUser);
        }
        else if ("COMMENT".equals(targetType)) {
            ItemReviewCommentEntity comment = this.reviewMapper.selectCommentById(targetId);
            if (comment != null) {

                comment.setContent("해당 댓글은 운영 정책 위반으로 인해 블라인드 처리되었습니다.");
                result = this.reviewMapper.updateComment(comment) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;

                if (result == CommonResult.SUCCESS) {
                    this.reportMapper.deleteReportsByTarget("COMMENT", targetId);
                }
            }
        }
        return result;
    }
    @Transactional
    public CommonResult keepReport(String targetType, Long targetId) {
        // 신고 테이블에서 해당 건을 삭제하여 리스트에서 제거함
        return this.reportMapper.deleteReportsByTarget(targetType, targetId) > 0
                ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }
}