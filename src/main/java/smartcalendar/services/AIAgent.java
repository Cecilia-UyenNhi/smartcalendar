
package smartcalendar.services;

import java.util.Objects;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import smartcalendar.models.Event;

public class AIAgent {

    private final LLM llm;
    private final CalendarFacade facade;
    private Event pendingEvent;
    private Event pendingDeleteEvent;
    private Event pendingEditOriginal;
    private Event pendingEditUpdated;
    // ========================================
    // CONSTRUCTOR
    // ========================================

    public AIAgent(CalendarFacade facade) {
        this.facade = Objects.requireNonNull(
            facade, "CalendarFacade is required."
        );

        this.llm = new LLM();
    }

    // ========================================
    // HANDLE USER REQUEST
    // ========================================

    public String handleRequest(String request) {

        if (request == null || request.isBlank()) {
            return "Please enter a message.";
        }

        String userRequest = request.trim();
        // Handle confirmation for editing an event
if (pendingEditOriginal != null && pendingEditUpdated != null) {

    if (userRequest.equalsIgnoreCase("yes")) {

        Event original = pendingEditOriginal;
        Event updated = pendingEditUpdated;

        pendingEditOriginal = null;
        pendingEditUpdated = null;

        try {
            // Make sure the original event still exists
            boolean stillExists = facade.getEvents().stream()
                .anyMatch(event ->
                    event.getTitle().equals(original.getTitle())
                    && event.getDate().equals(original.getDate())
                    && event.getStartTime().equals(original.getStartTime())
                    && event.getEndTime().equals(original.getEndTime())
                    && Objects.equals(
                        event.getLocation(),
                        original.getLocation()
                    )
                );

            if (!stillExists) {
                return "The original event no longer exists. "
                     + "No changes were saved.";
            }

            // Execute the existing EditEvent Command
            facade.editEvent(original, updated);

            return "Event updated successfully!\n\n"
                + "Title: " + updated.getTitle() + "\n"
                + "Date: " + updated.getDate() + "\n"
                + "Time: " + updated.getStartTime()
                + " - " + updated.getEndTime();

        } catch (Exception e) {
            return "Could not update event: " + e.getMessage();
        }
    }

    if (userRequest.equalsIgnoreCase("no")) {
        pendingEditOriginal = null;
        pendingEditUpdated = null;

        return "Okay, I cancelled the edit. "
             + "No changes were saved.";
    }

    return "Please reply Yes or No to confirm the edit.";
}
        // Handle confirmation for deleting an event
if (pendingDeleteEvent != null) {

    if (userRequest.equalsIgnoreCase("yes")) {

        Event eventToDelete = pendingDeleteEvent;
        pendingDeleteEvent = null;

        try {
            // Execute the existing DeleteEvent command
            boolean stillExists = facade.getEvents().stream()
        .anyMatch(event ->
            event.getTitle().equals(eventToDelete.getTitle())
            && event.getDate().equals(eventToDelete.getDate())
            && event.getStartTime().equals(eventToDelete.getStartTime())
            && event.getEndTime().equals(eventToDelete.getEndTime())
            && java.util.Objects.equals(
                event.getLocation(),
                eventToDelete.getLocation()
            )
        );

    if (!stillExists) {
        return "This event no longer exists in your calendar. "
             + "Nothing was deleted.";
    }

    // Execute the existing DeleteEvent command
    facade.deleteEvent(eventToDelete);

            return "Event deleted successfully!\n\n"
                + "Title: " + eventToDelete.getTitle() + "\n"
                + "Date: " + eventToDelete.getDate();

        } catch (Exception e) {
            return "Could not delete event: " + e.getMessage();
        }
    }

    if (userRequest.equalsIgnoreCase("no")) {
        pendingDeleteEvent = null;
        return "Okay, I cancelled the deletion.";
    }

    return "Please reply Yes or No to confirm deletion.";
}
        // Handle confirmation for a pending event
if (pendingEvent != null) {

    if (userRequest.equalsIgnoreCase("yes")) {

        Event confirmedEvent = pendingEvent;
        pendingEvent = null;

        try {
            // Check duplicates again before saving
    if (facade.isDuplicate(confirmedEvent)) {
        return "This event already exists in your calendar. "
             + "No duplicate was added.";
    }

    // Check for scheduling conflicts again
    java.util.List<Event> conflicts =
        facade.getConflictingEvents(confirmedEvent);

    if (!conflicts.isEmpty()) {
        System.out.println(
            "Saving event with " + conflicts.size()
            + " scheduling conflict(s), as confirmed by user."
        );
    }
            facade.addEvent(confirmedEvent);

            return "Event added successfully!\n\n"
                + "Title: " + confirmedEvent.getTitle() + "\n"
                + "Date: " + confirmedEvent.getDate() + "\n"
                + "Time: " + confirmedEvent.getStartTime()
                + " - " + confirmedEvent.getEndTime();

        } catch (Exception e) {
            return "Could not add event: " + e.getMessage();
        }
    }

    if (userRequest.equalsIgnoreCase("no")) {
        pendingEvent = null;
        return "Okay, I cancelled the event.";
    }

    return "Please reply Yes or No to confirm the event.";
}

        // AI is optional; calendar still works without it.
        if (!llm.isConfigured()) {
            return "Sissy's AI features are currently unavailable. "
                 + "Please configure your Gemini API key.";
        }

        try {
            return interpretRequest(userRequest);

        } catch (Exception e) {
            System.err.println(
                "AIAgent error: " + e.getMessage()
            );

            return "Sorry, I couldn't process your request. "
                 + "Please try again.";
        }
    }

    // ========================================
    // INTERPRET USER REQUEST
    // ========================================

    public String interpretRequest(String request) {

        if (request == null || request.isBlank()) {
            return "Please enter a message.";
        }
String prompt = """
    You are Sissy, the AI assistant for SmartCalendar.

    Determine whether the user wants to:
    - add a calendar event
    - edit an existing event
    - delete an existing event
    - assign or change a keyword-based category color
    - remove a keyword-based category color rule
    - query, search, list, or count existing calendar events
    - find available free time between scheduled events
    - have a normal conversation

    For calendar actions, return ONLY a valid JSON object
    with these fields:

    {
      "action": "add | edit | delete",
      "title": "event title",
      "date": "YYYY-MM-DD",
      "startTime": "HH:mm",
      "endTime": "HH:mm",
      "location": null
    }

    Rules:
    - Use null for missing information.
    - Do not invent event details.
    - Interpret relative dates using today's date: %s.
    - Use 24-hour time.
    - Calculate endTime if a start time and duration are given.
    For delete, identify the existing event by title and date.

For edit, return ONLY a valid JSON object with:
{
  "action": "edit",
  "title": "existing event title",
  "date": "existing event date in YYYY-MM-DD",
  "startTime": null,
  "endTime": null,
  "location": null,
  "changes": {
    "title": null,
    "date": null,
    "startTime": null,
    "endTime": null,
    "location": null
  }
}

For edit requests:
- The top-level title and date identify the ORIGINAL event.
- Put requested new values inside "changes".
- Use null for fields the user does not want to change.
- Do not invent missing details.
- If the user specifies a duration and a new start time,
  calculate the new end time.
    - Do not claim an operation has been completed.
    - Do not include Markdown code fences around JSON.

    For keyword-based category coloring, return ONLY:
    {
      "action": "categorize",
      "keyword": "text that must appear in event titles",
      "color": "Green"
    }

    For removing a keyword-color rule, return ONLY:
    {
      "action": "remove_category",
      "keyword": "text that must appear in event titles"
    }

    Category rules:
    - Use "categorize" for requests to assign, set, make, or change
      the color of all events whose titles contain a keyword.
    - Extract the keyword exactly as requested, without surrounding
      phrases like "all events containing" or "in the title".
    - Supported colors: Red, Orange, Yellow, Green, Blue,
      Purple, Pink, Brown, Black.
    - If keyword or color is missing, use null; never guess.
    - Use "remove_category" only when the user wants to remove
      the saved keyword-color rule, not delete calendar events.
    - Do not claim the rule was saved; the application performs it.

    For calendar searches and questions, return ONLY:
    {
      "action": "query",
      "keyword": null,
      "startDate": null,
      "endDate": null,
      "type": null,
      "location": null,
      "afterTime": null,
      "beforeTime": null,
      "upcoming": false,
      "nextOnly": false,
      "countOnly": false
    }

    Query rules:
    - Use query for questions about saved events, schedules, deadlines,
      assignments, exams, meetings, dates, locations, and counts.
    - keyword is a course code or title substring (e.g. EECS3311).
    - Use YYYY-MM-DD for startDate/endDate; include both dates.
    - For a single date, set startDate and endDate to that date.
    - Resolve today, tomorrow, this week, next week, this month,
      next month, and next N days using today's date above.
    - Use type: deadline, assignment, exam, meeting, or class when applicable.
    - Use HH:mm 24-hour time for afterTime and beforeTime.
    - For morning, use beforeTime "12:00"; for afternoon, use
      afterTime "12:00" and beforeTime "17:00"; for evening,
      use afterTime "17:00".
    - Set upcoming true for future or remaining events.
    - Set nextOnly true for "next event" and upcoming true.
    - Set countOnly true when asking how many events match.
    - Multiple filters may be combined; use null when unspecified.
    - Do not invent calendar events or answer from general knowledge.
    - Do not interpret requests to edit or remove events as queries.

    For requests to find available time, return ONLY:
    {
      "action": "find_free_time",
      "startDate": "YYYY-MM-DD",
      "endDate": "YYYY-MM-DD",
      "afterTime": "HH:mm",
      "beforeTime": "HH:mm",
      "durationMinutes": 120
    }

    Free-time rules:
    - Use find_free_time when the user asks when they are free,
      asks for an open slot, or wants time for an activity.
    - Use the user's requested minimum duration in minutes.
      Convert hours to minutes (e.g. 1.5 hours = 90).
    - If no duration is given, use null; do not invent one.
    - Resolve relative dates using today's date above.
    - For a single day, use the same startDate and endDate.
    - If no date is specified, use today's date for both dates.
    - If no time bounds are specified, use 00:00 and 23:59.
    - afterTime applies to the first day; beforeTime applies to
      the last day. Use 24-hour HH:mm notation.
    - Never invent free slots: the application calculates them.
    - Do not use query for free-time requests.

    For normal conversation, respond with a valid JSON object:

    {
      "action": "chat",
      "message": "Your helpful response"
    }

    User request:
    %s
    """.formatted(
        java.time.LocalDate.now(),
        request
    );

        String response = llm.interpret(prompt);

JsonObject result = JsonParser.parseString(response)
    .getAsJsonObject();

String action = result.get("action").getAsString();

System.out.println("Detected action: " + action);
if ("find_free_time".equalsIgnoreCase(action)) {
    return handleFindFreeTime(result);
}
if ("query".equalsIgnoreCase(action)) {
    return handleQuery(result);
}
if ("categorize".equalsIgnoreCase(action)) {
    String keyword = getOptionalJsonString(result, "keyword");
    String color = getOptionalJsonString(result, "color");

    if (keyword == null) {
        return "Which event-title keyword should I color?";
    }
    if (color == null) {
        return "Which color should I assign to events containing \""
            + keyword + "\"?";
    }

    try {
        long matchingEvents = facade.getEvents().stream()
            .filter(event -> event.getTitle() != null
                && event.getTitle().toLowerCase(java.util.Locale.ROOT)
                    .contains(keyword.toLowerCase(java.util.Locale.ROOT)))
            .count();

        facade.categorizeEvent(keyword, color);
        return "Saved the " + color + " color rule for titles containing \""
            + keyword + "\". Found " + matchingEvents
            + " matching existing event(s). Future matching events "
            + "will also use this color.";
    } catch (IllegalArgumentException e) {
        return "Could not assign category color: " + e.getMessage();
    } catch (Exception e) {
        return "Could not save category color rule: " + e.getMessage();
    }
}

if ("remove_category".equalsIgnoreCase(action)) {
    String keyword = getOptionalJsonString(result, "keyword");
    if (keyword == null) {
        return "Which keyword-color rule should I remove?";
    }

    try {
        facade.removeCategory(keyword);
        return "Removed the color rule for titles containing \""
            + keyword + "\". Existing event colors remain unchanged.";
    } catch (Exception e) {
        return "Could not remove category color rule: " + e.getMessage();
    }
}

if ("add".equalsIgnoreCase(action)) {

    String title = result.get("title").getAsString();
    String date = result.get("date").getAsString();
    String startTime = result.get("startTime").getAsString();
    String endTime = result.get("endTime").getAsString();

    Event proposedEvent = new Event(
        title,
        date,
        startTime,
        endTime
    );

    System.out.println(
        "Proposed event: " + proposedEvent.getTitle()
        + " on " + proposedEvent.getDate()
    );
    // 1. Check for duplicate events
if (facade.isDuplicate(proposedEvent)) {
    return "This event already exists in your calendar:\n\n"
        + "Title: " + proposedEvent.getTitle() + "\n"
        + "Date: " + proposedEvent.getDate() + "\n"
        + "Time: " + proposedEvent.getStartTime()
        + " - " + proposedEvent.getEndTime()
        + "\n\nNo duplicate was added.";
}

// 2. Check for scheduling conflicts
java.util.List<Event> conflicts =
    facade.getConflictingEvents(proposedEvent);

String conflictWarning = "";

if (!conflicts.isEmpty()) {

    StringBuilder warning = new StringBuilder(
        "⚠ Scheduling conflict detected!\n\n"
        + "This event overlaps with:\n"
    );

    for (Event existing : conflicts) {
        warning.append("- ")
            .append(existing.getTitle())
            .append(" (")
            .append(existing.getStartTime())
            .append(" - ")
            .append(existing.getEndTime())
            .append(")\n");
    }

    conflictWarning = warning.toString() + "\n";
}
    pendingEvent = proposedEvent;

return conflictWarning
    + "I found the following event:\n\n"
    + "Title: " + proposedEvent.getTitle() + "\n"
    + "Date: " + proposedEvent.getDate() + "\n"
    + "Time: " + proposedEvent.getStartTime()
    + " - " + proposedEvent.getEndTime() + "\n\n"
    + "Would you like me to add this event? (Yes/No)";
}
if ("delete".equalsIgnoreCase(action)) {

    String title = result.get("title").getAsString();

    String date = null;

    if (result.has("date") && !result.get("date").isJsonNull()) {
        date = result.get("date").getAsString();
    }

    java.util.List<Event> matches = new java.util.ArrayList<>();

    for (Event event : facade.getEvents()) {

        boolean titleMatches =
            event.getTitle().equalsIgnoreCase(title);

        boolean dateMatches =
            date == null || event.getDate().equals(date);

        if (titleMatches && dateMatches) {
            matches.add(event);
        }
    }

    System.out.println("Delete matches found: " + matches.size());

    if (matches.isEmpty()) {
        return "I couldn't find that event in your calendar.";
    }

    if (matches.size() > 1) {
        return "I found multiple matching events. "
             + "Please specify the exact date and time.";
    }

    Event eventToDelete = matches.get(0);

    pendingDeleteEvent = eventToDelete;

    return "I found this event:\n\n"
        + "Title: " + eventToDelete.getTitle() + "\n"
        + "Date: " + eventToDelete.getDate() + "\n"
        + "Time: " + eventToDelete.getStartTime()
        + " - " + eventToDelete.getEndTime() + "\n\n"
        + "Would you like me to DELETE this event? (Yes/No)";
}
if ("edit".equalsIgnoreCase(action)) {

    String title = result.has("title")
        && !result.get("title").isJsonNull()
        ? result.get("title").getAsString()
        : null;

    String date = result.has("date")
        && !result.get("date").isJsonNull()
        ? result.get("date").getAsString()
        : null;

    if (title == null || title.isBlank()) {
        return "Please specify which event you want to edit.";
    }

    java.util.List<Event> matches = new java.util.ArrayList<>();

    for (Event event : facade.getEvents()) {

        boolean titleMatches =
            event.getTitle().equalsIgnoreCase(title);

        boolean dateMatches =
            date == null || event.getDate().equals(date);

        if (titleMatches && dateMatches) {
            matches.add(event);
        }
    }

    if (matches.isEmpty()) {
        return "I couldn't find that event in your calendar.";
    }

    if (matches.size() > 1) {
        return "I found multiple matching events. "
             + "Please specify the exact date and time.";
    }

    Event originalEvent = matches.get(0);
    // Get the changes requested by the user
if (!result.has("changes")
        || !result.get("changes").isJsonObject()) {

    return "I couldn't identify what you want to change. "
         + "Please try again.";
}

JsonObject changes = result.getAsJsonObject("changes");

System.out.println(
    "Requested changes: " + changes
);
String newTitle = getChangedValue(
    changes, "title", originalEvent.getTitle()
);

String newDate = getChangedValue(
    changes, "date", originalEvent.getDate()
);

String newStart = getChangedValue(
    changes, "startTime", originalEvent.getStartTime()
);

String newEnd = getChangedValue(
    changes, "endTime", originalEvent.getEndTime()
);

String newLocation = getChangedValue(
    changes, "location", originalEvent.getLocation()
);

// Preserve the original duration if only the start time changes
if (changes.has("startTime")
        && !changes.get("startTime").isJsonNull()
        && (!changes.has("endTime")
            || changes.get("endTime").isJsonNull())) {

    java.time.LocalTime oldStart =
        java.time.LocalTime.parse(originalEvent.getStartTime());

    java.time.LocalTime oldEnd =
        java.time.LocalTime.parse(originalEvent.getEndTime());

    java.time.Duration duration =
        java.time.Duration.between(oldStart, oldEnd);

    java.time.LocalTime updatedStart =
        java.time.LocalTime.parse(newStart);

    if (duration.isNegative() || duration.isZero()
            || duration.toMinutes() >= 1440
            || updatedStart.toSecondOfDay()
               + duration.getSeconds() >= 86400) {
        return "The updated time is invalid or extends past midnight.";
    }

    newEnd = updatedStart.plus(duration).toString();
}

// Construct the updated event without saving it
Event updatedEvent = new Event(
    newTitle,
    newDate,
    newStart,
    newEnd
);

updatedEvent.setLocation(newLocation);
updatedEvent.setCatColor(originalEvent.getCatColor());
// Store the original and updated events
pendingEditOriginal = originalEvent;
pendingEditUpdated = updatedEvent;

// Show the proposed changes
return "I found the event you want to edit:\n\n"
    + "Current event:\n"
    + "Title: " + originalEvent.getTitle() + "\n"
    + "Date: " + originalEvent.getDate() + "\n"
    + "Time: " + originalEvent.getStartTime()
    + " - " + originalEvent.getEndTime() + "\n"
    + "Location: " + originalEvent.getLocation() + "\n\n"

    + "Updated event:\n"
    + "Title: " + updatedEvent.getTitle() + "\n"
    + "Date: " + updatedEvent.getDate() + "\n"
    + "Time: " + updatedEvent.getStartTime()
    + " - " + updatedEvent.getEndTime() + "\n"
    + "Location: " + updatedEvent.getLocation() + "\n\n"

    + "Would you like me to save these changes? (Yes/No)";

    
}
if ("chat".equalsIgnoreCase(action)) {
    if (result.has("message") && !result.get("message").isJsonNull()) {
        return result.get("message").getAsString();
    }

    return "I'm here to help with your calendar!";
}
return response;
    }
    // Find available intervals using the existing Strategy pattern.
    private String handleFindFreeTime(JsonObject request) {
        try {
            String durationText = getOptionalJsonString(request, "durationMinutes");
            if (durationText == null) {
                return "How much free time do you need? For example, 30 minutes or 2 hours.";
            }

            double minutes = Double.parseDouble(durationText);
            if (!Double.isFinite(minutes) || minutes < 1) {
                return "Please provide a duration of at least one minute.";
            }

            String startText = getOptionalJsonString(request, "startDate");
            String endText = getOptionalJsonString(request, "endDate");
            String afterText = getOptionalJsonString(request, "afterTime");
            String beforeText = getOptionalJsonString(request, "beforeTime");

            LocalDate firstDay = startText == null
                ? LocalDate.now() : LocalDate.parse(startText);
            LocalDate lastDay = endText == null
                ? firstDay : LocalDate.parse(endText);

            LocalTime fromTime = afterText == null
                ? LocalTime.MIDNIGHT : LocalTime.parse(afterText);
            LocalTime toTime = beforeText == null
                ? LocalTime.of(23, 59) : LocalTime.parse(beforeText);

            LocalDateTime start = firstDay.atTime(fromTime);
            LocalDateTime end = lastDay.atTime(toTime);

            if (!start.isBefore(end)) {
                return "The search end must be after the search start.";
            }

            List<String> slots = facade.findFreeTime(start, end, minutes / 60.0);
            if (slots.isEmpty()) {
                return "I couldn't find an available interval of at least "
                    + formatDuration(minutes) + " in that time range.";
            }

            StringBuilder answer = new StringBuilder(
                "I found " + slots.size() + " available interval(s) "
                + "lasting at least " + formatDuration(minutes) + ":\n\n"
            );
            for (int i = 0; i < slots.size(); i++) {
                answer.append(i + 1).append(". ").append(slots.get(i));
                if (i < slots.size() - 1) answer.append("\n");
            }
            return answer.toString();
        } catch (IllegalArgumentException e) {
            return "I couldn't understand the free-time request: " + e.getMessage();
        } catch (Exception e) {
            System.err.println("Find free time failed: " + e.getMessage());
            return "I couldn't check your free time right now.";
        }
    }

    private String formatDuration(double minutes) {
        if (minutes == Math.rint(minutes)) {
            long total = (long) minutes;
            if (total % 60 == 0) return (total / 60) + " hour(s)";
            return total + " minute(s)";
        }
        return minutes + " minutes";
    }

    // Retrieve matching events from SQLite through CalendarFacade.
    private String handleQuery(JsonObject query) {
        try {
            String keyword = getOptionalJsonString(query, "keyword");
            String type = getOptionalJsonString(query, "type");
            String location = getOptionalJsonString(query, "location");
            String startText = getOptionalJsonString(query, "startDate");
            String endText = getOptionalJsonString(query, "endDate");
            String afterText = getOptionalJsonString(query, "afterTime");
            String beforeText = getOptionalJsonString(query, "beforeTime");

            LocalDate start = startText == null ? null : LocalDate.parse(startText);
            LocalDate end = endText == null ? null : LocalDate.parse(endText);
            LocalTime after = afterText == null ? null : LocalTime.parse(afterText);
            LocalTime before = beforeText == null ? null : LocalTime.parse(beforeText);

            boolean upcoming = getJsonBoolean(query, "upcoming");
            boolean nextOnly = getJsonBoolean(query, "nextOnly");
            boolean countOnly = getJsonBoolean(query, "countOnly");

            List<Event> matches = facade.queryEvents(
                keyword, start, end, type, location,
                after, before,
                (upcoming || nextOnly) ? LocalDateTime.now() : null
            );

            if (countOnly) {
                return "I found " + matches.size() + " matching event(s).";
            }

            if (matches.isEmpty()) {
                return "I couldn't find any matching events in your calendar.";
            }

            int limit = nextOnly ? 1 : matches.size();
            StringBuilder answer = new StringBuilder(
                "I found " + (nextOnly ? 1 : matches.size())
                + " matching event(s):\n\n"
            );

            for (int i = 0; i < limit; i++) {
                Event event = matches.get(i);
                answer.append(i + 1).append(". ")
                    .append(event.getTitle()).append("\n")
                    .append("   Date: ").append(event.getDate()).append("\n")
                    .append("   Time: ").append(event.getStartTime())
                    .append(" - ").append(event.getEndTime()).append("\n");

                if (event.getLocation() != null
                        && !event.getLocation().isBlank()) {
                    answer.append("   Location: ")
                        .append(event.getLocation()).append("\n");
                }
                if (i < limit - 1) {
                    answer.append("\n");
                }
            }
            return answer.toString();

        } catch (IllegalArgumentException e) {
            return "I couldn't understand the query filters: " + e.getMessage();
        } catch (Exception e) {
            System.err.println("Calendar query failed: " + e.getMessage());
            return "I couldn't search your calendar right now.";
        }
    }

    private boolean getJsonBoolean(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull()
            && object.get(key).getAsBoolean();
    }

    // Safely read optional fields from Gemini's JSON response.
    private String getOptionalJsonString(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        String value = object.get(key).getAsString().trim();
        return value.isEmpty() ? null : value;
    }

    private String getChangedValue(
    JsonObject changes,
    String key,
    String originalValue
) {
    if (!changes.has(key) || changes.get(key).isJsonNull()) {
        return originalValue;
    }

    return changes.get(key).getAsString();
}
    

    // ========================================
    // GETTERS
    // ========================================

    public LLM getLLM() {
        return llm;
    }

    public CalendarFacade getFacade() {
        return facade;
    }

    public boolean isAIAvailable() {
        return llm.isConfigured();
    }
}
