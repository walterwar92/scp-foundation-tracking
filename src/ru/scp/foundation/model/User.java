package ru.scp.foundation.model;

import java.time.LocalDateTime;

public record User(
    Long id,
    long personnelId,
    String login,
    String passwordHash,
    String salt,
    UserRole role,
    LocalDateTime createdAt
) {}
