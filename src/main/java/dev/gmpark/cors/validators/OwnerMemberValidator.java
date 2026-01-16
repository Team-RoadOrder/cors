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

    /**
     * 전화번호 정규식 (대한민국 휴대폰 표준)
     * 010, 011, 016, 017, 018, 019로 시작하며 총 10~11자리 숫자
     */
    public static final String PHONE_REGEX = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$";

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
//        return address != null && !address.trim().isEmpty() &&
//                addressDetail != null && !addressDetail.trim().isEmpty();
        return true;
    }
}