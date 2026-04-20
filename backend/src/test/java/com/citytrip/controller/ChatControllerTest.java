package com.citytrip.controller;

import com.citytrip.common.SystemBusyException;
import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ChatControllerTest {

    @Test
    void shouldFailFastWhenStreamingExecutorIsSaturated() {
        ChatService chatService = mock(ChatService.class);
        TaskExecutor rejectedExecutor = task -> {
            throw new TaskRejectedException("executor is full");
        };
        ChatController controller = new ChatController(chatService, rejectedExecutor);

        ChatReqDTO req = new ChatReqDTO();
        req.setQuestion("What should I do on a rainy day in Chengdu?");

        assertThatThrownBy(() -> controller.streamQuestion(req))
                .isInstanceOf(SystemBusyException.class);
    }
}