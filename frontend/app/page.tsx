"use client";

import {
  Conversation,
  ConversationContent,
  ConversationEmptyState,
  ConversationScrollButton,
} from "@/components/ai-elements/conversation";
import {
  Message,
  MessageContent,
  MessageResponse,
} from "@/components/ai-elements/message";
import {
  PromptInput,
  PromptInputBody,
  PromptInputFooter,
  PromptInputSubmit,
  PromptInputTextarea,
  type PromptInputMessage,
} from "@/components/ai-elements/prompt-input";
import {
  Tool,
  ToolContent,
  ToolHeader,
  ToolInput,
  ToolOutput,
} from "@/components/ai-elements/tool";
import { useChat } from "@ai-sdk/react";
import { DefaultChatTransport, type ToolUIPart } from "ai";
import { MailIcon } from "lucide-react";
import { useState } from "react";

export default function Home() {
  const [conversationId] = useState(() => crypto.randomUUID());

  const { messages, sendMessage, status } = useChat({
    transport: new DefaultChatTransport({
      api: "/api/chat",
      prepareSendMessagesRequest: ({ messages }) => {
        const last = messages.at(-1);
        const text = (last?.parts ?? [])
          .filter((p) => p.type === "text")
          .map((p) => (p as { text: string }).text)
          .join("");
        return { body: { message: text, conversationId } };
      },
    }),
  });

  const handleSubmit = (message: PromptInputMessage) => {
    if (!message.text?.trim()) return;
    sendMessage({ text: message.text });
  };

  return (
    <div className="mx-auto flex h-dvh w-full max-w-3xl flex-col gap-3 p-4">
      <header className="flex items-center gap-3">
        <div className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground">
          <MailIcon className="size-5" />
        </div>
        <div className="leading-tight">
          <h1 className="font-semibold">Inbox Assistant</h1>
          <p className="text-muted-foreground text-xs">
            Spring AI × AI Elements · streaming over SSE
          </p>
        </div>
      </header>

      <Conversation className="flex-1 rounded-xl border bg-card">
        <ConversationContent>
          {messages.length === 0 ? (
            <ConversationEmptyState
              icon={<MailIcon className="size-6" />}
              title="Ask about your inbox"
              description="Try: “any emails about invoices?”"
            />
          ) : (
            messages.map((m) => (
              <Message from={m.role} key={m.id}>
                <MessageContent>
                  {m.parts.map((part, i) => {
                    if (part.type === "text") {
                      return <MessageResponse key={i}>{part.text}</MessageResponse>;
                    }
                    if (part.type.startsWith("tool-")) {
                      const tool = part as ToolUIPart;
                      return (
                        <Tool key={i}>
                          <ToolHeader type={tool.type} state={tool.state} />
                          <ToolContent>
                            {tool.input !== undefined && (
                              <ToolInput input={tool.input} />
                            )}
                            <ToolOutput
                              output={tool.output}
                              errorText={tool.errorText}
                            />
                          </ToolContent>
                        </Tool>
                      );
                    }
                    return null;
                  })}
                </MessageContent>
              </Message>
            ))
          )}
        </ConversationContent>
        <ConversationScrollButton />
      </Conversation>

      <PromptInput onSubmit={handleSubmit}>
        <PromptInputBody>
          <PromptInputTextarea placeholder="Ask about your inbox…" />
        </PromptInputBody>
        <PromptInputFooter>
          <PromptInputSubmit status={status} />
        </PromptInputFooter>
      </PromptInput>
    </div>
  );
}
