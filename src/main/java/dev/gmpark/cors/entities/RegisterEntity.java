package dev.gmpark.cors.entities;

import lombok.*;


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

}
/*
CREATE SCHEMA `cors`;
CREATE TABLE `cors`.`user`
(

    `usertype` VARCHAR(10),
    `email` VARCHAR(40),
    `name` VARCHAR(20),
    `password` VARCHAR(20),
    `store_name` VARCHAR(30),
    `business_num` VARCHAR(30),
    `phone` VARCHAR(20),
    `style` VARCHAR(20),
    `address` VARCHAR(255),
    `address_detail` VARCHAR(255),
    `gender` VARCHAR(10),
    CONSTRAINT PRIMARY KEY (`email`)

);

*/
/*
TRUNCATE `study_memo`.memos;
*/

