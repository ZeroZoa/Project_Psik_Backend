package com.zerozoa.psik.dto.community;

import java.util.List;

public record PostHomeResponse(
        List<PostResponse> hot,
        List<PostResponse> newPosts,
        List<PostResponse> popular
) {}