package dev.gmpark.cors.vos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopProductVo {
    private int rank;
    private String productName;
    private long totalSales;
    private int totalQuantity; // 판매 수량 추가
    private String imagePath;
}
