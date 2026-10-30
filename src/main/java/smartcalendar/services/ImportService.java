
package smartcalendar.services;

import smartcalendar.database.EventRepo;
import smartcalendar.models.Event;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class ImportService {

    private final EventRepo repo;
    private final CalendarService calendarService;

    // Gemini / LLMService will be added later.

    public ImportService(
        EventRepo repo,
        CalendarService calendarService
    ) {
        this.repo = Objects.requireNonNull(repo);
        this.calendarService =
            Objects.requireNonNull(calendarService);
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

        // TODO: Extract information from PDF or image.
        // TODO: Send extracted information to Gemini.
        // TODO: Convert Gemini output into Event objects.
        // TODO: Save events using EventRepo.
        // TODO: Notify observers using CalendarService.

        throw new UnsupportedOperationException(
            "AI schedule importing is not implemented yet."
        );
    }

    // ========================================
    // EXTRACT PDF TEXT
    // ========================================

    public String extractPDF(File file) {

        // TODO: Implement PDF text extraction later.

        throw new UnsupportedOperationException(
            "PDF extraction is not implemented yet."
        );
    }

    // ========================================
    // EXTRACT IMAGE TEXT
    // ========================================

    public String extractImage(File file) {

        // TODO: Integrate Gemini image understanding later.

        throw new UnsupportedOperationException(
            "Image extraction is not implemented yet."
        );
    }

    // ========================================
    // PARSE EXTRACTED INFORMATION
    // ========================================

    public List<Event> parseEvents(String extractedInfo) {

        // TODO: Convert extracted information
        // into Event objects.

        throw new UnsupportedOperationException(
            "Event parsing is not implemented yet."
        );
    }
}
