package com.pvp.travelmatch.dto;
import com.pvp.travelmatch.entity.BookingOption;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record BookingOptionResponse(Long id, Long createdById, String createdByName, String category,
 String providerName, String bookingUrl, BigDecimal estimatedPrice, String currency, Integer travelersCovered,
 LocalDateTime startAt, LocalDateTime endAt, String notes, String status, Boolean preferred,
 LocalDateTime createdAt, LocalDateTime updatedAt) {
 public static BookingOptionResponse from(BookingOption x) {
  return new BookingOptionResponse(x.getId(), x.getCreatedBy().getId(), x.getCreatedBy().getName(), x.getCategory(),
   x.getProviderName(), x.getBookingUrl(), x.getEstimatedPrice(), x.getCurrency(), x.getTravelersCovered(),
   x.getStartAt(), x.getEndAt(), x.getNotes(), x.getStatus(), x.getPreferred(), x.getCreatedAt(), x.getUpdatedAt());
 }
}
