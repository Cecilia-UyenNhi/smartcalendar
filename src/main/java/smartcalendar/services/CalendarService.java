package smartcalendar.services;
import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;
import smartcalendar.observers.CalendarObserver;
import java.util.*;
public class CalendarService {
    private final EventRepo repo;
    private final List<CalendarObserver> observers=new ArrayList<>();
    public CalendarService(){this.repo=new EventRepo();}
    public List<Event> getEvents(){return repo.findAll();}
    public void addObserver(CalendarObserver observer){Objects.requireNonNull(observer);if(!observers.contains(observer))observers.add(observer);}
    public void removeObserver(CalendarObserver observer){observers.remove(observer);}
    public void updateViews(){List<Event> events=getEvents();for(CalendarObserver observer:List.copyOf(observers))observer.display(events);}
}
