package com.pvp.travelmatch.dto;

import com.pvp.travelmatch.entity.TravelComment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class TravelCommentResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userCity;
    private String comment;
    private LocalDateTime createdAt;
    private boolean ownComment;

    public static TravelCommentResponse fromEntity(TravelComment comment, Long currentUserId) {
        var user = comment.getUser();
        return TravelCommentResponse.builder()
                .id(comment.getId())
                .userId(user.getId())
                .userName(user.getName())
                .userCity(user.getCity())
                .comment(comment.getComment())
                .createdAt(comment.getCreatedAt())
                .ownComment(user.getId().equals(currentUserId))
                .build();
    }
}
