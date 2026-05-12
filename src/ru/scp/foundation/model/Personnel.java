package ru.scp.foundation.model;

public record Personnel(
    Long id,
    String fullName,
    String position,
    int clearanceLevel,
    long baseSiteId
) {}
