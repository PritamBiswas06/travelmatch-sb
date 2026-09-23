package com.pvp.travelmatch.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ResetPasswordRequest {

    private String email;
    private String password;

    public ResetPasswordRequest() {
    }

    public ResetPasswordRequest(
            String email,
            String password
    ) {
        this.email = email;
        this.password = password;
    }

}