package com.pvp.travelmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedPageResponse {
    private List<FeedPostResponse> content;
    private int page;
    private int size;
    private boolean hasMore;
}
