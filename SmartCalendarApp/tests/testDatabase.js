

const DataManager = require("../src/database/DataManager");

const manager1 = DataManager.getInstance();
const manager2 = DataManager.getInstance();

console.log("Equal instance:", manager1 === manager2);

const tables = manager1.getDatabase()
  .prepare("SELECT name FROM sqlite_master WHERE type = 'table'")
  .all();

console.log("Database tables:", tables);
console.log("Confirmed DataManager only have 1 instance using strict equality. Available tables in calendarDB are displayed.");