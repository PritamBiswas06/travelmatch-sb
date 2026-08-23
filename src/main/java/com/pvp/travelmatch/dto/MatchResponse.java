package com.pvp.travelmatch.dto;

import com.pvp.travelmatch.entity.TravelPlan;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MatchResponse {

    private TravelPlan travelPlan;
    private int score;
}