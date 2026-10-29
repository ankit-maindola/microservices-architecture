package com.example.meetingscheduler.ai;

import com.example.meetingscheduler.config.ConditionalOnGeminiApiKey;
import com.example.meetingscheduler.config.GeminiPromptLogger;
import com.example.meetingscheduler.exception.AiProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@ConditionalOnGeminiApiKey
public class MeetingIntentParser {

    private static final Logger log = LoggerFactory.getLogger(MeetingIntentParser.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final ZoneId defaultTimeZone;
    private final GoogleGenAiChatOptions intentCallOptions;
    private final boolean logGeminiRequests;

    public MeetingIntentParser(
            ChatClient.Builder chatClientBuilder,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${meeting.default-time-zone:UTC}") String defaultTimeZone,
            @Value("${meeting.intent-parser.model:gemini-2.0-flash}") String intentModel,
            @Value("${meeting.intent-parser.max-output-tokens:512}") int maxOutputTokens,
            @Value("${meeting.gemini.log-requests:true}") boolean logGeminiRequests) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.defaultTimeZone = ZoneId.of(defaultTimeZone);
        this.intentCallOptions = GoogleGenAiChatOptions.builder()
                .model(intentModel)
                .temperature(0.0)
                .maxOutputTokens(maxOutputTokens)
                .responseMimeType("application/json")
                .build();
        this.logGeminiRequests = logGeminiRequests;
    }

    public MeetingIntent parse(String prompt, ZoneId requestTimeZone) {
        ZoneId zoneId = requestTimeZone != null ? requestTimeZone : defaultTimeZone;
        String now = FORMATTER.format(clock.instant().atZone(zoneId));
        String system = systemPrompt(now, zoneId);
        String fullSystem = system + "\n\n" + MeetingIntentJsonSupport.RESPONSE_SHAPE;

        GeminiPromptLogger.logIntentParseRequest(logGeminiRequests, intentCallOptions, fullSystem, prompt);

        long started = System.nanoTime();
        try {
            String content = chatClient.prompt()
                    .options(intentCallOptions)
                    .system(fullSystem)
                    .user(prompt)
                    .call()
                    .content();
            GeminiPromptLogger.logIntentParseResponse(logGeminiRequests, content);
            MeetingIntent intent = MeetingIntentJsonSupport.read(objectMapper, content);
            MeetingIntent validated = validateIntent(intent);
            log.info("Gemini intent parsed in {} ms (model={})", elapsedMs(started), intentCallOptions.getModel());
            return validated;
        } catch (Exception ex) {
            log.error("Failed to interpret natural-language request after {} ms", elapsedMs(started), ex);
            String root = AiErrors.rootMessage(ex);
            String message = root != null
                    ? "Failed to interpret the natural-language request: " + root
                    : "Failed to interpret the natural-language request.";
            throw new AiProcessingException(message, ex);
        }
    }

    private MeetingIntent validateIntent(MeetingIntent intent) {
        if (intent == null) {
            throw new AiProcessingException("Gemini returned no structured intent.");
        }
        if (intent.getIntentType() == null) {
            throw new AiProcessingException("Gemini response missing intentType.");
        }
        return intent;
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    private String systemPrompt(String now, ZoneId zoneId) {
        return """
                Extract meeting scheduling intent from the user message.
                Now in zone %s: %s. Resolve "tomorrow" and clock times in that zone.
                Do not invent emails. Missing data → intentType CLARIFICATION_REQUIRED with missingFields and clarificationQuestion.
                intentType: SCHEDULE, CHECK_AVAILABILITY, LIST_MEETINGS, CANCEL_MEETING, UPDATE_MEETING, or CLARIFICATION_REQUIRED.
                """.formatted(zoneId.getId(), now);
    }
}
