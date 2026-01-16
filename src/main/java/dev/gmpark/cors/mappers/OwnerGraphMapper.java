package dev.gmpark.cors.mappers;

import dev.gmpark.cors.vos.PaymentListVo;
import dev.gmpark.cors.vos.SalesGraphVo;
import dev.gmpark.cors.vos.TopProductVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface OwnerGraphMapper {
    // 매출 추이 그래프 데이터 (일별)
    List<SalesGraphVo> selectDailySales(@Param("shopId") int shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 매출 추이 그래프 데이터 (월별)
    List<SalesGraphVo> selectMonthlySales(@Param("shopId") int shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 금주 매출 및 주문수
    Long selectWeeklySales(@Param("shopId") int shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    Integer selectWeeklyOrderCount(@Param("shopId") int shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 금주 결제 리스트
//    List<PaymentListVo> selectRecentPayments(@Param("shopId") int shopId, @Param("limit") int limit); : 전체기록 목적으로 주석처리
    List<PaymentListVo> selectRecentPayments(@Param("shopId") int shopId);

    // 매출 상위 제품
    List<TopProductVo> selectTopProducts(@Param("shopId") int shopId, @Param("limit") int limit);
}
