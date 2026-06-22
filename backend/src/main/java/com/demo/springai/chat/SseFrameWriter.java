package com.demo.springai.chat;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

record SseFrameWriter(SseEmitter sseEmitter) implements FrameWriter {
    @Override
    public void write(String frame) throws IOException {
        synchronized (sseEmitter) {
            sseEmitter.send(SseEmitter.event().data(frame));
        }
    }
}
