package com.pvp.travelmatch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Stable facade used by TrustCenterService; switch vendors without changing OTP logic. */
@Service
@Slf4j
public class TrustSmsService {
    private final SmsGateway gateway;
    private final SmsDeliveryAuditService audit;
    private final String provider;

    public TrustSmsService(SmsGateway gateway, SmsDeliveryAuditService audit,
                           @Value("${travelmatch.sms.provider:console}") String provider) {
        this.gateway = gateway;
        this.audit = audit;
        this.provider = provider;
    }

    public void sendOtp(String destination, String code) {
        try {
            String providerMessageId = gateway.sendOtp(destination, code);
            recordSafely(destination, "ACCEPTED", providerMessageId, null);
        } catch (ResponseStatusException ex) {
            recordSafely(destination, "FAILED", null, "HTTP_" + ex.getStatusCode().value());
            throw ex;
        } catch (RuntimeException ex) {
            recordSafely(destination, "FAILED", null, HttpStatus.BAD_GATEWAY.toString());
            throw ex;
        }
    }

    private void recordSafely(String destination, String status, String messageId, String errorCode) {
        try {
            audit.record(provider, destination, status, messageId, errorCode);
        } catch (RuntimeException ex) {
            // Audit persistence should not prevent an otherwise valid OTP send or hide the provider result.
            log.error("Could not persist SMS delivery audit record (provider={}, status={})", provider, status);
        }
    }
}
