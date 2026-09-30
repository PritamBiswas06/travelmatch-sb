package com.pvp.travelmatch.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AnalyticsMaintenanceService {
    private final AnalyticsService analytics;
    private final EmailService emailService;

    @Value("${travelmatch.analytics.daily-email-enabled:false}") private boolean emailEnabled;
    @Value("${travelmatch.analytics.email-recipients:}") private String recipients;

    @Scheduled(cron = "0 15 3 * * *", zone = "Asia/Kolkata")
    public void purgeOldEvents() {
        int deleted = analytics.purgeExpired();
        if (deleted > 0) System.out.println("Analytics retention cleanup removed " + deleted + " expired events.");
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void sendDailySummary() {
        if (!emailEnabled || recipients == null || recipients.isBlank()) return;
        var data = analytics.summary(1);
        String html = "<h2>TravelMatch daily analytics — " + LocalDate.now(ZoneId.of("Asia/Kolkata")) + "</h2>"
                + "<p>Active users: " + data.get("activeToday") + "</p>"
                + "<p>Successful logins: " + data.get("loginsToday") + "</p>"
                + "<p>Failed login attempts: " + data.get("failedLogins") + "</p>"
                + "<p>Registrations: " + data.get("registrations") + "</p>"
                + "<p>Page views: " + data.get("pageViews") + "</p>";
        Arrays.stream(recipients.split(",")).map(String::trim).filter(s -> !s.isBlank()).forEach(email -> {
            try { emailService.sendHtmlEmail(email, "TravelMatch daily analytics — " + LocalDate.now(ZoneId.of("Asia/Kolkata")), html); }
            catch (Exception ex) { System.err.println("Daily analytics email failed for configured recipient."); }
        });
    }
}
