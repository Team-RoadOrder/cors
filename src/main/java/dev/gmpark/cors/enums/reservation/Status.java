package dev.gmpark.cors.enums.reservation;


import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Status {
    PENDING ("PD","대기"),
    CONFIRMED ("CF","확정"),
    CANCELLED ( "CC", "취소"),
    COMPLETED("CP", "완료");
    public final String code;
    public final String name;
}
