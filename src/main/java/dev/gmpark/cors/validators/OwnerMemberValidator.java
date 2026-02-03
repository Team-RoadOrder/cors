package dev.gmpark.cors.validators;

import dev.gmpark.cors.entities.RegisterEntity;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OwnerMemberValidator {
    // 이름: 한글/영문 2~10자 (팀원 기준)
    public static final String NAME_REGEX = "^[가-힣A-Za-z]{2,10}$";

    // 비밀번호: 6~50자 (팀원 기준)
    public static final String PASSWORD_REGEX = "^[\\da-zA-Z`~!@#$%^&*()\\-_=+\\[{\\]}\\\\|;:'\",<.>\\/?]{6,50}$";

    // 02(서울), 0xx(지역), 010(휴대폰), 070(인터넷), 050(안심번호)
    public static final String PHONE_REGEX = "^(02|0[3-9]\\d|01\\d|070|050\\d)\\d{3,4}\\d{4}$";

    // 주소 정규식: 한글, 영문, 숫자, 공백, 특수문자(-,(),.[]) 허용, 2~100자
    public static final String ADDRESS_REGEX = "^[가-힣a-zA-Z0-9\\s\\-\\(\\)\\[\\]\\.,]{2,100}$";
    
    // 상세주소 정규식: 한글, 영문, 숫자, 공백, 특수문자(-,(),.[]) 허용, 1~100자 (빈 값 허용 안 함)
    public static final String ADDRESS_DETAIL_REGEX = "^[가-힣a-zA-Z0-9\\s\\-\\(\\)\\[\\]\\.,]{1,100}$";

    /**
     * 임직원 데이터 전체 유효성 검사
     */
    public static boolean validate(@NonNull RegisterEntity member) {
        return validateEmail(member.getEmail()) &&
                validateName(member.getName()) &&
                validatePhone(member.getPhone()) &&
                validateAddress(member.getAddress(), member.getAddressDetail()) &&
                (member.getPassword() == null || member.getPassword().isEmpty() || validatePassword(member.getPassword()));
    }

    public static boolean validateEmail(String email) {
        return email != null && email.matches(EmailTokenValidator.EMAIL_REGEX)
                && ValidatorUtils.isLengthInBetween(email, 8, 50);
    }

    public static boolean validateName(String name) {
        return name != null && name.matches(NAME_REGEX);
    }

    public static boolean validatePhone(String phone) {
        if (phone == null) return false;
        // 하이픈 제거 후 숫자만 추출하여 검증 (데이터 정규화)
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        return cleanPhone.matches(PHONE_REGEX);
    }

    public static boolean validatePassword(String password) {
        if (password == null || password.isEmpty()) return true;

        if (password.startsWith("$2a$")) return true;

        return password.matches(PASSWORD_REGEX) && ValidatorUtils.isLengthInBetween(password, 6, 50);
    }

    public static boolean validateAddress(String address, String addressDetail) {
        if (address == null || !address.matches(ADDRESS_REGEX)) {
            return false;
        }
        if (addressDetail == null || !addressDetail.matches(ADDRESS_DETAIL_REGEX)) {
            return false;
        }
        return true;
    }
}