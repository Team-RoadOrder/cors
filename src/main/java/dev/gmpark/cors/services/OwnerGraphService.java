package dev.gmpark.cors.services;

import dev.gmpark.cors.mappers.OwnerGraphMapper;
import dev.gmpark.cors.vos.PaymentListVo;
import dev.gmpark.cors.vos.SalesGraphVo;
import dev.gmpark.cors.vos.SalesStatusVo;
import dev.gmpark.cors.vos.TopProductVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerGraphService {
    private final OwnerGraphMapper ownerGraphMapper;

    /**
     * 일별 매출 및 주문수 집계 (주간 차트용)
     * 목적: 12월 말 주문을 포함하기 위해 조회 범위를 최근 14일(2주)로 확장합니다.
     */
    public List<SalesGraphVo> getDailySales(int shopId) {
        LocalDate endDate = LocalDate.now();
        // 기존 7일에서 14일로 확장하여 12월 말의 주문 흐름을 차트에 반영합니다.
        LocalDate startDate = endDate.minusDays(13);

        return ownerGraphMapper.selectDailySales(shopId, startDate, endDate);
    }

    /**
     * 월별 매출 및 주문수 집계 (월간 차트용)
     * 목적: 연말연시(12월-1월) 흐름 파악을 위해 최근 6개월을 정규화하여 조회합니다.
     */
    public List<SalesGraphVo> getMonthlySales(int shopId) {
        LocalDate endDate = LocalDate.now();
        // 5개월 전 1일부터 조회하여 11월, 12월 매출이 누락 없이 합산되도록 합니다.
        LocalDate startDate = endDate.minusMonths(5).withDayOfMonth(1);

        return ownerGraphMapper.selectMonthlySales(shopId, startDate, endDate);
    }

    /**
     * 금주 현황 분석 (전주 대비 정규화된 증감 지표)
     * 특징: 한 사람의 여러 번 주문도 각각의 주문ID로 정확히 합산하여 집계합니다.
     */
    public SalesStatusVo getWeeklyStatus(int shopId) {
        LocalDate today = LocalDate.now();

        // 주간 범위 계산 (월요일 시작 기준)
        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = thisWeekStart.minusDays(1);

        // Optional을 사용해 데이터가 없는 경우(Null) 0으로 정규화하여 NullPointerException을 방지합니다.
        long thisWeekSales = Optional.ofNullable(ownerGraphMapper.selectWeeklySales(shopId, thisWeekStart, today)).orElse(0L);
        int thisWeekOrders = Optional.ofNullable(ownerGraphMapper.selectWeeklyOrderCount(shopId, thisWeekStart, today)).orElse(0);

        long lastWeekSales = Optional.ofNullable(ownerGraphMapper.selectWeeklySales(shopId, lastWeekStart, lastWeekEnd)).orElse(0L);
        int lastWeekOrders = Optional.ofNullable(ownerGraphMapper.selectWeeklyOrderCount(shopId, lastWeekStart, lastWeekEnd)).orElse(0);

        // 증감율 계산 로직을 별도 메서드로 정규화하여 가독성을 높였습니다.
        return SalesStatusVo.builder()
                .currentSales(thisWeekSales)
                .salesChangePercent(calculateChangePercent(lastWeekSales, thisWeekSales))
                .currentOrderCount(thisWeekOrders)
                .orderCountChangePercent(calculateChangePercent((long) lastWeekOrders, (long) thisWeekOrders))
                .build();
    }

    /**
     * 증감율 계산 공통 유틸리티 (0으로 나누기 방지 및 반올림 처리)
     */
    private double calculateChangePercent(Long last, Long current) {
        if (last == null || last == 0) {
            return (current != null && current > 0) ? 100.0 : 0.0;
        }
        double percent = ((double) (current - last) / last) * 100;
        return Math.round(percent * 10.0) / 10.0; // 소수점 첫째자리 반올림
    }

    /**
     * 최근 결제 리스트 (최신순 5건)
     */
    public List<PaymentListVo> getRecentPayments(int shopId) {
        return ownerGraphMapper.selectRecentPayments(shopId, 5);
    }

    /**
     * 매출 상위 제품 리스트 (정규화된 순위 부여)
     */
    public List<TopProductVo> getTopProducts(int shopId) {
        List<TopProductVo> products = ownerGraphMapper.selectTopProducts(shopId, 5);
        AtomicInteger rank = new AtomicInteger(1);

        return products.stream()
                .peek(p -> p.setRank(rank.getAndIncrement()))
                .collect(Collectors.toList());
    }
}