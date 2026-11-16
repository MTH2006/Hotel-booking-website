
package com.hotelbooking.dto;

import com.hotelbooking.model.User;
import java.time.LocalDateTime;

public record UserSummaryResponse(
    Long id,
    String userUuid,
    String name,
    String email,
    String phone,
    String role,
    LocalDateTime createdAt,
    boolean active
) {
    public static UserSummaryResponse fromUser(User user) {
        return new UserSummaryResponse(
            user.getId(),
            user.getUserUuid(),
            user.getName(),
            user.getEmail(),
            user.getPhone(),
            user.getRole(),
            user.getCreatedAt(),
            user.isActive()
        );
    }
}
