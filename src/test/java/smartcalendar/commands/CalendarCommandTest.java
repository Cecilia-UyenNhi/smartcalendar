// Testing the Command pattern
package smartcalendar.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;
import smartcalendar.services.CalendarService;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CalendarCommandTest {

    private EventRepo repo;
    private CalendarService service;
    private Event original;

    @BeforeEach
    void setUp() {
        repo = mock(EventRepo.class);
        service = mock(CalendarService.class);

        original = new Event(
            "Jollibee", "2026-11-14", "09:00", "13:00"
        );
    }

    @Test
    void addCommandSavesEventAndNotifiesViews() {
        CalendarCommand command =
            new AddEvent(original, repo, service);

        command.execute();

        var order = inOrder(repo, service);
        order.verify(repo).save(original);
        order.verify(service).updateViews();
    }

    @Test
    void editCommandUpdatesEventAndNotifiesViews() {
        Event updated = new Event(
            "Jollibee", "2026-11-14", "10:00", "14:00"
        );

        CalendarCommand command =
            new EditEvent(original, updated, repo, service);

        command.execute();

        var order = inOrder(repo, service);
        order.verify(repo).updateDtb(original, updated);
        order.verify(service).updateViews();
    }

    @Test
    void deleteCommandDeletesEventAndNotifiesViews() {
        CalendarCommand command =
            new DeleteEvent(original, repo, service);

        command.execute();

        var order = inOrder(repo, service);
        order.verify(repo).delete(original);
        order.verify(service).updateViews();
    }

    @Test
    void invalidAddDoesNotCallRepositoryOrNotify() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AddEvent(null, repo, service).execute()
        );

        verifyNoInteractions(repo, service);
    }

    @Test
    void failedDeleteDoesNotNotifyViews() {
        doThrow(new IllegalArgumentException("Not found"))
            .when(repo).delete(original);

        assertThrows(
            IllegalArgumentException.class,
            () -> new DeleteEvent(original, repo, service).execute()
        );

        verify(service, never()).updateViews();
    }
}
