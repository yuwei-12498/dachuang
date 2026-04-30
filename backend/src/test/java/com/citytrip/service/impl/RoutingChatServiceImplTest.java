package com.citytrip.service.impl;

import com.citytrip.config.LlmProperties;
import com.citytrip.model.vo.ChatStatusVO;
import com.citytrip.service.application.community.CommunitySemanticSearchService;
import com.citytrip.service.impl.vivo.VivoEmbeddingClient;
import com.citytrip.service.impl.vivo.VivoRerankClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingChatServiceImplTest {

    @Test
    void getStatusShouldExposeExtendedReadinessFields() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("real");
        properties.getOpenai().setApiKey("sk-test");
        properties.getOpenai().setBaseUrl("https://api-ai.vivo.com.cn/v1");
        properties.getOpenai().getChat().setModel("Doubao-Seed-2.0-mini");
        properties.getOpenai().getTool().setModel("Volc-DeepSeek-V3.2");

        RoutingChatServiceImpl service = new RoutingChatServiceImpl(null, new MockChatServiceImpl(), properties);
        ChatStatusVO status = service.getStatus();

        assertThat(status.isToolReady()).isTrue();
        assertThat(status.getWarnings()).isNotNull();
    }

    @Test
    void getStatusShouldNotReportSemanticReadyForStubVectorClients() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("real");
        properties.getOpenai().setApiKey("sk-test");
        properties.getOpenai().setBaseUrl("https://api-ai.vivo.com.cn/v1");
        properties.getOpenai().getChat().setModel("Doubao-Seed-2.0-mini");
        properties.getOpenai().getTool().setModel("Volc-DeepSeek-V3.2");

        RoutingChatServiceImpl service = new RoutingChatServiceImpl(null, new MockChatServiceImpl(), properties);
        CommunitySemanticSearchService semanticSearchService = new CommunitySemanticSearchService(
                new VivoEmbeddingClient(),
                new VivoRerankClient()
        );
        ReflectionTestUtils.setField(service, "communitySemanticSearchService", semanticSearchService);

        ChatStatusVO status = service.getStatus();

        assertThat(status.isEmbeddingReady()).isFalse();
        assertThat(status.isRerankReady()).isFalse();
    }
}
