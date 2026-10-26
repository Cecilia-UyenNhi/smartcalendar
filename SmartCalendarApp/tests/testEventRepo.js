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

repo.save(event1);

console.log("Event saved successfully!");

const db = DataManager.getInstance().getDatabase();
const events = db.prepare("SELECT * FROM events").all();

console.table(events);