package com.pvp.travelmatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AnalyticsEventRequest {
    @NotBlank
    @Pattern(regexp = "PAGE_VIEW|FEATURE_USE|SESSION_START|SESSION_HEARTBEAT|SESSION_END")
    private String eventType;

    @Size(max = 180)
    private String pagePath;

    @Size(max = 80)
    @Pattern(regexp = "[A-Za-z0-9 _./-]*")
    private String eventLabel;

    @NotBlank @Size(max = 64)
    @Pattern(regexp = "[A-Za-z0-9-]+")
    private String sessionId;
}
