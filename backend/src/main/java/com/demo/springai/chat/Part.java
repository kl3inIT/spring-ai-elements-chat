package com.demo.springai.chat;

public sealed interface Part {

    record TextStart(String id) implements Part {
    }

    record TextDelta(String id, String delta) implements Part {
    }

    record TextEnd(String id) implements Part {
    }

    record ToolInputStart(String toolCallId, String toolName) implements Part {
    }

    record ToolInputAvailable(String toolCallId, String toolName, Object input) implements Part {
    }

    record ToolOutputAvailable(String toolCallId, Object output) implements Part {
    }
}
