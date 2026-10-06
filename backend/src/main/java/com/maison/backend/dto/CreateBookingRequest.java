package com.maison.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateBookingRequest {

    @JsonProperty("guest_name")
    private String guestName;

    @JsonProperty("guest_email")
    private String guestEmail;

    @JsonProperty("guest_phone")
    private String guestPhone;

    @JsonProperty("reservation_date")
    private String reservationDate;

    @JsonProperty("reservation_time")
    private String reservationTime;

    @JsonProperty("party_size")
    private Integer partySize = 2;

    private String occasion = "none";

    @JsonProperty("special_requests")
    private String specialRequests;

    @JsonProperty("table_id")
    private Long tableId;

    @JsonProperty("_token")
    private String token;

    public CreateBookingRequest() {}

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }

    public String getGuestPhone() { return guestPhone; }
    public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; }

    public String getReservationDate() { return reservationDate; }
    public void setReservationDate(String reservationDate) { this.reservationDate = reservationDate; }

    public String getReservationTime() { return reservationTime; }
    public void setReservationTime(String reservationTime) { this.reservationTime = reservationTime; }

    public Integer getPartySize() { return partySize; }
    public void setPartySize(Integer partySize) { this.partySize = partySize; }

    public String getOccasion() { return occasion; }
    public void setOccasion(String occasion) { this.occasion = occasion; }

    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }

    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
