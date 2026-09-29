package com.pvp.travelmatch.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
@Data
public class PhoneVerifyRequest {
    @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "OTP must contain six digits")
    private String code;
}
