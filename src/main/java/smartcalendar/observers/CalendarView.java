package smartcalendar.observers;
import smartcalendar.models.Event;
import java.util.List;
import java.util.function.Consumer;
public class CalendarView implements CalendarObserver {
    private final Consumer<List<Event>> renderer;
    public CalendarView(){this(events->{System.out.println("\n--- Calendar View ---");for(Event e:events)System.out.println(e);});}
    public CalendarView(Consumer<List<Event>> renderer){this.renderer=renderer;}
    @Override public void display(List<Event> events){renderer.accept(events);}
}
