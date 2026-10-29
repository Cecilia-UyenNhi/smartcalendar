
package smartcalendar.commands;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;
import smartcalendar.services.CalendarService;

public class DeleteEvent implements CalendarCommand {

    private final Event event;
    private final EventRepo repo;
    private final CalendarService service;

    public DeleteEvent(Event event, EventRepo repo,
                       CalendarService service) {
        this.event = event;
        this.repo = repo;
        this.service = service;
    }

    @Override
    public void execute() {
        if (event == null) {
            throw new IllegalArgumentException(
                "Event cannot be null."
            );
        }

        repo.delete(event);
        service.updateViews();
    }
}
