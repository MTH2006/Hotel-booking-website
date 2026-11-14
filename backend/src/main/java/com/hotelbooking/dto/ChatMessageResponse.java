package com.hotelbooking.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ChatMessageResponse {
    private String sessionId;
    private String reply;
    private String intent;
    private List<String> citations;
    private List<String> suggestions;
    private String toolCalled;
    private Map<String, Object> toolResult;
    private boolean handoffRecommended;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ChatMessageResponse() {}

    public ChatMessageResponse(String sessionId, String reply, String intent, List<String> citations, List<String> suggestions) {
        this.sessionId = sessionId;
        this.reply = reply;
        this.intent = intent;
        this.citations = citations;
        this.suggestions = suggestions;
        this.timestamp = LocalDateTime.now();
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public List<String> getCitations() { return citations; }
    public void setCitations(List<String> citations) { this.citations = citations; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public String getToolCalled() { return toolCalled; }
    public void setToolCalled(String toolCalled) { this.toolCalled = toolCalled; }

    public Map<String, Object> getToolResult() { return toolResult; }
    public void setToolResult(Map<String, Object> toolResult) { this.toolResult = toolResult; }

    public boolean isHandoffRecommended() { return handoffRecommended; }
    public void setHandoffRecommended(boolean handoffRecommended) { this.handoffRecommended = handoffRecommended; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

