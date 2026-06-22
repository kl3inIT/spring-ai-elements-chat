<div align="center">

# Spring AI × AI Elements

### A modern, streaming AI chat — **Java/Spring on the backend, a polished React UI on the frontend.**

Connect a **Spring AI** backend to **Vercel AI Elements** (`useChat`) over Server‑Sent Events,
without rewriting your backend in Node.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Next.js](https://img.shields.io/badge/Next.js-16-black?logo=nextdotjs&logoColor=white)](https://nextjs.org/)
[![AI SDK](https://img.shields.io/badge/AI%20SDK-6-000000?logo=vercel&logoColor=white)](https://ai-sdk.dev/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

## ✨ Why this exists

Most "Spring AI streaming" examples stop at streaming **raw text**. But a modern AI UI —
[Vercel **AI Elements**](https://ai-sdk.dev/elements) driven by the `useChat` hook — doesn't consume raw
text or a Java object. It consumes a **specific streaming wire protocol** (the AI SDK *UI Message Stream*,
over SSE).

This repo is the small **bridge** that makes the two speak the same language:

> a Spring controller that takes Spring AI's streaming output (text **and** tool calls) and emits exactly
> the chunks AI Elements understands — so your Java backend and a best‑in‑class React AI UI just work together.

> 🧪 Extracted as a clean, standalone demo from patterns discovered while building **Zero Mail**.

---

## 🏗️ Architecture

```
┌─────────────────────────────┐         SSE  (UI Message Stream, "v1")        ┌──────────────────────────────┐
│  Next.js 16 + React 19       │  ◀═══════════════════════════════════════════ │  Spring Boot 4.1 (servlet)    │
│  AI Elements + useChat       │                                                │                              │
│  DefaultChatTransport ──────▶│  POST /api/chat   { message }                  │  ChatController (SseEmitter) │
│                              │                                                │     │                        │
└─────────────────────────────┘                                                │     ▼ subscribe              │
        ▲ renders parts:                                                        │  Spring AI ChatClient.stream │
        text · tool · reasoning                                                 │     │  Flux<String> + @Tool     │
                                                                                │     ▼                        │
                                                                                │  UiMessageStreamEmitter      │
                                                                                │  (text-delta / tool / [DONE])│
                                                                                └──────────────────────────────┘
```

**Key idea:** `Flux` is how the server *produces* tokens (inside the JVM); **SSE** is how they *travel* to the
browser. The emitter is the translation layer that frames Spring AI output as the AI SDK's UI Message Stream.

---

## 🧰 Tech stack

| Layer | Tech | Version |
|-------|------|---------|
| Backend | Spring Boot | 4.1 |
| | Spring AI (OpenAI) | 2.0 |
| | Java | 25 |
| | Build | Gradle |
| Frontend | Next.js (App Router) | 16 |
| | React | 19 |
| | Vercel AI SDK (`ai`, `@ai-sdk/react`) | 6 |
| | AI Elements | shadcn/ui registry |
| Transport | Server‑Sent Events (AI SDK UI Message Stream `v1`) | — |

---

## 📁 Project structure

```
spring-ai-elements-chat/
├── backend/                         # Spring Boot 4.1 · Spring AI 2.0 · Java 25 (Gradle)
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/demo/springai/
│       │   ├── SpringAiElementsApplication.java
│       │   ├── config/
│       │   │   ├── ChatClientConfig.java       # ChatClient + system prompt + tools
│       │   │   ├── WebCorsConfig.java          # CORS for the Next.js dev origin
│       │   │   └── ChatStreamConfig.java       # SSE keepalive scheduler
│       │   ├── chat/
│       │   │   ├── ChatController.java         # POST /api/chat  → SseEmitter
│       │   │   ├── ChatStreamService.java      # merge text Flux + tool events
│       │   │   ├── dto/ChatRequest.java
│       │   │   └── stream/UiMessageStreamEmitter.java   # UI Message Stream encoder
│       │   └── tools/WeatherTool.java          # @Tool demo (one tool call)
│       └── resources/application.yml           # spring.ai.openai.* · server.port
│
└── frontend/                        # Next.js 16 · React 19 · AI SDK 6
    ├── app/
    │   ├── page.tsx                 # chat page (useChat + AI Elements)
    │   ├── layout.tsx
    │   └── globals.css
    ├── components/ai-elements/      # installed via the shadcn registry
    └── next.config.ts               # dev rewrite /api → :8080 (avoids CORS in dev)
```

> Backend is scaffolded. Frontend + the bridge code land next — see the [roadmap](#-roadmap).

---

## 🚀 Getting started

### Prerequisites
- **JDK 25**
- **Node.js 20+**
- An **OpenAI API key**

### 1. Backend

```bash
cd backend

# provide your key (never commit it)
export OPENAI_API_KEY=sk-...        # Windows PowerShell: $env:OPENAI_API_KEY="sk-..."

./gradlew bootRun                   # starts on http://localhost:8080
```

### 2. Frontend

```bash
cd frontend
npm install
npm run dev                         # starts on http://localhost:3000
```

Open **http://localhost:3000** and chat. Replies stream in token by token, and the demo tool renders
live in the conversation.

---

## 🔌 The wire protocol (the interesting bit)

`useChat` (via `DefaultChatTransport`) opens the SSE stream and parses **UI Message Stream** chunks.
The backend emits, per turn:

```
data: {"type":"start","messageId":"..."}
data: {"type":"text-start","id":"0"}
data: {"type":"text-delta","id":"0","delta":"Hello"}
data: {"type":"tool-input-available","toolCallId":"...","toolName":"getWeather","input":{...}}
data: {"type":"tool-output-available","toolCallId":"...","output":{...}}
data: {"type":"text-end","id":"0"}
data: {"type":"finish"}
data: [DONE]
```

Response header: `x-vercel-ai-ui-message-stream: v1`.

---

## 🗺️ Roadmap

- [x] Backend scaffold (Spring Boot 4.1 · Spring AI 2.0 · Java 25 · Gradle)
- [ ] `UiMessageStreamEmitter` + `ChatController` (SSE bridge)
- [ ] Demo `@Tool` (weather) surfaced as a tool part
- [ ] Next.js frontend with AI Elements + `useChat`
- [ ] End‑to‑end run + demo GIF

---

## 📄 License

MIT — see [LICENSE](LICENSE).

<div align="center">
<sub>Built by <a href="https://www.linkedin.com/in/kl3init/">Đạt Phan</a> · Spring AI without leaving the JVM.</sub>
</div>
