package com.maison.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SendOtpRequest {
    private String email;
    private String purpose;

    @JsonProperty("full_name")
    private String fullName;

    public SendOtpRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}
