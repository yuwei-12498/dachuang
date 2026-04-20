package com.citytrip.controller;

import com.citytrip.annotation.LoginRequired;
import com.citytrip.common.SystemBusyException;
import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.vo.ChatStatusVO;
import com.citytrip.model.vo.ChatVO;
import com.citytrip.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/chat/messages")
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final long STREAM_TIMEOUT_MILLIS = 60_000L;

    private final ChatService chatService;
    private final TaskExecutor chatStreamExecutor;

    public ChatController(ChatService chatService,
                          @Qualifier("chatStreamExecutor") TaskExecutor chatStreamExecutor) {
        this.chatService = chatService;
        this.chatStreamExecutor = chatStreamExecutor;
    }

    @LoginRequired
    @PostMapping
    public ChatVO askQuestion(@RequestBody ChatReqDTO req) {
        log.info("Received chat request. questionLength={}, hasContext={}", questionLength(req), hasContext(req));
        return chatService.answerQuestion(req);
    }

    @LoginRequired
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamQuestion(@RequestBody ChatReqDTO req) {
        log.info("Received streaming chat request. questionLength={}, hasContext={}", questionLength(req), hasContext(req));
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        AtomicBoolean connectionOpen = new AtomicBoolean(true);
        emitter.onCompletion(() -> {
            connectionOpen.set(false);
            log.debug("Streaming chat request completed.");
        });
        emitter.onTimeout(() -> {
            connectionOpen.set(false);
            log.warn("Streaming chat request timed out.");
            emitter.complete();
        });
        emitter.onError(ex -> {
            connectionOpen.set(false);
            log.debug("Streaming chat connection closed. reason={}", ex == null ? "unknown" : ex.getMessage());
        });

        try {
            chatStreamExecutor.execute(() -> handleStreamingRequest(req, emitter, connectionOpen));
        } catch (TaskRejectedException ex) {
            log.warn("Streaming chat request rejected because the executor is saturated.");
            throw new SystemBusyException("当前聊天请求较多，请稍后重试");
        }
        return emitter;
    }

    @LoginRequired
    @GetMapping("/status")
    public ChatStatusVO getStatus() {
        return chatService.getStatus();
    }

    private void handleStreamingRequest(ChatReqDTO req, SseEmitter emitter, AtomicBoolean connectionOpen) {
        try {
            AtomicBoolean emittedAnyToken = new AtomicBoolean(false);
            ChatVO result = chatService.streamAnswer(req, token -> {
                if (sendEvent(emitter, tokenEvent(token), connectionOpen)) {
                    emittedAnyToken.set(true);
                }
            });
            if (!connectionOpen.get()) {
                emitter.complete();
                return;
            }
            if (!emittedAnyToken.get() && result != null && result.getAnswer() != null && !result.getAnswer().trim().isEmpty()) {
                sendEvent(emitter, tokenEvent(result.getAnswer()), connectionOpen);
            }
            sendEvent(emitter, metaEvent(result == null ? List.of() : result.getRelatedTips()), connectionOpen);
            sendEvent(emitter, doneEvent(), connectionOpen);
            emitter.complete();
        } catch (Exception ex) {
            if (!connectionOpen.get()) {
                log.debug("Streaming chat background task stopped after the client disconnected.");
                emitter.complete();
                return;
            }
            log.warn("Streaming chat request failed. reason={}", ex.getMessage(), ex);
            sendEvent(emitter, errorEvent(ex.getMessage()), connectionOpen);
            emitter.complete();
        }
    }

    private boolean sendEvent(SseEmitter emitter, Map<String, Object> payload, AtomicBoolean connectionOpen) {
        if (!connectionOpen.get()) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().data(payload, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException | IllegalStateException ex) {
            connectionOpen.set(false);
            log.debug("Failed to send SSE event because the connection is no longer writable. reason={}", ex.getMessage());
            return false;
        }
    }

    private Map<String, Object> tokenEvent(String token) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "token");
        payload.put("content", token);
        return payload;
    }

    private Map<String, Object> metaEvent(List<String> relatedTips) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "meta");
        payload.put("relatedTips", relatedTips == null ? List.of() : relatedTips);
        return payload;
    }

    private Map<String, Object> doneEvent() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "done");
        return payload;
    }

    private Map<String, Object> errorEvent(String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "error");
        payload.put("message", message == null || message.trim().isEmpty() ? "Streaming request failed" : message);
        return payload;
    }

    private int questionLength(ChatReqDTO req) {
        if (req == null || req.getQuestion() == null) {
            return 0;
        }
        return req.getQuestion().trim().length();
    }

    private boolean hasContext(ChatReqDTO req) {
        return req != null && req.getContext() != null;
    }
}
