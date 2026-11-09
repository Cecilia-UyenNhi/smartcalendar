package smartcalendar.cli;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;
import smartcalendar.services.AIAgent;
import smartcalendar.services.CalendarFacade;
import smartcalendar.services.CalendarService;
import smartcalendar.services.ImportService;

import java.io.File;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Terminal interface to the same SmartCalendar services used by JavaFX. */
public final class SmartCalendarCLI {
    private final Scanner input = new Scanner(System.in);
    private final CalendarFacade facade;
    private final AIAgent agent;

    public SmartCalendarCLI() {
        EventRepo repo = new EventRepo();
        CalendarService calendarService = new CalendarService();
        facade = new CalendarFacade(repo, calendarService);
        facade.setImportService(new ImportService(repo, calendarService));
        agent = new AIAgent(facade);
    }

    public static void main(String[] args) {
        new SmartCalendarCLI().run();
    }

    public void run() {
        System.out.println("Welcome to SmartCalendar CLI. Type 'help' for commands.");
        while (true) {
            System.out.print("smartcalendar> ");
            if (!input.hasNextLine()) return;
            String line = input.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+", 2);
            String command = parts[0].toLowerCase(Locale.ROOT);
            try {
                switch (command) {
                    case "exit", "quit" -> { System.out.println("Goodbye!"); return; }
                    case "help" -> help();
                    case "list" -> printEvents(facade.getEvents());
                    case "today" -> printEvents(facade.queryCalendar("date", LocalDate.now().toString()));
                    case "tomorrow" -> printEvents(facade.queryCalendar("date", LocalDate.now().plusDays(1).toString()));
                    case "week" -> showRange(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), 7);
                    case "month" -> {
                        YearMonth month = YearMonth.now();
                        showRange(month.atDay(1), month.lengthOfMonth());
                    }
                    case "deadlines" -> showDeadlines();
                    case "search" -> search();
                    case "free" -> findFree();
                    case "conflicts" -> conflicts();
                    case "import" -> importFile(parts.length > 1 ? parts[1] : null);
                    case "chat" -> chat();
                    case "add", "edit", "delete", "categorize" -> {
                        System.out.println("Describe your request to Sissy (type 'back' to cancel):");
                        String request = read("You> ");
                        if (!request.equalsIgnoreCase("back")) {
                            System.out.println("Sissy> " + agent.handleRequest(request));
                        }
                    }
                    default -> {
                        // Natural language is accepted directly at the CLI prompt.
                        System.out.println("Sissy> " + agent.handleRequest(line));
                    }
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void help() {
        System.out.println("""
            Commands:
              help          Show commands
              list          List all events
              today         Today's events
              tomorrow      Tomorrow's events
              week          Events this calendar week
              month         Events this month
              deadlines     Deadlines in the next seven days
              search        Search by title, date, range, type, or location
              free          Find available time slots
              conflicts     Check a proposed event for scheduling conflicts
              import        Import a course outline or schedule file
              chat          Conversation mode with Sissy
              add/edit/delete/categorize  Ask Sissy to perform an action
              exit          Quit
            You may also type a natural-language request directly.
            Dates: YYYY-MM-DD; times: HH:mm (24-hour clock).
            """);
    }

    private void showRange(LocalDate first, int days) {
        LocalDate last = first.plusDays(days - 1);
        System.out.println("Events from " + first + " through " + last + ":");
        printEvents(facade.queryCalendar("date_range", first + "," + last));
    }

    private void showDeadlines() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(6);
        List<Event> events = facade.queryCalendar("deadlines", "").stream()
            .filter(event -> {
                LocalDate date = LocalDate.parse(event.getDate());
                return !date.isBefore(start) && !date.isAfter(end);
            }).toList();
        System.out.println("Deadlines " + start + " through " + end + ":");
        printEvents(events);
    }

    private void search() {
        System.out.println("Search by: title, date, date_range, type, location, category, upcoming, next");
        String kind = read("Search type> ").toLowerCase(Locale.ROOT);
        if (kind.equals("back")) return;
        String value = (kind.equals("upcoming") || kind.equals("next")) ? "" : read("Value> ");
        if (value.equalsIgnoreCase("back")) return;
        printEvents(facade.queryCalendar(kind.equals("title") ? "name" : kind, value));
    }

    private void findFree() {
        System.out.println("Enter a date and time window; duration is measured in hours (e.g. 1.5).");
        LocalDateTime start = LocalDateTime.parse(read("Start (YYYY-MM-DDTHH:mm)> "));
        LocalDateTime end = LocalDateTime.parse(read("End (YYYY-MM-DDTHH:mm)> "));
        double duration = Double.parseDouble(read("Minimum duration (hours)> "));
        List<String> slots = facade.findFreeTime(start, end, duration);
        if (slots.isEmpty()) System.out.println("No free intervals meet that duration.");
        else slots.forEach(slot -> System.out.println("  " + slot));
    }

    private void conflicts() {
        String title = read("Proposed event title> ");
        String date = read("Date (YYYY-MM-DD)> ");
        String start = read("Start time (HH:mm)> ");
        String end = read("End time (HH:mm)> ");
        Event proposed = new Event(title, date, start, end);
        List<Event> conflicts = facade.getConflictingEvents(proposed);
        if (conflicts.isEmpty()) System.out.println("No scheduling conflicts found.");
        else { System.out.println("Conflicts found:"); printEvents(conflicts); }
    }

    private void importFile(String path) {
        if (path == null || path.isBlank()) path = read("File path> ");
        path = path.trim();
        if (path.startsWith("\"") && path.endsWith("\"") && path.length() >= 2)
            path = path.substring(1, path.length() - 1);
        File file = new File(path);
        if (!file.isFile()) { System.out.println("File not found: " + path); return; }
        List<Event> imported = facade.importSchedule(file);
        System.out.println("Import returned " + imported.size() + " event(s).");
        printEvents(imported);
    }

    private void chat() {
        System.out.println("Sissy chat: type 'back' to return to the CLI.");
        while (true) {
            String request = read("You> ");
            if (request.equalsIgnoreCase("back")) return;
            if (request.isBlank()) continue;
            System.out.println("Sissy> " + agent.handleRequest(request));
        }
    }

    private String read(String prompt) {
        System.out.print(prompt);
        if (!input.hasNextLine()) throw new IllegalStateException("Input closed");
        return input.nextLine().trim();
    }

    private void printEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            System.out.println("No matching events.");
            return;
        }
        List<Event> ordered = new ArrayList<>(events);
        ordered.sort(Comparator.comparing(Event::getDate)
            .thenComparing(Event::getStartTime)
            .thenComparing(Event::getTitle));
        int n = 1;
        for (Event event : ordered) {
            System.out.printf("%d. %s | %s | %s - %s%n", n++,
                event.getTitle(), event.getDate(), event.getStartTime(), event.getEndTime());
            if (event.getLocation() != null && !event.getLocation().isBlank())
                System.out.println("   Location: " + event.getLocation());
        }
    }
}
