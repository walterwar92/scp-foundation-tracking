package ru.scp.foundation.model;

public record ContainmentSite(
    Long id,
    String siteCode,
    String location,
    Integer capacity,
    int securityLevel
) {}
