package smartcalendar.gui;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;
import smartcalendar.observers.*;
import smartcalendar.services.CalendarService;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
public class SmartCalendarUI {
    private final BorderPane root=new BorderPane();
    private final CalendarService service=new CalendarService();
    private final EventRepo repo=new EventRepo();
    private final GridPane monthGrid=new GridPane();
    private final ListView<Event> weeklyList=new ListView<>();
    private final Label monthLabel=new Label();
    private YearMonth shown=YearMonth.now();
    private LocalDate selected=LocalDate.now();
    public SmartCalendarUI(){
        Button prev=new Button("◀"),next=new Button("▶"),today=new Button("Today"),add=new Button("Add Event"),edit=new Button("Edit Selected"),delete=new Button("Delete Selected");
        prev.setOnAction(e->{shown=shown.minusMonths(1);refresh();});next.setOnAction(e->{shown=shown.plusMonths(1);refresh();});today.setOnAction(e->{shown=YearMonth.now();selected=LocalDate.now();refresh();});
        add.setOnAction(e->eventDialog(null));edit.setOnAction(e->{Event ev=weeklyList.getSelectionModel().getSelectedItem();if(ev!=null)eventDialog(ev);else alert("Select an event from the list first.");});
        delete.setOnAction(e->{Event ev=weeklyList.getSelectionModel().getSelectedItem();if(ev!=null){try{repo.delete(ev);refresh();}catch(Exception ex){alert(ex.getMessage());}}else alert("Select an event from the list first.");});
        HBox header=new HBox(10,prev,next,today,monthLabel,add,edit,delete);header.setPadding(new Insets(12));root.setTop(header);
        monthGrid.setHgap(4);monthGrid.setVgap(4);monthGrid.setPadding(new Insets(10));
        ScrollPane calendarScroll=new ScrollPane(monthGrid);calendarScroll.setFitToWidth(true);calendarScroll.setFitToHeight(true);root.setCenter(calendarScroll);
        VBox right=new VBox(8,new Label("Events for selected week"),weeklyList);right.setPadding(new Insets(12));right.setPrefWidth(310);VBox.setVgrow(weeklyList,Priority.ALWAYS);root.setRight(right);
        weeklyList.setCellFactory(v->new ListCell<>(){@Override protected void updateItem(Event ev,boolean empty){super.updateItem(ev,empty);setText(empty||ev==null?null:ev.getDate()+"  "+ev.getStartTime()+"–"+ev.getEndTime()+"\n"+ev.getTitle());}});
        service.addObserver(new CalendarView(this::drawMonth));service.addObserver(new WeeklySchedView(this::drawWeek));
    }
    public BorderPane getRoot(){return root;}
    public void refresh(){service.updateViews();}
    private void drawMonth(List<Event> events){
        monthLabel.setText(shown.getMonth().getDisplayName(TextStyle.FULL,Locale.ENGLISH)+" "+shown.getYear());monthGrid.getChildren().clear();monthGrid.getColumnConstraints().clear();
        for(int c=0;c<7;c++){ColumnConstraints cc=new ColumnConstraints();cc.setPercentWidth(100.0/7);monthGrid.getColumnConstraints().add(cc);}
        String[] days={"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};for(int c=0;c<7;c++){Label label=new Label(days[c]);label.setStyle("-fx-font-weight:bold");monthGrid.add(label,c,0);}
        int offset=shown.atDay(1).getDayOfWeek().getValue()-1;
        for(int d=1;d<=shown.lengthOfMonth();d++){
            LocalDate day=shown.atDay(d);int pos=offset+d-1;int row=1+pos/7,col=pos%7;
            VBox cell=new VBox(3);cell.setPadding(new Insets(5));cell.setMinHeight(85);cell.setMaxWidth(Double.MAX_VALUE);
            cell.setStyle(day.equals(selected)?"-fx-border-color:#536dfe;-fx-background-color:#eef2ff;":"-fx-border-color:#d9dde6;-fx-background-color:#ffffff;");
            Label dateLabel=new Label(String.valueOf(d));dateLabel.setStyle("-fx-font-weight:bold");cell.getChildren().add(dateLabel);
            for(Event ev:events){if(ev.getDate().equals(day.toString())){Label item=new Label(ev.getStartTime()+" "+ev.getTitle());item.setWrapText(true);item.setMaxWidth(130);item.setStyle("-fx-font-size:10px;-fx-text-fill:#234d9d;");cell.getChildren().add(item);}}
            cell.setOnMouseClicked(e->{selected=day;refresh();});monthGrid.add(cell,col,row);
        }
    }
    private void drawWeek(List<Event> events){LocalDate monday=selected.with(DayOfWeek.MONDAY),sunday=monday.plusDays(6);weeklyList.getItems().setAll(events.stream().filter(ev->{LocalDate d=LocalDate.parse(ev.getDate());return !d.isBefore(monday)&&!d.isAfter(sunday);}).toList());}
    private void eventDialog(Event old){
        Dialog<ButtonType> dialog=new Dialog<>();dialog.setTitle(old==null?"Add Event":"Edit Event");dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        TextField title=new TextField(old==null?"":old.getTitle()),start=new TextField(old==null?"09:00":old.getStartTime()),end=new TextField(old==null?"10:00":old.getEndTime()),location=new TextField(old==null||old.getLocation()==null?"":old.getLocation()),color=new TextField(old==null||old.getCatColor()==null?"":old.getCatColor());
        DatePicker date=new DatePicker(old==null?selected:LocalDate.parse(old.getDate()));
        GridPane form=new GridPane();form.setHgap(10);form.setVgap(10);form.setPadding(new Insets(15));
        String[] labels={"Title","Date","Start (HH:mm)","End (HH:mm)","Location","Category color"};Node[] fields={title,date,start,end,location,color};for(int i=0;i<labels.length;i++){form.add(new Label(labels[i]),0,i);form.add(fields[i],1,i);}dialog.getDialogPane().setContent(form);
        Optional<ButtonType> result=dialog.showAndWait();if(result.isPresent()&&result.get()==ButtonType.OK){
            try{Event updated=new Event(title.getText(),date.getValue().toString(),start.getText(),end.getText(),location.getText(),color.getText());if(old==null)repo.save(updated);else repo.updateDtb(old,updated);selected=date.getValue();shown=YearMonth.from(selected);refresh();}catch(Exception ex){alert("Could not save event: "+ex.getMessage());}
        }
    }
    private void alert(String message){new Alert(Alert.AlertType.ERROR,message,ButtonType.OK).showAndWait();}
}
