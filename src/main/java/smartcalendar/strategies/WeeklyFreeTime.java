
package smartcalendar.strategies;

import smartcalendar.models.Event;

import java.time.*;
import java.util.*;

public class WeeklyFreeTime implements FreeTimeStrategy {

    private final HourlyFreeTime hourly =
        new HourlyFreeTime();

    @Override
    public List<String> findFreeTime(
        List<Event> events,
        LocalDateTime start,
        LocalDateTime end,
        double duration
    ) {
        if (start == null || end == null
                || !start.isBefore(end)
                || !Double.isFinite(duration)
                || duration <= 0) {
            throw new IllegalArgumentException(
                "Invalid date range or duration."
            );
        }

        List<String> results = new ArrayList<>();

        LocalDate day = start.toLocalDate();

        while (!day.isAfter(end.toLocalDate())) {

            LocalDateTime dayStart =
                day.atStartOfDay();

            LocalDateTime nextDay =
                day.plusDays(1).atStartOfDay();

            // Restrict each day to the search range.
            LocalDateTime windowStart =
                start.isAfter(dayStart) ? start : dayStart;

            LocalDateTime windowEnd =
                end.isBefore(nextDay) ? end : nextDay;

            if (windowStart.isBefore(windowEnd)) {

                // The hourly strategy requires one date.
                // End at 23:59 for full-day windows.
                LocalDateTime hourlyEnd = windowEnd;

                if (windowEnd.equals(nextDay)) {
                    hourlyEnd = day.atTime(23, 59);
                }

                if (windowStart.isBefore(hourlyEnd)) {
                    results.addAll(
                        hourly.findFreeTime(
                            events,
                            windowStart,
                            hourlyEnd,
                            duration
                        )
                    );
                }
            }

            day = day.plusDays(1);
        }

        return results;
    }
}
