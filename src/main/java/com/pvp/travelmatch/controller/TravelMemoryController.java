package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.TravelMemoryResponse;
import com.pvp.travelmatch.service.TravelMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/travel-memories")
@RequiredArgsConstructor
public class TravelMemoryController {
    private final TravelMemoryService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TravelMemoryResponse create(
            @RequestParam Long travelPlanId,
            @RequestParam(required = false) String caption,
            @RequestParam("file") MultipartFile file) {
        return service.create(travelPlanId, caption, file);
    }

    @GetMapping("/user/{userId}")
    public List<TravelMemoryResponse> getForUser(@PathVariable Long userId) {
        return service.getForUser(userId);
    }

    @GetMapping("/{id}")
    public TravelMemoryResponse getOne(@PathVariable Long id) {
        return service.getOne(id);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        service.delete(id);
        return Map.of("message", "Memory deleted");
    }
}
