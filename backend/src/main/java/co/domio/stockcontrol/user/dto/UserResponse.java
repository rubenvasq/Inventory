package co.domio.stockcontrol.user.dto;

import co.domio.stockcontrol.user.Role;
import co.domio.stockcontrol.user.User;
import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        Role role,
        boolean enabled,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
