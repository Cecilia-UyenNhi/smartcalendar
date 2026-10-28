package smartcalendar.database;
import java.sql.*;
public final class DataManager {
    private static final DataManager INSTANCE = new DataManager();
    private final Connection db;
    private DataManager(){
        try {
            db=DriverManager.getConnection("jdbc:sqlite:calendarDB");
            try(Statement st=db.createStatement()) {st.executeUpdate("CREATE TABLE IF NOT EXISTS events (title TEXT NOT NULL, date TEXT NOT NULL, startTime TEXT NOT NULL, endTime TEXT NOT NULL, location TEXT, catColor TEXT)");}
        }catch(SQLException e){throw new IllegalStateException("Cannot initialize SQLite",e);}
    }
    public static DataManager getInstance(){return INSTANCE;}
    public Connection getDatabase(){return db;}
}
