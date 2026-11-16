package com.hotelbooking.dto;

public class ChatMessageRequest {
    private String sessionId;
    private String message;
    private Long hotelId;
    private String clientTimestamp;

    public ChatMessageRequest() {}

    public ChatMessageRequest(String sessionId, String message) {
        this.sessionId = sessionId;
        this.message = message;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }

    public String getClientTimestamp() { return clientTimestamp; }
    public void setClientTimestamp(String clientTimestamp) { this.clientTimestamp = clientTimestamp; }
}

