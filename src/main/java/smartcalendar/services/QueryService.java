
package smartcalendar.services;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class QueryService {

    private final EventRepo repo;

    public QueryService(EventRepo repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    // ========================================
    // BASIC QUERIES
    // ========================================

    public List<Event> findByDate(LocalDate date) {

        Objects.requireNonNull(date, "Date cannot be null.");

        return filterEvents(
            null, date, date, null, null,
            null, null, null, false
        );
    }

    public List<Event> findByName(String name) {

        if (name == null || name.isBlank()) {
            return new ArrayList<>();
        }

        return filterEvents(
            name, null, null, null, null,
            null, null, null, false
        );
    }

    public List<Event> findByDateRange(
        LocalDate startDate,
        LocalDate endDate
    ) {

        Objects.requireNonNull(startDate);
        Objects.requireNonNull(endDate);

        return filterEvents(
            null, startDate, endDate, null, null,
            null, null, null, false
        );
    }

    public List<Event> findDeadlines() {

        return filterEvents(
            null, null, null, "deadline", null,
            null, null, null, false
        );
    }

    public List<Event> findDeadlinesBetween(
        LocalDate startDate,
        LocalDate endDate
    ) {

        return filterEvents(
            null, startDate, endDate, "deadline", null,
            null, null, null, false
        );
    }

    // ========================================
    // ADVANCED QUERIES
    // ========================================

    public List<Event> findByLocation(String location) {

        if (location == null || location.isBlank()) {
            return new ArrayList<>();
        }

        return filterEvents(
            null, null, null, null, location,
            null, null, null, false
        );
    }

    public List<Event> findByType(String type) {

        if (type == null || type.isBlank()) {
            return new ArrayList<>();
        }

        return filterEvents(
            null, null, null, type, null,
            null, null, null, false
        );
    }

    public List<Event> findUpcomingEvents() {

        return filterEvents(
            null, null, null, null, null,
            null, null, LocalDateTime.now(), false
        );
    }

    public Event findNextEvent() {

        List<Event> upcoming = findUpcomingEvents();

        return upcoming.isEmpty() ? null : upcoming.get(0);
    }

    public List<Event> findAfterTime(LocalTime time) {

        Objects.requireNonNull(time);

        return filterEvents(
            null, null, null, null, null,
            time, null, null, false
        );
    }

    public List<Event> findBeforeTime(LocalTime time) {

        Objects.requireNonNull(time);

        return filterEvents(
            null, null, null, null, null,
            null, time, null, false
        );
    }

    public List<Event> findTomorrowMorning() {

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        return filterEvents(
            null,
            tomorrow,
            tomorrow,
            null,
            null,
            null,
            LocalTime.NOON,
            null,
            false
        );
    }

    // ========================================
    // COMBINED FILTERS
    // ========================================

    public List<Event> filterEvents(
        String keyword,
        LocalDate startDate,
        LocalDate endDate,
        String type,
        String location,
        LocalTime afterTime,
        LocalTime beforeTime,
        LocalDateTime upcomingFrom,
        boolean unused
    ) {

        if (startDate != null && endDate != null
                && startDate.isAfter(endDate)) {

            throw new IllegalArgumentException(
                "Start date cannot be after end date."
            );
        }

        List<Event> results = new ArrayList<>();

        for (Event event : repo.findAll()) {

            String title = event.getTitle();

            LocalDate eventDate;
            LocalTime eventStart;
            LocalTime eventEnd;

            try {
                eventDate = LocalDate.parse(event.getDate());
                eventStart = LocalTime.parse(event.getStartTime());
                eventEnd = LocalTime.parse(event.getEndTime());

            } catch (DateTimeParseException
                    | NullPointerException e) {
                continue;
            }

            if (!containsIgnoreCase(title, keyword)) {
                continue;
            }

            if (startDate != null
                    && eventDate.isBefore(startDate)) {
                continue;
            }

            if (endDate != null
                    && eventDate.isAfter(endDate)) {
                continue;
            }

            if (!matchesType(title, type)) {
                continue;
            }

            if (!containsIgnoreCase(
                    event.getLocation(), location)) {
                continue;
            }

            if (afterTime != null
                    && eventStart.isBefore(afterTime)) {
                continue;
            }

            if (beforeTime != null
                    && !eventStart.isBefore(beforeTime)) {
                continue;
            }

            if (upcomingFrom != null) {

                LocalDateTime eventEndDateTime =
                    LocalDateTime.of(eventDate, eventEnd);

                if (!eventEndDateTime.isAfter(upcomingFrom)) {
                    continue;
                }
            }

            results.add(event);
        }

        sortEvents(results);
        return results;
    }

    // ========================================
    // COUNT QUERIES
    // ========================================

    public int countEvents(
        String keyword,
        LocalDate startDate,
        LocalDate endDate,
        String type
    ) {

        return filterEvents(
            keyword,
            startDate,
            endDate,
            type,
            null,
            null,
            null,
            null,
            false
        ).size();
    }

    // ========================================
    // SEARCH
    // ========================================

    public List<Event> search(String input) {

        if (input == null || input.isBlank()) {
            return new ArrayList<>();
        }

        String query = input.trim();

        try {
            LocalDate date = LocalDate.parse(query);
            return findByDate(date);

        } catch (DateTimeParseException ex) {
            return findByName(query);
        }
    }

    // ========================================
    // HELPERS
    // ========================================

    private boolean containsIgnoreCase(
        String value,
        String keyword
    ) {

        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        return value != null
            && value.toLowerCase(Locale.ROOT)
                .contains(
                    keyword.trim().toLowerCase(Locale.ROOT)
                );
    }

    private boolean matchesType(
        String title,
        String type
    ) {

        if (type == null || type.isBlank()) {
            return true;
        }

        if (title == null) {
            return false;
        }

        String normalizedTitle =
            title.toLowerCase(Locale.ROOT);

        String normalizedType =
            type.trim().toLowerCase(Locale.ROOT);

        switch (normalizedType) {

            case "deadline":
            case "deadlines":
                return containsAny(
                    normalizedTitle,
                    "assignment", "homework",
                    "deadline", "due", "submission",
                    "report", "project"
                );

            case "assignment":
            case "assignments":
                return containsAny(
                    normalizedTitle,
                    "assignment", "homework"
                );

            case "exam":
            case "exams":
                return containsAny(
                    normalizedTitle,
                    "exam", "midterm", "test",
                    "quiz", "final"
                );

            case "meeting":
            case "meetings":
                return containsAny(
                    normalizedTitle,
                    "meeting", "appointment"
                );

            case "class":
            case "classes":
                return containsAny(
                    normalizedTitle,
                    "class", "lecture",
                    "tutorial", "lab"
                );

            default:
                return normalizedTitle.contains(normalizedType);
        }
    }

    private boolean containsAny(
        String text,
        String... keywords
    ) {

        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private void sortEvents(List<Event> events) {

        events.sort(
            Comparator.comparing(
                Event::getDate,
                Comparator.nullsLast(
                    Comparator.naturalOrder()
                )
            ).thenComparing(
                Event::getStartTime,
                Comparator.nullsLast(
                    Comparator.naturalOrder()
                )
            )
        );
    }
}
