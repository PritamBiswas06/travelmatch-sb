package com.pvp.travelmatch.dto;
import jakarta.validation.constraints.*;
public record TripChecklistRequest(@NotBlank @Size(max=180) String title, Boolean completed) {}
