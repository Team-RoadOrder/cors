package dev.gmpark.cors.vos;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ReviewReportVo {
    // 신고 요약 정보
    private String targetType;      // 'REVIEW' 또는 'COMMENT'
    private Long targetId;          // 원본 PK
    private int reportCount;        // 누적 신고 횟수
    private LocalDateTime lastReportedAt; // 가장 최근 신고 일시

    // 원문 정보 (JOIN 결과)
    private String originalContent; // 원문 내용
    private String authorEmail;     // 작성자 이메일
    private String category;        // 'REVIEW' 또는 'COMMENT'

    // [추가 포인트 1] 원본 게시글 작성일 (작성일이 "없음"으로 뜨던 원인 해결)
    private LocalDateTime originalCreatedAt;

    // [추가 포인트 2] 신고 사유 코드 (undefined 방지)
    private String reasonCode;

    private String status;          // 처리 상태
}