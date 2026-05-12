package ru.scp.foundation.access;

import ru.scp.foundation.auth.Session;
import ru.scp.foundation.model.ObjectClass;
import ru.scp.foundation.model.ScpObject;

public final class AccessControl {

    private AccessControl() {}

    /** Доступ к карточке SCP по уровню допуска и классу объекта. */
    public static boolean canViewScp(Session s, ScpObject obj) {
        if (s.isO5()) return true;
        return switch (obj.objectClass()) {
            case SAFE   -> s.clearanceLevel() >= 1;
            case EUCLID -> s.clearanceLevel() >= 3;
            case KETER  -> s.clearanceLevel() >= 4;
        };
    }

    public static boolean canViewObjectClass(Session s, ObjectClass cls) {
        if (s.isO5()) return true;
        return switch (cls) {
            case SAFE   -> s.clearanceLevel() >= 1;
            case EUCLID -> s.clearanceLevel() >= 3;
            case KETER  -> s.clearanceLevel() >= 4;
        };
    }

    /** Может ли пользователь редактировать данные (CUD). */
    public static boolean canEdit(Session s) {
        return s.isO5();
    }

    /** Может ли пользователь видеть управление пользователями. */
    public static boolean canManageUsers(Session s) {
        return s.isO5();
    }

    /** Может ли пользователь видеть инциденты (привязано к видимости SCP). */
    public static boolean canViewIncidentsOfClass(Session s, ObjectClass cls) {
        return canViewObjectClass(s, cls);
    }
}
