const CalendarObserver = require("./CalendarObserver");

class WeeklySchedView extends CalendarObserver {
    display(events) {
        console.log("\n--- Weekly Schedule View ---");

        for (const event of events) {
            console.log(event.toString());
            console.log("--------------------");
        }
    }
}

module.exports = WeeklySchedView;