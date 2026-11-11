
//Testing the Facade pattern
package smartcalendar.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CalendarFacadeTest {

    private EventRepo repo;
    private CalendarService calendarService;
    private CalendarFacade facade;

    @BeforeEach
    void setUp() {
        repo = mock(EventRepo.class);
        calendarService = mock(CalendarService.class);

        facade = new CalendarFacade(repo, calendarService);

        when(repo.findAll()).thenReturn(List.of());
    }

    @Test
    void facadeAddsEventThroughCommand() {
        Event event = new Event(
            "KFC", "2026-11-15", "12:00", "16:00"
        );

        when(calendarService.getEvents())
            .thenReturn(List.of(event));

        assertEquals(
            List.of(event),
            facade.addEvent(event)
        );

        verify(repo).save(event);
        verify(calendarService).updateViews();
    }

    @Test
    void facadeEditsEventThroughCommand() {
        Event oldEvent = new Event(
            "Garage", "2026-11-15", "09:00", "12:00"
        );

        Event updated = new Event(
            "Garage", "2026-11-15", "10:00", "13:00"
        );

        when(calendarService.getEvents())
            .thenReturn(List.of(updated));

        assertEquals(
            List.of(updated),
            facade.editEvent(oldEvent, updated)
        );

        verify(repo).updateDtb(oldEvent, updated);
        verify(calendarService).updateViews();
    }

    @Test
    void facadeDeletesEventThroughCommand() {
        Event event = new Event(
            "Jollibee", "2026-11-15", "09:00", "12:00"
        );

        facade.deleteEvent(event);

        verify(repo).delete(event);
        verify(calendarService).updateViews();
    }

    @Test
    void facadeQueriesEventsByCategory() {
        Event event = new Event(
            "Jollibee", "2026-11-15",
            "09:00", "12:00", null, "Blue"
        );

        when(repo.findByCat("Blue"))
            .thenReturn(List.of(event));

        assertEquals(
            List.of(event),
            facade.queryCalendar("category", "Blue")
        );
    }

    @Test
    void facadeSelectsHourlyOrWeeklyFreeTimeAutomatically() {
        when(repo.findAll()).thenReturn(List.of());

        assertEquals(
            List.of("2026-11-16 09:00 - 12:00"),
            facade.findFreeTime(
                LocalDateTime.parse("2026-11-16T09:00"),
                LocalDateTime.parse("2026-11-16T12:00"),
                1
            )
        );

        assertEquals(
            List.of(
                "2026-11-16 09:00 - 00:00",
                "2026-11-17 00:00 - 12:00"
            ),
            facade.findFreeTime(
                LocalDateTime.parse("2026-11-16T09:00"),
                LocalDateTime.parse("2026-11-17T12:00"),
                1
            )
        );
    }

    @Test
    void facadeRejectsUnknownQueryType() {
        assertThrows(
            IllegalArgumentException.class,
            () -> facade.queryCalendar(
                "unknown", "anything"
            )
        );
    }
}
