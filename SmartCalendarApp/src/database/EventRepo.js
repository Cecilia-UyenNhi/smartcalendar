const DatabaseManager = require("./DataManager");
const Event = require("../models/Events");

class EventRepo {
    #db;

    constructor() {
        this.#db = DatabaseManager.getInstance().getDatabase();
    }
    save(event) {
        const sql = `
        INSERT INTO events
        (title, date, startTime, endTime, location, catColor)
        VALUES (?, ?, ?, ?, ?, ?)`;

    const query = this.#db.prepare(sql);

    query.run(
        event.title,
        event.date,
        event.startTime,
        event.endTime,
        event.location,
        event.catColor
    );
    }

    findAll(){
        const sql = `SELECT * FROM events`;//query
        const query = this.#db.prepare(sql); //prepare instruction for SQLite
        const rows = query.all();// fetch all matching rows following the query sql

        const events = [];
        // Convert row each row into an Event object
        for (const row of rows) {
            const event = new Event(
                row.title,
                row.date,
                row.startTime,
                row.endTime,
                row.location,
                row.catColor
            );
            events.push(event);// add each Event object in the Event List
        }
        return events;
    }

    findByTitle(title) {
        const sql = `SELECT * FROM events WHERE title = ?`; //? is a placeholder
        const rows = this.#db.prepare(sql).all(title);//? is filled with title param
        if (rows.length === 0)
        throw new Error(`No event found with title "${title}".`);
        const events = [];
        for (const row of rows) {
            const event = new Event(
                row.title,
                row.date,
                row.startTime,
                row.endTime,
                row.location,
                row.catColor
            );
            events.push(event);// add each Event object in the Event List
        }
        return events;
    }

    findByCat(catColor) {
        const sql = `SELECT * FROM events WHERE catColor = ?`; //? is a placeholder
        const rows = this.#db.prepare(sql).all(catColor);//? is filled with title param
        if (rows.length === 0)
        throw new Error(`No event found with category color "${catColor}".`);
        const events = [];
        for (const row of rows) {
            const event = new Event(
                row.title,
                row.date,
                row.startTime,
                row.endTime,
                row.location,
                row.catColor
            );
            events.push(event);// add each Event object in the Event List
        }
        return events;
    }
    findByDate(date) {
        const sql = `SELECT * FROM events WHERE date = ?`; //? is a placeholder
        const rows = this.#db.getDatabase().prepare(sql).all(date);//? is filled with title param
        const events = [];
        for (const row of rows) {
            const event = new Event(
                row.title,
                row.date,
                row.startTime,
                row.endTime,
                row.location,
                row.catColor
            );
            events.push(event);// add each Event object in the Event List
        }
        return events;
    }
    delete(event){
        if (!event.title || !event.date) 
            throw new Error("Title AND date are required to delete an event.");
        const sql = `DELETE FROM events WHERE title = ? AND date = ?`;
        // .run() delete a row in the table
        const result =this.#db.prepare(sql).run(event.title, event.date);
        if (result.changes === 0) 
            throw new Error(`Event "${event.title}" not found. Delete failed.`);
    }
    updateDtb(oldEvent, newEvent) {
        if (!newEvent || !oldEvent) 
            throw new Error("Both old and new events are required."); 
        if (!oldEvent.title || !oldEvent.date)
            throw new Error("Old event title and date are required.");
        if (!newEvent.title || !newEvent.date)
            throw new Error("New event title and date are required.");
        // Check how many events match
        const checkSql = `
            SELECT COUNT(*) AS count
            FROM events
            WHERE title = ?
            AND date = ?
        `;
        const sql = `
            UPDATE events
            SET 
                title = ?,
                date = ?,
                startTime = ?,
                endTime = ?,
                location = ?,
                catColor = ?
            WHERE title = ?
            AND date = ?
            `;
        const old = [oldEvent.title, oldEvent.date];
        const transaction = this.#db.transaction(() => {
            const query = this.#db.prepare(checkSql).get(...old); 
            if (query.count === 0) 
                throw new Error(`Event "${oldEvent.title}" not found. Update failed.`);
            const result = this.#db.prepare(sql).run(
                newEvent.title,
                newEvent.date,
                newEvent.startTime,
                newEvent.endTime,
                newEvent.location,
                newEvent.catColor,
                ...old
            );
            if (result.changes === 0)
                throw new Error("Update failed.");
        });
    transaction();
    }
}

module.exports = EventRepo;