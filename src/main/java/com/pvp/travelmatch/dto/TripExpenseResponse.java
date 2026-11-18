package com.pvp.travelmatch.dto;
import com.pvp.travelmatch.entity.TripExpense;
import java.math.BigDecimal;
import java.time.LocalDate;
public record TripExpenseResponse(Long id, Long paidById, String paidByName, String description, BigDecimal amount,
 String currency, String category, LocalDate expenseDate) {
 public static TripExpenseResponse from(TripExpense x) { return new TripExpenseResponse(x.getId(),x.getPaidBy().getId(),
  x.getPaidBy().getName(),x.getDescription(),x.getAmount(),x.getCurrency(),x.getCategory(),x.getExpenseDate()); }
}
