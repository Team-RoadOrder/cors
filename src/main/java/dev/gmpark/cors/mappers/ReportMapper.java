package dev.gmpark.cors.mappers;

import dev.gmpark.cors.entities.ReviewReportEntity;
import dev.gmpark.cors.vos.ReviewReportVo; // 원문 내용을 담을 VO 추가 권장
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ReportMapper {
    int insertReport(ReviewReportEntity report);

    // 관리자용: 신고 목록 조회
    List<ReviewReportVo> selectReportSummaryList(@Param("limit") int limit, @Param("offset") int offset);

    int updateReportStatus(@Param("targetType") String targetType,
                           @Param("targetId") Long targetId,
                           @Param("status") String status);

    // [정규화] 특정 타겟 신고 삭제
    int deleteReportsByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    // [수정] 리뷰 하위 댓글들의 신고 기록 일괄 삭제 (영향받은 행 수 반환)
    int deleteChildReportsByReviewId(@Param("targetId") Long targetId);
}