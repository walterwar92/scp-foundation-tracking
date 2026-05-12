package ru.scp.foundation.model;

public enum UserRole {
    O5, RESEARCHER;
    public static UserRole fromDb(String s) { return valueOf(s); }
}
