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

SmartCalendar provides a standalone CLI that allows users to access calendar features and interact with Sissy directly from a terminal without launching the JavaFX GUI.

The CLI supports two input methods:
- **Interactive Mode:** Accepts commands and natural language requests through standard input.
- **Task File Mode:** Accepts a text file containing a sequence of commands, natural language requests, and follow-up responses.

#### Interactive Mode

From the project root directory, run:

```powershell
mvn clean compile
mvn exec:java "-Dexec.mainClass=smartcalendar.cli.SmartCalendarCLI"
```

Available commands:

| Command | Description |
|---|---|
| `help` | Display available commands |
| `list` | Display all stored events |
| `today` | Display today's events |
| `tomorrow` | Display tomorrow's events |
| `week` | Display the current weekly schedule |
| `month` | Display the current monthly schedule |
| `add` | Add an event |
| `edit` | Modify an existing event |
| `delete` | Delete an event |
| `categorize` | Assign, update, or remove category rules |
| `search` | Search calendar events using filters |
| `free` | Find available time slots |
| `conflicts` | Check for scheduling conflicts |
| `import` | Import events from a PDF document |
| `deadlines` | Display deadlines within the next seven days |
| `chat` | Start a conversation with Sissy |
| `exit` or `quit` | Exit the application |

Users can also enter natural language requests directly without selecting a predefined command.

In chat mode, type `back` to return to the main CLI.

Example CLI session:

```text
smartcalendar> help
smartcalendar> list
smartcalendar> week
smartcalendar> chat
You> What events do I have tomorrow?
Sissy> [Displays matching events]
You> back
smartcalendar> exit
```

#### Task File Mode (Batch Execution)

SmartCalendar supports standalone batch execution through the `-input` command-line option. This allows users to provide a text file containing multiple requests and execute them sequentially without manually entering each command.


##### Input File Format

The input file supports:

- Predefined CLI commands.
- Natural language requests interpreted by Sissy.
- Answers to follow-up questions.
- Confirmation and cancellation responses (`yes` or `no`).
- PDF file paths and event selection instructions.
- Comments beginning with `#`.
- Empty lines for readability.

Each non-empty, non-comment line is processed sequentially. Follow-up answers and confirmation responses must appear immediately after the corresponding request in the expected order.

Example input file:

```text
# Add an event
Add CLI Demo Lecture on December 15, 2026 from 10am to 12pm.
yes

# Categorize the event
Categorize all events containing CLI Demo as blue.

# Query the event
search
title
CLI Demo Lecture

# Import selected events from a PDF
import "src/sample-pdf/Fast_Food_Job_Schedule_Fall_2026.pdf"
only 1
yes

# Display the weekly schedule
week

exit
```

##### Running the Provided task.txt File

The repository includes a `task.txt` file located in the project root directory. This file provides a working example of standalone CLI execution and demonstrates all ten implemented calendar features (F01–F10), including both deterministic and AI-powered operations.

Project structure:

```text
smartcalendar/
├── src/
│   ├── main/
│   ├── test/
│   └── sample-pdf/
├── pom.xml
├── README.md
└── task.txt
```

To execute the provided task file, run the following command from the project root directory:

```powershell
mvn compile exec:java "-Dexec.mainClass=smartcalendar.cli.SmartCalendarCLI" "-Dexec.args=-input task.txt"
```

The CLI reads the input file, executes each request, displays the corresponding results and follow-up responses in the terminal, and terminates after processing the file.

Users can also create their own text files and supply the corresponding file path through the `-input` option.

##### Execution Notes

- The Gemini API key must be configured before executing AI-powered operations.
- Deterministic commands can execute without accessing the Gemini API.
- Batch execution introduces a delay before AI requests to reduce the likelihood of exceeding Gemini API rate limits.
- Both execution modes share the same calendar services and persistent SQLite database as the GUI.
- For reproducible demonstration results, use a clean test database. Existing events may affect duplicate detection, scheduling conflicts, and PDF import results.
- Scripted follow-up responses assume that the preceding operation produces the expected prompt.
- Calendar and weekly schedules are displayed in text format when using the CLI.

## 5. Features

SmartCalendar implements the following ten proposed features.

| ID | Feature | Classification | Description |
|---|---|---|---|
| F01 | Calendar Display | Deterministic | Display stored calendar events |
| F02 | Add Event | Hybrid | Create events using natural language |
| F03 | Categorize Event | Hybrid | Assign colors based on keyword rules |
| F04 | Edit Event | Hybrid | Modify existing calendar events |
| F05 | Delete Event | Hybrid | Remove existing calendar events |
| F06 | Query Calendar | Hybrid | Search events using filters or natural language |
| F07 | Free-Time Finder | Hybrid | Identify available time slots |
| F08 | Schedule Conflict Detection | Hybrid | Detect overlapping and duplicated events |
| F09 | Import Course Schedule | Hybrid | Extract and import events from PDF documents |
| F10 | Weekly Schedule | Deterministic | Display events scheduled for the current week |

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

### Import through the GUI

1. Launch SmartCalendar.
2. Select the PDF import option.
3. Choose a document from `src/sample-pdf/`.
4. Review the extracted events.
5. Select the desired events and confirm the import.

### Import through the CLI

Enter the `import` command followed by the file path:

```text
import "src/sample-pdf/Fast_Food_Job_Schedule_Fall_2026.pdf"
```

After reviewing the extracted events, users can choose from the following options:

| Selection | Description |
|---|---|
| `all` | Import all extracted events |
| `none` | Cancel the import |
| `exclude 2` | Import all events except event 2 |
| `exclude 2,5` | Exclude multiple events |
| `only 1,3` | Import only the specified events |

The CLI displays the selected events for final confirmation before saving them to the calendar.

PDF import is also supported in task file mode. The provided `task.txt` includes an example of selective PDF import.

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


## 7. Sample PDF Files

The `src/sample-pdf/` directory contains fictional course outlines and employee work schedules for testing SmartCalendar's AI-powered PDF import feature.

All sample documents contain fictional information and are provided for demonstration purposes.