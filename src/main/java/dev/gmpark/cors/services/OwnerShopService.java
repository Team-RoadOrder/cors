package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.entities.ShopItemImagesEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.results.register.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnerShopService {

    // [수정 1] final을 꼭 붙여야 @RequiredArgsConstructor가 작동해서 주입해줍니다!
    private final OwnerShopMapper ownerShopMapper;

    // 맥북 경로 확인 완료
    private final String UPLOAD_PATH = "/Users/parkgyumin/Desktop/upload/";

    @Transactional
    public CommonResult registerShopItem(ShopItemEntity item, MultipartFile[] images) {

        // [수정 2] 아까 Mapper 인터페이스에 만든 이름은 'insertItem' 입니다.
        // insertItem이 실행되면 item객체 안에 id값이 채워집니다.
        if (ownerShopMapper.insertItem(item) == 0) {
            return CommonResult.FAILURE;
        }

        // 2. 이미지 파일 처리
        if (images != null && images.length > 0) {
            File uploadDir = new File(UPLOAD_PATH);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            for (MultipartFile file : images) {
                if (file.isEmpty()) continue;

                String originalName = file.getOriginalFilename();
                // [안전장치] 파일명에 확장자가 없을 경우 대비
                if (originalName == null || !originalName.contains(".")) {
                    continue;
                }

                String ext = originalName.substring(originalName.lastIndexOf("."));
                String uuidName = UUID.randomUUID().toString() + ext;

                try {
                    // 3. 하드디스크 저장
                    file.transferTo(new File(UPLOAD_PATH + uuidName));

                    // 4. DB 저장
                    ShopItemImagesEntity imageEntity = ShopItemImagesEntity.builder()
                            .productId(item.getId())
                            .originalName(originalName)
                            .imagePath(uuidName) // [수정] "/images/" 제거! 순수 파일명(uuidName)만 저장
                            .build();

                    // [수정 3] 이미지 저장 메서드 호출
                    ownerShopMapper.insertShopItemImage(imageEntity);

                } catch (IOException e) {
                    e.printStackTrace();
                    // [중요] 런타임 예외를 던져야 롤백(상품 등록 취소)이 됩니다.
                    throw new RuntimeException("파일 저장 중 오류 발생");
                }
            }
        }
        return CommonResult.SUCCESS;
    }
}