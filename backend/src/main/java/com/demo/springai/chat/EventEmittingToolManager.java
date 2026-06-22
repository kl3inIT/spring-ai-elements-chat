package com.demo.springai.chat;

import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventEmittingToolManager implements ToolCallingManager {

    public static final String EVENTS_KEY = "toolEvents";

    private final ToolCallingManager delegate;
    private final ObjectMapper objectMapper;

    public EventEmittingToolManager(ToolCallingManager delegate, ObjectMapper objectMapper) {
        this.delegate = delegate;
        this.objectMapper = objectMapper;
    }

    @Override
    public @NonNull List<ToolDefinition> resolveToolDefinitions(@NonNull ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public @NonNull ToolExecutionResult executeToolCalls(@NonNull Prompt prompt, @NonNull ChatResponse chatResponse) {
        Consumer<Part> emit = sink(prompt);

        var generation = chatResponse.getResult();
        if (emit != null && generation != null) {
            for (AssistantMessage.ToolCall call : generation.getOutput().getToolCalls()) {
                emit.accept(new Part.ToolInputStart(call.id(), call.name()));
                emit.accept(new Part.ToolInputAvailable(call.id(), call.name(), parse(call.arguments())));
            }
        }

        ToolExecutionResult result = delegate.executeToolCalls(prompt, chatResponse);

        if (emit != null) {
            List<Message> history = result.conversationHistory();
            if (!history.isEmpty()
                    && history.getLast() instanceof ToolResponseMessage toolResponse) {
                for (ToolResponseMessage.ToolResponse r : toolResponse.getResponses()) {
                    emit.accept(new Part.ToolOutputAvailable(r.id(), parse(r.responseData())));
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Consumer<Part> sink(Prompt prompt) {
        if (prompt.getOptions() instanceof ToolCallingChatOptions opts) {
            Map<String, Object> toolContext = opts.getToolContext();
            if (toolContext != null) {
                Object value = toolContext.get(EVENTS_KEY);
                return value instanceof Consumer<?> c ? (Consumer<Part>) c : null;
            }
        }
        return null;
    }

    private Object parse(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception _) {
            return json;
        }
    }
}
