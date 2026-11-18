package com.pvp.travelmatch.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record TripExpenseRequest(
 @NotBlank @Size(max=160) String description,
 @NotNull @DecimalMin(value="0.01") @Digits(integer=10,fraction=2) BigDecimal amount,
 @Pattern(regexp="[A-Z]{3}") String currency,
 @Pattern(regexp="ACCOMMODATION|TRANSPORT|ACTIVITY|FOOD|OTHER") String category,
 @NotNull LocalDate expenseDate) {}
