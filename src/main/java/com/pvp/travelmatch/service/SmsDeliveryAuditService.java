package com.pvp.travelmatch.service;

import com.pvp.travelmatch.entity.SmsDeliveryLog;
import com.pvp.travelmatch.repository.SmsDeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SmsDeliveryAuditService {
    private final SmsDeliveryLogRepository logs;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String provider, String destination, String status, String messageId, String errorCode) {
        logs.save(SmsDeliveryLog.builder()
                .provider(safe(provider, 30))
                .recipientMasked(mask(destination))
                .deliveryStatus(safe(status, 20))
                .providerMessageId(safeNullable(messageId, 100))
                .providerErrorCode(safeNullable(errorCode, 40))
                .build());
    }

    private String mask(String destination) {
        if (destination == null || destination.isBlank()) return "unknown";
        String digits = destination.replaceAll("\\D", "");
        return "***" + (digits.length() >= 4 ? digits.substring(digits.length() - 4) : digits);
    }

    private String safe(String value, int max) {
        if (value == null || value.isBlank()) return "unknown";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String safeNullable(String value, int max) {
        if (value == null || value.isBlank()) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
