
package smartcalendar.services;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;
import smartcalendar.commands.*;

import smartcalendar.strategies.*;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public class CalendarFacade {

    private final EventRepo repo;

    private final CalendarService calendarService;
    private final CategoryService categoryService;
    private final ConflictService conflictService;
    private final FreeTimeService freeTimeService;
    private final QueryService queryService;

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

        event.setCatColor(categoryService.categorize(event));

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

        updatedEvent.setCatColor(
            categoryService.categorize(updatedEvent)
        );

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

                return queryService.findByDateRange(
                    LocalDate.parse(dates[0].trim()),
                    LocalDate.parse(dates[1].trim())
                );
            }

            case "category":
                return repo.findByCat(info);

            case "deadline":
            case "deadlines":
                return queryService.findDeadlines();

            case "type":
                return queryService.findByType(info);

            case "location":
                return queryService.findByLocation(info);

            case "upcoming":
                return queryService.findUpcomingEvents();

            case "next":
                Event next = queryService.findNextEvent();
                return next == null ? List.of() : List.of(next);

            case "after_time":
                return queryService.findAfterTime(
                    LocalTime.parse(info)
                );

            case "before_time":
                return queryService.findBeforeTime(
                    LocalTime.parse(info)
                );

            case "tomorrow_morning":
                return queryService.findTomorrowMorning();

            default:
                throw new IllegalArgumentException(
                    "Unknown query type: " + queryType
                );
        }
    }

    public List<Event> queryEvents(
        String keyword,
        LocalDate startDate,
        LocalDate endDate,
        String type,
        String location,
        LocalTime afterTime,
        LocalTime beforeTime,
        LocalDateTime upcomingFrom
    ) {
        return queryService.filterEvents(
            keyword,
            startDate,
            endDate,
            type,
            location,
            afterTime,
            beforeTime,
            upcomingFrom,
            false
        );
    }

    public int countEvents(
        String keyword,
        LocalDate startDate,
        LocalDate endDate,
        String type
    ) {
        return queryService.countEvents(
            keyword,
            startDate,
            endDate,
            type
        );
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

    public boolean isDuplicate(Event event) {
        Objects.requireNonNull(event);
        return conflictService.isDuplicate(event);
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

    public void applyCategoryRule(Event event) {
        Objects.requireNonNull(event);

        event.setCatColor(
            categoryService.categorize(event)
        );
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
