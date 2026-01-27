package dev.gmpark.cors.entities;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(of = "id")
public class ReservationEntity {
    private int id;
    private String userEmail;
    private int shopId;
    private LocalDateTime visitDate;
    private String status;
    private LocalDateTime createdAt;
}
