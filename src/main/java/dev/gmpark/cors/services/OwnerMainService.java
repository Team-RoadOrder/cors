package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.mappers.OrderMapper;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.mappers.ReservationMapper;
import dev.gmpark.cors.mappers.ShopInfoMapper;
import dev.gmpark.cors.results.CommonResult;
import dev.gmpark.cors.validators.OwnerMemberValidator;
import dev.gmpark.cors.validators.ShopItemValidator;
import dev.gmpark.cors.vos.OrderHistoryVo;
import dev.gmpark.cors.vos.ReservationItemVo;
import lombok.RequiredArgsConstructor; // 이걸 쓰면 코드가 깔끔해집니다
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor // 생성자 주입 자동화
public class OwnerMainService {

    private final ShopInfoMapper shopInfoMapper;
    private final OwnerShopMapper ownerShopMapper;
    private final ReservationMapper reservationMapper;
    private final OrderMapper orderMapper;

    // 설정 파일에서 경로 가져오기
    @Value("${file.upload-dir}")
    private String uploadDir;

    public ShopInfoEntity getShopByEmail(String email) {
        return this.shopInfoMapper.selectShopByUserEmail(email);
    }
    public List<ReservationItemVo> getReservationsByShopId(int shopId) {
        return this.reservationMapper.selectReservationsByShopId(shopId);
    }

    public String saveShopInfo(RegisterEntity user, ShopInfoEntity shopInfo) {
        shopInfo.setUserEmail(user.getEmail());
        ShopInfoEntity existingShop = shopInfoMapper.selectShopByUserEmail(user.getEmail());
        boolean isNewShop = (existingShop == null);
        String nameRegex = "^[가-힣a-zA-Z0-9\\s]+$";
        if (shopInfo.getShopName() == null || !shopInfo.getShopName().matches(nameRegex)) {
            return "FAILURE"; // 매장 이름 형식이 맞지 않음
        }
        String timeRegex = "^\\d{2}:\\d{2}\\s*[~-]\\s*\\d{2}:\\d{2}$";
        if (shopInfo.getShopTime() == null || !shopInfo.getShopTime().matches(timeRegex)) {
            return "FAILURE";
        }
        if (shopInfo.getShopCategory() == null || shopInfo.getShopCategory().trim().isEmpty()) {
            return "FAILURE";
        }
        String addressRegex = "^[가-힣a-zA-Z0-9\\s\\-\\(\\)\\[\\]\\.,]{2,100}$";
        if (shopInfo.getShopAddress() == null || !shopInfo.getShopAddress().matches(addressRegex)) {
            return "FAILURE"; // 주소 형식이 맞지 않음
        }
        if (shopInfo.getShopTel() == null || !OwnerMemberValidator.validatePhone(shopInfo.getShopTel())) {
            return "FAILURE"; // 매장 전화번호 형식 오류
        }
        if (isNewShop) {
            if (shopInfo.getProfileImageFile() == null || shopInfo.getProfileImageFile().isEmpty()) {
                return "FAILURE";
            }
            if (shopInfo.getBackgroundImageFile() == null || shopInfo.getBackgroundImageFile().isEmpty()) {
                return "FAILURE";
            }
        }
        try {
            MultipartFile profileFile = shopInfo.getProfileImageFile();
            if (profileFile != null && !profileFile.isEmpty()) {
                String path = uploadFile(profileFile);
                shopInfo.setProfileImage(path);
            } else if (!isNewShop) {
                // 수정인데 파일이 안 넘어왔으면 기존 이미지 경로 유지
                shopInfo.setProfileImage(existingShop.getProfileImage());
            }

            MultipartFile backgroundFile = shopInfo.getBackgroundImageFile();
            if (backgroundFile != null && !backgroundFile.isEmpty()) {
                String path = uploadFile(backgroundFile);
                shopInfo.setBackgroundImage(path);
            } else if (!isNewShop) {
                // 수정인데 파일이 안 넘어왔으면 기존 이미지 경로 유지
                shopInfo.setBackgroundImage(existingShop.getBackgroundImage());
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "FAILURE"; // 파일 저장 중 에러
        }
        int result;
        if (isNewShop) {
            result = shopInfoMapper.insert(shopInfo);
        } else {
            shopInfo.setShopId(existingShop.getShopId());
            result = shopInfoMapper.update(shopInfo);
        }
        return result > 0 ? "SUCCESS" : "FAILURE";
    }

    private String uploadFile(MultipartFile file) throws IOException {
        // [수정] 하드코딩 제거, 설정값 사용
        // 경로 끝에 슬래시 없으면 붙여주는 로직 포함
        String path = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";

        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName != null && originalFileName.contains(".")
                ? originalFileName.substring(originalFileName.lastIndexOf("."))
                : ".jpg";

        String savedFileName = timestamp + "_" + System.currentTimeMillis() + extension;
        File dest = new File(path + savedFileName); // path 변수 사용
        file.transferTo(dest);

        return "/images/" + savedFileName;
    }

    public ShopItemEntity[] getItemsByUser(RegisterEntity user) {
        ShopInfoEntity dbShop = this.shopInfoMapper.selectShopByUserEmail(user.getEmail());
        if (dbShop == null) {
            return new ShopItemEntity[0];
        }
        return this.ownerShopMapper.selectAllByShopId(dbShop.getShopId());
    }
    // 상품수정 : 세션 유저의 매장 ID와 상품의 ID 대조
    public CommonResult modify(RegisterEntity user,ShopItemEntity shopItem ) {
        // [보안] 현재 로그인한 사장님의 매장 정보 조회
        ShopInfoEntity myShop = this.shopInfoMapper.selectShopByUserEmail(user.getEmail());
        //수정하려는 원본 데이터 조회
        ShopItemEntity dbItem = this.ownerShopMapper.selectItemById(shopItem.getId());
        // [IDOR 방어] 매장이 없거나, 상품의 shopId가 내 매장과 다르면 차단
        if (dbItem == null || myShop == null || dbItem.getShopId() != myShop.getShopId()) {
            return CommonResult.FAILURE;
        }
        if (!ShopItemValidator.validateItemName(shopItem)) return CommonResult.FAILURE;
        if (!ShopItemValidator.validateColor(shopItem)) return CommonResult.FAILURE;
        if (!ShopItemValidator.validateSize(shopItem)) return CommonResult.FAILURE;
        if (!ShopItemValidator.validateStyle(shopItem)) return CommonResult.FAILURE;
        if (!ShopItemValidator.validatePrice(shopItem)) return CommonResult.FAILURE;
        if (!ShopItemValidator.validateCategory(shopItem)) return CommonResult.FAILURE;

        if (shopItem.getSize() == null || shopItem.getSize().isBlank()) {
            shopItem.setSize("FREE");
        } else {
            shopItem.setSize(shopItem.getSize().replaceAll("\\s+", ""));
        }
        dbItem.setItemName(shopItem.getItemName());
        dbItem.setColor(shopItem.getColor());
        dbItem.setSize(shopItem.getSize());
        dbItem.setPrice(shopItem.getPrice());
        dbItem.setStyle(shopItem.getStyle());
        dbItem.setMainCategory(shopItem.getMainCategory());
        dbItem.setSubCategory(shopItem.getSubCategory());

        return this.ownerShopMapper.updateItem(dbItem) > 0  ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }
    //상품삭제: 소유권 확인 후 삭제 처리
    public CommonResult delete(RegisterEntity user,Long id) {
        if( id == null ) return CommonResult.FAILURE;

        ShopInfoEntity myShop = this.shopInfoMapper.selectShopByUserEmail(user.getEmail());
        ShopItemEntity dbItem = this.ownerShopMapper.selectItemById(id);
        // [IDOR 방어] 타 매장 상품 삭제 시도 원천 차단
        if (dbItem == null || myShop == null || dbItem.getShopId() != myShop.getShopId()) {
            return CommonResult.FAILURE;
        }
        return this.ownerShopMapper.deleteItemById(id) > 0  ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }
    /*예약변경:예약 데이터 소유권 검증 */
    public CommonResult updateReservationStatus(RegisterEntity user,int reservationId, String status) {
        // [보안] 현재 사장님의 매장 ID 확보
        ShopInfoEntity myShop = this.shopInfoMapper.selectShopByUserEmail(user.getEmail());
        if (myShop == null) return CommonResult.FAILURE;
        // [검증] 해당 예약이 현재 사장님의 매장에 속해있는지 리스트 기반 확인
        List<ReservationItemVo> reservations = this.reservationMapper.selectReservationsByShopId(myShop.getShopId());
        boolean isMine = reservations.stream().anyMatch(r -> r.getReservationId() == reservationId);

        // [IDOR 방어] 내 매장 예약이 아니면 실패 반환
        if (!isMine) return CommonResult.FAILURE;

        // 2. 업데이트 실행
       return this.reservationMapper.updateReservationStatus(reservationId, status) > 0
               ? CommonResult.SUCCESS
               : CommonResult.FAILURE;
    }

    public OrderHistoryVo[] getOrdersByShopId(int shopId) {
        return this.orderMapper.getAllOrdersByShopId(shopId);
    }

    //관리자 프로필사진목적
    public Object getShop(String email) {
        return this.shopInfoMapper.selectShopByUserEmail(email);
    }
}