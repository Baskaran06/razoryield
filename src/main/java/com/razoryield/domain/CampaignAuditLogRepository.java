package com.razoryield.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignAuditLogRepository extends JpaRepository<CampaignAuditLog, UUID> {

    List<CampaignAuditLog> findByCampaignIdOrderByCreatedAtAsc(UUID campaignId);

    boolean existsByRazorpayPaymentId(String razorpayPaymentId);
}
