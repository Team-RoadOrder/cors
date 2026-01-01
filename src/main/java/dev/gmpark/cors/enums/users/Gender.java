package dev.gmpark.cors.enums.users;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Gender {
    FEMALE("F", "여자"),
    MALE("M", "남자");
    public final String code;
    public final String displayText;
}
