package com.citytrip.analytics;

import com.citytrip.analytics.event.RoutePlanFactTrackedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RoutePlanFactEventListener {

    private final RoutePlanFactPersistenceService routePlanFactPersistenceService;

    public RoutePlanFactEventListener(RoutePlanFactPersistenceService routePlanFactPersistenceService) {
        this.routePlanFactPersistenceService = routePlanFactPersistenceService;
    }

    @Async("analyticsEventExecutor")
    @EventListener
    public void onRoutePlanFactTracked(RoutePlanFactTrackedEvent event) {
        if (event == null || event.getCommand() == null) {
            return;
        }
        routePlanFactPersistenceService.persist(event.getCommand());
    }
}
