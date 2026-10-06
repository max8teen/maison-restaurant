package com.maison.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubmitReviewRequest {

    @JsonProperty("booking_id")
    private Long bookingId;

    private Integer rating;

    @JsonProperty("food_rating")
    private Integer foodRating;

    @JsonProperty("service_rating")
    private Integer serviceRating;

    @JsonProperty("ambience_rating")
    private Integer ambienceRating;

    private String comment;

    @JsonProperty("_token")
    private String token;

    public SubmitReviewRequest() {}

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public Integer getFoodRating() { return foodRating; }
    public void setFoodRating(Integer foodRating) { this.foodRating = foodRating; }

    public Integer getServiceRating() { return serviceRating; }
    public void setServiceRating(Integer serviceRating) { this.serviceRating = serviceRating; }

    public Integer getAmbienceRating() { return ambienceRating; }
    public void setAmbienceRating(Integer ambienceRating) { this.ambienceRating = ambienceRating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
