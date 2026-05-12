package ru.scp.foundation.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Перевод текстовых данных из seed-БД на русский.
 *
 * Метод {@link #t(String)} возвращает RU-эквивалент английского текста,
 * если {@link Lang} в режиме русского И в словаре есть совпадение. Иначе
 * возвращает исходную строку без изменений.
 *
 * Применяется в TableView-колонках (описания, локации, должности,
 * специализации, тексты процедур) и в TextArea просмотра ревизий.
 *
 * Имена (Dr. Alto Clef), коды объектов (SCP-173), коллсайны MTF и
 * пользовательский ввод НЕ переводятся.
 */
public final class DataI18n {

    private static final Map<String, String> EN_TO_RU = new HashMap<>();

    static {
        // ===== scp_objects.description =====
        EN_TO_RU.put("Hostile concrete statue. Cannot move while observed.",
                     "Враждебная бетонная статуя. Не может двигаться, пока за ней наблюдают.");
        EN_TO_RU.put("Highly adaptive reptilian entity. Multiple termination attempts.",
                     "Высоко адаптивная рептилоидная сущность. Многократные попытки ликвидации.");
        EN_TO_RU.put("Humanoid in plague doctor attire. Believes it cures \"the pestilence\".",
                     "Гуманоид в одеянии чумного доктора. Считает, что лечит \"чуму\".");
        EN_TO_RU.put("Humanoid that responds violently to facial observation.",
                     "Гуманоид, реагирующий насилием на наблюдение за его лицом.");
        EN_TO_RU.put("Mechanical device transforming inputs based on setting.",
                     "Механическое устройство, трансформирующее объекты в зависимости от настройки.");

        // ===== containment_sites.location =====
        EN_TO_RU.put("United States, classified",      "США, засекречено");
        EN_TO_RU.put("United States, undisclosed",     "США, нераскрыто");
        EN_TO_RU.put("Nevada, desert region",          "Невада, пустыня");
        EN_TO_RU.put("Eastern Europe",                 "Восточная Европа");
        EN_TO_RU.put("Bio-research wing, Site-66",     "Био-исследовательское крыло, Site-66");

        // ===== personnel.position =====
        EN_TO_RU.put("Senior Researcher",        "Старший исследователь");
        EN_TO_RU.put("Head of Psychology",       "Заведующий психологией");
        EN_TO_RU.put("Field Operative",          "Полевой оперативник");
        EN_TO_RU.put("Containment Specialist",   "Специалист по содержанию");
        EN_TO_RU.put("Security Director",        "Директор безопасности");
        EN_TO_RU.put("Junior Researcher",        "Младший исследователь");

        // ===== mtf_teams.specialization =====
        EN_TO_RU.put("Red Right Hand — direct O5 enforcement",
                     "Красная Правая Рука — прямое исполнение приказов O5");
        EN_TO_RU.put("Nine-Tailed Fox — recontainment",
                     "Девятихвостый Лис — рекаунтаймент");
        EN_TO_RU.put("Debuggers — informational anomalies",
                     "Дебаггеры — информационные аномалии");
        EN_TO_RU.put("Hammer Down — heavy assault",
                     "Молот Опускается — тяжёлый штурм");
        EN_TO_RU.put("Maz Hatters — chemical/biohazard",
                     "Безумные Шляпники — хим. и био-угрозы");
        // Краткие варианты из Firebird seed:
        EN_TO_RU.put("Red Right Hand",   "Красная Правая Рука");
        EN_TO_RU.put("Nine-Tailed Fox",  "Девятихвостый Лис");
        EN_TO_RU.put("Debuggers",        "Дебаггеры");
        EN_TO_RU.put("Hammer Down",      "Молот Опускается");
        EN_TO_RU.put("Maz Hatters",      "Безумные Шляпники");

        // ===== incidents.description =====
        EN_TO_RU.put("Containment breach during routine maintenance. 3 personnel casualties before recontainment.",
                     "Нарушение содержания во время регламентных работ. 3 жертвы среди персонала до рекаунтаймента.");
        EN_TO_RU.put("Subject attempted breach. Heavy weapons deployed.",
                     "Объект попытался прорваться. Применено тяжёлое вооружение.");
        EN_TO_RU.put("Subject expressed agitation during interview. No physical incident.",
                     "Объект проявил возбуждение во время интервью. Физических инцидентов нет.");
        EN_TO_RU.put("Visual contact established with junior staff. One casualty.",
                     "Установлен визуальный контакт с младшим персоналом. Одна жертва.");
        EN_TO_RU.put("Setting dial malfunction during routine experiment. No anomalous effect.",
                     "Неисправность настроечного диска во время рутинного эксперимента. Аномальных эффектов нет.");
        // Краткие из Firebird:
        EN_TO_RU.put("Containment breach. 3 casualties.",
                     "Нарушение содержания. 3 жертвы.");
        EN_TO_RU.put("Subject attempted breach.",
                     "Объект попытался прорваться.");
        EN_TO_RU.put("Subject expressed agitation during interview.",
                     "Объект проявил возбуждение во время интервью.");
        EN_TO_RU.put("Visual contact with junior staff. One casualty.",
                     "Визуальный контакт с младшим персоналом. Одна жертва.");
        EN_TO_RU.put("Setting dial malfunction.",
                     "Неисправность настроечного диска.");

        // ===== procedure_revisions.procedure_text =====
        EN_TO_RU.put("Subject must be kept under direct observation by minimum 2 personnel at all times. Blinking shifts coordinated.",
                     "Объект должен находиться под прямым наблюдением минимум 2 сотрудников постоянно. Смены моргания координируются.");
        EN_TO_RU.put("Revised: minimum 3 personnel with overlapping observation windows. Camera redundancy required.",
                     "Пересмотрено: минимум 3 сотрудника с перекрывающимися окнами наблюдения. Требуется резервирование камер.");
        EN_TO_RU.put("Subject contained in reinforced cell. Standard armaments insufficient.",
                     "Объект содержится в усиленной камере. Стандартное вооружение недостаточно.");
        EN_TO_RU.put("Subject is cooperative within research contexts. Maintain dialogue protocols.",
                     "Объект сотрудничает в исследовательских контекстах. Соблюдать протоколы диалога.");
        EN_TO_RU.put("Device operates without anomalous effect on operator. Refine setting documentation.",
                     "Устройство работает без аномального воздействия на оператора. Дополнить документацию по настройкам.");
        // Краткие из Firebird:
        EN_TO_RU.put("Min. 2 personnel observation.",
                     "Минимум 2 наблюдателя.");
        EN_TO_RU.put("Min. 3 personnel + camera redundancy.",
                     "Минимум 3 наблюдателя + резервирование камер.");
        EN_TO_RU.put("Reinforced cell required.",
                     "Требуется усиленная камера.");
        EN_TO_RU.put("Dialogue protocols.",
                     "Протоколы диалога.");
        EN_TO_RU.put("Refine setting documentation.",
                     "Дополнить документацию по настройкам.");
    }

    private DataI18n() {}

    /**
     * Переводит на русский, если активна локаль RU и в словаре есть совпадение.
     * Иначе возвращает исходную строку (пользовательские записи показываются
     * как введены).
     */
    public static String t(String englishText) {
        if (englishText == null) return null;
        if (Lang.current().getLanguage().equals("ru")) {
            String ru = EN_TO_RU.get(englishText);
            if (ru != null) return ru;
        }
        return englishText;
    }
}
