package dev.gmpark.cors.services;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ReservationItemsEntity;
import dev.gmpark.cors.mappers.ReservationMapper;
import dev.gmpark.cors.vos.ReservationItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyService {
    private final ReservationMapper reservationMapper;
    public List<ReservationItemVo> getAllReservations(RegisterEntity sessionUser) {
        return this.reservationMapper.selectReservationsByEmail(sessionUser.getEmail());
    }
}
