package com.razoryield.campaign;

import com.razoryield.domain.CampaignAuditLog;
import com.razoryield.domain.CampaignAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The only writer of the audit trail. It appends and nothing else: there is no update or delete
 * path here, by design.
 */
@Service
public class AuditService {

    private final CampaignAuditLogRepository auditLogRepository;

    public AuditService(CampaignAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Appends one immutable row. Joins the caller's transaction so a campaign and the row explaining
     * it commit or roll back together.
     */
    @Transactional
    public CampaignAuditLog append(CampaignAuditLog entry) {
        return auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<CampaignAuditLog> trailFor(UUID campaignId) {
        return auditLogRepository.findByCampaignIdOrderByCreatedAtAsc(campaignId);
    }
}
