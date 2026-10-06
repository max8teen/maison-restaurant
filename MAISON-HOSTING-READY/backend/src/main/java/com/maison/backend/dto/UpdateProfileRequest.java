package com.maison.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateProfileRequest {

    @JsonProperty("full_name")
    private String fullName;

    private String phone;
    private String gender;
    private String dob;

    @JsonProperty("_token")
    private String token;

    public UpdateProfileRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
