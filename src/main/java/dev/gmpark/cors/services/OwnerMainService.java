package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.entities.ShopInfoEntity;
import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.mappers.ShopInfoMapper;
import dev.gmpark.cors.results.register.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OwnerMainService {

    private final ShopInfoMapper shopInfoMapper;
    private final OwnerShopMapper ownerShopMapper;
    @Autowired
    public OwnerMainService(ShopInfoMapper shopInfoMapper, OwnerShopMapper ownerShopMapper) {
        this.shopInfoMapper = shopInfoMapper;
        this.ownerShopMapper = ownerShopMapper;
    }

    public ShopInfoEntity getShopByEmail(String email) {
        return this.shopInfoMapper.selectShopByUserEmail(email);
    }

    // OwnerMainService.java 내부

    public String saveShopInfo(RegisterEntity user, ShopInfoEntity shopInfo) {

        // 1. FK 설정 (누구의 매장인지)
        shopInfo.setUserEmail(user.getEmail());

        // 2. 파일 업로드 처리 (엔티티에서 파일 꺼내서 저장 후 경로 설정)
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

        // 3. DB 저장 및 수정 로직 (★ 여기가 핵심 수정 부분)
        ShopInfoEntity existingShop = shopInfoMapper.selectShopByUserEmail(user.getEmail());

        int result;
        if (existingShop == null) {
            // [신규 등록]
            result = shopInfoMapper.insert(shopInfo);
        } else {
            // [수정]
            // ★ 중요: 기존 DB에 있는 ID를 가져와서 세팅해줘야 정확한 대상을 수정합니다.
            shopInfo.setShopId(existingShop.getShopId());

            // ★ 안전장치: 사용자가 이미지를 '변경' 안 했을 경우 (파일이 없을 때)
            // 기존 DB에 있던 이미지 경로를 그대로 유지해줍니다. (안 그러면 null로 덮어씌워질 위험)
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
        // 1. 저장할 경로: WebMvcConfig에 설정한 실제 경로와 일치시켜야 함
        // (끝에 슬래시 / 꼭 붙여주세요)
        String uploadDir = "/Users/parkgyumin/Desktop/upload/";

        // 2. 폴더가 없으면 생성
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 3. 파일명 생성 (중복 방지)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String originalFileName = file.getOriginalFilename();
        // 확장자 추출 (null 체크 등 안전장치 추가 가능)
        String extension = originalFileName != null && originalFileName.contains(".")
                ? originalFileName.substring(originalFileName.lastIndexOf("."))
                : ".jpg";

        String savedFileName = timestamp + "_" + System.currentTimeMillis() + extension;

        // 4. 실제 파일 저장 (맥 바탕화면 upload 폴더로 들어감)
        File dest = new File(uploadDir + savedFileName);
        file.transferTo(dest);

        // 5. [중요] DB에 저장할 경로 리턴
        // WebMvcConfig에서 "/images/**" 로 매핑했으므로, DB에는 "/images/파일명" 으로 저장해야 함
        return "/images/" + savedFileName;
    }
    // [수정] 메서드 파라미터 변경: 세션 어노테이션 제거하고 User 객체만 받음
    public ShopItemEntity[] getItemsByUser(RegisterEntity user) {

        // 1. 이메일로 내 매장 정보 찾기
        // (주의: Service 안에서 this.ownerMainService... 이렇게 자기 자신을 부르면 안 됩니다. Mapper를 직접 쓰세요)
        ShopInfoEntity dbShop = this.shopInfoMapper.selectShopByUserEmail(user.getEmail());

        // 2. 매장 정보가 없으면 (등록 전) 빈 배열 리턴
        if (dbShop == null) {
            return new ShopItemEntity[0];
        }

        // 3. 찾은 매장 ID(PK)로 상품 리스트(FK) 조회
        // selectAll() 대신 selectAllByShopId() 사용
        return this.ownerShopMapper.selectAllByShopId(dbShop.getShopId());
    }
    public CommonResult modify(ShopItemEntity shopItem ) {
        ShopItemEntity dbItem = this.ownerShopMapper.selectItemById(shopItem.getId());
        System.out.println(shopItem.getId());
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
        if( id == null ) {
            return CommonResult.FAILURE;
        }
        ShopItemEntity dbItem = this.ownerShopMapper.selectItemById(id);
        if (dbItem == null) {
            return CommonResult.FAILURE;
        }
        return this.ownerShopMapper.deleteItemById(id) > 0  ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }
}
