package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.TravelSquadCreateRequest;
import com.pvp.travelmatch.dto.TravelSquadInviteRequest;
import com.pvp.travelmatch.dto.TravelSquadMessageRequest;
import com.pvp.travelmatch.dto.TravelSquadMessageResponse;
import com.pvp.travelmatch.dto.TravelSquadResponse;
import com.pvp.travelmatch.service.TravelSquadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/squads")
@RequiredArgsConstructor
public class TravelSquadController {

    private final TravelSquadService squadService;

    @GetMapping("/my")
    public Page<TravelSquadResponse> getMySquads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return squadService.getMySquads(page, size);
    }

    @GetMapping("/invitations")
    public List<TravelSquadResponse> getMyInvitations() {
        return squadService.getMyInvitations();
    }

    @PostMapping
    public TravelSquadResponse createSquad(
            @Valid @RequestBody TravelSquadCreateRequest request) {
        return squadService.createSquad(request);
    }

    @GetMapping("/{squadId}")
    public TravelSquadResponse getSquad(@PathVariable Long squadId) {
        return squadService.getSquad(squadId);
    }

    @PostMapping("/{squadId}/invite")
    public TravelSquadResponse invite(
            @PathVariable Long squadId,
            @Valid @RequestBody TravelSquadInviteRequest request) {
        return squadService.invite(squadId, request);
    }

    @PostMapping("/{squadId}/accept")
    public TravelSquadResponse acceptInvitation(@PathVariable Long squadId) {
        return squadService.acceptInvitation(squadId);
    }

    @PostMapping("/{squadId}/decline")
    public Map<String, String> declineInvitation(@PathVariable Long squadId) {
        squadService.declineInvitation(squadId);
        return Map.of("message", "Squad invitation declined");
    }

    @DeleteMapping("/{squadId}/members/{userId}")
    public Map<String, String> removeMember(
            @PathVariable Long squadId,
            @PathVariable Long userId) {
        squadService.removeMember(squadId, userId);
        return Map.of("message", "Member removed");
    }

    @PostMapping("/{squadId}/leave")
    public Map<String, String> leaveSquad(@PathVariable Long squadId) {
        squadService.leaveSquad(squadId);
        return Map.of("message", "You left the squad");
    }

    @GetMapping("/{squadId}/messages")
    public Page<TravelSquadMessageResponse> getMessages(
            @PathVariable Long squadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return squadService.getMessages(squadId, page, size);
    }

    @PostMapping("/{squadId}/messages")
    public TravelSquadMessageResponse sendMessage(
            @PathVariable Long squadId,
            @Valid @RequestBody TravelSquadMessageRequest request) {
        return squadService.sendMessage(squadId, request);
    }
}
