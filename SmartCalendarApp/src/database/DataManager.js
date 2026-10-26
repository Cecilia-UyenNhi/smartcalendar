const Database = require('better-sqlite3');


class DataManager {
    static instance = null;
    
    constructor(){
        if (DataManager.instance) 
            return DataManager.instance;

        this.db = new Database("calendarDB");
        this.db.exec(`
            CREATE TABLE IF NOT EXISTS events(               
                title TEXT NOT NULL,
                date DATE NOT NULL,
                startTime TIME NOT NULL,
                endTime TIME NOT NULL,
                location TEXT,
                catColor TEXT
            ) 
         `);
        DataManager.instance = this;
    }
    static getInstance() {
        if (!DataManager.instance) 
            DataManager.instance = new DataManager();       
        return DataManager.instance;
    }
    getDatabase(){
        return this.db;
    }  
    
}
module.exports = DataManager;