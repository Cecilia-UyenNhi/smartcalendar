const Event = require("../src/models/Events");
const EventRepo = require("../src/database/EventRepo");
const DataManager = require("../src/database/DataManager");

const repo = new EventRepo();

const event1 = new Event(
    "EECS 3311 Lecture",
    "2026-10-26",
    "10:00",
    "12:00",
    "LSB 106",
    "Blue"
);
const event2 = new Event(
    "EECS 3221 Lecture",
    "2026-10-27",
    "10:00",
    "12:00",
    "LSB 106",
    "Purple"
);
const event3 = new Event(
    "EECS 3311 Lab Test 1",
    "2026-10-28",
    "10:00",
    "12:00",
    "LSB 106",
    "Blue"
);
const db = DataManager.getInstance().getDatabase();
// Test save new event
repo.save(event1);
repo.save(event2);
repo.save(event3);
console.log("Event saved successfully! Initial table: ");

events = db.prepare("SELECT * FROM events").all();
console.table(events);// Display all available events on current Events Table

// Test findAll
console.log("Testing findAll() method:");
console.log(repo.findAll().toString());
console.log("\n");

// Test findByTitle
console.log("Find EECS 3221 Lecture event:");
console.log(repo.findByTitle("EECS 3221 Lecture").toString());
console.log("\n");

// Test updateDtb
const event4 = new Event(
    "EECS 3311 Lab Test 1",
    "2026-10-28",
    "12:00",
    "14:00",
    "LSB 106",
    "Blue"
);
console.log("Update EECS 3311 Lab Test 1 event to new time: 10-12 to 12-14");
repo.updateDtb(event3, event4);
events = db.prepare("SELECT * FROM events").all();
console.table(events);

// Test delete
repo.delete(event4);
console.log("Delete EECS 3311 Lab Test 1 event");
events = db.prepare("SELECT * FROM events").all();
console.table(events);

console.log("Testing exception:");
// Test findByTitle with non-existed title
try {
    repo.findByTitle("Biology 1000");
} catch (error) {
    console.log("Test non-existed Biology 1000 event:", error.message);
}

// Test finbyCat with non-existed catColor
try {
    repo.findByCat("Orange");
} catch (error) {
    console.log("Test non-existed Orange category:", error.message);
}
// Test missing old event
try {
    repo.updateDtb(null, new Event(
        "Lecture", "2026-10-26", "10:00", "12:00"
    ));
} catch (error) {
    console.log("Test 1:", error.message);
}
// Test: Original event DNE in database
try {
    const oldEvent = new Event(
        "Biology 1000", "2026-10-26", "10:00", "12:00"
    );

    const newEvent = new Event(
        "Biology 1000", "2026-10-27", "14:00", "16:00"
    );

    repo.updateDtb(oldEvent, newEvent);
} catch (error) {
    console.log("Test OG event DNE:", error.message);
}

// Test delete with non-existed event
try{
    const nonExistentEvent = new Event(
        "Biology 1000", "2026-10-26", "10:00", "12:00"
    );
    repo.delete(nonExistentEvent);
} catch (error) {
    console.log("Test delete non-existed Biology 1000 event:", error.message);
}
  

