package ru.scp.foundation.model;

import java.time.LocalDate;

public record ProcedureRevision(
    Long id,
    long scpId,
    int revisionNumber,
    LocalDate revisionDate,
    String procedureText,
    long approvedById
) {}
