package com.razoryield.api;

import com.razoryield.campaign.CampaignOrchestrationService;
import com.razoryield.campaign.OrchestrationResult;
import com.razoryield.domain.Campaign;
import com.razoryield.domain.CampaignAuditLog;
import com.razoryield.domain.CampaignAuditLogRepository;
import com.razoryield.domain.CampaignRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CampaignOrchestrationController {

    private final CampaignOrchestrationService orchestrator;
    private final CampaignRepository campaignRepository;
    private final CampaignAuditLogRepository auditLogRepository;

    public CampaignOrchestrationController(CampaignOrchestrationService orchestrator,
                                           CampaignRepository campaignRepository,
                                           CampaignAuditLogRepository auditLogRepository) {
        this.orchestrator = orchestrator;
        this.campaignRepository = campaignRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /** Scans stagnant stock, gets a discount proposed for each, and runs it through the policy gate. */
    @PostMapping("/campaigns/orchestrate")
    public OrchestrationResult orchestrate() {
        return orchestrator.runCycle();
    }

    @GetMapping("/campaigns")
    public List<Campaign> campaigns() {
        return campaignRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @GetMapping("/audit")
    public List<CampaignAuditLog> audit() {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @GetMapping("/audit/{campaignId}")
    public List<CampaignAuditLog> auditFor(@PathVariable UUID campaignId) {
        return auditLogRepository.findByCampaignIdOrderByCreatedAtAsc(campaignId);
    }
}
