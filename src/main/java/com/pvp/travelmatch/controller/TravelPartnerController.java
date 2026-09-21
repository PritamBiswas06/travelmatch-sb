package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.entity.TravelPartner;
import com.pvp.travelmatch.dto.TravelPartnerResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
import com.pvp.travelmatch.service.TravelPartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/partner")
@RequiredArgsConstructor

public class TravelPartnerController {

    private final TravelPartnerService travelPartnerService;

    @GetMapping("/my")
    public Page<TravelPartnerResponse> myPartners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return travelPartnerService.getMyPartners(page, size);
    }
}
