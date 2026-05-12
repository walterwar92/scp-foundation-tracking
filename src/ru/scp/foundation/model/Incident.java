package ru.scp.foundation.model;

import java.time.LocalDateTime;

public record Incident(
    Long id,
    LocalDateTime occurredAt,
    long scpId,
    long siteId,
    Long mtfId,
    int severity,
    String description
) {}
