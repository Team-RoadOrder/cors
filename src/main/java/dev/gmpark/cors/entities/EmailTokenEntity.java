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
