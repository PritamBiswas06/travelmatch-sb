package com.pvp.travelmatch.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class SafetyCircleContactRequest {
    @NotBlank @Size(max = 80) private String name;
    @Size(max = 24) private String phone;
    @Email @Size(max = 180) private String email;
    public void setEmail(String email) { this.email = email == null || email.isBlank() ? null : email.trim(); }
    @Size(max = 40) private String relationship;
}
