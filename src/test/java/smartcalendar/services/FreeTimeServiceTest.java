
// Testing Strategy pattern 
package smartcalendar.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;
import smartcalendar.strategies.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FreeTimeServiceTest {

    private EventRepo repo;
    private FreeTimeService service;

    @BeforeEach
    void setUp() {
        repo = mock(EventRepo.class);
        service = new FreeTimeService(repo);
    }

    @Test
    void hourlyStrategyFindsGapsBetweenEvents() {
        when(repo.findAll()).thenReturn(List.of(
            new Event("Lecture", "2026-11-16",
                      "10:00", "11:00"),
            new Event("Lunch", "2026-11-16",
                      "13:00", "14:00")
        ));

        service.setStrategy(new HourlyFreeTime());

        List<String> result = service.findFreeTime(
            LocalDateTime.parse("2026-11-16T09:00"),
            LocalDateTime.parse("2026-11-16T15:00"),
            1.0
        );

        assertEquals(List.of(
            "2026-11-16 09:00 - 10:00",
            "2026-11-16 11:00 - 13:00",
            "2026-11-16 14:00 - 15:00"
        ), result);
    }

    @Test
    void weeklyStrategyFindsFreeTimeAcrossDays() {
        when(repo.findAll()).thenReturn(List.of());

        service.setStrategy(new WeeklyFreeTime());

        List<String> result = service.findFreeTime(
            LocalDateTime.parse("2026-11-16T09:00"),
            LocalDateTime.parse("2026-11-17T12:00"),
            2.0
        );

        assertEquals(List.of(
            "2026-11-16 09:00 - 00:00",
            "2026-11-17 00:00 - 12:00"
        ), result);
    }

    @Test
    void strategyCanBeReplacedAtRuntime() {
        when(repo.findAll()).thenReturn(List.of());

        FreeTimeStrategy custom =
            mock(FreeTimeStrategy.class);

        var start =
            LocalDateTime.parse("2026-11-16T09:00");
        var end =
            LocalDateTime.parse("2026-11-16T12:00");

        when(custom.findFreeTime(
            anyList(), eq(start), eq(end), eq(1.0)
        )).thenReturn(List.of("custom result"));

        service.setStrategy(custom);

        assertEquals(
            List.of("custom result"),
            service.findFreeTime(start, end, 1.0)
        );

        verify(custom).findFreeTime(
            anyList(), eq(start), eq(end), eq(1.0)
        );
    }

    @Test
    void strategyCannotBeNull() {
        assertThrows(
            NullPointerException.class,
            () -> service.setStrategy(null)
        );
    }
}
