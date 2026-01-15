package dev.gmpark.cors.vos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentListVo {
    private String customerName;
    private String productName;
    private String status;
    private LocalDateTime orderDate;

}
