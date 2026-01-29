package dev.gmpark.cors.entities;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class OrderEntity {
    private Long id;
    private String orderId; // Removed as we use ID
    private String paymentKey; // Added for Toss Payments
    private LocalDateTime paidAt; // Added for Toss Payments
    private String userEmail;
    private Long totalPrice;
    private String status;
    private LocalDateTime createdAt;
    private String receiverName;
    private String receiverPhone;
    private String address;
    private String addressDetail;
    private String request;
}
