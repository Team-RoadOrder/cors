package dev.gmpark.cors.entities;


import lombok.*;
import lombok.experimental.SuperBuilder;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(of = "id")
public class ReservationItemsEntity {
        private int id;
        private int reservationId;
        private Long itemId;
        private String size;
}

