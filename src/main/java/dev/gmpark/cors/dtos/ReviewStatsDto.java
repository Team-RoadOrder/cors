package dev.gmpark.cors.dtos;

// 리뷰 별점 통계
public class ReviewStatsDto {
    private double averageRating;   // 평균 점수 (예: 4.5)
    private int totalCount;        // 전체 리뷰 개수 (예: 2개)
    private int[] ratingCounts;    // 각 점수별 개수 (1점~5점)

    public ReviewStatsDto(double averageRating, int totalCount, int[] ratingCounts) {
        this.averageRating = averageRating;
        this.totalCount = totalCount;
        this.ratingCounts = ratingCounts;
    }

    public double getAverageRating() { return averageRating; }
    public int getTotalCount() { return totalCount; }
    public int[] getRatingCounts() { return ratingCounts; }
}