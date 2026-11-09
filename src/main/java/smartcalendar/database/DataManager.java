
package smartcalendar.database;

import java.sql.*;

public final class DataManager {

    private static final DataManager INSTANCE = new DataManager();
    private final Connection db;

    private DataManager() {
        try {
            db = DriverManager.getConnection("jdbc:sqlite:calendarDB");

            try (Statement st = db.createStatement()) {

                // Existing events table — unchanged
                st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS events (" +
                    "title TEXT NOT NULL, " +
                    "date TEXT NOT NULL, " +
                    "startTime TEXT NOT NULL, " +
                    "endTime TEXT NOT NULL, " +
                    "location TEXT, " +
                    "catColor TEXT)"
                );

                // NEW: Persist keyword-to-color rules
                st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS category_rules (" +
                    "keyword TEXT PRIMARY KEY COLLATE NOCASE, " +
                    "color TEXT NOT NULL)"
                );
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                "Cannot initialize SQLite", e
            );
        }
    }

    public static DataManager getInstance() {
        return INSTANCE;
    }

    public Connection getDatabase() {
        return db;
    }
}
