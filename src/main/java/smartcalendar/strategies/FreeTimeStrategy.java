
package smartcalendar.strategies;

import smartcalendar.models.Event;
import java.time.LocalDateTime;
import java.util.List;

public interface FreeTimeStrategy {

    List<String> findFreeTime(
        List<Event> events,
        LocalDateTime start,
        LocalDateTime end,
        double duration
    );
}
