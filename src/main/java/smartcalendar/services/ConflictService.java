
package smartcalendar.services;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ConflictService {

    private final EventRepo repo;

    public ConflictService(EventRepo repo) {
        this.repo = repo;
    }

    // Return all events that overlap the proposed event.
    public List<Event> checkConflict(Event proposed) {
        return checkConflict(proposed, null);
    }

    // When editing, exclude the original event.
    public List<Event> checkConflict(
            Event proposed,
            Event original) {

        if (proposed == null) {
            throw new IllegalArgumentException(
                "Proposed event cannot be null."
            );
        }

        List<Event> conflicts = new ArrayList<>();

        LocalTime newStart =
            LocalTime.parse(proposed.getStartTime());

        LocalTime newEnd =
            LocalTime.parse(proposed.getEndTime());

        if (!newStart.isBefore(newEnd)) {
            throw new IllegalArgumentException(
                "Start time must be before end time."
            );
        }

        for (Event existing : repo.findAll()) {

            // Only compare events on the same date.
            if (!existing.getDate().equals(
                    proposed.getDate())) {
                continue;
            }

            // Do not compare an edited event with itself.
            if (original != null
                    && sameEvent(existing, original)) {
                continue;
            }

            LocalTime existingStart =
                LocalTime.parse(existing.getStartTime());

            LocalTime existingEnd =
                LocalTime.parse(existing.getEndTime());

            // Two time intervals overlap when:
            // newStart < existingEnd AND
            // newEnd > existingStart
            boolean overlap =
                newStart.isBefore(existingEnd)
                && newEnd.isAfter(existingStart);

            if (overlap) {
                conflicts.add(existing);
            }
        }

        return conflicts;
    }

    public boolean hasConflict(Event event) {
        return !checkConflict(event).isEmpty();
    }

    // Compare the stored identifying fields.
    private boolean sameEvent(Event a, Event b) {

        return a.getTitle().equals(b.getTitle())
            && a.getDate().equals(b.getDate())
            && a.getStartTime().equals(b.getStartTime())
            && a.getEndTime().equals(b.getEndTime());
    }
}
