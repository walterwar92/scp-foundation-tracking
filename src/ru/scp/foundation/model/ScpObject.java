package ru.scp.foundation.model;

import java.time.LocalDate;

public record ScpObject(
    Long id,
    String itemNumber,
    String codeName,
    ObjectClass objectClass,
    String description,
    LocalDate discoveredAt
) {}
