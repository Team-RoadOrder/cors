package dev.gmpark.cors.entities;

import lombok.*;

import java.time.LocalDateTime;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = {"email","code","salt"})
public class EmailTokenEntity {
    private String email;
    private String code;
    private String salt;
    private boolean isVerified;
    private boolean isUsed;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
/*
CREATE TABLE `cors`.`email_tokens` (
        `email` VARCHAR(50),
        `code` VARCHAR(6),
        `salt` VARCHAR(255),
        `is_verified` BOOLEAN,
        `is_used` BOOLEAN,
        `created_at` DATETIME,
        `expires_at` DATETIME,
CONSTRAINT PRIMARY KEY (`email`,`code`,`salt`)
)*/
