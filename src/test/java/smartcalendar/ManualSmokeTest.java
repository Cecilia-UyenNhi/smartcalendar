package smartcalendar;
import smartcalendar.models.Event;
import smartcalendar.services.CalendarService;
import smartcalendar.observers.*;
public class ManualSmokeTest {
    public static void main(String[] args){
        Event e=new Event("EECS 3311 Lecture","2026-10-26","10:00","12:00","LSB 106","Blue");
        System.out.println(e);
        CalendarService service=new CalendarService();
        CalendarView calendarView=new CalendarView();WeeklySchedView weeklyView=new WeeklySchedView();
        service.addObserver(calendarView);service.addObserver(weeklyView);service.updateViews();
        service.removeObserver(weeklyView);service.updateViews();
    }
}
