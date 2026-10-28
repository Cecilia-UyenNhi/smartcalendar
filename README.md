# SmartCalendar — Java migration

This project translates the uploaded JavaScript source into Java 21. It includes the original Event model, SQLite DataManager singleton, EventRepo operations, CalendarService, CalendarObserver interface, CalendarView and WeeklySchedView. The original FullCalendar HTML/JS page is replaced by a **JavaFX month calendar and weekly event list** with add/edit/delete dialogs.

## Requirements
- JDK 21
- Apache Maven (or VS Code Maven extension with a working Maven installation)
- VS Code Extension Pack for Java (optional)

## Run
Open this folder in VS Code, open a terminal in the folder containing `pom.xml`, then run:

```powershell
mvn clean javafx:run
```

Maven downloads JavaFX and SQLite JDBC dependencies on the first run. SQLite creates `calendarDB` in the working directory. Your original database file is **not included** in this migration. The app starts with an empty database unless you copy an existing compatible `calendarDB` into the project directory.

## Architecture
- `models/Event.java` — Event model, validation, getters/setters
- `database/DataManager.java` — SQLite singleton
- `database/EventRepo.java` — save, findAll, findByTitle, findByCat, findByDate, findByDRange, findByTRange, updateDtb, delete
- `services/CalendarService.java` — register/remove observers, get events, notify observers
- `observers/CalendarObserver.java` — Java interface
- `observers/CalendarView.java` and `WeeklySchedView.java` — observer implementations, with optional JavaFX renderer callbacks
- `gui/SmartCalendarUI.java` — JavaFX frontend
- `Main.java` — JavaFX application entry point

## Notes
- The source code identified events using `(title,date)` rather than unique IDs. To prevent unintended bulk edits/deletes, this Java version refuses update/delete when multiple rows match. Longer term, add a database primary key.
- The uploaded JS `findByDate` used `this.#db.getDatabase()` even though `#db` was already a connection; the Java translation corrects this.
- The uploaded UI did not connect FullCalendar to the database. The JavaFX UI now displays actual stored events and refreshes through observers.
- No AI assistant, PDF importer, free-time service, conflict service, or other unimplemented planned feature is included: these were not in the uploaded source.
- This environment did not have Maven installed, so the complete Maven/JavaFX build could not be executed here. The dependency-free model and observer classes can be checked with `javac`.
