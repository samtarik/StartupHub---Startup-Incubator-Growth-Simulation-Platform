package com.startuphub.pattern.strategy;

import com.startuphub.model.RevenueModel;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Picks the right RevenueStrategy for a RevenueModel at runtime. Spring injects every
 * RevenueStrategy bean, so adding a new revenue model means adding one enum value and
 * one strategy class - nothing here or in the service needs to change (Open/Closed).
 */
@Component
public class RevenueStrategyResolver {

    private final Map<RevenueModel, RevenueStrategy> byModel = new EnumMap<>(RevenueModel.class);

    public RevenueStrategyResolver(List<RevenueStrategy> strategies) {
        for (RevenueStrategy s : strategies) byModel.put(s.model(), s);
    }

    public RevenueStrategy resolve(RevenueModel model) {
        if (model == null) throw new IllegalArgumentException("Choose a revenue model.");
        RevenueStrategy strategy = byModel.get(model);
        if (strategy == null) throw new IllegalArgumentException("Unsupported revenue model: " + model);
        return strategy;
    }
}
