package dev.gmpark.cors.entities;

import lombok.*;

import java.time.LocalDateTime;


@NoArgsConstructor // 매개변수가 없는 생성자를 만들어라
@AllArgsConstructor // 모든 매개변수를 가지는 생성자를 만들어라
@Getter // 모든 멤버 변수에 대한 Getter메서드를 생성해달라
@Setter // 모든 멤버 변수에 대한 Setter메서드를 생성해달라
@EqualsAndHashCode(of = "email")
public class RegisterEntity {

    private String email;
    private String name;
    private String storeName;
    private String businessNum;
    private String usertype;
    private String password;
    private String phone;
    private String style;
    private String address;
    private String addressDetail;
    private String gender;
    private int level;
    private String ownerEmail;   //새로생김
    private LocalDateTime lastLogOutAt; //새로생김
    private LocalDateTime createdAt;
    private String socialTypeCode;
    private String socialId;
    private int point;
}
