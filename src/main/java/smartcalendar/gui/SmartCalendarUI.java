
package smartcalendar.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;
import smartcalendar.observers.*;
import smartcalendar.services.CalendarService;
import smartcalendar.services.CalendarFacade;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

public class SmartCalendarUI {

    private final BorderPane root = new BorderPane();

    private final CalendarService service = new CalendarService();
    private final EventRepo repo = new EventRepo();
    private final CalendarFacade facade;

    private final GridPane monthGrid = new GridPane();
    private final ListView<Event> weeklyList = new ListView<>();
    private final Label monthLabel = new Label();

    private YearMonth shown = YearMonth.now();
    private LocalDate selected = LocalDate.now();

    // An event can be selected from either calendar view.
    private Event selectedEvent = null;

    // Store monthly event labels so selection can be highlighted.
    private final Map<Label, Event> monthEventLabels =
        new HashMap<>();

    private static final String BACKGROUND = "#E0EDFA";
    private static final String PRIMARY = "#315D85";
    private static final String LIGHT_BLUE = "#F2F7FD";
    private static final String EVENT_BLUE = "#C6E0FA";
    private static final String SELECTED_BLUE = "#A9CFF2";

    public SmartCalendarUI() {
            facade = new CalendarFacade(repo, service);

        // ========================================
        // 1. CALENDAR CONTROLS
        // ========================================

        Button prev = new Button("◀");
        Button next = new Button("▶");
        Button today = new Button("Today");

        Button add = new Button("+ Add Event");
        Button edit = new Button("Edit Selected");
        Button delete = new Button("Delete Selected");

        prev.setOnAction(e -> {
            shown = shown.minusMonths(1);
            clearEventSelection();
            refresh();
        });

        next.setOnAction(e -> {
            shown = shown.plusMonths(1);
            clearEventSelection();
            refresh();
        });

        today.setOnAction(e -> {
            shown = YearMonth.now();
            selected = LocalDate.now();
            clearEventSelection();
            refresh();
        });

        add.setOnAction(e -> eventDialog(null));

        // Edit whichever event was selected,
        // whether from the month or week view.
        edit.setOnAction(e -> {
            if (selectedEvent == null) {
                alert(
                    "Select an event from the monthly calendar " +
                    "or weekly schedule first."
                );
                return;
            }

            eventDialog(selectedEvent);
        });

        // Delete whichever event was selected.
        
        delete.setOnAction(e -> {

            if (selectedEvent == null) {
                alert("Select an event from the calendar first.");
                return;
            }

            Event eventToDelete = selectedEvent;

            // Ask for confirmation before deleting
            Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete \"" +
                eventToDelete.getTitle() + "\"?",
                ButtonType.YES,
                ButtonType.NO
            );

            confirmation.setTitle("Delete Event");
            confirmation.setHeaderText("Confirm deletion");

            Optional<ButtonType> answer =
                confirmation.showAndWait();

            if (answer.isPresent()
                && answer.get() == ButtonType.YES) {

                try {
                    facade.deleteEvent(eventToDelete);

                    clearEventSelection();

                } catch (Exception ex) {
                    alert("Could not delete event: " + ex.getMessage());
                }
            }
        });


        styleButton(prev);
        styleButton(next);
        styleButton(today);
        styleButton(add);
        styleButton(edit);
        styleButton(delete);

        monthLabel.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + PRIMARY + ";"
        );

        HBox navigation = new HBox(
            8, prev, next, today, monthLabel
        );
        navigation.setAlignment(Pos.CENTER_LEFT);

        HBox actions = new HBox(6, add, edit, delete);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox toolbar = new VBox(8, navigation, actions);
        toolbar.setPadding(new Insets(4, 2, 4, 2));

        // ========================================
        // 2. MONTHLY CALENDAR
        // ========================================

        monthGrid.setHgap(5);
        monthGrid.setVgap(5);
        monthGrid.setMaxWidth(Double.MAX_VALUE);

        ScrollPane calendarScroll =
            new ScrollPane(monthGrid);

        calendarScroll.setFitToWidth(true);
        calendarScroll.setFitToHeight(true);
        calendarScroll.setHbarPolicy(
            ScrollPane.ScrollBarPolicy.NEVER
        );

        calendarScroll.setStyle(
            "-fx-background-color: white;" +
            "-fx-background: white;" +
            "-fx-border-color: transparent;"
        );

        VBox calendarCard = new VBox(calendarScroll);

        // Slightly smaller white card:
        // reduced internal padding from 10 to 7.
        calendarCard.setPadding(new Insets(7));

        calendarCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;"
        );

        VBox.setVgrow(calendarScroll, Priority.ALWAYS);

        // ========================================
        // 3. WEEKLY SCHEDULE
        // ========================================

        Label weeklyTitle = new Label("Weekly Schedule");

        weeklyTitle.setStyle(
            "-fx-font-size: 17px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + PRIMARY + ";"
        );

        weeklyList.setCellFactory(v -> new ListCell<>() {

            @Override
            protected void updateItem(
                Event ev,
                boolean empty
            ) {
                super.updateItem(ev, empty);

                if (empty || ev == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(
                    ev.getDate() + "  " +
                    ev.getStartTime() + "–" +
                    ev.getEndTime() + "\n" +
                    ev.getTitle()
                );

                boolean isSelected =
                    getListView().getSelectionModel()
                        .getSelectedItem() == ev;

                if (isSelected) {
                    setStyle(
                        "-fx-background-color: " +
                        SELECTED_BLUE + ";" +
                        "-fx-text-fill: " + PRIMARY + ";" +
                        "-fx-padding: 8;"
                    );
                } else {
                    setStyle(
                        "-fx-background-color: " +
                        LIGHT_BLUE + ";" +
                        "-fx-text-fill: " + PRIMARY + ";" +
                        "-fx-padding: 8;"
                    );
                }
            }
        });

        // Clicking a weekly event updates the shared selection.
        weeklyList.getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, oldEvent, newEvent) -> {
                if (newEvent != null) {
                    selectEvent(newEvent);
                }
            });

        VBox weeklyCard = new VBox(
            8,
            weeklyTitle,
            weeklyList
        );

        // Smaller white card than before.
        weeklyCard.setPadding(new Insets(7));
        weeklyCard.setPrefHeight(175);

        weeklyCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;"
        );

        VBox.setVgrow(weeklyList, Priority.ALWAYS);

        // ========================================
        // 4. LEFT SIDE — 70%
        // ========================================

        VBox left = new VBox(
            10,
            toolbar,
            calendarCard,
            weeklyCard
        );

        // More blue space around the white cards.
        left.setPadding(new Insets(12));
        left.setMinWidth(0);

        left.setStyle(
            "-fx-background-color: " + BACKGROUND + ";"
        );

        VBox.setVgrow(calendarCard, Priority.ALWAYS);

        // ========================================
        // 5. SISSY CHATBOT — 30%
        // ========================================

        Label sissyTitle = new Label("Sissy");

        sissyTitle.setStyle(
            "-fx-font-size: 21px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + PRIMARY + ";"
        );

        Label subtitle =
            new Label("AI Calendar Assistant");

        subtitle.setStyle(
            "-fx-text-fill: #7895AD;"
        );

        VBox chatHeader = new VBox(
            3,
            sissyTitle,
            subtitle
        );

        VBox messages = new VBox(10);
        messages.setPadding(new Insets(10));

        Label greeting = new Label(
            "Hi! I'm Sissy. I can help you organize your calendar!"
        );

        greeting.setWrapText(true);
        greeting.setMaxWidth(Double.MAX_VALUE);

        greeting.setStyle(
            "-fx-background-color: #EAF3FD;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 12;" +
            "-fx-text-fill: " + PRIMARY + ";"
        );

        messages.getChildren().add(greeting);

        ScrollPane chatHistory =
            new ScrollPane(messages);

        chatHistory.setFitToWidth(true);
        chatHistory.setHbarPolicy(
            ScrollPane.ScrollBarPolicy.NEVER
        );

        chatHistory.setStyle(
            "-fx-background-color: white;" +
            "-fx-background: white;" +
            "-fx-border-color: transparent;"
        );

        VBox.setVgrow(
            chatHistory,
            Priority.ALWAYS
        );

        TextField chatInput = new TextField();
        chatInput.setPromptText("Ask Sissy...");
        chatInput.setMaxWidth(Double.MAX_VALUE);

        Button attach = new Button("📎");
        Button send = new Button("Send");

        styleButton(attach);
        styleButton(send);

        HBox.setHgrow(
            chatInput,
            Priority.ALWAYS
        );

        HBox inputBar = new HBox(
            6,
            attach,
            chatInput,
            send
        );

        inputBar.setAlignment(Pos.CENTER);

        // Placeholder until AIAgent is implemented.
        send.setOnAction(e -> {
            String request =
                chatInput.getText().trim();

            if (!request.isEmpty()) {

                Label userMessage =
                    new Label(request);

                userMessage.setWrapText(true);
                userMessage.setMaxWidth(
                    Double.MAX_VALUE
                );

                userMessage.setStyle(
                    "-fx-background-color: #DCEBFA;" +
                    "-fx-background-radius: 10;" +
                    "-fx-padding: 10;" +
                    "-fx-text-fill: " + PRIMARY + ";"
                );

                messages.getChildren()
                    .add(userMessage);

                Label response = new Label(
                    "Sissy's AI functionality is coming soon."
                );

                response.setWrapText(true);
                response.setMaxWidth(
                    Double.MAX_VALUE
                );

                response.setStyle(
                    "-fx-background-color: #EAF3FD;" +
                    "-fx-background-radius: 10;" +
                    "-fx-padding: 10;"
                );

                messages.getChildren()
                    .add(response);

                chatInput.clear();
                chatHistory.setVvalue(1.0);
            }
        });

        chatInput.setOnAction(
            e -> send.fire()
        );

        attach.setOnAction(e ->
            new Alert(
                Alert.AlertType.INFORMATION,
                "Schedule importing will be connected later."
            ).showAndWait()
        );

        VBox right = new VBox(
            10,
            chatHeader,
            new Separator(),
            chatHistory,
            inputBar
        );

        right.setPadding(new Insets(12));
        right.setMinWidth(0);

        right.setStyle(
            "-fx-background-color: white;"
        );

        // ========================================
        // 6. 70% / 30% SPLIT
        // ========================================

        HBox layout = new HBox(left, right);
        layout.setSpacing(0);
        layout.setFillHeight(true);

        left.prefWidthProperty().bind(
            layout.widthProperty().multiply(0.70)
        );

        right.prefWidthProperty().bind(
            layout.widthProperty().multiply(0.30)
        );

        root.setCenter(layout);

        root.setStyle(
            "-fx-background-color: " +
            BACKGROUND + ";"
        );

        // ========================================
        // 7. OBSERVER PATTERN
        // ========================================

        service.addObserver(
            new CalendarView(this::drawMonth)
        );

        service.addObserver(
            new WeeklySchedView(this::drawWeek)
        );
    }

    // ========================================
    // REQUIRED BY Main.java
    // ========================================

    public BorderPane getRoot() {
        return root;
    }

    public void refresh() {
        service.updateViews();
    }

    // ========================================
    // SHARED EVENT SELECTION
    // ========================================

    private void selectEvent(Event event) {
        selectedEvent = event;

        // Update monthly event highlights.
        for (Map.Entry<Label, Event> entry :
                monthEventLabels.entrySet()) {

            Label label = entry.getKey();
            Event candidate = entry.getValue();

            if (candidate == event) {
                styleMonthEvent(label, true);
            } else {
                styleMonthEvent(label, false);
            }
        }
    }

    private void clearEventSelection() {
        selectedEvent = null;

        weeklyList.getSelectionModel()
            .clearSelection();

        for (Label label :
                monthEventLabels.keySet()) {
            styleMonthEvent(label, false);
        }
    }

    private void styleMonthEvent(
        Label item,
        boolean isSelected
    ) {
        if (isSelected) {
            item.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-text-fill: " + PRIMARY + ";" +
                "-fx-background-color: " +
                SELECTED_BLUE + ";" +
                "-fx-border-color: " +
                PRIMARY + ";" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 4;" +
                "-fx-background-radius: 4;" +
                "-fx-padding: 3;"
            );
        } else {
            item.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-text-fill: " + PRIMARY + ";" +
                "-fx-background-color: " +
                EVENT_BLUE + ";" +
                "-fx-background-radius: 4;" +
                "-fx-padding: 3;"
            );
        }
    }

    // ========================================
    // MONTHLY CALENDAR RENDERING
    // ========================================

    private void drawMonth(List<Event> events) {

        monthLabel.setText(
            shown.getMonth().getDisplayName(
                TextStyle.FULL,
                Locale.ENGLISH
            ) + " " + shown.getYear()
        );

        monthGrid.getChildren().clear();
        monthGrid.getColumnConstraints().clear();
        monthEventLabels.clear();

        for (int c = 0; c < 7; c++) {
            ColumnConstraints cc =
                new ColumnConstraints();

            cc.setPercentWidth(100.0 / 7);

            monthGrid.getColumnConstraints()
                .add(cc);
        }

        String[] days = {
            "Mon", "Tue", "Wed",
            "Thu", "Fri", "Sat", "Sun"
        };

        for (int c = 0; c < 7; c++) {

            Label label = new Label(days[c]);

            label.setStyle(
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " +
                PRIMARY + ";"
            );

            monthGrid.add(label, c, 0);
        }

        int offset =
            shown.atDay(1)
                .getDayOfWeek()
                .getValue() - 1;

        for (int d = 1;
                d <= shown.lengthOfMonth();
                d++) {

            LocalDate day = shown.atDay(d);

            int pos = offset + d - 1;
            int row = 1 + pos / 7;
            int col = pos % 7;

            VBox cell = new VBox(4);

            cell.setPadding(new Insets(6));
            cell.setMinHeight(85);
            cell.setMinWidth(0);
            cell.setMaxWidth(
                Double.MAX_VALUE
            );

            String background =
                day.equals(selected)
                    ? "#D2E5FA"
                    : LIGHT_BLUE;

            cell.setStyle(
                "-fx-background-color: " +
                background + ";" +
                "-fx-background-radius: 5;" +
                "-fx-border-color: #DFEAF5;" +
                "-fx-border-radius: 5;"
            );

            Label dateLabel = new Label(
                String.valueOf(d)
            );

            dateLabel.setStyle(
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " +
                PRIMARY + ";"
            );

            cell.getChildren().add(dateLabel);

            VBox eventItems = new VBox(3);

            for (Event ev : events) {

                if (ev.getDate().equals(
                        day.toString())) {

                    Label item = new Label(
                        ev.getStartTime() +
                        " " + ev.getTitle()
                    );

                    item.setWrapText(true);
                    item.setMaxWidth(
                        Double.MAX_VALUE
                    );

                    styleMonthEvent(
                        item,
                        ev == selectedEvent
                    );

                    monthEventLabels.put(
                        item,
                        ev
                    );

                    // Click an event in the month
                    // to select it for editing/deleting.
                    item.setOnMouseClicked(e -> {
                        selected = day;
                        selectEvent(ev);

                        // Stop the date cell from
                        // handling the same click.
                        e.consume();
                    });

                    eventItems.getChildren()
                        .add(item);
                }
            }

            ScrollPane eventScroll =
                new ScrollPane(eventItems);

            eventScroll.setFitToWidth(true);

            eventScroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
            );

            eventScroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;"
            );

            VBox.setVgrow(
                eventScroll,
                Priority.ALWAYS
            );

            cell.getChildren()
                .add(eventScroll);

            // Clicking an empty date changes
            // the selected week.
            cell.setOnMouseClicked(e -> {
                selected = day;
                clearEventSelection();
                refresh();
            });

            monthGrid.add(
                cell,
                col,
                row
            );
        }
    }

    // ========================================
    // WEEKLY SCHEDULE RENDERING
    // ========================================

    private void drawWeek(List<Event> events) {

        LocalDate monday =
            selected.with(
                DayOfWeek.MONDAY
            );

        LocalDate sunday =
            monday.plusDays(6);

        weeklyList.getItems().setAll(
            events.stream()
                .filter(ev -> {

                    LocalDate d =
                        LocalDate.parse(
                            ev.getDate()
                        );

                    return !d.isBefore(monday)
                        && !d.isAfter(sunday);
                })
                .toList()
        );
    }

    // ========================================
    // ADD / EDIT EVENT DIALOG
    // ========================================

    private void eventDialog(Event old) {

        Dialog<ButtonType> dialog =
            new Dialog<>();

        dialog.setTitle(
            old == null
                ? "Add Event"
                : "Edit Event"
        );

        dialog.getDialogPane()
            .getButtonTypes()
            .addAll(
                ButtonType.OK,
                ButtonType.CANCEL
            );

        TextField title = new TextField(
            old == null
                ? ""
                : old.getTitle()
        );

        TextField start = new TextField(
            old == null
                ? "09:00"
                : old.getStartTime()
        );

        TextField end = new TextField(
            old == null
                ? "10:00"
                : old.getEndTime()
        );

        TextField location = new TextField(
            old == null
                || old.getLocation() == null
                    ? ""
                    : old.getLocation()
        );

        TextField color = new TextField(
            old == null
                || old.getCatColor() == null
                    ? ""
                    : old.getCatColor()
        );

        DatePicker date = new DatePicker(
            old == null
                ? selected
                : LocalDate.parse(
                    old.getDate()
                )
        );

        GridPane form = new GridPane();

        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(
            new Insets(15)
        );

        String[] labels = {
            "Title",
            "Date",
            "Start (HH:mm)",
            "End (HH:mm)",
            "Location",
            "Category color"
        };

        Node[] fields = {
            title,
            date,
            start,
            end,
            location,
            color
        };

        for (int i = 0;
                i < labels.length;
                i++) {

            form.add(
                new Label(labels[i]),
                0,
                i
            );

            form.add(
                fields[i],
                1,
                i
            );
        }

        dialog.getDialogPane()
            .setContent(form);

        Optional<ButtonType> result =
            dialog.showAndWait();

        if (result.isPresent()
            && result.get()
                == ButtonType.OK) {

            try {

                Event updated = new Event(
                    title.getText(),
                    date.getValue()
                        .toString(),
                    start.getText(),
                    end.getText(),
                    location.getText(),
                    color.getText()
                );

                if (old == null) {
                    facade.addEvent(updated);
                } else {
                    facade.editEvent(old, updated);
                }

                selected = date.getValue();
                shown = YearMonth.from(
                    selected
                );

                clearEventSelection();
                refresh();

            } catch (Exception ex) {
                alert(
                    "Could not save event: " +
                    ex.getMessage()
                );
            }
        }
    }

    // ========================================
    // UI HELPERS
    // ========================================

    private void styleButton(
        Button button
    ) {
        button.setStyle(
            "-fx-background-color: " +
            EVENT_BLUE + ";" +
            "-fx-text-fill: " +
            PRIMARY + ";" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 7 11;"
        );
    }

    private void alert(
        String message
    ) {
        new Alert(
            Alert.AlertType.ERROR,
            message,
            ButtonType.OK
        ).showAndWait();
    }
}
