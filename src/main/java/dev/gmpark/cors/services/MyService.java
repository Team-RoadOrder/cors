package dev.gmpark.cors.services;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ReservationItemsEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.mappers.RegisterMapper;
import dev.gmpark.cors.mappers.ReservationMapper;
import dev.gmpark.cors.mappers.ShopInfoMapper;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.vos.LikeItemVo;
import dev.gmpark.cors.vos.LikeShopVo;
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
    private final ShopInfoMapper shopInfoMapper;
    private final OwnerShopMapper ownerShopMapper;
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
    public CommonResult updateUserAddress(RegisterEntity sessionUser, String newAddress, String newAddressDetail) {
        RegisterEntity dbUser = this.registerMapper.selectByEmail(sessionUser.getEmail());

        if (dbUser == null) {
            return CommonResult.FAILURE;
        }

        // DB 업데이트용 객체 설정
        dbUser.setAddress(newAddress);
        dbUser.setAddressDetail(newAddressDetail);

        int rows = this.registerMapper.update(dbUser);

        if (rows > 0) {
            // [수정 핵심] dbUser가 아니라 sessionUser(메모리에 있는 로그인 정보)를 업데이트해야 함
            sessionUser.setAddress(newAddress);
            sessionUser.setAddressDetail(newAddressDetail);

            return CommonResult.SUCCESS;
        } else {
            return CommonResult.FAILURE;
        }
    }
    public LikeShopVo[] getLikeShops(RegisterEntity sessionUser) {
        return this.shopInfoMapper.selectLikedShopsByUser(sessionUser.getEmail());

    }
    public LikeItemVo[] getLikeItems(RegisterEntity sessionUser) {
        if (sessionUser == null) return new LikeItemVo[0];
        return this.ownerShopMapper.selectLikeItemsByUser(sessionUser.getEmail());
    }
}
