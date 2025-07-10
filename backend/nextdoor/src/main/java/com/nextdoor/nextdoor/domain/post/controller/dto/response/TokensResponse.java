package com.nextdoor.nextdoor.domain.post.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokensResponse {
    private List<String> tokens;
    
    public static TokensResponse of(List<String> tokens) {
        return new TokensResponse(tokens);
    }
}