package dev.gmpark.cors.entities;

import lombok.*;


@NoArgsConstructor // 매개변수가 없는 생성자를 만들어라
@AllArgsConstructor // 모든 매개변수를 가지는 생성자를 만들어라
@Getter // 모든 멤버 변수에 대한 Getter메서드를 생성해달라
@Setter // 모든 멤버 변수에 대한 Setter메서드를 생성해달라
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RegisterEntity {
    @EqualsAndHashCode.Include
    private int id;
    private String name;
    private String storeName;
    private String businessNum;
    private String usertype;
    private String email;
    private String password;
    private String phone;
    private String address;
    private String addressDetail;
    private String gender;

}
/*
CREATE SCHEMA `cors`;

*/
/*
TRUNCATE `study_memo`.memos;
*/

