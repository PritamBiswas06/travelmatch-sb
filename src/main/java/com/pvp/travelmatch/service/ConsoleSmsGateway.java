package com.pvp.travelmatch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Local-development adapter only. It never sends a real SMS. */
@Component
@ConditionalOnProperty(name = "travelmatch.sms.provider", havingValue = "console")
public class ConsoleSmsGateway implements SmsGateway {
    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsGateway.class);
    private final Environment environment;

    public ConsoleSmsGateway(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String sendOtp(String destination, String code) {
        boolean localOrTest = environment.acceptsProfiles(Profiles.of("local", "test"));
        boolean production = environment.acceptsProfiles(Profiles.of("prod"));
        if (!localOrTest || production) {
            log.error("Refusing console SMS mode outside an explicit local/test profile");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Phone verification is not configured for this environment. Please try again later.");
        }
        String lastFour = destination != null && destination.length() >= 4
                ? destination.substring(destination.length() - 4) : "unknown";
        // This is intentionally visible only in local development logs for testing.
        log.warn("[LOCAL SMS TEST MODE] TravelMatch OTP for phone ending {} is {}. No SMS was sent.", lastFour, code);
        return "LOCAL-TEST";
    }
}
