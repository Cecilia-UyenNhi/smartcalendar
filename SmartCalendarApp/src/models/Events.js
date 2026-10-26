class Event {
    #title;
    #date;
    #startTime;
    #endTime;
    #location;
    #catColor;
    constructor(title, date, startTime, endTime, location = null, color = null) {
        if (!title || !date || !startTime || !endTime) 
            throw new Error("Title, date, start time, and end time are required.");
        if (startTime >= endTime)
            throw new Error("Start time must be before end time.");                       
        this.#title = title;
        this.#date = date;
        this.#startTime = startTime;
        this.#endTime = endTime;
        this.#location = location;
        this.#catColor = color;
    }
    get title() {
        return this.#title;
    }

    get date() {
        return this.#date;
    }

    get startTime() {
        return this.#startTime;
    }

    get endTime() {
        return this.#endTime;
    }

    get location() {
        return this.#location;
    }

    get catColor() {
        return this.#catColor;
    }

    // Setters
    set title(title) {
        if (!title) {
            throw new Error("Title is required.");
        }
        this.#title = title;
    }

    set date(date) {
        if (!date) {
            throw new Error("Date is required.");
        }
        this.#date = date;
    }

    set startTime(startTime) {
        if (!startTime) {
            throw new Error("Start time is required.");
        }
        if (this.#endTime !== undefined && startTime >= this.#endTime) {
            throw new Error("Start time must be earlier than end time.");
        }
        this.#startTime = startTime;
    }

    set endTime(endTime) {
        if (!endTime) {
            throw new Error("End time is required.");
        }
        if (this.#startTime !== undefined && endTime <= this.#startTime) {
            throw new Error("End time must be later than start time.");
        }
        this.#endTime = endTime;
    }

    set location(location) {
        this.#location = location;
    }

    set catColor(color) {
        this.#catColor = color;
    }

    toString() {
    return `{Title: ${this.#title}, Date: ${this.#date}, Start Time: ${this.#startTime}, End Time: ${this.#endTime}, Location: ${this.#location ?? "NA"}, Color: ${this.#catColor ?? "NA"}}`;
    }
}
module.exports = Event;