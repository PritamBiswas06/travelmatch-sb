package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.*;
import com.pvp.travelmatch.entity.*;
import com.pvp.travelmatch.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingHubService {
 private final TravelPlanRepository plans;
 private final UserRepository users;
 private final BookingOptionRepository options;
 private final TripExpenseRepository expenses;
 private final TripChecklistItemRepository checklist;

 @Transactional(readOnly=true)
 public List<BookingOptionResponse> getOptions(Long planId) {
  ownedPlan(planId, currentUser());
  return options.findByTravelPlanIdOrderByCreatedAtDesc(planId).stream().map(BookingOptionResponse::from).toList();
 }
 public BookingOptionResponse createOption(Long planId, BookingOptionRequest r) {
  User u=currentUser(); TravelPlan p=ownedPlan(planId,u); validateUrl(r.bookingUrl());
  BookingOption x=BookingOption.builder().travelPlan(p).createdBy(u).category(r.category())
   .providerName(r.providerName().trim()).bookingUrl(clean(r.bookingUrl())).estimatedPrice(r.estimatedPrice())
   .currency(r.currency()==null?"INR":r.currency()).travelersCovered(r.travelersCovered()).startAt(r.startAt())
   .endAt(r.endAt()).notes(clean(r.notes())).status(r.status()==null?"CONSIDERING":r.status())
   .preferred(Boolean.TRUE.equals(r.preferred())).build();
  if(Boolean.TRUE.equals(x.getPreferred())) clearPreferred(planId, x.getCategory(), null);
  return BookingOptionResponse.from(options.save(x));
 }
 public BookingOptionResponse updateOption(Long planId, Long optionId, BookingOptionRequest r) {
  User u=currentUser(); ownedPlan(planId,u);
  BookingOption x=options.findById(optionId).filter(o->o.getTravelPlan().getId().equals(planId))
   .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Booking option not found."));
  if(!x.getCreatedBy().getId().equals(u.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the creator can edit this option.");
  validateUrl(r.bookingUrl()); x.setCategory(r.category()); x.setProviderName(r.providerName().trim());
  x.setBookingUrl(clean(r.bookingUrl())); x.setEstimatedPrice(r.estimatedPrice()); x.setCurrency(r.currency()==null?"INR":r.currency());
  x.setTravelersCovered(r.travelersCovered()); x.setStartAt(r.startAt()); x.setEndAt(r.endAt()); x.setNotes(clean(r.notes()));
  if(r.status()!=null)x.setStatus(r.status()); x.setPreferred(Boolean.TRUE.equals(r.preferred()));
  if(Boolean.TRUE.equals(x.getPreferred())) clearPreferred(planId,x.getCategory(),x.getId());
  return BookingOptionResponse.from(options.save(x));
 }
 public void deleteOption(Long planId, Long optionId) {
  User u=currentUser(); TravelPlan p=ownedPlan(planId,u);
  BookingOption x=options.findById(optionId).filter(o->o.getTravelPlan().getId().equals(planId))
   .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Booking option not found."));
  if(!x.getCreatedBy().getId().equals(u.getId()) && !p.getUser().getId().equals(u.getId()))
   throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You cannot delete this option.");
  options.delete(x);
 }
 @Transactional(readOnly=true)
 public List<TripExpenseResponse> getExpenses(Long planId) {
  ownedPlan(planId,currentUser()); return expenses.findByTravelPlanIdOrderByExpenseDateDesc(planId).stream().map(TripExpenseResponse::from).toList();
 }
 public TripExpenseResponse addExpense(Long planId, TripExpenseRequest r) {
  User u=currentUser(); TravelPlan p=ownedPlan(planId,u);
  TripExpense x=TripExpense.builder().travelPlan(p).paidBy(u).description(r.description().trim()).amount(r.amount())
   .currency(r.currency()==null?"INR":r.currency()).category(r.category()==null?"OTHER":r.category()).expenseDate(r.expenseDate()).build();
  return TripExpenseResponse.from(expenses.save(x));
 }
 public void deleteExpense(Long planId, Long expenseId) {
  User u=currentUser(); TravelPlan p=ownedPlan(planId,u);
  TripExpense x=expenses.findById(expenseId).filter(e->e.getTravelPlan().getId().equals(planId))
   .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Expense not found."));
  if(!x.getPaidBy().getId().equals(u.getId()) && !p.getUser().getId().equals(u.getId()))
   throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You cannot delete this expense.");
  expenses.delete(x);
 }
 @Transactional(readOnly=true)
 public List<TripChecklistResponse> getChecklist(Long planId) {
  ownedPlan(planId,currentUser()); return checklist.findByTravelPlanIdOrderByCreatedAtAsc(planId).stream().map(TripChecklistResponse::from).toList();
 }
 public TripChecklistResponse addChecklistItem(Long planId, TripChecklistRequest r) {
  User u=currentUser(); TravelPlan p=ownedPlan(planId,u);
  return TripChecklistResponse.from(checklist.save(TripChecklistItem.builder().travelPlan(p).createdBy(u)
   .title(r.title().trim()).completed(Boolean.TRUE.equals(r.completed())).build()));
 }
 public TripChecklistResponse updateChecklistItem(Long planId, Long itemId, TripChecklistRequest r) {
  User u=currentUser(); TravelPlan p=ownedPlan(planId,u);
  TripChecklistItem x=checklist.findById(itemId).filter(i->i.getTravelPlan().getId().equals(planId))
   .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Checklist item not found."));
  if(!x.getCreatedBy().getId().equals(u.getId()) && !p.getUser().getId().equals(u.getId()))
   throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You cannot edit this checklist item.");
  x.setTitle(r.title().trim()); x.setCompleted(Boolean.TRUE.equals(r.completed())); return TripChecklistResponse.from(checklist.save(x));
 }
 public void deleteChecklistItem(Long planId, Long itemId) {
  User u=currentUser(); TravelPlan p=ownedPlan(planId,u);
  TripChecklistItem x=checklist.findById(itemId).filter(i->i.getTravelPlan().getId().equals(planId))
   .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Checklist item not found."));
  if(!x.getCreatedBy().getId().equals(u.getId()) && !p.getUser().getId().equals(u.getId()))
   throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You cannot delete this checklist item.");
  checklist.delete(x);
 }
 private void clearPreferred(Long planId,String category,Long exceptId) {
  options.findByTravelPlanIdOrderByCreatedAtDesc(planId).stream()
   .filter(o->o.getCategory().equals(category)&&Boolean.TRUE.equals(o.getPreferred())&&(exceptId==null||!o.getId().equals(exceptId)))
   .forEach(o->o.setPreferred(false));
 }
 private TravelPlan ownedPlan(Long id,User user) {
  TravelPlan p=plans.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Trip not found."));
  if(p.getUser()==null||!p.getUser().getId().equals(user.getId()))
   throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the trip owner can access this Booking Hub.");
  return p;
 }
 private User currentUser() {
  var a=SecurityContextHolder.getContext().getAuthentication();
  if(a==null||a.getName()==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sign in required.");
  return users.findByEmail(a.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"User not found."));
 }
 private String clean(String s){return s==null||s.isBlank()?null:s.trim();}
 private void validateUrl(String value) {
  if(value==null||value.isBlank())return;
  try { URI uri=URI.create(value.trim()); String scheme=uri.getScheme();
   if(uri.getHost()==null||scheme==null||(!scheme.equalsIgnoreCase("https")&&!scheme.equalsIgnoreCase("http")))
    throw new IllegalArgumentException();
  } catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Booking URL must be a valid HTTP or HTTPS URL.");}
 }
}
