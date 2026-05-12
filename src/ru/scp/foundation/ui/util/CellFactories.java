package ru.scp.foundation.ui.util;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Callback;
import javafx.util.Duration;
import ru.scp.foundation.model.ObjectClass;
import ru.scp.foundation.util.Lang;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Кастомные TableCell-фабрики, реализующие визуальные элементы из дизайна:
 *   classChip(...)    — цветной бейдж для ObjectClass (Safe/Euclid/Keter)
 *   severityBar(...)  — 5 вертикальных полосок, закрашенных по severity 1..5
 *   date(...)         — форматированная дата
 *   dateTime(...)     — форматированный timestamp
 *   monoNumber(...)   — оранжевая моноширинная подача числа
 */
public final class CellFactories {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private CellFactories() {}

    /** Бейдж для ObjectClass: Safe / Euclid / Keter с цветной обводкой. */
    public static <S> Callback<TableColumn<S, ObjectClass>, TableCell<S, ObjectClass>> classChip() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(ObjectClass item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                String key = "class." + item.name().toLowerCase();
                Label chip = new Label(Lang.t(key));
                chip.getStyleClass().setAll("class-chip", item.name().toLowerCase());
                setText(null);
                setGraphic(chip);
            }
        };
    }

    /** Шкала severity (1..5 окрашенных красных полосок). */
    public static <S> Callback<TableColumn<S, Number>, TableCell<S, Number>> severityBar() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                int sev = item.intValue();
                HBox bar = new HBox();
                bar.getStyleClass().add("severity-bar");
                bar.setAlignment(Pos.CENTER_LEFT);
                for (int i = 1; i <= 5; i++) {
                    Region cell = new Region();
                    cell.getStyleClass().add("bar-cell");
                    if (i <= sev) cell.getStyleClass().add("on");
                    bar.getChildren().add(cell);
                }
                setText(null);
                setGraphic(bar);
            }
        };
    }

    /** LocalDate → "yyyy-MM-dd" */
    public static <S> Callback<TableColumn<S, LocalDate>, TableCell<S, LocalDate>> date() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                setText(empty || item == null ? "" : DATE_FMT.format(item));
            }
        };
    }

    /** LocalDateTime → "yyyy-MM-dd HH:mm" */
    public static <S> Callback<TableColumn<S, LocalDateTime>, TableCell<S, LocalDateTime>> dateTime() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                setText(empty || item == null ? "" : DT_FMT.format(item));
            }
        };
    }

    /**
     * Ячейка показывает текст с переносом по словам. Tooltip убран
     * намеренно — Popup-tooltip мигал из-за фоновых layout-passов JavaFX
     * (и без анимации фона тоже). Полный текст доступен через двойной клик
     * «редактировать».
     */
    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> wrappingText() {
        return col -> {
            TableCell<S, String> cell = new TableCell<S, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                }
            };
            cell.setWrapText(true);
            return cell;
        };
    }

    /** Моноширинный оранжевый идентификатор. */
    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> mono() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                if (empty || item == null) {
                    setText("");
                    return;
                }
                setText(item.toString());
                if (!getStyleClass().contains("col-mono")) getStyleClass().add("col-mono");
            }
        };
    }
}
