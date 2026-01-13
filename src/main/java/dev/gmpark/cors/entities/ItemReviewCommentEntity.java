package dev.gmpark.cors.entities;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemReviewCommentEntity {
    private Long id;
    private Long reviewId;
    private String userEmail;
    private String userName;
    private String content;
    private Long parentId;      // [핵심] 대댓글용 부모 ID
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}