package com.example.meetingscheduler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;

/**
 * Logs outbound ChatClient payloads (what Spring AI sends to Gemini). API keys are never logged.
 */
public class GeminiOutboundLoggingAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(GeminiOutboundLoggingAdvisor.class);

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        log.info("Gemini outbound request:\n{}", formatRequest(request));
        ChatClientResponse response = chain.nextCall(request);
        if (log.isDebugEnabled()) {
            log.debug("Gemini inbound response:\n{}", formatResponse(response));
        }
        return response;
    }

    @Override
    public String getName() {
        return GeminiOutboundLoggingAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    static String formatRequest(ChatClientRequest request) {
        StringBuilder out = new StringBuilder();
        Prompt prompt = request.prompt();
        if (prompt != null) {
            ChatOptions options = prompt.getOptions();
            if (options != null) {
                out.append("options: ").append(options).append('\n');
            }
            if (prompt.getInstructions() != null) {
                for (Message message : prompt.getInstructions()) {
                    out.append(message.getMessageType()).append(": ").append(message.getText()).append('\n');
                }
            }
        }
        if (!request.context().isEmpty()) {
            out.append("context: ").append(request.context()).append('\n');
        }
        return out.toString().trim();
    }

    private static String formatResponse(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null) {
            return "(empty)";
        }
        return String.valueOf(response.chatResponse());
    }
}
