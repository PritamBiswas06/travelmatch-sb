package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.TravelSquadCreateRequest;
import com.pvp.travelmatch.dto.TravelSquadInviteRequest;
import com.pvp.travelmatch.dto.TravelSquadMemberResponse;
import com.pvp.travelmatch.dto.TravelSquadMessageRequest;
import com.pvp.travelmatch.dto.TravelSquadMessageResponse;
import com.pvp.travelmatch.dto.TravelSquadResponse;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.TravelSquad;
import com.pvp.travelmatch.entity.TravelSquadMember;
import com.pvp.travelmatch.entity.TravelSquadMessage;
import com.pvp.travelmatch.entity.NotificationType;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.TravelPartnerRepository;
import com.pvp.travelmatch.repository.TravelPlanRepository;
import com.pvp.travelmatch.repository.TravelSquadMemberRepository;
import com.pvp.travelmatch.repository.TravelSquadMessageRepository;
import com.pvp.travelmatch.repository.TravelSquadRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelSquadService {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 30;
    private static final int MAX_MESSAGE_PAGE_SIZE = 50;

    private final TravelSquadRepository squadRepository;
    private final TravelSquadMemberRepository memberRepository;
    private final TravelSquadMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final TravelPartnerRepository travelPartnerRepository;
    private final BlockedUserService blockedUserService;
    private final NotificationService notificationService;

    // =========================================================
    // CURRENT USER
    // =========================================================

    private User getCurrentUser() {
        String email = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));
    }

    // =========================================================
    // LIST MY SQUADS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<TravelSquadResponse> getMySquads(int page, int size) {
        User currentUser = getCurrentUser();
        Page<TravelSquad> squads = squadRepository.findMyActiveSquads(
                currentUser.getId(),
                PageRequest.of(safePage(page), safePageSize(size))
        );

        return squads.map(squad -> toResponse(
                squad,
                currentUser,
                memberRepository.findBySquadIdAndStatusOrderByJoinedAtAsc(squad.getId(), "ACTIVE")
        ));
    }

    // =========================================================
    // INVITATIONS
    // =========================================================

    @Transactional(readOnly = true)
    public List<TravelSquadResponse> getMyInvitations() {
        User currentUser = getCurrentUser();

        return memberRepository
                .findByUserIdAndStatusOrderByUpdatedAtDesc(
                        currentUser.getId(),
                        "INVITED",
                        PageRequest.of(0, 30)
                )
                .getContent()
                .stream()
                .map(member -> toResponse(
                        member.getSquad(),
                        currentUser,
                        memberRepository.findBySquadIdAndStatusOrderByJoinedAtAsc(
                                member.getSquad().getId(),
                                "ACTIVE"
                        )
                ))
                .toList();
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Transactional
    public TravelSquadResponse createSquad(TravelSquadCreateRequest request) {
        User currentUser = getCurrentUser();

        TravelPlan plan = null;
        if (request.getTravelPlanId() != null) {
            plan = travelPlanRepository.findById(request.getTravelPlanId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Travel plan not found"
                    ));

            if (!plan.getUser().getId().equals(currentUser.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only create a squad from your own travel plan"
                );
            }
        }

        String destination = firstNonBlank(
                request.getDestination(),
                plan != null ? plan.getDestination() : null
        );

        if (destination == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Destination is required"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        TravelSquad squad = TravelSquad.builder()
                .name(request.getName().trim())
                .description(blankToNull(request.getDescription()))
                .destination(destination.trim())
                .startDate(plan != null ? plan.getStartDate() : request.getStartDate())
                .endDate(plan != null ? plan.getEndDate() : request.getEndDate())
                .maxMembers(request.getMaxMembers() == null ? 6 : request.getMaxMembers())
                .creator(currentUser)
                .travelPlan(plan)
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        if (squad.getStartDate() != null && squad.getEndDate() != null
                && squad.getEndDate().isBefore(squad.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End date cannot be before start date"
            );
        }

        TravelSquad saved = squadRepository.save(squad);

        memberRepository.save(
                TravelSquadMember.builder()
                        .squad(saved)
                        .user(currentUser)
                        .role("CREATOR")
                        .status("ACTIVE")
                        .joinedAt(now)
                        .updatedAt(now)
                        .build()
        );

        return toResponse(
                saved,
                currentUser,
                memberRepository.findBySquadIdAndStatusOrderByJoinedAtAsc(saved.getId(), "ACTIVE")
        );
    }

    // =========================================================
    // DETAILS
    // =========================================================

    @Transactional(readOnly = true)
    public TravelSquadResponse getSquad(Long squadId) {
        User currentUser = getCurrentUser();
        TravelSquad squad = getSquadOrThrow(squadId);
        requireActiveMember(squadId, currentUser.getId());

        return toResponse(
                squad,
                currentUser,
                memberRepository.findBySquadIdAndStatusOrderByJoinedAtAsc(squadId, "ACTIVE")
        );
    }

    // =========================================================
    // INVITE
    // =========================================================

    @Transactional
    public TravelSquadResponse invite(Long squadId, TravelSquadInviteRequest request) {
        User currentUser = getCurrentUser();
        TravelSquad squad = getSquadOrThrow(squadId);
        TravelSquadMember inviter = requireActiveMember(squadId, currentUser.getId());

        if (!"CREATOR".equals(inviter.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the squad creator can invite travelers"
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(squad.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This squad is not active");
        }

        User target = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Traveler not found"
                ));

        if (target.getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already in this squad");
        }

        if (blockedUserService.isBlockedEitherWay(currentUser.getId(), target.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This traveler cannot be invited because of the current block relationship"
            );
        }

        if (!travelPartnerRepository.arePartners(currentUser, target)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only invite accepted travel partners"
            );
        }

        long activeCount = memberRepository.countBySquadIdAndStatus(squadId, "ACTIVE");
        if (activeCount >= squad.getMaxMembers()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This squad is already full");
        }

        TravelSquadMember membership = memberRepository
                .findBySquadIdAndUserId(squadId, target.getId())
                .orElse(null);

        if (membership == null) {
            membership = TravelSquadMember.builder()
                    .squad(squad)
                    .user(target)
                    .role("MEMBER")
                    .status("INVITED")
                    .joinedAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        } else {
            if ("ACTIVE".equalsIgnoreCase(membership.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Traveler is already in this squad");
            }
            membership.setStatus("INVITED");
            membership.setRole("MEMBER");
            membership.setUpdatedAt(LocalDateTime.now());
        }

        memberRepository.save(membership);
        squad.setUpdatedAt(LocalDateTime.now());
        squadRepository.save(squad);

        notificationService.createSquadInviteNotification(
                target,
                currentUser,
                squad.getId(),
                squad.getName()
        );

        return toResponse(
                squad,
                currentUser,
                memberRepository.findBySquadIdAndStatusOrderByJoinedAtAsc(squadId, "ACTIVE")
        );
    }

    // =========================================================
    // ACCEPT / DECLINE INVITATION
    // =========================================================

    @Transactional
    public TravelSquadResponse acceptInvitation(Long squadId) {
        User currentUser = getCurrentUser();
        TravelSquad squad = getSquadOrThrow(squadId);
        TravelSquadMember membership = getMembership(squadId, currentUser.getId());

        if (!"INVITED".equalsIgnoreCase(membership.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "There is no pending invitation for you");
        }

        long activeCount = memberRepository.countBySquadIdAndStatus(squadId, "ACTIVE");
        if (activeCount >= squad.getMaxMembers()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This squad is already full");
        }

        membership.setStatus("ACTIVE");
        membership.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(membership);

        squad.setUpdatedAt(LocalDateTime.now());
        squadRepository.save(squad);

        notificationService.createSquadMemberNotification(
                squad.getCreator(),
                currentUser,
                squad.getId(),
                "👋 " + currentUser.getName() + " joined your squad " + squad.getName() + ".",
                NotificationType.SQUAD_MEMBER_JOINED
        );

        return toResponse(
                squad,
                currentUser,
                memberRepository.findBySquadIdAndStatusOrderByJoinedAtAsc(squadId, "ACTIVE")
        );
    }

    @Transactional
    public void declineInvitation(Long squadId) {
        User currentUser = getCurrentUser();
        TravelSquadMember membership = getMembership(squadId, currentUser.getId());

        if (!"INVITED".equalsIgnoreCase(membership.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "There is no pending invitation for you");
        }

        membership.setStatus("DECLINED");
        membership.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(membership);
    }

    // =========================================================
    // REMOVE / LEAVE
    // =========================================================

    @Transactional
    public void removeMember(Long squadId, Long userId) {
        User currentUser = getCurrentUser();
        TravelSquad squad = getSquadOrThrow(squadId);
        TravelSquadMember creator = requireActiveMember(squadId, currentUser.getId());

        if (!"CREATOR".equals(creator.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the squad creator can remove members");
        }

        if (currentUser.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The squad creator cannot remove themselves");
        }

        TravelSquadMember target = getMembership(squadId, userId);
        if (!"ACTIVE".equalsIgnoreCase(target.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Traveler is not an active squad member");
        }

        target.setStatus("REMOVED");
        target.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(target);

        squad.setUpdatedAt(LocalDateTime.now());
        squadRepository.save(squad);

        notificationService.createSquadMemberNotification(
                target.getUser(),
                currentUser,
                null,
                "You were removed from " + squad.getName() + ".",
                NotificationType.SQUAD_MEMBER_LEFT
        );
    }

    @Transactional
    public void leaveSquad(Long squadId) {
        User currentUser = getCurrentUser();
        TravelSquad squad = getSquadOrThrow(squadId);
        TravelSquadMember membership = requireActiveMember(squadId, currentUser.getId());

        if ("CREATOR".equals(membership.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The squad creator cannot leave. Create another squad or close this squad instead."
            );
        }

        membership.setStatus("LEFT");
        membership.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(membership);

        squad.setUpdatedAt(LocalDateTime.now());
        squadRepository.save(squad);

        notificationService.createSquadMemberNotification(
                squad.getCreator(),
                currentUser,
                squad.getId(),
                "👋 " + currentUser.getName() + " left your squad " + squad.getName() + ".",
                NotificationType.SQUAD_MEMBER_LEFT
        );
    }

    // =========================================================
    // GROUP CHAT
    // =========================================================

    @Transactional(readOnly = true)
    public Page<TravelSquadMessageResponse> getMessages(Long squadId, int page, int size) {
        User currentUser = getCurrentUser();
        getSquadOrThrow(squadId);
        requireActiveMember(squadId, currentUser.getId());

        PageRequest pageable = PageRequest.of(
                safePage(page),
                Math.min(Math.max(size, 1), MAX_MESSAGE_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        return messageRepository.findBySquadIdOrderByTimestampDesc(squadId, pageable)
                .map(this::toMessageResponse);
    }

    @Transactional
    public TravelSquadMessageResponse sendMessage(Long squadId, TravelSquadMessageRequest request) {
        User currentUser = getCurrentUser();
        TravelSquad squad = getSquadOrThrow(squadId);
        requireActiveMember(squadId, currentUser.getId());

        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }

        TravelSquadMessage message = TravelSquadMessage.builder()
                .squad(squad)
                .sender(currentUser)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();

        TravelSquadMessage saved = messageRepository.save(message);

        squad.setUpdatedAt(LocalDateTime.now());
        squadRepository.save(squad);

        // Notify every active squad member except the sender. The notification
        // service deduplicates unread messages from the same sender/squad.
        List<TravelSquadMember> activeMembers =
                memberRepository.findBySquadIdAndStatusOrderByJoinedAtAsc(squadId, "ACTIVE");

        for (TravelSquadMember member : activeMembers) {
            notificationService.createSquadMessageNotification(
                    member.getUser(),
                    currentUser,
                    squad.getId(),
                    squad.getName()
            );
        }

        return toMessageResponse(saved);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private TravelSquad getSquadOrThrow(Long squadId) {
        return squadRepository.findById(squadId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel squad not found"
                ));
    }

    private TravelSquadMember getMembership(Long squadId, Long userId) {
        return memberRepository.findBySquadIdAndUserId(squadId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You are not a member of this squad"
                ));
    }

    private TravelSquadMember requireActiveMember(Long squadId, Long userId) {
        TravelSquadMember member = getMembership(squadId, userId);
        if (!"ACTIVE".equalsIgnoreCase(member.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not an active member of this squad"
            );
        }
        return member;
    }

    private TravelSquadResponse toResponse(
            TravelSquad squad,
            User viewer,
            List<TravelSquadMember> activeMembers) {

        TravelSquadMember viewerMembership = memberRepository
                .findBySquadIdAndUserId(squad.getId(), viewer.getId())
                .orElse(null);

        List<TravelSquadMemberResponse> members = activeMembers.stream()
                .map(this::toMemberResponse)
                .toList();

        return TravelSquadResponse.builder()
                .id(squad.getId())
                .name(squad.getName())
                .description(squad.getDescription())
                .destination(squad.getDestination())
                .startDate(squad.getStartDate())
                .endDate(squad.getEndDate())
                .maxMembers(squad.getMaxMembers())
                .memberCount(activeMembers.size())
                .creatorId(squad.getCreator().getId())
                .creatorName(squad.getCreator().getName())
                .status(squad.getStatus())
                .createdAt(squad.getCreatedAt())
                .updatedAt(squad.getUpdatedAt())
                .travelPlanId(squad.getTravelPlan() != null ? squad.getTravelPlan().getId() : null)
                .viewerRole(viewerMembership != null ? viewerMembership.getRole() : null)
                .viewerMembershipStatus(viewerMembership != null ? viewerMembership.getStatus() : null)
                .members(members)
                .build();
    }

    private TravelSquadMemberResponse toMemberResponse(TravelSquadMember member) {
        User user = member.getUser();
        return TravelSquadMemberResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .city(user.getCity())
                .country(user.getCountry())
                .role(member.getRole())
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    private TravelSquadMessageResponse toMessageResponse(TravelSquadMessage message) {
        return TravelSquadMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .build();
    }

    private int safePage(int page) {
        return Math.max(page, 0);
    }

    private int safePageSize(int size) {
        if (size <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
