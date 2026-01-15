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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerGraphService {
    private final OwnerGraphMapper ownerGraphMapper;

    public List<SalesGraphVo> getDailySales(int shopId) {
        // 최근 1주일 데이터 (오늘 포함)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        return ownerGraphMapper.selectDailySales(shopId, startDate, endDate);
    }

    public List<SalesGraphVo> getMonthlySales(int shopId) {
        // 최근 6개월 데이터 (이번달 포함)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(5).withDayOfMonth(1);
        return ownerGraphMapper.selectMonthlySales(shopId, startDate, endDate);
    }

    public SalesStatusVo getWeeklyStatus(int shopId) {
        LocalDate today = LocalDate.now();
        
        // 이번주 (월요일 ~ 오늘)
        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate thisWeekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)); // 이번주 일요일까지로 설정하거나 오늘까지로 설정

        // 지난주 (지난주 월요일 ~ 지난주 일요일)
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = thisWeekStart.minusDays(1);

        Long thisWeekSales = ownerGraphMapper.selectWeeklySales(shopId, thisWeekStart, thisWeekEnd);
        Integer thisWeekOrderCount = ownerGraphMapper.selectWeeklyOrderCount(shopId, thisWeekStart, thisWeekEnd);

        Long lastWeekSales = ownerGraphMapper.selectWeeklySales(shopId, lastWeekStart, lastWeekEnd);
        Integer lastWeekOrderCount = ownerGraphMapper.selectWeeklyOrderCount(shopId, lastWeekStart, lastWeekEnd);

        double salesChangePercent = 0.0;
        if (lastWeekSales > 0) {
            salesChangePercent = ((double) (thisWeekSales - lastWeekSales) / lastWeekSales) * 100;
        } else if (thisWeekSales > 0) {
            salesChangePercent = 100.0; // 지난주 0, 이번주 있음 -> 100% 증가로 표시 (혹은 무한대)
        }

        double orderCountChangePercent = 0.0;
        if (lastWeekOrderCount > 0) {
            orderCountChangePercent = ((double) (thisWeekOrderCount - lastWeekOrderCount) / lastWeekOrderCount) * 100;
        } else if (thisWeekOrderCount > 0) {
            orderCountChangePercent = 100.0;
        }

        return SalesStatusVo.builder()
                .currentSales(thisWeekSales)
                .salesChangePercent(Math.round(salesChangePercent * 10.0) / 10.0) // 소수점 첫째자리 반올림
                .currentOrderCount(thisWeekOrderCount)
                .orderCountChangePercent(Math.round(orderCountChangePercent * 10.0) / 10.0)
                .build();
    }

    public List<PaymentListVo> getRecentPayments(int shopId) {
        return ownerGraphMapper.selectRecentPayments(shopId, 5);
    }

    public List<TopProductVo> getTopProducts(int shopId) {
        List<TopProductVo> products = ownerGraphMapper.selectTopProducts(shopId, 5);
        AtomicInteger rank = new AtomicInteger(1);
        return products.stream()
                .peek(p -> p.setRank(rank.getAndIncrement()))
                .collect(Collectors.toList());
    }
}
