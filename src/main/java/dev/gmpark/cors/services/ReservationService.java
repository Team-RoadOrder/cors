package dev.gmpark.cors.services;


import dev.gmpark.cors.dtos.ReservationDto;
import dev.gmpark.cors.entities.ReservationEntity;
import dev.gmpark.cors.entities.ReservationItemsEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.mappers.ReservationMapper;
import dev.gmpark.cors.mappers.ShopInfoMapper;
import dev.gmpark.cors.results.CommonResult;
import dev.gmpark.cors.vos.ReservationItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final OwnerShopMapper ownerShopMapper;
    private final ReservationMapper reservationMapper;
    private final ShopInfoMapper shopInfoMapper;
    public ShopItemEntity[] getItemsByShopId(ShopInfoEntity shopInfo) {
       return this.ownerShopMapper.selectAllByShopId(shopInfo.getShopId());
    }
    @Transactional // 예약과 상품 저장이 모두 성공하거나, 모두 실패해야 함
    public CommonResult registerReservation(ReservationDto dto, String userEmail) {
        LocalDateTime now = LocalDateTime.now();

        if (dto.getVisitDate() == null || dto.getVisitDate().isBefore(now)) {
            return CommonResult.FAILURE;
        }

        ReservationEntity reservation = ReservationEntity.builder()
                .userEmail(userEmail)
                .shopId(dto.getShopId()) // int -> Long 자동 변환 주의 (Entity가 Long이면 DTO도 Long 추천)
                .visitDate(dto.getVisitDate())
                .status("대기")
                .createdAt(now) // 생성 시간 명시적 설정
                .build();

        int insertCount = this.reservationMapper.insertReservation(reservation);
        if (insertCount == 0) return CommonResult.FAILURE;

        // 3. 상품들(Items)에 방금 만든 예약번호(ID) 심어주기
        for (ReservationItemsEntity item : dto.getItems()) {
            item.setReservationId(reservation.getId()); // 여기가 핵심 연결고리!
        }

        // 4. 상품들 insert (한 번에 넣거나 반복문으로 넣거나)
        this.reservationMapper.insertReservationItems(dto.getItems());

        return CommonResult.SUCCESS;
    }
    public CommonResult deleteReservation( int reservationId ) {
        ReservationItemVo dbReservation = this.reservationMapper.selectReservationById(reservationId);
        if (dbReservation == null) return CommonResult.FAILURE;
        if (!dbReservation.getStatus().equals("대기")) {
            return CommonResult.FAILURE;
        }
        // 대기 상태일때를 제외하고는 삭제불가함
      return   this.reservationMapper.deleteReservationById(reservationId) > 0
                ? CommonResult.SUCCESS
                : CommonResult.FAILURE;
    }


}
