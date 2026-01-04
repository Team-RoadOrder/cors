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

    // 여기가 핵심! 상품은 여러 개일 수 있으니 List로 받습니다.
    private List<ReservationItemsEntity> items;
}