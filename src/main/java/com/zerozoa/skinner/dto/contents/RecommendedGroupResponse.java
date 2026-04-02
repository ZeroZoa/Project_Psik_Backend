package com.zerozoa.skinner.dto.contents;

import java.util.List;

public record RecommendedGroupResponse(
        String concern,
        String concernDisplayName,
        List<IngredientResponse> ingredients
) {}