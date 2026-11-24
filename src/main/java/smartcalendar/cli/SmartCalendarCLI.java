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
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

/** Terminal interface to the same SmartCalendar services used by JavaFX. */
public final class SmartCalendarCLI {
    private Scanner input = new Scanner(System.in);
    private boolean batchMode = false;
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

        // Mode 1: Interactive CLI
        if (args.length == 0) {
            new SmartCalendarCLI().run();
            return;
        }

        // Mode 2: Execute a task from a file
        if (args.length == 2 && args[0].equalsIgnoreCase("-input")) {
            new SmartCalendarCLI().runTaskFile(args[1]);
            return;
        }

        // Invalid command-line arguments
        System.out.println("Invalid arguments.");
        System.out.println("Supported options:");
        System.out.println("  No arguments: Start interactive CLI");
        System.out.println("  -input <file>: Execute a task from a text file");
    }


    public void run() {
        System.out.println("Welcome to SmartCalendar CLI. Type 'help' for commands.");

        while (true) {
            System.out.print("smartcalendar> ");

            if (!input.hasNextLine()) return;

            String line = input.nextLine().trim();

            if (batchMode) {
                System.out.println(line);
        }

            if (line.isEmpty()) continue;

            String command = line.toLowerCase(Locale.ROOT);

            try {
                // Handle file import with a supplied path
                // Handle file import with a supplied path
                if (command.startsWith("import ")) {
                    String path = line.substring(7).trim();

                    int firstQuote = path.indexOf('"');
                    int lastQuote = path.lastIndexOf('"');

                    if (firstQuote != -1 && lastQuote > firstQuote) {
                        path = path.substring(firstQuote + 1, lastQuote);
                    }

                    importFile(path);
                    continue;
                }

                // Match exact commands only
                switch (command) {
                    case "exit", "quit" -> {
                        System.out.println("Goodbye!");
                        return;
                    }

                    case "help" -> help();

                    case "list" -> printEvents(facade.getEvents());

                    case "today" -> printEvents(
                        facade.queryCalendar("date",
                            LocalDate.now().toString())
                    );

                    case "tomorrow" -> printEvents(
                        facade.queryCalendar("date",
                            LocalDate.now().plusDays(1).toString())
                    );

                    case "week" -> showRange(
                        LocalDate.now().with(
                            TemporalAdjusters.previousOrSame(
                                DayOfWeek.MONDAY)), 7
                    );

                    case "month" -> {
                        YearMonth month = YearMonth.now();
                        showRange(month.atDay(1), month.lengthOfMonth());
                    }

                    case "deadlines" -> showDeadlines();

                    case "search" -> search();

                    case "free" -> findFree();

                    case "conflicts" -> conflicts();

                    case "import" -> importFile(null);

                    case "chat" -> chat();

                    case "add", "edit", "delete", "categorize" -> {
                        System.out.println(
                            "Describe your request to Sissy (type 'back' to cancel):"
                        );

                        String request = read("You> ");

                        if (!request.equalsIgnoreCase("back")) {
                            System.out.println(
                                "Sissy> " + agent.handleRequest(request)
                            );
                        }
                    }

                    // Forward complete natural language requests
                    default -> {
                        if (batchMode) {
                            Thread.sleep(6000);
                        }

                        System.out.println(
                            "Sissy> " + agent.handleRequest(line)
                        );
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
        if (path == null || path.isBlank()) {
            path = read("File path> ");
        }

        path = path.trim();

        if (path.startsWith("\"")
                && path.endsWith("\"")
                && path.length() >= 2) {
            path = path.substring(1, path.length() - 1);
        }

        File file = new File(path);

        if (!file.isFile()) {
            System.out.println("File not found: " + path);
            return;
        }

        // Step 1: Extract events without saving
        List<Event> imported = new ArrayList<>(
            facade.importSchedule(file)
        );

        if (imported.isEmpty()) {
            System.out.println("No new events to import.");
            return;
        }

        // Step 2: Sort events to match displayed numbering
        imported.sort(
            Comparator.comparing(Event::getDate)
                .thenComparing(Event::getStartTime)
                .thenComparing(Event::getTitle)
        );

        System.out.println(
            "Extracted " + imported.size() + " event(s):"
        );

        printEvents(imported);

        // Step 3: Allow users to select events
        System.out.println("""
            Import options:
            all             Import all events
            none            Cancel import
            exclude 10      Exclude event 10
            exclude 2,5,10  Exclude multiple events
            only 1,3,5      Import selected events only
            """);

        while (true) {
            String answer = read("Your selection> ")
                .toLowerCase(Locale.ROOT);

            List<Event> selected = new ArrayList<>();

            if (answer.equals("none") || answer.equals("no")
                    || answer.equals("n")) {
                System.out.println("Import cancelled.");
                return;
            }

            if (answer.equals("all") || answer.equals("yes")
                    || answer.equals("y")) {
                selected.addAll(imported);

            } else if (answer.startsWith("exclude ")
                    || answer.startsWith("only ")) {

                boolean exclude = answer.startsWith("exclude ");

                String numbers = answer.substring(
                    exclude ? 8 : 5
                ).trim();

                Set<Integer> indexes = new HashSet<>();

                try {
                    for (String number : numbers.split(",", -1)) {
                        int index = Integer.parseInt(number.trim());

                        if (index < 1 || index > imported.size()) {
                            throw new IllegalArgumentException();
                        }

                        indexes.add(index);
                    }

                    for (int i = 0; i < imported.size(); i++) {
                        boolean contains = indexes.contains(i + 1);

                        if (exclude ? !contains : contains) {
                            selected.add(imported.get(i));
                        }
                    }

                } catch (IllegalArgumentException e) {
                    System.out.println(
                        "Invalid event number. Please try again."
                    );
                    continue;
                }

            } else {
                System.out.println("Invalid selection. Try again.");
                continue;
            }

            if (selected.isEmpty()) {
                System.out.println("No events selected.");
                continue;
            }

            // Step 4: Preview selected events
            System.out.println(
                selected.size() + " of " + imported.size()
                + " events selected:"
            );

            printEvents(selected);

            // Step 5: Final confirmation
            while (true) {
                String confirm = read("Confirm import? (y/n)> ");

                if (confirm.equalsIgnoreCase("y")
                        || confirm.equalsIgnoreCase("yes")) {

                    List<Event> saved =
                        facade.saveImportedEvents(selected);

                    System.out.println(
                        "Successfully saved " + saved.size()
                        + " event(s)."
                    );
                    return;
                }

                if (confirm.equalsIgnoreCase("n")
                        || confirm.equalsIgnoreCase("no")) {
                    System.out.println(
                        "No events saved. Please select again."
                    );
                    break;
                }

                System.out.println("Please enter y or n.");
            }
        }
    }


   
    private void runTaskFile(String filePath) {
        try {
            Path path = Path.of(filePath);

            if (!Files.isRegularFile(path)) {
                System.out.println("Task file not found: " + filePath);
                return;
            }

            List<String> tasks = Files.readAllLines(path)
                .stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("#"))
                .toList();

            if (tasks.isEmpty()) {
                System.out.println("Task file is empty.");
                return;
            }

            System.out.println("SmartCalendar - Task File Mode");
            System.out.println("Reading tasks from: " + filePath);
            System.out.println("Total input lines: " + tasks.size());

            batchMode = true;

            // Use the task file instead of standard input
            input = new Scanner(String.join("\n", tasks));

            // Reuse existing CLI commands and follow-up questions
            run();

            System.out.println("\nAll input lines processed.");

        } catch (IOException e) {
            System.out.println("Unable to read task file: " + e.getMessage());

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());

        } finally {
            batchMode = false;
        }
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

        if (!input.hasNextLine()) {
            throw new IllegalStateException("Input closed");
        }

        String answer = input.nextLine().trim();

        if (batchMode) {
            System.out.println(answer);
        }

        return answer;
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
