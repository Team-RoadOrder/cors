package dev.gmpark.cors.entities;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ReviewReportEntity {
    private Long id;              // PK
    private String targetType;    // 'REVIEW' 또는 'COMMENT'
    private Long targetId;        // 대상의 ID (item_reviews 또는 item_review_comments)
    private String reporterEmail; // 신고자 이메일
    private String reporterName;  // 신고자 이름 (기록 보존용)
    private String reasonCode;    // 신고 사유 코드 (SPAM, ABUSE 등)
    private String status;        // 처리 상태 (기본값: PENDING)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}