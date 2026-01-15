package dev.gmpark.cors.mappers;

import dev.gmpark.cors.entities.OwnerMemberEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OwnerMemberMapper {
    // 1. 이메일 중복 체크 (전체 대상) - 동일
    int countByEmail(@Param("email") String email);

    // 2. [수정] 해당 매장(shopId) 내 최고 관리자 인원 수 확인
    // storeName 대신 shopId를 사용합니다.
    int countAdminByShop(@Param("shopId") Long shopId, @Param("level") int level);

    // 3. [수정] 매장별 임직원 목록 조회
    // 이제 storeName이 아닌 shopId로 조회하여 정확한 매장 소속을 식별합니다.
    List<OwnerMemberEntity> selectMembersByShop(@Param("shopId") Long shopId,
                                                @Param("myLevel") int myLevel,
                                                @Param("filterLevel") int filterLevel,
                                                @Param("keyword") String keyword);

    // 4. 임직원 추가 (Entity에 shopId가 포함되어 있어야 함)
    int insertMember(OwnerMemberEntity member);

    // 5. 임직원 정보 수정
    int updateMemberByAdmin(OwnerMemberEntity member);

    // 6. 임직원 삭제
    int deleteMemberByEmail(@Param("email") String email);

    // 7. [수정] 단일 조회 (유저 정보 + 매장 정보를 한 번에 가져오는 쿼리로 발전 가능)
    OwnerMemberEntity selectMemberByEmail(@Param("email") String email);

    // 8. 로그아웃 시간 업데이트
    int updateLastLogOutAt(@Param("email") String email);

    /**
     * 9. [수정] shopId 기반 매장 정보 조회
     * 이제 텍스트인 storeName이 아니라, 유저 정보에 담긴 shopId로 조회합니다.
     * 이 메서드가 제대로 작동해야 '화이트라벨' 페이지가 뜨지 않습니다.
     */
    ShopInfoEntity selectShopByShopId(@Param("shopId") Long shopId);
}