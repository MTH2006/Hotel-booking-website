package com.hotelbooking.dto;

public class HandoffRequestDto {
    private String sessionId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String questionSummary;

    public HandoffRequestDto() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getQuestionSummary() { return questionSummary; }
    public void setQuestionSummary(String questionSummary) { this.questionSummary = questionSummary; }
}

