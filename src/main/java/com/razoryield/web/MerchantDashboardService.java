package com.razoryield.web;

import com.razoryield.domain.Campaign;
import com.razoryield.domain.CampaignAuditLog;
import com.razoryield.domain.CampaignAuditLogRepository;
import com.razoryield.domain.CampaignRepository;
import com.razoryield.domain.CampaignStatus;
import com.razoryield.domain.Product;
import com.razoryield.domain.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MerchantDashboardService {

    private final CampaignRepository campaignRepository;
    private final CampaignAuditLogRepository auditLogRepository;
    private final ProductRepository productRepository;

    public MerchantDashboardService(CampaignRepository campaignRepository,
                                  CampaignAuditLogRepository auditLogRepository,
                                  ProductRepository productRepository) {
        this.campaignRepository = campaignRepository;
        this.auditLogRepository = auditLogRepository;
        this.productRepository = productRepository;
    }

    public long calculateRecoveredRevenuePaise() {
        List<CampaignAuditLog> settled = auditLogRepository.findBySettlementStatusOrderByCreatedAtDesc("SETTLED");
        return settled.stream().mapToLong(CampaignAuditLog::getOfferPricePaise).sum();
    }

    public int calculateUnitsMoved() {
        List<CampaignAuditLog> settled = auditLogRepository.findBySettlementStatusOrderByCreatedAtDesc("SETTLED");
        Map<String, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getSku, p -> p, (a, b) -> a));

        return settled.stream()
                .mapToInt(log -> {
                    Product p = productMap.get(log.getSku());
                    return p != null ? p.getStockQty() : 1;
                })
                .sum();
    }

    public List<CampaignAuditLog> getSettledPayments() {
        return auditLogRepository.findBySettlementStatusOrderByCreatedAtDesc("SETTLED");
    }
}
