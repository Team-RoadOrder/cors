package dev.gmpark.cors.validators;

import dev.gmpark.cors.dtos.SingleOrderDto;
import dev.gmpark.cors.entities.RegisterEntity;

import java.util.regex.Pattern;

public class PayValidator {
    public static boolean validateReceiverName(String name) {
        return name != null && Pattern.matches("^[가-힣a-zA-Z]{2,10}$", name);
    }

    public static boolean validateReceiverPhone(String phone) {
        return phone != null && Pattern.matches("^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$", phone);
    }

    public static boolean validateAddress(String address) {
        return address != null && !address.trim().isEmpty();
    }

    public static boolean validateAddressDetail(String addressDetail) {
        return addressDetail != null && !addressDetail.trim().isEmpty();
    }

    public static boolean validatePoints(int usedPoints, RegisterEntity user) {
        return usedPoints >= 0 && usedPoints <= user.getPoint();
    }
    
    public static boolean validateOrderItems(SingleOrderDto dto) {
        boolean hasCartItems = dto.getCartIds() != null && !dto.getCartIds().isEmpty();
        boolean hasSingleItem = dto.getItemId() != null && dto.getSize() != null;
        return hasCartItems || hasSingleItem;
    }
}
