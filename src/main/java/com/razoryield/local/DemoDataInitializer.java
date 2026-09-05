package com.razoryield.local;

import com.razoryield.campaign.CampaignOrchestrationService;
import com.razoryield.domain.CampaignRepository;
import com.razoryield.web.MerchantDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DemoDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final CampaignOrchestrationService orchestrationService;
    private final CampaignRepository campaignRepository;
    private final MerchantDashboardService merchantDashboardService;

    public DemoDataInitializer(CampaignOrchestrationService orchestrationService,
                               CampaignRepository campaignRepository,
                               MerchantDashboardService merchantDashboardService) {
        this.orchestrationService = orchestrationService;
        this.campaignRepository = campaignRepository;
        this.merchantDashboardService = merchantDashboardService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (campaignRepository.count() == 0) {
            log.info("Populating initial clearance proposals via orchestration cycle...");
            orchestrationService.runCycle();
        }
        merchantDashboardService.getSettledPayments();
    }
}