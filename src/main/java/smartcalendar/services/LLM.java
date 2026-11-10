
package smartcalendar.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LLM {

    // Fast model for chat and calendar commands
    private static final String FAST_MODEL = "gemini-3.5-flash-lite";

    // Advanced model for PDF document extraction
    private static final String DOCUMENT_MODEL = "gemini-3.8-flash";

    private final String apiKey;
    private final HttpClient client;

    // ========================================
    // CONSTRUCTOR
    // ========================================

    public LLM() {

        apiKey = System.getenv("GEMINI_API_KEY");

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    // ========================================
    // API KEY CONFIGURATION
    // ========================================

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    // ========================================
    // NATURAL-LANGUAGE INTERPRETATION
    // ========================================

    public String interpret(String prompt) {

        if (prompt == null || prompt.isBlank()) {
            return "Please enter a message.";
        }

        if (!isConfigured()) {
            return "Sissy's AI features are currently unavailable. "
                 + "Please configure your Gemini API key.";
        }

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(textPart);

        try {
            return sendRequest(parts, FAST_MODEL);

        } catch (RuntimeException e) {

            System.err.println(
                "Gemini text request failed: " + e.getMessage()
            );

            return "Sorry, I couldn't connect to Gemini right now. "
                 + "Please check your API key and internet connection.";
        }
    }

    // ========================================
    // ANALYZE EXTRACTED PDF TEXT
    // ========================================

    public String analyzeDocument(String extractedText) {

        if (extractedText == null || extractedText.isBlank()) {
            throw new IllegalArgumentException(
                "Document text cannot be empty."
            );
        }

        if (!isConfigured()) {
            throw new IllegalStateException(
                "Gemini API key is required for document analysis."
            );
        }

        JsonObject textPart = new JsonObject();

        textPart.addProperty(
            "text",
            "Extract calendar events and academic deadlines from this document. "
            + "Return ONLY a valid JSON object with an 'events' array. "
            + "Each event must contain: title, date, startTime, endTime, "
            + "dueTime, location, and type. "
            + "Use YYYY-MM-DD for dates and HH:mm (24-hour) for times. "
            + "Use type 'deadline' for assignments and submission deadlines, "
            + "and type 'event' for classes, tests, exams, and meetings. "
            + "For deadlines without an explicitly stated time, set dueTime to null. "
            + "For regular events, preserve startTime and endTime when provided. "
            + "Use null for missing information. "
            + "Do not invent dates, times, or events. "

            + "IMPORTANT SHORT CALENDAR TITLE RULES: "
            + "Use concise, readable titles suitable for small calendar cells. "
            + "When a course code is provided, use COURSECODE followed by one space, "
            + "then a short event name, e.g. 'ECON1000 Test 1', "
            + "'EECS3311 Assignment 2', 'BIOLOGY1010 Lab Report 2', "
            + "'MATH2030 Quiz 3', 'EECS3221 Final Exam'. "
            + "Normalize spaces inside course codes (EECS 3311 -> EECS3311). "
            + "Do not add a hyphen after the course code. "
            + "Avoid redundant course names, descriptions, and words such as "
            + "'scheduled', 'number', 'submission deadline', and 'examination'. "
            + "Keep distinguishing details like event numbers and Lab Report versus Lab. "
            + "Do not invent course codes, event numbers, dates, or times. "
            + "If the document lacks a course code, use only the short event name. "
            + "Preserve all extracted dates, times, and event types. "

            + "Do not include Markdown formatting or code fences.\n\n"
            + extractedText
        );

        JsonArray parts = new JsonArray();
        parts.add(textPart);

        try {
            return sendRequest(parts, DOCUMENT_MODEL);

        } catch (RuntimeException e) {

            boolean overloaded =
                e.getMessage() != null
                && e.getMessage().contains("HTTP 503");

            boolean timedOut =
                e.getCause()
                instanceof java.net.http.HttpTimeoutException;

            if (overloaded || timedOut) {

                System.out.println(
                    "Document model unavailable or timed out. "
                    + "Switching to fast model..."
                );

                return sendRequest(parts, FAST_MODEL);
            }

            throw e;
        }
    }

    // ========================================
    // SEND GEMINI API REQUEST
    // ========================================

    private String sendRequest(JsonArray parts, String modelID) {

        if (!isConfigured()) {
            throw new IllegalStateException(
                "Gemini API key is not configured."
            );
        }

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contents);

        String url =
            "https://generativelanguage.googleapis.com/"
            + "v1beta/models/"
            + modelID
            + ":generateContent";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", apiKey)
            .timeout(Duration.ofSeconds(180))
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    requestBody.toString()
                )
            )
            .build();

        try {
            HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                    "Gemini API returned HTTP "
                    + response.statusCode()
                    + ": " + response.body()
                );
            }

            JsonObject result =
                JsonParser.parseString(response.body())
                    .getAsJsonObject();

            JsonArray candidates =
                result.getAsJsonArray("candidates");

            if (candidates == null || candidates.isEmpty()) {
                throw new RuntimeException(
                    "Gemini returned no response candidates."
                );
            }

            JsonObject candidate =
                candidates.get(0).getAsJsonObject();

            if (!candidate.has("content")) {
                throw new RuntimeException(
                    "Gemini returned no text content."
                );
            }

            JsonObject responseContent =
                candidate.getAsJsonObject("content");

            JsonArray responseParts =
                responseContent.getAsJsonArray("parts");

            if (responseParts == null || responseParts.isEmpty()) {
                throw new RuntimeException(
                    "Gemini response contains no parts."
                );
            }

            StringBuilder answer = new StringBuilder();

            for (var part : responseParts) {

                JsonObject obj = part.getAsJsonObject();

                if (obj.has("text")
                        && !obj.get("text").isJsonNull()) {

                    answer.append(
                        obj.get("text").getAsString()
                    );
                }
            }

            if (answer.isEmpty()) {
                throw new RuntimeException(
                    "Gemini returned an empty text response."
                );
            }

            return answer.toString();

        } catch (IOException e) {

            throw new RuntimeException(
                "Unable to connect to Gemini API.", e
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                "Gemini request was interrupted.", e
            );
        }
    }
}
