package com.maison.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateReviewRequest {

    private Long id;
    private String status;

    @JsonProperty("_token")
    private String token;

    public UpdateReviewRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
