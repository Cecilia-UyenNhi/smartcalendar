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
        const query = this.#db.getDatabase().prepare(sql);
        const rows = query.all();// prepare instruction for SQLite

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
        const rows = this.#db.getDatabase().prepare(sql).all(title);//? is filled with title param
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
        const rows = this.#db.getDatabase().prepare(sql).all(catColor);//? is filled with title param
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
        const sql = `DELETE FROM events WHERE title = ? AND date = ?`;
        this.#db.getDatabase().prepare(sql).run(event.title, event.date);
    }
    updateDtb(event) {
    const sql = `
        UPDATE events
        SET date = ?,
            startTime = ?,
            endTime = ?,
            location = ?,
            catColor = ?
        WHERE title = ?
    `;

    const query = this.#db.getDatabase().prepare(sql);

    query.run(
        event.date,
        event.startTime,
        event.endTime,
        event.location,
        event.catColor,
        event.title
    );
}
}

module.exports = EventRepo;