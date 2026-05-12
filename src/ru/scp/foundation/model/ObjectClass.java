package ru.scp.foundation.model;

public enum ObjectClass {
    SAFE("Safe"), EUCLID("Euclid"), KETER("Keter");

    private final String dbValue;
    ObjectClass(String dbValue) { this.dbValue = dbValue; }
    public String dbValue() { return dbValue; }

    public static ObjectClass fromDb(String s) {
        for (var c : values()) if (c.dbValue.equals(s)) return c;
        throw new IllegalArgumentException("Unknown object_class: " + s);
    }
}
