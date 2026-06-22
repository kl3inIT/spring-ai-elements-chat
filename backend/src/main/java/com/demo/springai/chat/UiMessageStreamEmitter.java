package com.demo.springai.chat;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class UiMessageStreamEmitter {

    private final FrameWriter frameWriter;
    private final ObjectMapper objectMapper;
    private final String messageId = UUID.randomUUID().toString();
    private final AtomicBoolean started = new AtomicBoolean(false);

    public UiMessageStreamEmitter(FrameWriter frameWriter, ObjectMapper objectMapper) {
        this.frameWriter = frameWriter;
        this.objectMapper = objectMapper;
    }

    public void write(Part part) {
        emitStartIfNeeded();
        writeJsonFrame(toPayload(part));
    }

    public void finish() {
        emitStartIfNeeded();
        writeJsonFrame(Map.of("type", "finish"));
    }

    public void error(String userFacingMessage) {
        emitStartIfNeeded();
        writeJsonFrame(fields("type", "error",
                "errorText", userFacingMessage == null ? "The stream failed." : userFacingMessage));
    }

    public void done() {
        writeFrame("[DONE]");
    }

    private static Map<String, Object> toPayload(Part part) {
        return switch (part) {
            case Part.TextStart p -> fields("type", "text-start", "id", p.id());
            case Part.TextDelta p -> fields("type", "text-delta", "id", p.id(),
                    "delta", p.delta() == null ? "" : p.delta());
            case Part.TextEnd p -> fields("type", "text-end", "id", p.id());
            case Part.ToolInputStart p -> fields("type", "tool-input-start",
                    "toolCallId", p.toolCallId(), "toolName", p.toolName());
            case Part.ToolInputAvailable p -> fields("type", "tool-input-available",
                    "toolCallId", p.toolCallId(), "toolName", p.toolName(), "input", p.input());
            case Part.ToolOutputAvailable p -> fields("type", "tool-output-available",
                    "toolCallId", p.toolCallId(), "output", p.output());
        };
    }

    private void emitStartIfNeeded() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        writeJsonFrame(fields("type", "start", "messageId", messageId));
    }

    private void writeJsonFrame(Map<String, ?> payload) {
        writeFrame(objectMapper.writeValueAsString(payload));
    }

    private void writeFrame(String frame) {
        try {
            frameWriter.write(frame);
        } catch (IOException e) {
            throw new IllegalStateException("could not send SSE frame", e);
        }
    }

    private static Map<String, Object> fields(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
