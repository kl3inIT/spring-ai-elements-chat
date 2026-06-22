package com.demo.springai.chat;

import java.io.IOException;

@FunctionalInterface
public interface FrameWriter {
    void write(String frame) throws IOException;
}
