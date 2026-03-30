package com.citytrip.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    private String provider = "low";
    private boolean fallbackToMock = true;
    private int timeoutSeconds = 20;
    private OpenAiProperties openai = new OpenAiProperties();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isFallbackToMock() {
        return fallbackToMock;
    }

    public void setFallbackToMock(boolean fallbackToMock) {
        this.fallbackToMock = fallbackToMock;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public OpenAiProperties getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAiProperties openai) {
        this.openai = openai;
    }

    public boolean isMockOnly() {
        return "mock".equalsIgnoreCase(provider);
    }

    public boolean isRealOnly() {
        return "real".equalsIgnoreCase(provider);
    }

    public boolean isAuto() {
        return "auto".equalsIgnoreCase(provider);
    }

    public boolean canTryReal() {
        return !isMockOnly() && openai != null && openai.isEnabled() && hasText(openai.getApiKey());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static class OpenAiProperties {
        private boolean enabled = true;
        private String apiKey;
        private String baseUrl = "https://api.555615.xyz/v1";
        private String model = "gpt-5.1";
        private double temperature = 0.7D;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
    }
}
