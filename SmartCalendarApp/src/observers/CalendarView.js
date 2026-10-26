const CalendarObserver = require("./CalendarObserver");

class CalendarView extends CalendarObserver {
    display(events) {
        console.log("\n--- Calendar View ---");

        console.table(events.map(event => ({
            title: event.title,
            date: event.date,
            startTime: event.startTime,
            endTime: event.endTime,
            location: event.location,
            catColor: event.catColor
        })));
    }
}

module.exports = CalendarView;