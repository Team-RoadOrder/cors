package dev.gmpark.cors.vos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesGraphVo {
    private String date;
    private long sales;
    private int orderCount;
}
