
package smartcalendar.services;

import smartcalendar.commands.AddEvent;
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
    private final CategoryService categoryService;

    public ImportService(
        EventRepo repo,
        CalendarService calendarService
    ) {
        this.repo = Objects.requireNonNull(repo);
        this.calendarService =
            Objects.requireNonNull(calendarService);

        this.llm = new LLM();
        this.conflictService = new ConflictService(repo);
        this.categoryService = new CategoryService(repo);
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

        if (filename.endsWith(".pdf")) {

            String pdfText = extractPDF(file);

            if (pdfText.isBlank()) {
                throw new IllegalArgumentException(
                    "No readable text found in the PDF."
                );
            }

            extractedInfo = llm.analyzeDocument(pdfText);

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

        List<Event> events = parseEvents(extractedInfo);

        List<Event> previewEvents = new java.util.ArrayList<>();

        for (Event event : events) {

            if (conflictService.isDuplicate(event)) {
                System.out.println(
                    "Skipping existing event: " + event.getTitle()
                );
                continue;
            }

            previewEvents.add(event);
        }

        return previewEvents;
    }

    public List<Event> saveImportedEvents(List<Event> events) {

        Objects.requireNonNull(events, "Events are required");

        List<Event> savedEvents = new java.util.ArrayList<>();

        for (Event event : events) {

            if (conflictService.isDuplicate(event)) {
                System.out.println(
                    "Skipping duplicate: " + event.getTitle()
                );
                continue;
            }

            String color = categoryService.categorize(event);

            if (color != null) {
                event.setCatColor(color);
            }

            new AddEvent(
                event,
                repo,
                calendarService
            ).execute();

            savedEvents.add(event);
        }

        return savedEvents;
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
    // PARSE EXTRACTED EVENTS
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

                if (title == null || date == null) {
                    continue;
                }

                try {
                    Event event;

                    if ("deadline".equalsIgnoreCase(type)) {

                        event = Event.createDeadline(
                            title, date, dueTime
                        );

                    } else {

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
