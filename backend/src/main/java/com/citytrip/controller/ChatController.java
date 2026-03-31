package com.citytrip.controller;

import com.citytrip.annotation.LoginRequired;
import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.vo.ChatStatusVO;
import com.citytrip.model.vo.ChatVO;
import com.citytrip.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/messages")
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @LoginRequired
    @PostMapping
    public ChatVO askQuestion(@RequestBody ChatReqDTO req) {
        log.info("Received chat request. question={}, context={}", req.getQuestion(), req.getContext());
        return chatService.answerQuestion(req);
    }

    @LoginRequired
    @GetMapping("/status")
    public ChatStatusVO getStatus() {
        return chatService.getStatus();
    }
}
