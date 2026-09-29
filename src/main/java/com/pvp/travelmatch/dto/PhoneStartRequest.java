package com.pvp.travelmatch.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
@Data
public class PhoneStartRequest {
    @NotBlank @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$", message = "Enter a valid phone number in international format, e.g. +919876543210")
    private String phoneNumber;
}
