
package smartcalendar.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;
import smartcalendar.observers.CalendarView;
import smartcalendar.observers.WeeklySchedView;
import smartcalendar.services.CalendarFacade;
import smartcalendar.services.CalendarService;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class SmartCalendarUI {

    // ==========================================
    // COLORS
    // ==========================================

    private static final String BACKGROUND = "#E0EDFA";
    private static final String PRIMARY = "#315D85";
    private static final String LIGHT_BLUE = "#F2F7FD";
    private static final String EVENT_BLUE = "#C6E0FA";

    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm");

    // ==========================================
    // SERVICES AND DATABASE
    // ==========================================

    private final BorderPane root = new BorderPane();

    private final CalendarService service =
        new CalendarService();

    private final EventRepo repo = new EventRepo();

    private final CalendarFacade facade;

    // ==========================================
    // CALENDAR STATE
    // ==========================================

    private YearMonth shown = YearMonth.now();
    private LocalDate selected = LocalDate.now();

    private Event selectedEvent = null;

    private final GridPane monthGrid = new GridPane();
    private final ListView<Event> weeklyList = new ListView<>();

    private final Label monthLabel = new Label();
    private final Label weekRangeLabel = new Label();

    private final Map<Label, Event> monthEventLabels =
        new HashMap<>();

    // ==========================================
    // INLINE EVENT EDITOR
    // ==========================================

    private final VBox editorCard = new VBox(8);

    private final Label editorHeading =
        new Label("Add / Edit Event");

    private final TextField editorTitle = new TextField();
    private final DatePicker editorDate = new DatePicker();

    private final TextField editorStart = new TextField();
    private final TextField editorEnd = new TextField();

    private final TextField editorLocation =
        new TextField();

    private final ComboBox<String> editorColor =
        CategoryColors.makeComboBox();

    private Event editingOriginal = null;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public SmartCalendarUI() {

        facade = new CalendarFacade(repo, service);

        // ======================================
        // 1. CALENDAR TOOLBAR
        // ======================================

        Button prev = new Button("◀");
        Button next = new Button("▶");
        Button today = new Button("Today");

        Button add = new Button("＋ Add Event");
        Button edit = new Button("✎ Edit Selected");
        Button delete = new Button("🗑 Delete Selected");

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
            selected = LocalDate.now();
            shown = YearMonth.from(selected);
            clearEventSelection();
            refresh();
        });

        // Open blank editor.
        add.setOnAction(e -> openEditor(null));

        // Open editor with selected event.
        edit.setOnAction(e -> {

            if (selectedEvent == null) {
                alert(
                    "Select an event from the monthly " +
                    "calendar or weekly schedule first."
                );
                return;
            }

            openEditor(selectedEvent);
        });

        // Delete selected event.
        delete.setOnAction(e -> {

            if (selectedEvent == null) {
                alert("Select an event first.");
                return;
            }

            Event eventToDelete = selectedEvent;

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

                    closeEditor();
                    clearEventSelection();

                } catch (Exception ex) {
                    alert(
                        "Could not delete event: " +
                        ex.getMessage()
                    );
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

        HBox actions = new HBox(
            6, add, edit, delete
        );
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox toolbar = new VBox(
            8, navigation, actions
        );
        toolbar.setPadding(new Insets(4, 2, 4, 2));

        // ======================================
        // 2. MONTHLY CALENDAR
        // ======================================

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

        calendarCard.setPadding(new Insets(7));

        calendarCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;"
        );

        VBox.setVgrow(
            calendarScroll,
            Priority.ALWAYS
        );

        // ======================================
        // 3. WEEKLY SCHEDULE
        // ======================================

        Label weeklyTitle =
            new Label("Weekly Schedule");

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

                String bg =
                    CategoryColors.displayHex(
                        ev.getCatColor()
                    );

                boolean selectedCell =
                    getListView().getSelectionModel()
                        .getSelectedItem() == ev;

                setStyle(
                    "-fx-background-color: " + bg + ";" +
                    "-fx-text-fill: " + PRIMARY + ";" +
                    "-fx-padding: 8;" +
                    "-fx-background-radius: 6;" +
                    (selectedCell
                        ? "-fx-border-color: " +
                          PRIMARY + ";" +
                          "-fx-border-width: 2;" +
                          "-fx-border-radius: 6;"
                        : "-fx-border-color: transparent;")
                );
            }
        });

        weeklyList.getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, oldEvent, newEvent) -> {

                if (newEvent != null) {
                    selectEvent(newEvent);
                }
            });

        // Keep weekly sliding navigation.
        Button weekPrev = new Button("◀");
        Button weekNext = new Button("▶");

        styleButton(weekPrev);
        styleButton(weekNext);

        weekRangeLabel.setStyle(
            "-fx-text-fill: " + PRIMARY + ";" +
            "-fx-font-weight: bold;"
        );

        weekPrev.setOnAction(e -> {
            selected = selected.minusWeeks(1);
            shown = YearMonth.from(selected);
            clearEventSelection();
            refresh();
        });

        weekNext.setOnAction(e -> {
            selected = selected.plusWeeks(1);
            shown = YearMonth.from(selected);
            clearEventSelection();
            refresh();
        });

        Region weekSpacer = new Region();

        HBox.setHgrow(
            weekSpacer,
            Priority.ALWAYS
        );

        HBox weeklyHeader = new HBox(
            8,
            weeklyTitle,
            weekSpacer,
            weekPrev,
            weekRangeLabel,
            weekNext
        );

        weeklyHeader.setAlignment(Pos.CENTER_LEFT);

        VBox weeklyCard = new VBox(
            8, weeklyHeader, weeklyList
        );

        weeklyCard.setPadding(new Insets(10));
        weeklyCard.setPrefHeight(260);
        weeklyCard.setMinHeight(260);

        weeklyCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;"
        );

        VBox.setVgrow(
            weeklyList,
            Priority.ALWAYS
        );

        // ======================================
        // 4. LEFT PANEL - 70%
        // ======================================

        VBox left = new VBox(
            10,
            toolbar,
            calendarCard,
            weeklyCard
        );

        left.setPadding(new Insets(12));
        left.setMinWidth(0);

        left.setStyle(
            "-fx-background-color: " +
            BACKGROUND + ";"
        );

        VBox.setVgrow(
            calendarCard,
            Priority.ALWAYS
        );

        // ======================================
        // 5. SISSY CHAT
        // ======================================

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

        // Keep robot logo.
        Label robotLogo = new Label("🤖");

        robotLogo.setStyle(
            "-fx-font-size: 26px;" +
            "-fx-background-color: #E0EDFA;" +
            "-fx-background-radius: 28;" +
            "-fx-padding: 6;"
        );

        VBox chatNames = new VBox(
            3, sissyTitle, subtitle
        );

        HBox chatHeader = new HBox(
            9, robotLogo, chatNames
        );

        chatHeader.setAlignment(Pos.CENTER_LEFT);

        // ======================================
        // 6. CHAT BUBBLES
        // ======================================

        VBox messages = new VBox(10);
        messages.setPadding(new Insets(10));

        messages.getChildren().add(
            new ChatBubble(
                "Hi! I'm Sissy. I can help you " +
                "organize your calendar!",
                false
            )
        );

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
            6, attach, chatInput, send
        );

        inputBar.setAlignment(Pos.CENTER);

        // Temporary chat until Gemini is ready.
        send.setOnAction(e -> {

            String request =
                chatInput.getText().trim();

            if (request.isEmpty()) {
                return;
            }

            // User: right bubble.
            messages.getChildren().add(
                new ChatBubble(request, true)
            );

            // Sissy: left bubble.
            messages.getChildren().add(
                new ChatBubble(
                    "Sissy's AI functionality " +
                    "is coming soon.",
                    false
                )
            );

            chatInput.clear();

            Platform.runLater(
                () -> chatHistory.setVvalue(1.0)
            );
        });

        chatInput.setOnAction(
            e -> send.fire()
        );

        attach.setOnAction(e ->
            new Alert(
                Alert.AlertType.INFORMATION,
                "Schedule importing will be " +
                "connected later."
            ).showAndWait()
        );

        VBox chatCard = new VBox(
            8,
            chatHeader,
            new Separator(),
            chatHistory,
            inputBar
        );

        chatCard.setPadding(new Insets(10));
        chatCard.setMinHeight(180);
        chatCard.setMinWidth(0);
        chatCard.setMaxWidth(Double.MAX_VALUE);

        chatCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;"
        );

        VBox.setVgrow(
            chatHistory,
            Priority.ALWAYS
        );

        VBox.setVgrow(
            chatCard,
            Priority.ALWAYS
        );

        // ======================================
        // 7. EVENT EDITOR BELOW SISSY
        // ======================================

        buildEditorCard();

        VBox right = new VBox(
            10,
            chatCard,
            editorCard
        );

        right.setPadding(new Insets(12));
        right.setMinWidth(0);

        right.setStyle(
            "-fx-background-color: " +
            BACKGROUND + ";"
        );

        editorCard.setMaxWidth(Double.MAX_VALUE);

        // ======================================
        // 8. 70% / 30% LAYOUT
        // ======================================

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

        // ======================================
        // 9. OBSERVER PATTERN
        // ======================================

        service.addObserver(
            new CalendarView(this::drawMonth)
        );

        service.addObserver(
            new WeeklySchedView(this::drawWeek)
        );

        refresh();
    }

    // ==========================================
    // REQUIRED BY MAIN.JAVA
    // ==========================================

    public BorderPane getRoot() {
        return root;
    }

    public void refresh() {
        service.updateViews();
    }

    // ==========================================
    // SHARED EVENT SELECTION
    // ==========================================

    private void selectEvent(Event event) {

        selectedEvent = event;

        for (Map.Entry<Label, Event> entry :
                monthEventLabels.entrySet()) {

            styleMonthEvent(
                entry.getKey(),
                entry.getValue() == event
            );
        }

        weeklyList.refresh();
    }

    private void clearEventSelection() {

        selectedEvent = null;

        weeklyList.getSelectionModel()
            .clearSelection();

        for (Label label :
                monthEventLabels.keySet()) {

            styleMonthEvent(label, false);
        }

        weeklyList.refresh();
    }

    // ==========================================
    // MONTHLY EVENT COLOR
    // ==========================================

    private void styleMonthEvent(
        Label item,
        boolean isSelected
    ) {

        Event ev = monthEventLabels.get(item);

        String background = ev == null
            ? CategoryColors.hex("Blue")
            : CategoryColors.displayHex(
                ev.getCatColor()
            );

        item.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-text-fill: " + PRIMARY + ";" +
            "-fx-background-color: " +
            background + ";" +
            "-fx-background-radius: 4;" +
            "-fx-padding: 3;" +
            (isSelected
                ? "-fx-border-color: " +
                  PRIMARY + ";" +
                  "-fx-border-width: 2;" +
                  "-fx-border-radius: 4;"
                : "-fx-border-color: transparent;")
        );
    }

    // ==========================================
    // DRAW MONTHLY CALENDAR
    // ==========================================

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
            cell.setMaxWidth(Double.MAX_VALUE);

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

                if (!ev.getDate().equals(
                        day.toString())) {
                    continue;
                }

                Label item = new Label(
                    ev.getStartTime() +
                    " " + ev.getTitle()
                );

                item.setWrapText(true);
                item.setMaxWidth(Double.MAX_VALUE);

                // Store event before styling.
                monthEventLabels.put(item, ev);

                styleMonthEvent(
                    item,
                    ev == selectedEvent
                );

                item.setOnMouseClicked(e -> {

                    selected = day;

                    selectEvent(ev);

                    // Update the weekly schedule
                    // to the event's selected week.
                    drawWeek(service.getEvents());

                    e.consume();
                });

                eventItems.getChildren().add(item);
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

            cell.getChildren().add(eventScroll);

            cell.setOnMouseClicked(e -> {

                selected = day;

                clearEventSelection();
                refresh();
            });

            monthGrid.add(cell, col, row);
        }
    }

    // ==========================================
    // DRAW WEEKLY SCHEDULE
    // ==========================================

    private void drawWeek(List<Event> events) {

        LocalDate monday =
            selected.with(DayOfWeek.MONDAY);

        LocalDate sunday =
            monday.plusDays(6);

        DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("MMM d");

        weekRangeLabel.setText(
            monday.format(formatter) +
            " – " +
            sunday.format(formatter) +
            ", " +
            sunday.getYear()
        );

        List<Event> weekEvents =
            events.stream()
                .filter(ev -> {

                    LocalDate date =
                        LocalDate.parse(ev.getDate());

                    return !date.isBefore(monday)
                        && !date.isAfter(sunday);
                })
                .toList();

        weeklyList.getItems().setAll(weekEvents);

        weeklyList.refresh();
    }

    // ==========================================
    // BUILD INLINE EVENT EDITOR
    // ==========================================

    private void buildEditorCard() {

        editorHeading.setStyle(
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + PRIMARY + ";"
        );

        editorTitle.setPromptText("Event title");
        editorStart.setPromptText("HH:mm");
        editorEnd.setPromptText("HH:mm");

        editorLocation.setPromptText(
            "Location (optional)"
        );

        GridPane fields = new GridPane();

        fields.setHgap(7);
        fields.setVgap(6);

        ColumnConstraints labelColumn =
            new ColumnConstraints();

        labelColumn.setMinWidth(72);

        ColumnConstraints inputColumn =
            new ColumnConstraints();

        inputColumn.setHgrow(Priority.ALWAYS);

        fields.getColumnConstraints().addAll(
            labelColumn,
            inputColumn
        );

        fields.addRow(
            0,
            new Label("Title"),
            editorTitle
        );

        fields.addRow(
            1,
            new Label("Date"),
            editorDate
        );

        HBox times = new HBox(
            5,
            editorStart,
            new Label("to"),
            editorEnd
        );

        times.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(
            editorStart,
            Priority.ALWAYS
        );

        HBox.setHgrow(
            editorEnd,
            Priority.ALWAYS
        );

        editorStart.setMaxWidth(Double.MAX_VALUE);
        editorEnd.setMaxWidth(Double.MAX_VALUE);

        fields.addRow(
            2,
            new Label("Time"),
            times
        );

        fields.addRow(
            3,
            new Label("Location"),
            editorLocation
        );

        fields.addRow(
            4,
            new Label("Category"),
            editorColor
        );

        editorTitle.setMaxWidth(Double.MAX_VALUE);
        editorDate.setMaxWidth(Double.MAX_VALUE);

        editorLocation.setMaxWidth(
            Double.MAX_VALUE
        );

        editorColor.setMaxWidth(
            Double.MAX_VALUE
        );

        GridPane.setHgrow(
            editorTitle,
            Priority.ALWAYS
        );

        GridPane.setHgrow(
            editorDate,
            Priority.ALWAYS
        );

        GridPane.setHgrow(
            editorLocation,
            Priority.ALWAYS
        );

        GridPane.setHgrow(
            editorColor,
            Priority.ALWAYS
        );

        Button cancel = new Button("Cancel");
        Button save = new Button("Save Event");

        styleButton(cancel);
        styleButton(save);

        cancel.setOnAction(e -> closeEditor());
        save.setOnAction(e -> saveEditor());

        HBox actions = new HBox(
            8, cancel, save
        );

        actions.setAlignment(Pos.CENTER_RIGHT);

        editorCard.getChildren().setAll(
            editorHeading,
            fields,
            actions
        );

        editorCard.setPadding(new Insets(12));

        editorCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;"
        );

        closeEditor();
    }

    // ==========================================
    // OPEN EDITOR
    // ==========================================

    private void openEditor(Event old) {

        editingOriginal = old;

        editorHeading.setText(
            old == null
                ? "＋ Add Event"
                : "✎ Edit Event"
        );

        editorTitle.setText(
            old == null
                ? ""
                : old.getTitle()
        );

        editorDate.setValue(
            old == null
                ? selected
                : LocalDate.parse(old.getDate())
        );

        editorStart.setText(
            old == null
                ? "09:00"
                : old.getStartTime()
        );

        editorEnd.setText(
            old == null
                ? "10:00"
                : old.getEndTime()
        );

        editorLocation.setText(
            old == null || old.getLocation() == null
                ? ""
                : old.getLocation()
        );

        editorColor.setValue(
            CategoryColors.nameFor(
                old == null
                    ? null
                    : old.getCatColor()
            )
        );

        editorCard.setDisable(false);
        editorCard.setOpacity(1.0);

        editorTitle.requestFocus();
    }

    // ==========================================
    // CLOSE EDITOR
    // ==========================================

    private void closeEditor() {

        editingOriginal = null;

        editorHeading.setText(
            "▦ Add / Edit Event"
        );

        editorCard.setDisable(true);
        editorCard.setOpacity(0.50);
    }

    // ==========================================
    // SAVE EDITOR
    // ==========================================

    private void saveEditor() {

        try {

            String title =
                editorTitle.getText().trim();

            if (title.isEmpty()) {
                throw new IllegalArgumentException(
                    "Event title is required."
                );
            }

            if (editorDate.getValue() == null) {
                throw new IllegalArgumentException(
                    "Event date is required."
                );
            }

            LocalTime start = LocalTime.parse(
                editorStart.getText().trim()
            );

            LocalTime end = LocalTime.parse(
                editorEnd.getText().trim()
            );

            if (!start.isBefore(end)) {
                throw new IllegalArgumentException(
                    "End time must be after start time."
                );
            }

            Event updated = new Event(
                title,
                editorDate.getValue().toString(),
                start.format(TIME_FORMAT),
                end.format(TIME_FORMAT),
                editorLocation.getText().trim(),
                CategoryColors.hex(
                    editorColor.getValue()
                )
            );

            // Use Facade, not direct repository calls.
            // Check conflicts before saving.
List<Event> conflicts;

if (editingOriginal == null) {
    conflicts = facade.getConflictingEvents(updated);
} else {
    // Avoid treating the original event as a conflict
    // when editing it.
    conflicts = facade.getConflictingEvents(updated);
    conflicts.removeIf(ev ->
        ev.getTitle().equals(editingOriginal.getTitle())
        && ev.getDate().equals(editingOriginal.getDate())
        && ev.getStartTime().equals(editingOriginal.getStartTime())
        && ev.getEndTime().equals(editingOriginal.getEndTime())
    );
}

if (!conflicts.isEmpty()) {

    StringBuilder details = new StringBuilder();

    for (Event ev : conflicts) {
        details.append(
            ev.getTitle() + " (" +
            ev.getStartTime() + " - " +
            ev.getEndTime() + ")\n"
        );
    }

    Alert warning = new Alert(
        Alert.AlertType.CONFIRMATION
    );

    warning.setTitle("Scheduling Conflict");
    warning.setHeaderText(
        "This event overlaps with another event!"
    );
    warning.setContentText(
        details + "\nDo you want to save anyway?"
    );

    ButtonType saveAnyway =
        new ButtonType("Save Anyway");

    ButtonType cancel =
        new ButtonType(
            "Cancel",
            ButtonBar.ButtonData.CANCEL_CLOSE
        );

    warning.getButtonTypes().setAll(
        saveAnyway, cancel
    );

    Optional<ButtonType> response =
        warning.showAndWait();

    if (response.isEmpty()
        || response.get() != saveAnyway) {
        return;
    }
}

// Save using Facade and Command pattern.
if (editingOriginal == null) {
    facade.addEvent(updated);
} else {
    facade.editEvent(editingOriginal, updated);
}

            selected = editorDate.getValue();
            shown = YearMonth.from(selected);

            closeEditor();
            clearEventSelection();

            refresh();

        } catch (Exception ex) {

            alert(
                "Could not save event: " +
                ex.getMessage()
            );
        }
    }

    // ==========================================
    // UI HELPERS
    // ==========================================

    private void styleButton(Button button) {

        button.setStyle(
            "-fx-background-color: " +
            EVENT_BLUE + ";" +
            "-fx-text-fill: " +
            PRIMARY + ";" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 7 11;"
        );
    }

    private void alert(String message) {

        new Alert(
            Alert.AlertType.ERROR,
            message,
            ButtonType.OK
        ).showAndWait();
    }
}
