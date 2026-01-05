package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.mappers.ReservationMapper;
import dev.gmpark.cors.mappers.ShopInfoMapper;
import dev.gmpark.cors.results.register.CommonResult;
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

        try {
            MultipartFile profileFile = shopInfo.getProfileImageFile();
            if (profileFile != null && !profileFile.isEmpty()) {
                String path = uploadFile(profileFile);
                shopInfo.setProfileImage(path);
            }

            MultipartFile backgroundFile = shopInfo.getBackgroundImageFile();
            if (backgroundFile != null && !backgroundFile.isEmpty()) {
                String path = uploadFile(backgroundFile);
                shopInfo.setBackgroundImage(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "FAILURE";
        }

        ShopInfoEntity existingShop = shopInfoMapper.selectShopByUserEmail(user.getEmail());
        int result;
        if (existingShop == null) {
            result = shopInfoMapper.insert(shopInfo);
        } else {
            shopInfo.setShopId(existingShop.getShopId());
            if (shopInfo.getProfileImage() == null) {
                shopInfo.setProfileImage(existingShop.getProfileImage());
            }
            if (shopInfo.getBackgroundImage() == null) {
                shopInfo.setBackgroundImage(existingShop.getBackgroundImage());
            }
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

    public CommonResult modify(ShopItemEntity shopItem ) {
        ShopItemEntity dbItem = this.ownerShopMapper.selectItemById(shopItem.getId());
        if (dbItem == null) {
            return CommonResult.FAILURE;
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

    public CommonResult delete(Long id) {
        if( id == null ) return CommonResult.FAILURE;
        ShopItemEntity dbItem = this.ownerShopMapper.selectItemById(id);
        if (dbItem == null) return CommonResult.FAILURE;
        return this.ownerShopMapper.deleteItemById(id) > 0  ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }
    public CommonResult updateReservationStatus(int reservationId, String status) {
        // 2. 업데이트 실행
       return this.reservationMapper.updateReservationStatus(reservationId, status) > 0
               ? CommonResult.SUCCESS
               : CommonResult.FAILURE;
    }
}