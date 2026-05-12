package ru.scp.foundation.auth;

import ru.scp.foundation.model.UserRole;

public record Session(
    long userId,
    long personnelId,
    String login,
    String displayName,
    UserRole role,
    int clearanceLevel
) {
    public boolean isO5() {
        return role == UserRole.O5;
    }
}
