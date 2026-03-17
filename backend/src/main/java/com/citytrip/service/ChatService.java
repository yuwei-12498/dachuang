package com.citytrip.service;

import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.vo.ChatVO;

/**
 * 旅游助手问答服务
 */
public interface ChatService {
    ChatVO answerQuestion(ChatReqDTO req);
}
