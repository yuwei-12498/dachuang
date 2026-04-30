package com.citytrip.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmPropertiesTest {

    @Test
    void shouldReportToolConfigIssuesWhenToolSceneMissing() {
        LlmProperties properties = new LlmProperties();
        properties.getOpenai().setApiKey("sk-test");
        properties.getOpenai().setBaseUrl("https://api-ai.vivo.com.cn/v1");
        properties.getOpenai().setModel("Doubao-Seed-2.0-mini");
        properties.getOpenai().getChat().setModel("Doubao-Seed-2.0-mini");
        properties.getOpenai().getText().setModel("Doubao-Seed-2.0-pro");

        assertThat(properties.getRealToolConfigIssues())
                .contains("OPENAI_TOOL_MODEL is empty");
    }

    @Test
    void shouldWarnWhenConfiguredModelIsOutsideVivoAllowList() {
        LlmProperties properties = new LlmProperties();
        properties.getOpenai().setApiKey("sk-test");
        properties.getOpenai().setBaseUrl("https://api-ai.vivo.com.cn/v1");
        properties.getOpenai().getChat().setModel("BlueLM-7B-Chat");

        assertThat(properties.getRealModelConfigWarnings())
                .anyMatch(item -> item.contains("not in vivo allow-list"));
    }
}
