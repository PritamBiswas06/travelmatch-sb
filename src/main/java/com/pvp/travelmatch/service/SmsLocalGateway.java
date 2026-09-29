package com.pvp.travelmatch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

/** SMSLocal India SMS adapter. Requires an approved DLT entity, sender header and template. */
@Component
@ConditionalOnProperty(name = "travelmatch.sms.provider", havingValue = "smslocal")
public class SmsLocalGateway implements SmsGateway {
    private static final Logger log = LoggerFactory.getLogger(SmsLocalGateway.class);
    private static final Pattern INDIAN_E164 = Pattern.compile("^\\+91[6-9][0-9]{9}$");
    private static final Pattern MESSAGE_ID = Pattern.compile("^[0-9]{6,20}$");
    private static final Pattern ERROR_CODE = Pattern.compile("^[0-9]{3}$");

    private final String apiKey;
    private final String senderId;
    private final String templateId;
    private final String endpoint;
    private final HttpClient httpClient;

    public SmsLocalGateway(
            @Value("${travelmatch.sms.smslocal-api-key:}") String apiKey,
            @Value("${travelmatch.sms.smslocal-sender-id:}") String senderId,
            @Value("${travelmatch.sms.smslocal-template-id:}") String templateId,
            @Value("${travelmatch.sms.smslocal-endpoint:https://app.smslocal.in/api/smsapi}") String endpoint) {
        this.apiKey = apiKey;
        this.senderId = senderId;
        this.templateId = templateId;
        this.endpoint = endpoint;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public String sendOtp(String destination, String code) {
        if (blank(apiKey) || blank(senderId) || blank(templateId)) {
            log.error("SMSLocal provider selected but SMSLOCAL_API_KEY, SMSLOCAL_SENDER_ID or SMSLOCAL_TEMPLATE_ID is missing");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Phone verification is not configured yet. Please try again later.");
        }
        if (destination == null || !INDIAN_E164.matcher(destination).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The current SMS provider supports Indian mobile numbers only. Use +91 followed by a valid 10-digit number.");
        }

        String message = "Your TravelMatch verification code is " + code
                + ". It expires in 5 minutes. Do not share it.";
        String query = "key=" + enc(apiKey)
                + "&route=1"
                + "&sender=" + enc(senderId)
                + "&number=" + enc(destination.substring(3))
                + "&sms=" + enc(message)
                + "&templateid=" + enc(templateId);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + (endpoint.contains("?") ? "&" : "?") + query))
                    .timeout(Duration.ofSeconds(12))
                    .header("Accept", "text/plain")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body() == null ? "" : response.body().trim();

            if (response.statusCode() >= 200 && response.statusCode() < 300
                    && MESSAGE_ID.matcher(body).matches()) {
                // Log the provider reference only; never log the OTP, API key or full phone number.
                log.info("SMS OTP accepted by SMSLocal; providerMessageId={}", body);
                return body;
            }

            String safeCode = ERROR_CODE.matcher(body).matches() ? body : "HTTP_" + response.statusCode();
            log.warn("SMSLocal rejected OTP delivery; providerCode={}", safeCode);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    messageForProviderError(body, response.statusCode()));
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("SMSLocal OTP request interrupted");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to send the verification code right now.");
        } catch (Exception ex) {
            log.warn("SMSLocal OTP request failed: {}", ex.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to send the verification code right now.");
        }
    }

    private String messageForProviderError(String body, int status) {
        return switch (body) {
            case "101" -> "The SMS provider API key is invalid or revoked.";
            case "102" -> "The SMS sender ID is not approved. Check your DLT sender header.";
            case "103" -> "Enter a valid Indian mobile number.";
            case "108" -> "The SMS provider has no remaining credits. Please try again later.";
            case "110" -> "The SMS template was rejected. Check the approved DLT template ID and exact message text.";
            default -> status == 429
                    ? "The SMS provider is temporarily rate-limiting requests. Please try again shortly."
                    : "The SMS provider could not send the verification code. Check the DLT sender and template configuration.";
        };
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
