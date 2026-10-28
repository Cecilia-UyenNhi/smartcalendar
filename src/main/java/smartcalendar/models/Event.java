package smartcalendar.models;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
public class Event {
    private String title, location, catColor;
    private LocalDate date;
    private LocalTime startTime, endTime;
    public Event(String title, String date, String startTime, String endTime) { this(title,date,startTime,endTime,null,null); }
    public Event(String title, String date, String startTime, String endTime, String location, String color) {
        setTitle(title); setDate(date);
        this.startTime = LocalTime.parse(Objects.requireNonNull(startTime));
        this.endTime = LocalTime.parse(Objects.requireNonNull(endTime));
        validateTimes(); this.location=location; this.catColor=color;
    }
    private void validateTimes() { if (!startTime.isBefore(endTime)) throw new IllegalArgumentException("Start time must be before end time."); }
    public String getTitle(){return title;} public String getDate(){return date.toString();}
    public String getStartTime(){return startTime.toString();} public String getEndTime(){return endTime.toString();}
    public String getLocation(){return location;} public String getCatColor(){return catColor;}
    public void setTitle(String title){if(title==null||title.isBlank())throw new IllegalArgumentException("Title is required.");this.title=title;}
    public void setDate(String date){this.date=LocalDate.parse(Objects.requireNonNull(date));}
    public void setStartTime(String time){LocalTime t=LocalTime.parse(time);if(endTime!=null&&!t.isBefore(endTime))throw new IllegalArgumentException("Start time must be earlier than end time.");startTime=t;}
    public void setEndTime(String time){LocalTime t=LocalTime.parse(time);if(startTime!=null&&!t.isAfter(startTime))throw new IllegalArgumentException("End time must be later than start time.");endTime=t;}
    public void setLocation(String location){this.location=location;} public void setCatColor(String color){this.catColor=color;}
    @Override public String toString(){return "{Title: "+title+", Date: "+date+", Start Time: "+startTime+", End Time: "+endTime+", Location: "+(location==null?"NA":location)+", Color: "+(catColor==null?"NA":catColor)+"}";}
}
