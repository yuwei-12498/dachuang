package com.citytrip.controller;

import com.citytrip.common.Result;
import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.vo.ChatVO;
import com.citytrip.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/qa")
    public Result<ChatVO> askQuestion(@RequestBody ChatReqDTO req) {
        ChatVO vo = chatService.answerQuestion(req);
        return Result.success(vo);
    }
}
