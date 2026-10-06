package com.maison.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TableRequest {

    private Long id;

    @JsonProperty("table_number")
    private String tableNumber;

    private Integer capacity;
    private String location;
    private String status;

    @JsonProperty("_token")
    private String token;

    public TableRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
