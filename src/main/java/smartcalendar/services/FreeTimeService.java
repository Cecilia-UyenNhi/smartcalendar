
package smartcalendar.services;

import smartcalendar.database.EventRepo;
import smartcalendar.strategies.FreeTimeStrategy;
import smartcalendar.strategies.HourlyFreeTime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class FreeTimeService {

    private final EventRepo repo;
    private FreeTimeStrategy strategy;

    public FreeTimeService(EventRepo repo) {
        this.repo = Objects.requireNonNull(repo);
        this.strategy = new HourlyFreeTime();
    }

    public void setStrategy(FreeTimeStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy);
    }

    public List<String> findFreeTime(
        LocalDateTime start,
        LocalDateTime end,
        double duration
    ) {
        return strategy.findFreeTime(
            repo.findAll(),
            start,
            end,
            duration
        );
    }
}
