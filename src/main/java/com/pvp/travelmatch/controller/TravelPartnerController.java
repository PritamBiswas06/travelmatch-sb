package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.entity.TravelPartner;
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
    public List<TravelPartner> myPartners() {
        return travelPartnerService.getMyPartners();
    }
}
