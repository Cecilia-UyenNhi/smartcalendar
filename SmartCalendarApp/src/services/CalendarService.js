const EventRepo = require("../database/EventRepo");

class CalendarService {
    #repo;
    #observers;

    constructor() {
        this.#repo = new EventRepo();
        this.#observers = [];
    }

    getEvents(){
        return this.#repo.findAll();
    }

    addObserver(observer) {
        if (!this.#observers.includes(observer)) 
            this.#observers.push(observer);
    }
    removeObserver(observer) {
        const index = this.#observers.indexOf(observer);
        if (index !== -1) {
            this.#observers.splice(index, 1);
        }
    }
    updateViews(){
        const events = this.getEvents();
        for (const observer of this.#observers) {
            observer.display(events);
        }
    }
}

module.exports = CalendarService;