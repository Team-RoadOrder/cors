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


    private String normalizeText(String text, int min, int max) {
        if (text == null) return "";
        String cleaned = HtmlUtils.htmlEscape(text.trim()).replaceAll("\\s{2,}", " ");
        return (cleaned.length() < min || cleaned.length() > max) ? "" : cleaned;
    }



    @Transactional
    public CommonResult writeReview(ItemReviewEntity review, MultipartFile[] images, RegisterEntity user) {
        if (user == null) return CommonResult.FAILURE_SESSION;


        String content = normalizeText(review.getContent(), 1, 100);
        if (content.isEmpty() || (images != null && images.length > 3)) return CommonResult.FAILURE;
        review.setContent(content);

        Integer validOrderItemId = this.reviewMapper.selectAvailableReservationId(user.getEmail(), review.getItemId());


        if (validOrderItemId == null || validOrderItemId <= 0) {
            return CommonResult.FAILURE;
        }

        review.setReservationId(validOrderItemId.intValue());
        review.setUserEmail(user.getEmail());

        review.setUserEmail(user.getEmail());


        if (this.reviewMapper.insertReview(review) <= 0) return CommonResult.FAILURE;


        if (images != null && images.length > 0) return saveImages(review.getId(), images);

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

        String normalized = normalizeText(content, 1, 100);
        if (normalized.isEmpty()) return CommonResult.FAILURE;


        ItemReviewEntity entity = ItemReviewEntity.builder()
                .id(reviewId)
                .content(normalized)
                .rating(rating)
                .build();
        if (this.reviewMapper.updateReview(entity) <= 0) return CommonResult.FAILURE;


        if (images != null && images.length > 0) {

            List<String> oldFiles = this.reviewMapper.selectImagesByReviewId(reviewId);


            if (this.reviewMapper.deleteImagesByReviewId(reviewId) >= 0) {

                deletePhysicalFiles(oldFiles);
                return saveImages(reviewId, images);
            }
        }

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


        boolean isOwner = review.getUserEmail().equals(user.getEmail());
        boolean isShopOwner = this.reviewMapper.isShopOwnerOfReview(review.getShopId(), user.getEmail()) > 0;
        boolean isAdmin = user.getLevel() == 1;

        if (isOwner || isShopOwner || isAdmin) {
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
        if (content.isEmpty()) return CommonResult.FAILURE;

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

        if (comment == null || !comment.getUserEmail().equals(user.getEmail())) return CommonResult.FAILURE;

        String normalized = normalizeText(content, 1, 100);
        if (normalized.isEmpty()) return CommonResult.FAILURE;

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
}