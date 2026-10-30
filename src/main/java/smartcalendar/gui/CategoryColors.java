
package smartcalendar.gui;

import javafx.geometry.Insets;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CategoryColors {

    private CategoryColors() {}

    private static final Map<String, String> COLORS =
        new LinkedHashMap<>();

    static {
        COLORS.put("Red", "#F5B8B8");
        COLORS.put("Orange", "#F9D1A7");
        COLORS.put("Yellow", "#F8E9A1");
        COLORS.put("Green", "#BFE5C5");
        COLORS.put("Blue", "#BDD9F5");
        COLORS.put("Purple", "#D9C7F4");
        COLORS.put("Pink", "#F6C9E2");
        COLORS.put("Brown", "#DCC9B8");
        COLORS.put("Black", "#C4C7CE");
    }

    public static String hex(String name) {
        return COLORS.getOrDefault(name, COLORS.get("Blue"));
    }

    public static String nameFor(String stored) {
        if (stored != null) {
            for (Map.Entry<String, String> entry : COLORS.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(stored)
                    || entry.getValue().equalsIgnoreCase(stored)) {
                    return entry.getKey();
                }
            }
        }
        return "Blue";
    }

    public static String displayHex(String stored) {
        return hex(nameFor(stored));
    }

    public static ComboBox<String> makeComboBox() {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(COLORS.keySet());
        combo.setValue("Blue");
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setCellFactory(list -> colorCell());
        combo.setButtonCell(colorCell());
        return combo;
    }

    private static ListCell<String> colorCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);

                if (empty || name == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Region swatch = new Region();
                swatch.setMinSize(19, 19);
                swatch.setPrefSize(19, 19);
                swatch.setMaxSize(19, 19);

                swatch.setStyle(
                    "-fx-background-color: " + hex(name) + ";" +
                    "-fx-background-radius: 5;" +
                    "-fx-border-color: #B5C5D5;" +
                    "-fx-border-radius: 5;"
                );

                Label label = new Label(name);
                HBox content = new HBox(9, swatch, label);
                content.setPadding(new Insets(2));

                setGraphic(content);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        };
    }
}
