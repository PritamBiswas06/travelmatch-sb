package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.AnalyticsEventRequest;
import com.pvp.travelmatch.entity.AnalyticsEvent;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.entity.Role;
import com.pvp.travelmatch.repository.AnalyticsEventRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final AnalyticsEventRepository events;
    private final UserRepository users;
    @Value("${travelmatch.analytics.retention-days:365}") private int retentionDays;

    private static final ZoneId ANALYTICS_ZONE = ZoneId.of("Asia/Kolkata");

    private static final Set<String> ALLOWED_LABELS = Set.of(
            "Home", "Dashboard", "Trip Search", "Trip View", "Trip Create", "Trip Update",
            "Partner Search", "Partner Request", "Partner Accept", "Profile View", "Profile Update",
            "Profile Photo Upload", "Saved Trips", "Travel DNA", "Travel Squads", "Reviews",
            "Notifications", "Chat Opened", "Payment Flow", "Registration", "Login", "Other"
    );

    @Transactional
    public void recordLogin(User user) {
        User managedUser = users.findById(user.getId()).orElseThrow();
        if (managedUser.getRole() == Role.ADMIN) return;
        events.save(AnalyticsEvent.builder().user(managedUser).eventType("LOGIN_SUCCESS")
                .pagePath("/login").eventLabel("Login")
                .sessionId("login-" + UUID.randomUUID()).createdAt(LocalDateTime.now(ANALYTICS_ZONE)).build());
    }

    @Transactional
    public void recordLoginFailure(User user) {
        User managedUser = users.findById(user.getId()).orElseThrow();
        if (managedUser.getRole() == Role.ADMIN) return;
        events.save(AnalyticsEvent.builder().user(managedUser).eventType("LOGIN_FAILED")
                .pagePath("/login").eventLabel("Login Failed")
                .sessionId("failed-" + UUID.randomUUID()).createdAt(LocalDateTime.now(ANALYTICS_ZONE)).build());
    }

    @Transactional
    public void recordLoginByUserId(Long userId) {
        User user = users.findById(userId).orElseThrow();
        if (user.getRole() == Role.ADMIN) return;
        events.save(AnalyticsEvent.builder().user(user).eventType("LOGIN_SUCCESS")
                .pagePath("/oauth2/callback").eventLabel("Login")
                .sessionId("login-" + UUID.randomUUID()).createdAt(LocalDateTime.now(ANALYTICS_ZONE)).build());
    }

    @Transactional
    public void recordRegistration(User user) {
        User managedUser = users.findById(user.getId()).orElseThrow();
        events.save(AnalyticsEvent.builder().user(managedUser).eventType("REGISTRATION_SUCCESS")
                .pagePath("/register").eventLabel("Registration")
                .sessionId("reg-" + UUID.randomUUID()).createdAt(LocalDateTime.now(ANALYTICS_ZONE)).build());
    }

    @Transactional
    public void record(String email, AnalyticsEventRequest request) {
        User user = users.findByEmail(email).orElse(null);
        if (user == null || user.getRole() == Role.ADMIN) return;
        String type = request.getEventType();
        String path = sanitizePath(request.getPagePath());
        String label = request.getEventLabel();
        if (label != null && !ALLOWED_LABELS.contains(label)) label = "Other";
        // Do not persist query strings, fragments, message content, or arbitrary payloads.
        events.save(AnalyticsEvent.builder().user(user).eventType(type).pagePath(path)
                .eventLabel(label).sessionId(request.getSessionId()).createdAt(LocalDateTime.now(ANALYTICS_ZONE)).build());
    }

    private String sanitizePath(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String path = raw.split("[?#]", 2)[0].trim();
        if (!path.startsWith("/")) return null;
        if (path.length() > 180) path = path.substring(0, 180);
        return path.replaceAll("[^A-Za-z0-9/_-]", "");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(int days) {
        int window = Math.max(1, Math.min(days, 365));
        LocalDateTime now = LocalDateTime.now(ANALYTICS_ZONE);
        LocalDateTime from = now.toLocalDate().minusDays(window - 1L).atStartOfDay();
        List<AnalyticsEvent> data = events.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(from, now.plusNanos(1));
        Set<Long> activeUsers = data.stream().map(e -> e.getUser().getId()).collect(Collectors.toSet());
        long logins = data.stream().filter(e -> "LOGIN_SUCCESS".equals(e.getEventType())).count();
        long failedLogins = data.stream().filter(e -> "LOGIN_FAILED".equals(e.getEventType())).count();
        long pageViews = data.stream().filter(e -> "PAGE_VIEW".equals(e.getEventType())).count();
        long sessions = data.stream().filter(e -> "SESSION_START".equals(e.getEventType())).count();
        long registrations = data.stream().filter(e -> "REGISTRATION_SUCCESS".equals(e.getEventType())).count();

        Map<LocalDate, Set<Long>> dailyActive = new TreeMap<>();
        Map<LocalDate, Long> dailyLogins = new TreeMap<>();
        Map<LocalDate, Long> dailyFailedLogins = new TreeMap<>();
        Map<LocalDate, Long> dailyRegistrations = new TreeMap<>();
        Map<String, Long> features = new HashMap<>();
        Map<String, Long> pages = new HashMap<>();
        Map<Long, List<AnalyticsEvent>> byUser = new HashMap<>();
        for (AnalyticsEvent e : data) {
            LocalDate day = e.getCreatedAt().toLocalDate();
            dailyActive.computeIfAbsent(day, k -> new HashSet<>()).add(e.getUser().getId());
            if ("LOGIN_SUCCESS".equals(e.getEventType())) dailyLogins.merge(day, 1L, Long::sum);
            if ("LOGIN_FAILED".equals(e.getEventType())) dailyFailedLogins.merge(day, 1L, Long::sum);
            if ("REGISTRATION_SUCCESS".equals(e.getEventType())) dailyRegistrations.merge(day, 1L, Long::sum);
            if ("FEATURE_USE".equals(e.getEventType())) features.merge(e.getEventLabel() == null ? "Other" : e.getEventLabel(), 1L, Long::sum);
            if ("PAGE_VIEW".equals(e.getEventType())) pages.merge(e.getPagePath() == null ? "/" : e.getPagePath(), 1L, Long::sum);
            byUser.computeIfAbsent(e.getUser().getId(), k -> new ArrayList<>()).add(e);
        }
        List<Map<String, Object>> daily = new ArrayList<>();
        for (int i = window - 1; i >= 0; i--) {
            LocalDate day = now.toLocalDate().minusDays(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", day.toString()); row.put("activeUsers", dailyActive.getOrDefault(day, Set.of()).size());
            row.put("logins", dailyLogins.getOrDefault(day, 0L)); row.put("failedLogins", dailyFailedLogins.getOrDefault(day, 0L)); row.put("registrations", dailyRegistrations.getOrDefault(day, 0L));
            daily.add(row);
        }
        long activeToday = dailyActive.getOrDefault(now.toLocalDate(), Set.of()).size();
        long activeNow = data.stream().filter(e -> !e.getCreatedAt().isBefore(now.minusMinutes(5)))
                .map(e -> e.getUser().getId()).distinct().count();
        long loginToday = dailyLogins.getOrDefault(now.toLocalDate(), 0L);
        long sessionSeconds = estimateSessionSeconds(data);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rangeDays", window); result.put("totalUsers", users.countByRole(Role.USER)); result.put("activeUsers", activeUsers.size());
        result.put("activeToday", activeToday); result.put("activeNow", activeNow); result.put("logins", logins); result.put("failedLogins", failedLogins); result.put("loginsToday", loginToday);
        result.put("sessions", sessions); result.put("pageViews", pageViews); result.put("registrations", registrations);
        result.put("averageSessionSeconds", sessions == 0 ? 0 : sessionSeconds / Math.max(1, sessions));
        result.put("daily", daily); result.put("topFeatures", topMap(features, 10)); result.put("topPages", topMap(pages, 10));
        result.put("users", userActivity(byUser));
        return result;
    }

    private long estimateSessionSeconds(List<AnalyticsEvent> data) {
        Map<String, List<AnalyticsEvent>> sessions = data.stream().collect(Collectors.groupingBy(AnalyticsEvent::getSessionId));
        long total = 0;
        for (List<AnalyticsEvent> list : sessions.values()) {
            LocalDateTime start = list.stream().filter(e -> "SESSION_START".equals(e.getEventType())).map(AnalyticsEvent::getCreatedAt).min(LocalDateTime::compareTo).orElse(null);
            LocalDateTime end = list.stream().map(AnalyticsEvent::getCreatedAt).max(LocalDateTime::compareTo).orElse(null);
            if (start != null && end != null) total += Math.max(0, Math.min(ChronoUnit.SECONDS.between(start, end), 8 * 60 * 60));
        }
        return total;
    }

    private List<Map<String, Object>> topMap(Map<String, Long> map, int limit) {
        return map.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(limit).map(e -> {
            Map<String, Object> row = new LinkedHashMap<>(); row.put("name", e.getKey()); row.put("count", e.getValue()); return row;
        }).toList();
    }

    private List<Map<String, Object>> userActivity(Map<Long, List<AnalyticsEvent>> byUser) {
        return byUser.entrySet().stream().map(entry -> {
            List<AnalyticsEvent> list = entry.getValue(); AnalyticsEvent latest = list.stream().max(Comparator.comparing(AnalyticsEvent::getCreatedAt)).orElseThrow();
            User user = latest.getUser();
            Map<String, Object> row = new LinkedHashMap<>(); row.put("userId", user.getId()); row.put("name", user.getName()); row.put("email", user.getEmail());
            row.put("logins", list.stream().filter(e -> "LOGIN_SUCCESS".equals(e.getEventType())).count());
            row.put("failedLogins", list.stream().filter(e -> "LOGIN_FAILED".equals(e.getEventType())).count());
            row.put("sessions", list.stream().filter(e -> "SESSION_START".equals(e.getEventType())).map(AnalyticsEvent::getSessionId).distinct().count());
            row.put("pageViews", list.stream().filter(e -> "PAGE_VIEW".equals(e.getEventType())).count()); row.put("lastActiveAt", latest.getCreatedAt().toString());
            row.put("lastAction", latest.getEventType()); row.put("lastPage", latest.getPagePath()); return row;
        }).sorted((a, b) -> String.valueOf(b.get("lastActiveAt")).compareTo(String.valueOf(a.get("lastActiveAt")))).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> activity(int days, Long userId, int page, int size) {
        int window = Math.max(1, Math.min(days, 365));
        LocalDateTime from = LocalDate.now(ANALYTICS_ZONE).minusDays(window - 1L).atStartOfDay();
        LocalDateTime to = LocalDateTime.now(ANALYTICS_ZONE).plusNanos(1);
        Page<AnalyticsEvent> result = userId == null
                ? events.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(from, to, PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))))
                : events.findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(userId, from, to, PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))));
        List<Map<String, Object>> content = result.getContent().stream().map(e -> {
            Map<String, Object> row = new LinkedHashMap<>(); row.put("id", e.getId()); row.put("userId", e.getUser().getId());
            row.put("name", e.getUser().getName()); row.put("email", e.getUser().getEmail()); row.put("eventType", e.getEventType());
            row.put("pagePath", e.getPagePath()); row.put("eventLabel", e.getEventLabel()); row.put("sessionId", e.getSessionId()); row.put("createdAt", e.getCreatedAt().toString()); return row;
        }).toList();
        Map<String, Object> out = new LinkedHashMap<>(); out.put("content", content); out.put("totalElements", result.getTotalElements());
        out.put("totalPages", result.getTotalPages()); out.put("page", result.getNumber()); out.put("size", result.getSize()); return out;
    }

    @Transactional
    public int purgeExpired() { return events.deleteOlderThan(LocalDateTime.now(ANALYTICS_ZONE).minusDays(Math.max(1, retentionDays))); }

    @Transactional(readOnly = true)
    public String csv(int days) {
        int window = Math.max(1, Math.min(days, 365));
        LocalDateTime from = LocalDate.now(ANALYTICS_ZONE).minusDays(window - 1L).atStartOfDay();
        List<AnalyticsEvent> data = events.findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(from);
        StringBuilder csv = new StringBuilder("event_id,user_id,name,email,event_type,page_path,feature,session_id,timestamp\r\n");
        for (AnalyticsEvent e : data) {
            csv.append(e.getId()).append(',').append(e.getUser().getId()).append(',')
                    .append(escapeCsv(e.getUser().getName())).append(',').append(escapeCsv(e.getUser().getEmail())).append(',')
                    .append(escapeCsv(e.getEventType())).append(',').append(escapeCsv(e.getPagePath())).append(',')
                    .append(escapeCsv(e.getEventLabel())).append(',').append(escapeCsv(e.getSessionId())).append(',')
                    .append(escapeCsv(e.getCreatedAt())).append("\r\n");
        }
        return csv.toString();
    }

    private String escapeCsv(Object value) { String s = value == null ? "" : String.valueOf(value); return "\"" + s.replace("\"", "\"\"") + "\""; }
}
