
//Testing Observer pattern
package smartcalendar.services;

import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import smartcalendar.observers.CalendarObserver;

import static org.mockito.Mockito.*;

class CalendarServiceTest {

    @Test
    void registeredObserverReceivesCurrentEvents() {
        CalendarService service = new CalendarService();
        CalendarObserver observer =
            mock(CalendarObserver.class);

        service.addObserver(observer);

        try (MockedStatic<Platform> platform =
                 mockStatic(Platform.class)) {

            platform.when(Platform::isFxApplicationThread)
                    .thenReturn(true);

            service.updateViews();
        }

        verify(observer).display(anyList());
    }

    @Test
    void removedObserverDoesNotReceiveNotifications() {
        CalendarService service = new CalendarService();
        CalendarObserver observer =
            mock(CalendarObserver.class);

        service.addObserver(observer);
        service.removeObserver(observer);

        service.updateViews();

        verifyNoInteractions(observer);
    }

    @Test
    void registeringSameObserverTwiceDoesNotDuplicateNotification() {
        CalendarService service = new CalendarService();
        CalendarObserver observer =
            mock(CalendarObserver.class);

        service.addObserver(observer);
        service.addObserver(observer);

        try (MockedStatic<Platform> platform =
                 mockStatic(Platform.class)) {

            platform.when(Platform::isFxApplicationThread)
                    .thenReturn(true);

            service.updateViews();
        }

        verify(observer, times(1)).display(anyList());
    }
}
