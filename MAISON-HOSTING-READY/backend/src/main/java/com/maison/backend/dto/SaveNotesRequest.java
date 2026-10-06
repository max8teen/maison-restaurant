package com.maison.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SaveNotesRequest {

    @JsonProperty("user_id")
    private Long userId;

    private String note;

    @JsonProperty("_token")
    private String token;

    public SaveNotesRequest() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
