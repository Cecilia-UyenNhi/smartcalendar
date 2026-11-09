
package smartcalendar.services;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class ImportService {

    private final EventRepo repo;
    private final CalendarService calendarService;
    private final LLM llm; 
    private final ConflictService conflictService;



    public ImportService(
        EventRepo repo,
        CalendarService calendarService
    ) {
        this.repo = Objects.requireNonNull(repo);
        this.calendarService =
            Objects.requireNonNull(calendarService);

        this.llm = new LLM();
        this.conflictService = new ConflictService(repo);
    }

    // ========================================
    // IMPORT SCHEDULE
    // ========================================

    public List<Event> importFile(File file) {

        Objects.requireNonNull(file, "File required");

        if (!file.isFile()) {
            throw new IllegalArgumentException(
                "The selected file does not exist."
            );
        }

        String filename =
            file.getName().toLowerCase(Locale.ROOT);

        String extractedInfo;

        // PDF: extract locally using PDFBox
        // PDF: extract text with PDFBox, then analyze with Gemini 3.8 Flash
if (filename.endsWith(".pdf")) {

    String pdfText = extractPDF(file);

    if (pdfText.isBlank()) {
        throw new IllegalArgumentException(
            "No readable text found in the PDF."
        );
    }

    extractedInfo = llm.analyzeDocument(pdfText);

        // PNG/JPEG: extract using Gemini
        } else if (filename.endsWith(".png")
                || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")) {

            extractedInfo = extractImage(file);

        } else {
            throw new IllegalArgumentException(
                "Unsupported file type. "
                + "Please select PDF, PNG, or JPEG."
            );
        }

        if (extractedInfo.isBlank()) {
            throw new IllegalArgumentException(
                "No schedule information was extracted."
            );
        }

        // Next step: convert extracted text into events.
        List<Event> events = parseEvents(extractedInfo);

        List<Event> importedEvents = new java.util.ArrayList<>();

for (Event event : events) {

    // Skip events already stored in the calendar
    if (conflictService.isDuplicate(event)) {
        System.out.println(
            "Skipping duplicate event: " + event.getTitle()
        );
        continue;
    }

    // Save the new event to SQLite
    repo.save(event);
    importedEvents.add(event);
}

// Notify observers only when new events were imported
if (!importedEvents.isEmpty()) {
    calendarService.updateViews();
}

return importedEvents;
    }

    // ========================================
    // EXTRACT PDF TEXT
    // ========================================

    public String extractPDF(File file) {

        Objects.requireNonNull(file, "PDF file required");

        if (!file.isFile()) {
            throw new IllegalArgumentException(
                "The selected PDF does not exist."
            );
        }

        try (PDDocument document = Loader.loadPDF(file)) {

            PDFTextStripper stripper =
                new PDFTextStripper();

            String extractedText =
                stripper.getText(document);

            return extractedText;

        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to extract text from PDF.", e
            );
        }
    }

    // ========================================
    // EXTRACT IMAGE TEXT
    // ========================================
    public List<Event> parseEvents(String extractedInfo) {

    if (extractedInfo == null || extractedInfo.isBlank()) {
        return List.of();
    }

    List<Event> events = new java.util.ArrayList<>();

    try {
        JsonObject root = JsonParser.parseString(extractedInfo)
                .getAsJsonObject();

        JsonArray eventArray = root.getAsJsonArray("events");

        if (eventArray == null) {
            throw new IllegalArgumentException(
                "Gemini response is missing the events array."
            );
        }

        for (JsonElement element : eventArray) {

            JsonObject obj = element.getAsJsonObject();

            String title = getOptionalString(obj, "title");
            String date = getOptionalString(obj, "date");
            String type = getOptionalString(obj, "type");
            String startTime = getOptionalString(obj, "startTime");
            String endTime = getOptionalString(obj, "endTime");
            String dueTime = getOptionalString(obj, "dueTime");
            String location = getOptionalString(obj, "location");

            // Skip entries without a title or date
            if (title == null || date == null) {
                continue;
            }

            try {
                Event event;

                if ("deadline".equalsIgnoreCase(type)) {

                    // Defaults to 11:59 PM if no time is given
                    event = Event.createDeadline(
                        title, date, dueTime
                    );

                } else {

                    // Regular events need both start and end times
                    if (startTime == null || endTime == null) {
                        continue;
                    }

                    event = new Event(
                        title, date, startTime, endTime
                    );
                }

                event.setLocation(location);
                events.add(event);

            } catch (IllegalArgumentException e) {
                System.err.println(
                    "Skipping invalid event: " + title
                    + " (" + e.getMessage() + ")"
                );
            }
        }

        return events;

    } catch (JsonParseException | IllegalStateException e) {
        throw new IllegalArgumentException(
            "Failed to parse Gemini's event JSON.", e
        );
    }
    }
    // ========================================
// EXTRACT IMAGE TEXT
// ========================================

public String extractImage(File file) {

    Objects.requireNonNull(file, "Image file required");

    if (!file.isFile()) {
        throw new IllegalArgumentException(
            "The selected image does not exist."
        );
    }

    if (!llm.isConfigured()) {
        throw new IllegalStateException(
            "Gemini API key is required "
            + "to extract information from images."
        );
    }

    return llm.analyzeImage(file);
}

    private String getOptionalString(JsonObject obj, String key) {

    if (!obj.has(key) || obj.get(key).isJsonNull()) {
        return null;
    }

    String value = obj.get(key).getAsString().trim();

    return value.isEmpty() ? null : value;
    }
}
