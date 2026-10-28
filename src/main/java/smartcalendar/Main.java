package smartcalendar;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import smartcalendar.gui.SmartCalendarUI;
public class Main extends Application {
    @Override public void start(Stage stage){SmartCalendarUI ui=new SmartCalendarUI();stage.setTitle("SmartCalendar (Java)");stage.setScene(new Scene(ui.getRoot(),1050,720));stage.show();ui.refresh();}
    public static void main(String[] args){launch(args);}
}
