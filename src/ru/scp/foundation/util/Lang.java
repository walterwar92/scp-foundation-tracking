package ru.scp.foundation.util;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Простой i18n-singleton. Хранит текущую локаль и подгружает ResourceBundle
 * из resources/i18n/messages_<lang>.properties.
 *
 * Кнопка переключения EN/RU вызывает {@link #toggle()}, после чего сцены
 * перезагружаются с новым бандлом (FXMLLoader.setResources).
 */
public final class Lang {

    public static final Locale EN = Locale.forLanguageTag("en");
    public static final Locale RU = Locale.forLanguageTag("ru");

    private static Locale current = EN;
    private static ResourceBundle bundle = load(EN);

    private Lang() {}

    public static Locale current() { return current; }

    public static ResourceBundle bundle() { return bundle; }

    public static void toggle() {
        current = current.getLanguage().equals("en") ? RU : EN;
        bundle = load(current);
    }

    public static String code() {
        return current.getLanguage().toUpperCase();
    }

    public static String otherCode() {
        return current.getLanguage().equals("en") ? "RU" : "EN";
    }

    /**
     * Перевод по ключу. Если ключ не найден — возвращается сам ключ
     * (видно в логе/UI что забыли добавить).
     */
    public static String t(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }

    private static ResourceBundle load(Locale locale) {
        return ResourceBundle.getBundle("i18n.messages", locale);
    }
}
