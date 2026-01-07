package dev.gmpark.cors.services;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ReservationItemsEntity;
import dev.gmpark.cors.mappers.RegisterMapper;
import dev.gmpark.cors.mappers.ReservationMapper;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.vos.ReservationItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyService {
    private final ReservationMapper reservationMapper;
    private final RegisterMapper registerMapper;
    public List<ReservationItemVo> getAllReservations(RegisterEntity sessionUser) {
        return this.reservationMapper.selectReservationsByEmail(sessionUser.getEmail());
    }
    public CommonResult updateUserName(RegisterEntity sessionUser, String newName) {
        RegisterEntity dbUser = this.registerMapper.selectByEmail(sessionUser.getEmail());

        if (dbUser == null) {
            return CommonResult.FAILURE;
        }
        dbUser.setName(newName);
        int rows = this.registerMapper.update(dbUser);

        if (rows > 0) {
            sessionUser.setName(newName);
            return CommonResult.SUCCESS;
        } else {
            return CommonResult.FAILURE;
        }
    }
    public CommonResult updateUserPhone(RegisterEntity sessionUser, String newPhone) {
        RegisterEntity dbUser = this.registerMapper.selectByEmail(sessionUser.getEmail());
        if (dbUser == null) {
            return CommonResult.FAILURE;
        }
        dbUser.setPhone(newPhone);
        int rows = this.registerMapper.update(dbUser);
        if (rows > 0) {
            sessionUser.setPhone(newPhone);
            return CommonResult.SUCCESS;
        } else {
            return CommonResult.FAILURE;
        }
    }
}
