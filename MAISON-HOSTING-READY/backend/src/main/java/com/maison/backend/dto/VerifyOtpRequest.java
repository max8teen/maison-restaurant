package com.maison.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VerifyOtpRequest {
    private String email;
    private String otp;
    private String purpose;

    @JsonProperty("full_name")
    private String fullName;

    private String phone;
    private String password;

    public VerifyOtpRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
