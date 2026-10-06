package com.maison.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WaitlistRequest {

    private String action = "join";
    private String date;
    private String time;

    @JsonProperty("party_size")
    private Integer partySize = 2;

    private Long id;

    @JsonProperty("_token")
    private String token;

    public WaitlistRequest() {}

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public Integer getPartySize() { return partySize; }
    public void setPartySize(Integer partySize) { this.partySize = partySize; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
