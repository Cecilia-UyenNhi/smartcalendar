
package smartcalendar.database;

import smartcalendar.models.Event;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class EventRepo {

    private final Connection db;

    public EventRepo() {
        db = DataManager.getInstance().getDatabase();
    }

    // ========================================
    // HELPER: BIND EVENT VALUES
    // ========================================

    private static void bindEvent(
        PreparedStatement ps,
        Event event
    ) throws SQLException {

        ps.setString(1, event.getTitle());
        ps.setString(2, event.getDate());
        ps.setString(3, event.getStartTime());
        ps.setString(4, event.getEndTime());
        ps.setString(5, event.getLocation());
        ps.setString(6, event.getCatColor());
    }

    // ========================================
    // HELPER: CONVERT RESULT TO EVENT
    // ========================================

    private Event createEvent(ResultSet rs)
            throws SQLException {

        return new Event(
            rs.getString("title"),
            rs.getString("date"),
            rs.getString("startTime"),
            rs.getString("endTime"),
            rs.getString("location"),
            rs.getString("catColor")
        );
    }

    // ========================================
    // SAVE EVENT
    // ========================================

    public void save(Event event) {

        Objects.requireNonNull(
            event, "Event required"
        );

        String sql =
            "INSERT INTO events " +
            "(title, date, startTime, endTime, " +
            "location, catColor) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps =
                 db.prepareStatement(sql)) {

            bindEvent(ps, event);
            ps.executeUpdate();

        } catch (SQLException ex) {
            throw new IllegalStateException(
                "Save failed", ex
            );
        }
    }

    // ========================================
    // GENERAL SEARCH HELPER
    // ========================================

    private List<Event> search(
        String sql,
        String... args
    ) {

        List<Event> results = new ArrayList<>();

        try (PreparedStatement ps =
                 db.prepareStatement(sql)) {

            for (int i = 0; i < args.length; i++) {
                ps.setString(i + 1, args[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    results.add(createEvent(rs));
                }
            }

        } catch (SQLException ex) {
            throw new IllegalStateException(
                "Query failed", ex
            );
        }

        return results;
    }

    // ========================================
    // FIND ALL EVENTS
    // ========================================

    public List<Event> findAll() {

        return search(
            "SELECT * FROM events " +
            "ORDER BY date, startTime"
        );
    }

    // ========================================
    // FIND BY TITLE
    // ========================================

    public List<Event> findByTitle(String title) {

        if (title == null || title.isBlank()) {
            return new ArrayList<>();
        }

        return search(
            "SELECT * FROM events " +
            "WHERE LOWER(title) LIKE ? " +
            "ORDER BY date, startTime",
            "%" + title.trim().toLowerCase() + "%"
        );
    }

    // ========================================
    // FIND BY CATEGORY COLOR
    // ========================================

    public List<Event> findByCat(String color) {

        if (color == null || color.isBlank()) {
            return new ArrayList<>();
        }

        return search(
            "SELECT * FROM events " +
            "WHERE catColor = ? " +
            "ORDER BY date, startTime",
            color
        );
    }

    // ========================================
    // FIND BY DATE
    // ========================================

    public List<Event> findByDate(String date) {

        Objects.requireNonNull(date);

        LocalDate.parse(date);

        return search(
            "SELECT * FROM events " +
            "WHERE date = ? " +
            "ORDER BY startTime",
            date
        );
    }

    // ========================================
    // FIND BY DATE RANGE
    // ========================================

    public List<Event> findByDRange(
        String from,
        String to
    ) {

        LocalDate startDate = LocalDate.parse(from);
        LocalDate endDate = LocalDate.parse(to);

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                "Start date must not be after end date."
            );
        }

        return search(
            "SELECT * FROM events " +
            "WHERE date BETWEEN ? AND ? " +
            "ORDER BY date, startTime",
            from,
            to
        );
    }

    // ========================================
    // FIND BY TIME RANGE
    // ========================================

    // Find events whose time overlaps
    // the requested interval.
    public List<Event> findByTRange(
        String from,
        String to
    ) {

        LocalTime start = LocalTime.parse(from);
        LocalTime end = LocalTime.parse(to);

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException(
                "Start time must be before end time."
            );
        }

        return search(
            "SELECT * FROM events " +
            "WHERE startTime < ? " +
            "AND endTime > ? " +
            "ORDER BY date, startTime",
            to,
            from
        );
    }

    // ========================================
    // HELPER: IDENTIFY EXACT EVENT
    // ========================================

    /*
     * Until Event has a unique database ID,
     * identify a saved event using all six fields.
     *
     * IS ? is SQLite's null-safe comparison.
     */

    private static final String EVENT_MATCH =
        "title IS ? AND date IS ? " +
        "AND startTime IS ? AND endTime IS ? " +
        "AND location IS ? AND catColor IS ?";

    private int matchCount(Event event)
            throws SQLException {

        String sql =
            "SELECT COUNT(*) FROM events WHERE " +
            EVENT_MATCH;

        try (PreparedStatement ps =
                 db.prepareStatement(sql)) {

            bindEvent(ps, event);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ========================================
    // DELETE EVENT
    // ========================================

    public void delete(Event event) {

        Objects.requireNonNull(
            event, "Event required"
        );

        boolean originalAutoCommit;

        try {
            originalAutoCommit = db.getAutoCommit();
        } catch (SQLException ex) {
            throw new IllegalStateException(
                "Could not start delete", ex
            );
        }

        try {
            db.setAutoCommit(false);

            int count = matchCount(event);

            if (count != 1) {
                throw new IllegalArgumentException(
                    count == 0
                        ? "Event not found."
                        : "Multiple identical events found. " +
                          "Delete cancelled."
                );
            }

            String sql =
                "DELETE FROM events WHERE " +
                EVENT_MATCH;

            try (PreparedStatement ps =
                     db.prepareStatement(sql)) {

                bindEvent(ps, event);

                int deleted = ps.executeUpdate();

                if (deleted != 1) {
                    throw new SQLException(
                        "Expected to delete one event."
                    );
                }
            }

            db.commit();

        } catch (Exception ex) {

            rollback();

            throw new IllegalStateException(
                "Delete failed: " + ex.getMessage(),
                ex
            );

        } finally {
            restoreAutoCommit(originalAutoCommit);
        }
    }

    // ========================================
    // UPDATE EVENT
    // ========================================

    public void updateDtb(
        Event oldEvent,
        Event newEvent
    ) {

        Objects.requireNonNull(
            oldEvent, "Old event required"
        );

        Objects.requireNonNull(
            newEvent, "New event required"
        );

        boolean originalAutoCommit;

        try {
            originalAutoCommit = db.getAutoCommit();
        } catch (SQLException ex) {
            throw new IllegalStateException(
                "Could not start update", ex
            );
        }

        try {
            db.setAutoCommit(false);

            int count = matchCount(oldEvent);

            if (count != 1) {
                throw new IllegalArgumentException(
                    count == 0
                        ? "Event not found."
                        : "Multiple identical events found. " +
                          "Update cancelled."
                );
            }

            String sql =
                "UPDATE events SET " +
                "title = ?, date = ?, " +
                "startTime = ?, endTime = ?, " +
                "location = ?, catColor = ? " +
                "WHERE " + EVENT_MATCH;

            try (PreparedStatement ps =
                     db.prepareStatement(sql)) {

                // New event values (1-6).
                bindEvent(ps, newEvent);

                // Old event values (7-12).
                ps.setString(7, oldEvent.getTitle());
                ps.setString(8, oldEvent.getDate());
                ps.setString(9, oldEvent.getStartTime());
                ps.setString(10, oldEvent.getEndTime());
                ps.setString(11, oldEvent.getLocation());
                ps.setString(12, oldEvent.getCatColor());

                int updated = ps.executeUpdate();

                if (updated != 1) {
                    throw new SQLException(
                        "Expected to update one event."
                    );
                }
            }

            db.commit();

        } catch (Exception ex) {

            rollback();

            throw new IllegalStateException(
                "Update failed: " + ex.getMessage(),
                ex
            );

        } finally {
            restoreAutoCommit(originalAutoCommit);
        }
    }

    // ========================================
    // TRANSACTION HELPERS
    // ========================================

    private void rollback() {

        try {
            db.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void restoreAutoCommit(boolean original) {

        try {
            db.setAutoCommit(original);
        } catch (SQLException ex) {
            throw new IllegalStateException(
                "Could not restore database transaction mode",
                ex
            );
        }
    }
}
