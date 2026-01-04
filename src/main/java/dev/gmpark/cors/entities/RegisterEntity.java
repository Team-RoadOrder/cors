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
create table `cors`.`email_tokens`
        (
                email       varchar(50)          not null,
code        varchar(6)           not null,
salt        varchar(255)         not null,
is_verified tinyint(1) default 0 null,
is_used     tinyint(1) default 0 null,
created_at  datetime             null,
expires_at  datetime             null,
primary key (email, code, salt)
);

create table `cors`.`users`
        (
                usertype       varchar(10)  null,
email          varchar(40)  not null
primary key,
name           varchar(20)  null,
password       varchar(20)  null,
store_name     varchar(30)  null,
business_num   varchar(30)  null,
phone          varchar(20)  null,
style          varchar(20)  null,
address        varchar(255) null,
address_detail varchar(255) null,
gender         varchar(10)  null
        );

create table `cors`.`shop_info`
        (
                shop_id          int unsigned auto_increment comment '매장 고유 ID (PK)'
                        primary key,
                user_email       varchar(40)  not null comment '사장님 이메일 (FK)',
shop_name        varchar(50)  not null comment '가게이름',
shop_time        varchar(30)  null comment '운영시간',
shop_category    varchar(20)  null comment '카테고리',
shop_address     varchar(100) null comment '주소',
shop_tel         varchar(30)  null comment '전화번호',
profile_image    varchar(255) null comment '프로필이미지 경로',
background_image varchar(255) null comment '배경이미지 경로',
constraint fk_shop_user_email
foreign key (user_email) references users (email)
on update cascade on delete cascade
)
comment '매장 정보';

create table `cors`.`shop_items`
        (
                id               bigint auto_increment comment 'PK'
                        primary key,
                shop_id          int unsigned                         not null comment '매장 PK (FK)',
                item_name        varchar(100)                         not null comment '상품명',
color            varchar(50)                          not null comment '색상',
size             varchar(50)                          not null comment '사이즈',
price            bigint   default 0                   not null comment '가격',
style            varchar(20)                          not null,
main_category    varchar(50)                          not null comment '대분류',
sub_category     varchar(50)                          not null comment '중분류',
detail_category  varchar(50)                          null comment '소분류 (선택)',
main_image_index int      default 0                   not null comment '메인 이미지 인덱스 번호',
created_at       datetime default current_timestamp() not null comment '생성일',
updated_at       datetime default current_timestamp() not null comment '수정일',
deleted_at       datetime                             null comment '삭제일 (Soft Delete용)',
열_name           int                                  null,
constraint fk_items_shop_id
foreign key (shop_id) references shop_info (shop_id)
on update cascade on delete cascade
)
comment '상품 정보' collate = utf8mb4_uca1400_ai_ci;

create table `cors`.`shop_item_images`
        (
                id            bigint auto_increment comment 'PK'
                        primary key,
                product_id    bigint                               not null comment 'shop_items의 id (FK)',
                image_path    varchar(255)                         not null comment '웹 접근 경로',
original_name varchar(255)                         not null comment '원본 파일명',
created_at    datetime default current_timestamp() not null comment '생성일',
constraint fk_img_pid
foreign key (product_id) references shop_items (id)
on update cascade on delete cascade
)
comment '상품 이미지 정보' collate = utf8mb4_uca1400_ai_ci;

CREATE TABLE `cors`.`reservations` (
                                       `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
                                       `user_email` VARCHAR(40) NOT NULL,
                                       `shop_id` INT UNSIGNED NOT NULL,
                                       `visit_date` DATETIME NOT NULL,
                                       `status` VARCHAR(20) NOT NULL DEFAULT '대기',
                                       `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       CONSTRAINT PRIMARY KEY (`id`),
                                       CONSTRAINT FOREIGN KEY (`user_email`) REFERENCES `cors`.`users`(`email`)
                                           ON DELETE CASCADE
                                           ON UPDATE CASCADE,
                                       CONSTRAINT FOREIGN KEY (`shop_id`) REFERENCES `cors`.`shop_info`(`shop_id`)
                                           ON DELETE CASCADE
                                           ON UPDATE CASCADE
);

CREATE TABLE `cors`.`reservation_items` (
                                            `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
                                            `reservation_id` INT UNSIGNED NOT NULL,
                                            `item_id` BIGINT NOT NULL,
                                            `size` VARCHAR(50) NOT NULL,
                                            CONSTRAINT PRIMARY KEY (`id`),
                                            CONSTRAINT FOREIGN KEY (`reservation_id`) REFERENCES `cors`.`reservations`(`id`)
                                                ON DELETE CASCADE
                                                ON UPDATE CASCADE,
                                            CONSTRAINT FOREIGN KEY (`item_id`) REFERENCES `cors`.`shop_items`(`id`)
                                                ON DELETE CASCADE
                                                ON UPDATE CASCADE
);
*/


/*
TRUNCATE `study_memo`.memos;
*/

