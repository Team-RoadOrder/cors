package dev.gmpark.cors.validators;

import dev.gmpark.cors.entities.ShopItemEntity;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class ShopItemValidator {

    public static final String NAME_REGEX = "^[가-힣a-zA-Z0-9\\s()\\[\\]\\-_&/]+$";



    // 사이즈: 콤마로 구분된 형식 (예: S,M,L) - 공백 허용
    public static final String SIZE_REGEX = "^[^,\\s]+(\\s*,\\s*[^,\\s]+)*$";

    // 허용된 스타일 목록
    public static final List<String> ALLOWED_STYLES = Arrays.asList(
            "스트릿", "미니멀", "댄디", "캐주얼", "빈티지", "모던", "스포티", "페미닌"
    );

    public static boolean validateItemName(@NonNull ShopItemEntity item) {
        return validateItemName(item.getItemName());
    }

    public static boolean validateItemName(String itemName) {
        return itemName != null &&
                ValidatorUtils.isLengthInBetween(itemName, 2, 50) && // 길이 2~50
                itemName.matches(NAME_REGEX);
    }
    public static boolean validateColor(@NonNull ShopItemEntity item) {
        return validateColor(item.getColor());
    }

    public static boolean validateColor(String color) {
        return color != null &&
                ValidatorUtils.isLengthInBetween(color, 1, 20);

    }

    public static boolean validateSize(@NonNull ShopItemEntity item) {
        return validateSize(item.getSize());
    }

    public static boolean validateSize(String size) {
        if (size == null || size.trim().isEmpty()) {
            return true;
        }
        return size.matches(SIZE_REGEX);
    }
    public static boolean validatePrice(@NonNull ShopItemEntity item) {
        var price = item.getPrice();

        // 2. null 체크를 가장 먼저 해야 합니다.
        if (price == null) {
            return false;
        }

        return price > 0 && price <= Integer.MAX_VALUE;
    }

    // 오버로딩된 메서드 (int나 Integer를 받는 경우)
    public static boolean validatePrice(Integer price) {
        return price != null && price > 0;
    }

    public static boolean validateStyle(@NonNull ShopItemEntity item) {
        return validateStyle(item.getStyle());
    }

    public static boolean validateStyle(String style) {
        return style != null && ALLOWED_STYLES.contains(style);
    }

    public static boolean validateCategory(@NonNull ShopItemEntity item) {
        return validateCategory(item.getMainCategory(), item.getSubCategory());
    }

    public static boolean validateCategory(String mainCategory, String subCategory) {
        return mainCategory != null && !mainCategory.trim().isEmpty() &&
                subCategory != null && !subCategory.trim().isEmpty();
    }

    public static boolean validateImages(MultipartFile[] images) {
        if (images == null || images.length == 0) {
            return false;
        }
        // 배열 길이는 있는데, 실제 파일이 비어있는 경우 체크
        for (MultipartFile img : images) {
            if (!img.isEmpty()) {
                return true; // 하나라도 유효한 파일이 있으면 통과
            }
        }
        return false;
    }
}