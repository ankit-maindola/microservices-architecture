package com.example.meetingscheduler.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@Conditional(GeminiApiKeyCondition.class)
public class GeminiChatConfiguration {

    @Bean
    public Client googleGenAiClient(
            Environment environment,
            @Value("${meeting.gemini-http-timeout-ms:120000}") int httpTimeoutMs) {
        JvmTrustStoreConfigurer.ensureConfigured(environment);
        String apiKey = GeminiApiKeys.resolve(environment);
        return Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder().timeout(httpTimeoutMs).build())
                .build();
    }

    @Bean
    public ChatModel chatModel(
            Client googleGenAiClient,
            @Value("${spring.ai.google.genai.chat.options.model:gemini-2.0-flash}") String model,
            @Value("${spring.ai.google.genai.chat.options.temperature:0.1}") double temperature) {
        return GoogleGenAiChatModel.builder()
                .genAiClient(googleGenAiClient)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "meeting.gemini.log-requests", havingValue = "true")
    public GeminiOutboundLoggingAdvisor geminiOutboundLoggingAdvisor() {
        return new GeminiOutboundLoggingAdvisor();
    }

    @Bean
    public ChatClient.Builder chatClientBuilder(
            ChatModel chatModel,
            ObjectProvider<GeminiOutboundLoggingAdvisor> loggingAdvisor) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        GeminiOutboundLoggingAdvisor advisor = loggingAdvisor.getIfAvailable();
        if (advisor != null) {
            builder.defaultAdvisors(advisor);
        }
        return builder;
    }
}
