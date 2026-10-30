
package smartcalendar.services;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;
import smartcalendar.commands.*;

import smartcalendar.strategies.HourlyFreeTime;
import smartcalendar.strategies.WeeklyFreeTime;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class CalendarFacade {

    private final EventRepo repo;

    private final CalendarService calendarService;
    private final CategoryService categoryService;
    private final ConflictService conflictService;
    private final FreeTimeService freeTimeService;
    private final QueryService queryService;

    // Connected later, once Gemini is ready.
    private ImportService importService;

    // ========================================
    // CONSTRUCTOR
    // ========================================

    public CalendarFacade(
        EventRepo repo,
        CalendarService calendarService
    ) {
        this.repo = Objects.requireNonNull(repo);
        this.calendarService =
            Objects.requireNonNull(calendarService);

        this.categoryService = new CategoryService(repo);
        this.conflictService = new ConflictService(repo);
        this.freeTimeService = new FreeTimeService(repo);
        this.queryService = new QueryService(repo);
    }

    // ========================================
    // ADD EVENT - COMMAND PATTERN
    // ========================================

    public List<Event> addEvent(Event event) {

        Objects.requireNonNull(event);

        // Check conflicts before adding.
        // This is a warning-only policy.
        List<Event> conflicts =
            conflictService.checkConflict(event);

        if (!conflicts.isEmpty()) {
            System.out.println(
                "Warning: Event overlaps " +
                conflicts.size() + " existing event(s)."
            );
        }

        CalendarCommand command =
            new AddEvent(event, repo, calendarService);

        command.execute();

        return calendarService.getEvents();
    }

    // ========================================
    // EDIT EVENT - COMMAND PATTERN
    // ========================================

    public List<Event> editEvent(
        Event oldEvent,
        Event updatedEvent
    ) {
        Objects.requireNonNull(oldEvent);
        Objects.requireNonNull(updatedEvent);

        List<Event> conflicts =
            conflictService.checkConflict(
                updatedEvent, oldEvent
            );

        if (!conflicts.isEmpty()) {
            System.out.println(
                "Warning: Updated event overlaps " +
                conflicts.size() + " existing event(s)."
            );
        }

        CalendarCommand command =
            new EditEvent(
                oldEvent,
                updatedEvent,
                repo,
                calendarService
            );

        command.execute();

        return calendarService.getEvents();
    }

    // ========================================
    // DELETE EVENT - COMMAND PATTERN
    // ========================================

    public void deleteEvent(Event event) {

        Objects.requireNonNull(event);

        CalendarCommand command =
            new DeleteEvent(
                event, repo, calendarService
            );

        command.execute();
    }

    // ========================================
    // QUERY CALENDAR
    // ========================================

    public List<Event> queryCalendar(
        String queryType,
        String info
    ) {
        Objects.requireNonNull(queryType);
        Objects.requireNonNull(info);

        switch (queryType.trim().toLowerCase()) {

            case "date":
                return queryService.findByDate(
                    LocalDate.parse(info)
                );

            case "name":
            case "title":
                return queryService.findByName(info);

            case "date_range": {
                String[] dates = info.split(",");

                if (dates.length != 2) {
                    throw new IllegalArgumentException(
                        "Use startDate,endDate."
                    );
                }

                return repo.findByDRange(
                    dates[0].trim(),
                    dates[1].trim()
                );
            }

            case "category":
                return repo.findByCat(info);

            default:
                throw new IllegalArgumentException(
                    "Unknown query type: " + queryType
                );
        }
    }

    // ========================================
    // FIND FREE TIME - STRATEGY PATTERN
    // ========================================

    public List<String> findFreeTime(
        LocalDateTime start,
        LocalDateTime end,
        double duration
    ) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);

        // Select strategy based on the date range.
        if (start.toLocalDate().equals(
                end.toLocalDate())) {

            freeTimeService.setStrategy(
                new HourlyFreeTime()
            );

        } else {

            freeTimeService.setStrategy(
                new WeeklyFreeTime()
            );
        }

        return freeTimeService.findFreeTime(
            start, end, duration
        );
    }

    // ========================================
    // CHECK SCHEDULING CONFLICT
    // ========================================

    public boolean checkConflict(Event event) {
        return conflictService.hasConflict(event);
    }

    public List<Event> getConflictingEvents(
        Event event
    ) {
        return conflictService.checkConflict(event);
    }

    // ========================================
    // CATEGORIZE EVENTS
    // ========================================

    public List<Event> categorizeEvent(
        String keyword,
        String color
    ) {
        categoryService.addCategory(
            keyword, color
        );

        List<Event> events =
            categoryService.categorizeEvents();

        calendarService.updateViews();

        return events;
    }

    public void removeCategory(String keyword) {

        categoryService.removeCategory(keyword);
    }

    // ========================================
    // IMPORT COURSE OUTLINE / SCHEDULE
    // ========================================

    public void setImportService(
        ImportService importService
    ) {
        this.importService =
            Objects.requireNonNull(importService);
    }

    public List<Event> importSchedule(File file) {

        if (importService == null) {
            throw new IllegalStateException(
                "ImportService is not connected yet."
            );
        }

        return importService.importFile(file);
    }

    // ========================================
    // READ ALL EVENTS
    // ========================================

    public List<Event> getEvents() {
        return calendarService.getEvents();
    }
}
