package ru.scp.foundation.model;

import java.time.LocalDate;

public record ContainmentHistory(
    Long id,
    long scpId,
    long siteId,
    LocalDate movedIn,
    LocalDate movedOut
) {}
