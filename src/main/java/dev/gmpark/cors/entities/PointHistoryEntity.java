package dev.gmpark.cors.entities;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "index")
public class PointHistoryEntity {
    private int index;
    private String userEmail;
    private int amount;
    private String type; // "EARN" or "USE"
    private String orderId;
    private LocalDateTime createdAt;
}