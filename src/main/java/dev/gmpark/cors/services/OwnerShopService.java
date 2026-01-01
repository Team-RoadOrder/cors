package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.entities.ShopItemImagesEntity;
import dev.gmpark.cors.mappers.OwnerShopMapper;
import dev.gmpark.cors.results.register.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnerShopService {

    private final OwnerShopMapper ownerShopMapper;

    // [수정] final String UPLOAD_PATH 제거하고 아래처럼 변경
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public CommonResult registerShopItem(ShopItemEntity item, MultipartFile[] images) {
        if (ownerShopMapper.insertItem(item) == 0) {
            return CommonResult.FAILURE;
        }

        if (images != null && images.length > 0) {
            // [수정] 경로 보정
            String path = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";

            File dir = new File(path);
            if (!dir.exists()) dir.mkdirs();

            for (MultipartFile file : images) {
                if (file.isEmpty()) continue;

                String originalName = file.getOriginalFilename();
                if (originalName == null || !originalName.contains(".")) {
                    continue;
                }

                String ext = originalName.substring(originalName.lastIndexOf("."));
                String uuidName = UUID.randomUUID().toString() + ext;

                try {
                    // [수정] path 변수 사용
                    file.transferTo(new File(path + uuidName));

                    ShopItemImagesEntity imageEntity = ShopItemImagesEntity.builder()
                            .productId(item.getId())
                            .originalName(originalName)
                            .imagePath(uuidName)
                            .build();

                    ownerShopMapper.insertShopItemImage(imageEntity);

                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("파일 저장 중 오류 발생");
                }
            }
        }
        return CommonResult.SUCCESS;
    }
}