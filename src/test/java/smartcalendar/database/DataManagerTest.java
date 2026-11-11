// Testing the Singleton pattern
package smartcalendar.database;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

class DataManagerTest {
    @Test void getInstanceAlwaysReturnsSameObject() {
        assertSame(DataManager.getInstance(), DataManager.getInstance());
    }

    @Test void singletonProvidesSameOpenDatabaseConnection() throws Exception {
        Connection first = DataManager.getInstance().getDatabase();
        Connection second = DataManager.getInstance().getDatabase();
        assertSame(first, second);
        assertFalse(first.isClosed());
        try (Statement statement = first.createStatement();
             ResultSet rs = statement.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table' " +
                 "AND name IN ('events', 'category_rules')")) {
            int tables = 0;
            while (rs.next()) tables++;
            assertEquals(2, tables);
        }
    }
}
