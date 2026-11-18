package com.pvp.travelmatch.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record BookingOptionRequest(
 @NotBlank @Pattern(regexp="ACCOMMODATION|TRANSPORT|ACTIVITY|OTHER") String category,
 @NotBlank @Size(max=160) String providerName,
 @Size(max=1000) String bookingUrl,
 @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal estimatedPrice,
 @Pattern(regexp="[A-Z]{3}") String currency,
 @Min(1) @Max(100) Integer travelersCovered,
 LocalDateTime startAt, LocalDateTime endAt,
 @Size(max=1500) String notes,
 @Pattern(regexp="CONSIDERING|SHORTLISTED|READY_TO_BOOK|BOOKED|CANCELLED") String status,
 Boolean preferred) {}
