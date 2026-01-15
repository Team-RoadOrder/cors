package dev.gmpark.cors.vos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesStatusVo {
    private long currentSales;
    private double salesChangePercent;
    private int currentOrderCount;
    private double orderCountChangePercent;
}
