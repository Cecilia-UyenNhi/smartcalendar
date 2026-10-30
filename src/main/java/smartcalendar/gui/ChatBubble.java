
package smartcalendar.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class ChatBubble extends HBox {

    private static final String BLUE = "#315D85";

    private static final DateTimeFormatter CLOCK =
        DateTimeFormatter.ofPattern("h:mm a");

    public ChatBubble(String message, boolean fromUser) {

        setAlignment(
            fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT
        );

        setMaxWidth(Double.MAX_VALUE);

        // Message text
        Label content = new Label(message);
        content.setWrapText(true);
        content.setMaxWidth(260);

        content.setStyle(
            "-fx-text-fill: " + BLUE + ";" +
            "-fx-font-size: 12px;"
        );

        // Rounded pill-shaped bubble
        HBox bubble = new HBox(content);
        bubble.setAlignment(Pos.CENTER_LEFT);
        bubble.setPadding(new Insets(12, 17, 12, 17));
        bubble.setMaxWidth(300);

        String background = fromUser
            ? "#D4E8FC"
            : "#FFFFFF";

        bubble.setStyle(
            "-fx-background-color: " + background + ";" +
            "-fx-background-radius: 30;" +
            "-fx-border-color: " + BLUE + ";" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 30;"
        );

        // Timestamp below each bubble
        Label timestamp = new Label(
            LocalTime.now().format(CLOCK)
        );

        timestamp.setStyle(
            "-fx-font-size: 10px;" +
            "-fx-text-fill: #7895AD;"
        );

        VBox container = new VBox(
            4, bubble, timestamp
        );

        container.setAlignment(
            fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT
        );

        getChildren().add(container);
    }
}
