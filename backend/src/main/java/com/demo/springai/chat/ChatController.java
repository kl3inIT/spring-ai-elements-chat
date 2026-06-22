package com.demo.springai.chat;

import com.demo.springai.tools.EmailTools;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.function.Consumer;

@RestController
@RequestMapping("/api")
@SuppressWarnings("UnknownHttpHeader")
public class ChatController {

    private static final String UI_MESSAGE_STREAM_HEADER = "x-vercel-ai-ui-message-stream";
    private static final String TEXT_PART_ID = "0";
    private static final String DEFAULT_CONVERSATION_ID = "default";

    private final ChatClient chatClient;
    private final EmailTools emailTools;
    private final ObjectMapper objectMapper;

    public ChatController(ChatClient chatClient, EmailTools emailTools, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.emailTools = emailTools;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request, HttpServletResponse response) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message must not be blank");
        }

        response.setHeader(UI_MESSAGE_STREAM_HEADER, "v1");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform");

        String conversationId = StringUtils.hasText(request.conversationId()) ? request.conversationId() : DEFAULT_CONVERSATION_ID;

        SseEmitter emitter = new SseEmitter(120_000L);
        UiMessageStream.pipe(streamTurn(request.message(), conversationId), emitter, objectMapper);
        return emitter;
    }

    private Flux<Part> streamTurn(String userMessage, String conversationId) {
        Sinks.Many<Part> toolEvents = Sinks.many().unicast().onBackpressureBuffer();
        Consumer<Part> emit = toolEvents::tryEmitNext;

        Flux<Part> deltas = chatClient
                .prompt()
                .user(userMessage)
                .tools(emailTools)
                .toolContext(Map.of(EventEmittingToolManager.EVENTS_KEY, emit))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .<Part>map(token -> new Part.TextDelta(TEXT_PART_ID, token))
                .doFinally(_ -> toolEvents.tryEmitComplete());

        Flux<Part> text = Flux.concat(Flux.just(new Part.TextStart(TEXT_PART_ID)), deltas, Flux.just(new Part.TextEnd(TEXT_PART_ID)));

        return Flux.merge(text, toolEvents.asFlux());
    }
}
