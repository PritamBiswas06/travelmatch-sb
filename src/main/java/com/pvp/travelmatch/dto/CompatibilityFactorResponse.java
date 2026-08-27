package com.pvp.travelmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// One line of the "Why this match?" breakdown, e.g. "Destination: Excellent".
// Only factors that could actually be evaluated (both sides had the
// relevant data) are included.
@Data
@AllArgsConstructor
public class CompatibilityFactorResponse {
    private String label;   // e.g. "Destination", "Travel Style"
    private String rating;  // Excellent / Very Good / Good / Fair / Poor
}