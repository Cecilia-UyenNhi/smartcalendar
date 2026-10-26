const Event = require("../src/models/Events");

const event1 = new Event(
    "Lecture",
    "2026-10-26",
    "10:00",
    "12:00"
);
console.log("Event 1: \n" + event1.toString());

//Invalid time
try{
const event2 = new Event(
    "Lecture",
    "2026-10-26",
    "12:00",
    "10:00"
);
console.log(event2.toString());
} catch (error) {
    console.error("Event 2: " + error.message);
}

try{
    const event3 = new Event(
    null,
    "2026-10-26",
    "10:00",
    "12:00"
);
console.log(event3.toString());
} catch (error) {
    console.error("Event 3: " + error.message);
}

const event4 = new Event(
    "Lecture",
    "2026-10-26",
    "10:00",
    "12:00",
    "LSB 106",
    "Blue"
);
console.log("Event 4: \n" + event4.toString());





