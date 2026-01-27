package dev.gmpark.cors.dtos;

import dev.gmpark.cors.entities.ReservationItemsEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
public class ReservationDto {
    private int shopId;
    private LocalDateTime visitDate;

    private List<ReservationItemsEntity> items;
}