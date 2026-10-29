
package smartcalendar.commands;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;
import smartcalendar.services.CalendarService;

public class EditEvent implements CalendarCommand {

    private final Event oldEvent;
    private final Event updatedEvent;
    private final EventRepo repo;
    private final CalendarService service;

    public EditEvent(Event oldEvent, Event updatedEvent,
                     EventRepo repo, CalendarService service) {
        this.oldEvent = oldEvent;
        this.updatedEvent = updatedEvent;
        this.repo = repo;
        this.service = service;
    }

    @Override
    public void execute() {
        if (oldEvent == null || updatedEvent == null) {
            throw new IllegalArgumentException(
                "Original and updated events are required."
            );
        }

        repo.updateDtb(oldEvent, updatedEvent);
        service.updateViews();
    }
}
