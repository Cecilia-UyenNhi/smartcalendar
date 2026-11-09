
package smartcalendar.services;

import java.util.Objects;

public class AIAgent {

    private final LLM llm;
    private final CalendarFacade facade;

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
            You are Sissy, the AI assistant for SmartCalendar,
            a calendar and scheduling application.

            Your responsibilities include:
            - Helping users organize their schedules.
            - Understanding natural-language calendar requests.
            - Identifying event titles, dates, times, and locations.
            - Answering questions about scheduling.
            - Helping users manage deadlines and appointments.

            Important rules:
            - Do not invent dates, times, or event details.
            - If information is missing, ask the user to clarify.
            - Be concise, friendly, and helpful.
            - Do not claim to have created, edited, or deleted
              a calendar event unless the application has
              actually completed that operation.
            - You cannot directly access the calendar database.
            - Do not claim to know the user's calendar events
              unless that information was explicitly provided.

            User request:
            """ + request;

        return llm.interpret(prompt);
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
