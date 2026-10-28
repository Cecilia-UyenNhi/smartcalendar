package smartcalendar.observers;
import smartcalendar.models.Event;
import java.util.List;
import java.util.function.Consumer;
public class WeeklySchedView implements CalendarObserver {
    private final Consumer<List<Event>> renderer;
    public WeeklySchedView(){this(events->{System.out.println("\n--- Weekly Schedule View ---");for(Event e:events){System.out.println(e);System.out.println("--------------------");}});}
    public WeeklySchedView(Consumer<List<Event>> renderer){this.renderer=renderer;}
    @Override public void display(List<Event> events){renderer.accept(events);}
}
