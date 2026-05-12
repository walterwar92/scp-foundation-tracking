package ru.scp.foundation.model;

public record MtfTeam(
    Long id,
    String callsign,
    String specialization,
    long baseSiteId
) {}
