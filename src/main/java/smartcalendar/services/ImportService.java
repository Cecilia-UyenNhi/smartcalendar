
package smartcalendar.services;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;

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

    public ImportService(
        EventRepo repo,
        CalendarService calendarService
    ) {
        this.repo = Objects.requireNonNull(repo);
        this.calendarService =
            Objects.requireNonNull(calendarService);

        this.llm = new LLM();
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
        if (filename.endsWith(".pdf")) {
            extractedInfo = extractPDF(file);

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

        // Saving and observer updates will be added
        // after event parsing is implemented.
        return events;
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

    // ========================================
    // PARSE EXTRACTED INFORMATION
    // ========================================

    public List<Event> parseEvents(String extractedInfo) {

        if (extractedInfo == null
                || extractedInfo.isBlank()) {
            return List.of();
        }

        // TODO:
        // 1. Send extracted text to Gemini.
        // 2. Ask Gemini to return structured JSON.
        // 3. Convert JSON into Event objects.
        // 4. Validate dates, times, and required fields.

        throw new UnsupportedOperationException(
            "Event parsing is not implemented yet."
        );
    }
}
