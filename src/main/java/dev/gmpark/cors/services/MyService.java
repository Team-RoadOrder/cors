package dev.gmpark.cors.services;


import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ReservationItemsEntity;
import dev.gmpark.cors.mappers.*;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.validators.OwnerMemberValidator;
import dev.gmpark.cors.vos.LikeItemVo;
import dev.gmpark.cors.vos.LikeShopVo;
import dev.gmpark.cors.vos.OrderHistoryVo;
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
    private final OrderMapper orderMapper;
    public List<ReservationItemVo> getAllReservations(RegisterEntity sessionUser) {
        return this.reservationMapper.selectReservationsByEmail(sessionUser.getEmail());
    }
    public CommonResult updateUserName(RegisterEntity sessionUser, String newName) {
        if (!OwnerMemberValidator.validateName(newName)) {
            return CommonResult.FAILURE;
        }
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
        if (!OwnerMemberValidator.validatePhone(newPhone)) {
            return CommonResult.FAILURE;
        }
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
        if (!OwnerMemberValidator.validateAddress(newAddress, newAddressDetail)) {
            return CommonResult.FAILURE;
        }
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
    public CommonResult updateUserStyle(RegisterEntity sessionUser, String newStyle) {
        RegisterEntity dbUser = this.registerMapper.selectByEmail(sessionUser.getEmail());

        if (dbUser == null) {
            return CommonResult.FAILURE;
        }

        dbUser.setStyle(newStyle);
        int rows = this.registerMapper.update(dbUser);

        if (rows > 0) {
            sessionUser.setStyle(newStyle);
            return CommonResult.SUCCESS;
        } else {
            return CommonResult.FAILURE;
        }
    }
    public CommonResult deleteUser(RegisterEntity sessionUser) {
        RegisterEntity dbUser = this.registerMapper.selectByEmail(sessionUser.getEmail());
        if (dbUser == null) {
            return CommonResult.FAILURE;
        }
        int rows = this.registerMapper.delete(dbUser);
        if (rows > 0) {
            return CommonResult.SUCCESS;
        }  else {
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
    public OrderHistoryVo[] getOrderHistory(RegisterEntity sessionUser) {
        return this.orderMapper.getAllOrders( sessionUser.getEmail());
    }
}
