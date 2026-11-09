
package smartcalendar.services;

import smartcalendar.database.DataManager;
import smartcalendar.database.EventRepo;
import smartcalendar.gui.CategoryColors;
import smartcalendar.models.Event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CategoryService {

    private final EventRepo repo;
    private final Map<String, String> rules = new HashMap<>();
    private final Connection db;

    public CategoryService(EventRepo repo) {
        this.repo = Objects.requireNonNull(repo);
        this.db = DataManager.getInstance().getDatabase();

        // NEW: Restore previously saved rules.
        loadRules();
    }

    // NEW: Load saved keyword-color rules from SQLite.
    private void loadRules() {

        String sql = "SELECT keyword, color FROM category_rules";

        try (PreparedStatement ps = db.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rules.put(
                    rs.getString("keyword"),
                    rs.getString("color")
                );
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                "Could not load category rules.", e
            );
        }
    }

    // Add or replace a keyword-color rule.
    public void addCategory(String keyword, String color) {

        if (keyword == null || keyword.isBlank()
                || color == null || color.isBlank()) {
            throw new IllegalArgumentException(
                "Keyword and color cannot be empty."
            );
        }

        String normalizedKeyword =
            keyword.trim().toLowerCase(Locale.ROOT);

        // Accept existing palette names, case-insensitively.
        String normalizedColor = null;

        for (String supported : List.of(
                "Red", "Orange", "Yellow", "Green",
                "Blue", "Purple", "Pink", "Brown", "Black")) {

            if (supported.equalsIgnoreCase(color.trim())) {
                normalizedColor = supported;
                break;
            }
        }

        if (normalizedColor == null) {
            throw new IllegalArgumentException(
                "Unsupported category color: " + color
            );
        }

        // NEW: Save rule permanently.
        String sql =
            "INSERT INTO category_rules (keyword, color) " +
            "VALUES (?, ?) " +
            "ON CONFLICT(keyword) DO UPDATE SET color = excluded.color";

        try (PreparedStatement ps = db.prepareStatement(sql)) {

            ps.setString(1, normalizedKeyword);
            ps.setString(2, normalizedColor);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                "Could not save category rule.", e
            );
        }

        // Preserve existing in-memory rule behavior.
        rules.put(normalizedKeyword, normalizedColor);
    }

    // Remove a keyword-color rule.
    public void removeCategory(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return;
        }

        String normalizedKeyword =
            keyword.trim().toLowerCase(Locale.ROOT);

        // NEW: Delete saved rule.
        String sql =
            "DELETE FROM category_rules WHERE keyword = ?";

        try (PreparedStatement ps = db.prepareStatement(sql)) {

            ps.setString(1, normalizedKeyword);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                "Could not remove category rule.", e
            );
        }

        rules.remove(normalizedKeyword);
    }

    // Determine the color of an event.
    // Does not change the database.
    public String categorize(Event event) {

        if (event == null || event.getTitle() == null) {
            return null;
        }

        String title =
            event.getTitle().toLowerCase(Locale.ROOT);

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

            // Preserve your original update approach.
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
