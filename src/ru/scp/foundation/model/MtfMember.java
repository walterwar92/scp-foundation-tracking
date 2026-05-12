package ru.scp.foundation.model;

import java.time.LocalDate;

public record MtfMember(
    long mtfId,
    long personnelId,
    LocalDate joinedAt
) {}
