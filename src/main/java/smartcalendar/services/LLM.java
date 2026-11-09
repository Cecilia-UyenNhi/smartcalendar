
package smartcalendar.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;

public class LLM {

    // Gemini model
    private final String modelID = "gemini-3.8-flash";

    private final String apiKey;
    private final HttpClient client;

    // Constructor
    public LLM() {

        apiKey = System.getenv("GEMINI_API_KEY");

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    // Check whether an API key has been configured
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    // Interpret natural-language requests using Gemini
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
            return sendRequest(parts);

        } catch (RuntimeException e) {

            System.err.println(
                "Gemini text request failed: " + e.getMessage()
            );

            return "Sorry, I couldn't connect to Gemini right now. "
                 + "Please check your API key and internet connection.";
        }
    }

    // Analyze PNG or JPEG images using Gemini
    public String analyzeImage(File image) {

        if (!isConfigured()) {
            throw new IllegalStateException(
                "Gemini API key is required for image analysis."
            );
        }

        if (image == null || !image.isFile()) {
            throw new IllegalArgumentException(
                "Please provide a valid image file."
            );
        }

        return analyzeFile(
            image,
            "Extract all visible course names, calendar events, "
            + "assignment deadlines, tests, exams, dates, "
            + "times, and locations from this image. "
            + "Preserve the original information accurately. "
            + "Do not invent missing information."
        );
    }

    // Convert image into a Gemini API request
    private String analyzeFile(File file, String prompt) {

        String filename =
            file.getName().toLowerCase(Locale.ROOT);

        String mimeType;

        if (filename.endsWith(".png")) {
            mimeType = "image/png";

        } else if (filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")) {
            mimeType = "image/jpeg";

        } else {
            throw new IllegalArgumentException(
                "Only PNG and JPEG images are supported."
            );
        }

        try {
            byte[] bytes = Files.readAllBytes(file.toPath());

            // Keep inline image uploads reasonably small
            if (bytes.length > 10 * 1024 * 1024) {
                throw new IllegalArgumentException(
                    "Image must be smaller than 10 MB."
                );
            }

            String encodedImage =
                Base64.getEncoder().encodeToString(bytes);

            // Text instructions for Gemini
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", prompt);

            // Image content
            JsonObject inlineData = new JsonObject();
            inlineData.addProperty("mime_type", mimeType);
            inlineData.addProperty("data", encodedImage);

            JsonObject imagePart = new JsonObject();
            imagePart.add("inline_data", inlineData);

            // Combine instructions and image
            JsonArray parts = new JsonArray();
            parts.add(textPart);
            parts.add(imagePart);

            return sendRequest(parts);

        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to read image file.", e
            );
        }
    }

    // Send a request to the Gemini REST API
    private String sendRequest(JsonArray parts) {

        if (!isConfigured()) {
            throw new IllegalStateException(
                "Gemini API key is not configured."
            );
        }

        // Construct Gemini request JSON
        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contents);

        // Gemini API endpoint
        String url =
            "https://generativelanguage.googleapis.com/"
            + "v1beta/models/"
            + modelID
            + ":generateContent";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", apiKey)
            .timeout(Duration.ofSeconds(90))
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

            // Handle HTTP errors
            if (response.statusCode() != 200) {
                throw new RuntimeException(
                    "Gemini API returned HTTP "
                    + response.statusCode()
                    + ": " + response.body()
                );
            }

            // Parse JSON response
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
