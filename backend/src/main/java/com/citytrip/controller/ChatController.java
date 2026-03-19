package com.citytrip.controller;

import com.citytrip.common.Result;
import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.vo.ChatVO;
import com.citytrip.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatService chatService;

    @PostMapping("/qa")
    public Result<ChatVO> askQuestion(@RequestBody ChatReqDTO req) {
        log.info("【ChatController】收到前端发来的聊天请求, question={}, context={}", req.getQuestion(), req.getContext());
        ChatVO vo = chatService.answerQuestion(req);
        return Result.success(vo);
    }
}
