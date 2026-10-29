
package smartcalendar.strategies;

import smartcalendar.models.Event;

import java.time.*;
import java.util.*;

public class HourlyFreeTime implements FreeTimeStrategy {

    @Override
    public List<String> findFreeTime(
        List<Event> events,
        LocalDateTime start,
        LocalDateTime end,
        double duration
    ) {
        if (start == null || end == null
                || !start.isBefore(end)
                || !start.toLocalDate().equals(end.toLocalDate())
                || !Double.isFinite(duration)
                || duration <= 0) {
            throw new IllegalArgumentException(
                "Provide one day, a valid time range, " +
                "and a positive duration in hours."
            );
        }

        long requiredMinutes =
            Math.round(duration * 60);

        if (requiredMinutes < 1) {
            throw new IllegalArgumentException(
                "Duration must be at least one minute."
            );
        }

        List<LocalDateTime[]> busy = new ArrayList<>();

        for (Event event : events) {
            LocalDateTime eventStart = LocalDateTime.of(
                LocalDate.parse(event.getDate()),
                LocalTime.parse(event.getStartTime())
            );

            LocalDateTime eventEnd = LocalDateTime.of(
                LocalDate.parse(event.getDate()),
                LocalTime.parse(event.getEndTime())
            );

            // Ignore events outside the requested range.
            if (!eventStart.isBefore(end)
                    || !eventEnd.isAfter(start)) {
                continue;
            }

            // Clip the event to the requested range.
            LocalDateTime clippedStart =
                eventStart.isBefore(start) ? start : eventStart;

            LocalDateTime clippedEnd =
                eventEnd.isAfter(end) ? end : eventEnd;

            busy.add(new LocalDateTime[]{
                clippedStart, clippedEnd
            });
        }

        busy.sort(
            Comparator.comparing(interval -> interval[0])
        );

        List<String> freeSlots = new ArrayList<>();
        LocalDateTime cursor = start;

        for (LocalDateTime[] interval : busy) {
            LocalDateTime busyStart = interval[0];
            LocalDateTime busyEnd = interval[1];

            // Free gap before this event.
            if (cursor.isBefore(busyStart)) {
                addSlot(
                    freeSlots, cursor,
                    busyStart, requiredMinutes
                );
            }

            // Move past the busy interval.
            if (busyEnd.isAfter(cursor)) {
                cursor = busyEnd;
            }
        }

        // Free time after the final event.
        if (cursor.isBefore(end)) {
            addSlot(
                freeSlots, cursor,
                end, requiredMinutes
            );
        }

        return freeSlots;
    }

    private void addSlot(
        List<String> results,
        LocalDateTime start,
        LocalDateTime end,
        long requiredMinutes
    ) {
        long availableMinutes =
            Duration.between(start, end).toMinutes();

        if (availableMinutes >= requiredMinutes) {
            results.add(
                start.toLocalDate() + " " +
                start.toLocalTime() + " - " +
                end.toLocalTime()
            );
        }
    }
}
