package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.*;
import com.pvp.travelmatch.service.BookingHubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/travel/{planId}/booking-hub")
@RequiredArgsConstructor
public class BookingHubController {
 private final BookingHubService service;

 @GetMapping("/options") public List<BookingOptionResponse> options(@PathVariable Long planId){return service.getOptions(planId);}
 @PostMapping("/options") public BookingOptionResponse createOption(@PathVariable Long planId,@Valid @RequestBody BookingOptionRequest r){return service.createOption(planId,r);}
 @PutMapping("/options/{optionId}") public BookingOptionResponse updateOption(@PathVariable Long planId,@PathVariable Long optionId,@Valid @RequestBody BookingOptionRequest r){return service.updateOption(planId,optionId,r);}
 @DeleteMapping("/options/{optionId}") public void deleteOption(@PathVariable Long planId,@PathVariable Long optionId){service.deleteOption(planId,optionId);}

 @GetMapping("/expenses") public List<TripExpenseResponse> expenses(@PathVariable Long planId){return service.getExpenses(planId);}
 @PostMapping("/expenses") public TripExpenseResponse addExpense(@PathVariable Long planId,@Valid @RequestBody TripExpenseRequest r){return service.addExpense(planId,r);}
 @DeleteMapping("/expenses/{expenseId}") public void deleteExpense(@PathVariable Long planId,@PathVariable Long expenseId){service.deleteExpense(planId,expenseId);}

 @GetMapping("/checklist") public List<TripChecklistResponse> checklist(@PathVariable Long planId){return service.getChecklist(planId);}
 @PostMapping("/checklist") public TripChecklistResponse addChecklist(@PathVariable Long planId,@Valid @RequestBody TripChecklistRequest r){return service.addChecklistItem(planId,r);}
 @PutMapping("/checklist/{itemId}") public TripChecklistResponse updateChecklist(@PathVariable Long planId,@PathVariable Long itemId,@Valid @RequestBody TripChecklistRequest r){return service.updateChecklistItem(planId,itemId,r);}
 @DeleteMapping("/checklist/{itemId}") public void deleteChecklist(@PathVariable Long planId,@PathVariable Long itemId){service.deleteChecklistItem(planId,itemId);}
}
