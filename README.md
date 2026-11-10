# SmartCalendar — AI-Powered Calendar Assistant

SmartCalendar is a Java 21 desktop application that combines a JavaFX calendar interface, a command-line interface (CLI), and an AI assistant named **Sissy**, powered by Google's Gemini API.

SmartCalendar helps users manage events, organize academic deadlines, search their schedules using natural language, identify scheduling conflicts, find available time slots, and import course schedules from PDF documents.

## 1. Requirements

- Java Development Kit (JDK) 21
- Apache Maven
- Internet connection for Gemini AI features
- Gemini API key
- VS Code with Extension Pack for Java (recommended)

Maven automatically downloads the required dependencies, including JavaFX, SQLite JDBC, Gson, and Apache PDFBox.

## 2. Installation

1. Clone the GitHub repository or download the project.
2. Open the project folder in VS Code.
3. Open a terminal in the directory containing `pom.xml`.
4. Compile the application:

```powershell
mvn clean compile
```

## 3. Gemini API Configuration

SmartCalendar uses the Gemini API for natural-language processing and extracting calendar information from course outlines.

For security, actual API keys are **not stored in the source code or committed to GitHub**.

The repository includes a `.env.example` file containing:

```env
GEMINI_API_KEY=YOUR_LOCAL_API_KEY_HERE
```

### Configure the API key

1. Obtain a Gemini API key from Google AI Studio.
2. Use `.env.example` as a reference for the required variable name.
3. Set the environment variable locally.

For Windows PowerShell:

```powershell
$env:GEMINI_API_KEY="YOUR_ACTUAL_API_KEY"
```

4. Run SmartCalendar from the same terminal session.

Alternatively, configure `GEMINI_API_KEY` as a persistent environment variable in Windows.

SmartCalendar reads the key using:

```java
System.getenv("GEMINI_API_KEY")
```

**Important:** Java does not automatically load `.env` files. The environment variable must be configured before launching the application. If using a `.env` file, its contents must be loaded into the environment separately.

Never commit actual API keys, `.env` files containing secrets, or other credentials to GitHub.

## 4. Running SmartCalendar

### JavaFX GUI

Run:

```powershell
mvn clean javafx:run
```

This launches the graphical calendar interface.

### Command-Line Interface (CLI)

Run:

```powershell
mvn clean compile
mvn exec:java "-Dexec.mainClass=smartcalendar.cli.SmartCalendarCLI"
```

The CLI provides text-based access to calendar features.

Available commands include:

| Command | Description |
|---|---|
| `help` | Display available commands |
| `list` | Display stored events |
| `today` | Display today's events |
| `week` | Display weekly events |
| `month` | Display monthly events |
| `add` | Add an event |
| `edit` | Modify an existing event |
| `delete` | Delete an event |
| `search` | Search calendar events |
| `free` | Find available time slots |
| `category` | Manage event categories |
| `import` | Import course schedules |
| `deadlines` | Display academic deadlines |
| `chat` | Start a conversation with Sissy |
| `exit` | Exit the application |

In chat mode, type `back` to return to the main CLI.

Example CLI Session:

smartcalendar> help
smartcalendar> list
smartcalendar> add
smartcalendar> week
smartcalendar> chat

You> What events do I have tomorrow?
Sissy> [Displays matching events]

You> back
smartcalendar> exit

## 5. Features

SmartCalendar implements the following ten proposed features.

| ID | Feature | Description |
|---|---|---|
| F01 | Calendar Display | View events in a graphical calendar |
| F02 | Add Event | Create events manually or using Sissy |
| F03 | Categorize Event | Assign colors based on keywords |
| F04 | Edit Event | Modify existing calendar events |
| F05 | Delete Event | Remove events from the calendar |
| F06 | Query Calendar | Search events using filters or natural language |
| F07 | Find Free Time | Identify available time slots |
| F08 | Conflict Detection | Detect overlapping events |
| F09 | Import Course Outline | Extract events and deadlines from PDF |
| F10 | Weekly Schedule | Display events scheduled for the current week |

### Sissy AI Assistant

Sissy uses Gemini to interpret natural-language calendar requests.

Example requests:

- "Add a meeting on November 20 from 2 PM to 3 PM."
- "What events do I have tomorrow?"
- "Find me two hours of free time on Friday."
- "Delete my meeting on November 20."
- "Show me my upcoming deadlines."

Sissy interprets requests and invokes the appropriate calendar services. Calendar data is stored locally in SQLite rather than being maintained by Gemini.

### Course Outline Import

SmartCalendar supports importing schedule information from:

- PDF documents

The application extracts event details such as titles, dates, times, and locations using Gemini.

Extracted event titles are formatted concisely for calendar display.

For example:

- `ECON1000 Test 1`
- `EECS3311 Assignment 2`
- `MATH2030 Quiz 3`

Users can preview extracted events before saving them to the calendar.

In the CLI, use the `import` command to select a file.

## 6. Software Architecture

SmartCalendar follows an object-oriented, service-based architecture.

### Main Components

| Component | Responsibility |
|---|---|
| `models/Event.java` | Event data model |
| `database/DataManager.java` | SQLite database management |
| `database/EventRepo.java` | Database CRUD operations |
| `services/CalendarFacade.java` | Unified interface to calendar functionality |
| `services/CalendarService.java` | Calendar state and observer notifications |
| `services/QueryService.java` | Event searching and filtering |
| `services/FreeTimeService.java` | Free-time calculation |

### 7. Sample PDF Files

The `sample-pdf/` directory contains fictional course outlines and employee work schedules for testing SmartCalendar's AI-powered PDF import feature.

To test the feature:
1. Launch SmartCalendar.
2. Select the PDF import option.
3. Choose a document from `sample-pdf/`.
4. Review the events extracted by Sissy.
5. Confirm the events to add them to your calendar.

All sample documents contain fictional information and are provided for demonstration purposes.