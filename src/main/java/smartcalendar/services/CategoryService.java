
package smartcalendar.services;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryService {

    private final EventRepo repo;
    private final Map<String, String> rules = new HashMap<>();

    public CategoryService(EventRepo repo) {
        this.repo = repo;
    }

    // Add or replace a keyword-color rule.
    public void addCategory(String keyword, String color) {

        if (keyword == null || keyword.isBlank()
                || color == null || color.isBlank()) {
            throw new IllegalArgumentException(
                "Keyword and color cannot be empty."
            );
        }

        rules.put(
            keyword.trim().toLowerCase(),
            color.trim()
        );
    }

    // Remove a keyword-color rule.
    public void removeCategory(String keyword) {

        if (keyword != null) {
            rules.remove(keyword.trim().toLowerCase());
        }
    }

    // Determine the color of an event.
    // Does not change the database.
    public String categorize(Event event) {

        if (event == null || event.getTitle() == null) {
            return null;
        }

        String title = event.getTitle().toLowerCase();

        // Longest matching keyword takes priority.
        String bestKeyword = null;

        for (String keyword : rules.keySet()) {

            if (title.contains(keyword)) {

                if (bestKeyword == null
                        || keyword.length() > bestKeyword.length()) {
                    bestKeyword = keyword;
                }
            }
        }

        if (bestKeyword != null) {
            return rules.get(bestKeyword);
        }

        return event.getCatColor();
    }

    // Apply all current rules to saved events.
    public List<Event> categorizeEvents() {

        List<Event> events = repo.findAll();

        for (Event oldEvent : events) {

            String color = categorize(oldEvent);

            if (color == null
                    || color.equals(oldEvent.getCatColor())) {
                continue;
            }

            Event updated = new Event(
                oldEvent.getTitle(),
                oldEvent.getDate(),
                oldEvent.getStartTime(),
                oldEvent.getEndTime(),
                oldEvent.getLocation(),
                color
            );

            repo.updateDtb(oldEvent, updated);
        }

        return repo.findAll();
    }
}
