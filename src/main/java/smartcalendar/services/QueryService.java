
package smartcalendar.services;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class QueryService {

    private final EventRepo repo;

    public QueryService(EventRepo repo) {
        this.repo = repo;
    }

    // Query Type 1: Search by exact date.
    public List<Event> findByDate(LocalDate date) {

        if (date == null) {
            throw new IllegalArgumentException(
                "Date cannot be null."
            );
        }

        List<Event> results = new ArrayList<>();

        for (Event event : repo.findAll()) {

            if (event.getDate().equals(date.toString())) {
                results.add(event);
            }
        }

        sortEvents(results);
        return results;
    }

    // Query Type 2: Search by event title.
    // Partial matches are supported.
    public List<Event> findByName(String name) {

        if (name == null || name.isBlank()) {
            return new ArrayList<>();
        }

        List<Event> results = new ArrayList<>();

        String keyword = name.trim().toLowerCase();

        for (Event event : repo.findAll()) {

            if (event.getTitle() != null
                    && event.getTitle().toLowerCase()
                        .contains(keyword)) {

                results.add(event);
            }
        }

        sortEvents(results);
        return results;
    }

    // Search using either a date or an event name.
    public List<Event> search(String input) {

        if (input == null || input.isBlank()) {
            return new ArrayList<>();
        }

        String query = input.trim();

        // If the input is a date, search by date.
        try {
            LocalDate date = LocalDate.parse(query);
            return findByDate(date);

        } catch (java.time.format.DateTimeParseException ex) {
            // Otherwise, treat it as an event name.
            return findByName(query);
        }
    }

    // Sort results by date and starting time.
    private void sortEvents(List<Event> events) {

        events.sort(
            Comparator.comparing(Event::getDate)
                .thenComparing(Event::getStartTime)
        );
    }
}
