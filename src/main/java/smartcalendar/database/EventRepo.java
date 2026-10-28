package smartcalendar.database;
import smartcalendar.models.Event;
import java.sql.*;
import java.util.*;
public class EventRepo {
    private final Connection db;
    public EventRepo(){db=DataManager.getInstance().getDatabase();}
    private static void bindEvent(PreparedStatement ps,Event e) throws SQLException {
        ps.setString(1,e.getTitle());ps.setString(2,e.getDate());ps.setString(3,e.getStartTime());ps.setString(4,e.getEndTime());ps.setString(5,e.getLocation());ps.setString(6,e.getCatColor());
    }
    public void save(Event e){
        Objects.requireNonNull(e,"Event required");
        try(PreparedStatement ps=db.prepareStatement("INSERT INTO events (title,date,startTime,endTime,location,catColor) VALUES (?,?,?,?,?,?)")){bindEvent(ps,e);ps.executeUpdate();}
        catch(SQLException ex){throw new IllegalStateException("Save failed",ex);}
    }
    private List<Event> search(String sql,String... args){
        List<Event> result=new ArrayList<>();
        try(PreparedStatement ps=db.prepareStatement(sql)){
            for(int i=0;i<args.length;i++)ps.setString(i+1,args[i]);
            try(ResultSet rs=ps.executeQuery()){
                while(rs.next())result.add(new Event(rs.getString("title"),rs.getString("date"),rs.getString("startTime"),rs.getString("endTime"),rs.getString("location"),rs.getString("catColor")));
            }
        }catch(SQLException ex){throw new IllegalStateException("Query failed",ex);}return result;
    }
    public List<Event> findAll(){return search("SELECT * FROM events ORDER BY date,startTime");}
    public List<Event> findByTitle(String title){List<Event> r=search("SELECT * FROM events WHERE title=?",title);if(r.isEmpty())throw new NoSuchElementException("No event found with title "+title);return r;}
    public List<Event> findByCat(String color){List<Event> r=search("SELECT * FROM events WHERE catColor=?",color);if(r.isEmpty())throw new NoSuchElementException("No event found with category color "+color);return r;}
    public List<Event> findByDate(String date){return search("SELECT * FROM events WHERE date=? ORDER BY startTime",date);}
    public List<Event> findByDRange(String from,String to){return search("SELECT * FROM events WHERE date BETWEEN ? AND ? ORDER BY date,startTime",from,to);}
    public List<Event> findByTRange(String from,String to){return search("SELECT * FROM events WHERE startTime >= ? AND endTime <= ? ORDER BY date,startTime",from,to);}
    private int matchCount(Event e) throws SQLException {
        try(PreparedStatement ps=db.prepareStatement("SELECT COUNT(*) FROM events WHERE title=? AND date=?")){
            ps.setString(1,e.getTitle());ps.setString(2,e.getDate());try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getInt(1):0;}
        }
    }
    // The original JS code identifies rows by title + date, not by a unique ID.
    // Reject ambiguous matches so one edit/delete cannot accidentally change multiple rows.
    public void delete(Event event){Objects.requireNonNull(event,"Event required");
        try {db.setAutoCommit(false);int count=matchCount(event);
            if(count!=1)throw new IllegalArgumentException(count==0?"Event not found. Delete failed.":"Multiple matching events; delete cancelled.");
            try(PreparedStatement ps=db.prepareStatement("DELETE FROM events WHERE title=? AND date=?")){ps.setString(1,event.getTitle());ps.setString(2,event.getDate());ps.executeUpdate();}
            db.commit();
        }catch(Exception ex){rollback();throw new IllegalStateException("Delete failed: "+ex.getMessage(),ex);}finally{restoreAutoCommit();}
    }
    public void updateDtb(Event oldEvent,Event newEvent){Objects.requireNonNull(oldEvent,"Old event required");Objects.requireNonNull(newEvent,"New event required");
        try{db.setAutoCommit(false);int count=matchCount(oldEvent);
            if(count!=1)throw new IllegalArgumentException(count==0?"Event not found. Update failed.":"Multiple matching events; update cancelled.");
            try(PreparedStatement ps=db.prepareStatement("UPDATE events SET title=?,date=?,startTime=?,endTime=?,location=?,catColor=? WHERE title=? AND date=?")){
                bindEvent(ps,newEvent);ps.setString(7,oldEvent.getTitle());ps.setString(8,oldEvent.getDate());ps.executeUpdate();
            }db.commit();
        }catch(Exception ex){rollback();throw new IllegalStateException("Update failed: "+ex.getMessage(),ex);}finally{restoreAutoCommit();}
    }
    private void rollback(){try{db.rollback();}catch(SQLException ignored){}}
    private void restoreAutoCommit(){try{db.setAutoCommit(true);}catch(SQLException ignored){}}
}
