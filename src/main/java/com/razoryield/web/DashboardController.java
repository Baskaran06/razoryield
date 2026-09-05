package com.razoryield.web;

import com.razoryield.campaign.AuditService;
import com.razoryield.campaign.CampaignOrchestrationService;
import com.razoryield.domain.Campaign;
import com.razoryield.domain.CampaignAuditLog;
import com.razoryield.domain.CampaignAuditLogRepository;
import com.razoryield.domain.CampaignRepository;
import com.razoryield.domain.CampaignStatus;
import com.razoryield.domain.CustomerCohort;
import com.razoryield.domain.CustomerCohortRepository;
import com.razoryield.domain.Product;
import com.razoryield.domain.ProductRepository;
import com.razoryield.gateway.PaymentGateway;
import com.razoryield.policy.DiscountPolicyValidator;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

/**
 * Controller for the merchant-facing UI pages.
 * Integrates business metrics with clear information architecture.
 */
@Controller
public class DashboardController {

    private final CampaignRepository campaignRepository;
    private final CampaignAuditLogRepository auditLogRepository;
    private final ProductRepository productRepository;
    private final DiscountPolicyValidator policyValidator;
    private final CampaignOrchestrationService orchestrator;
    private final PaymentGateway paymentGateway;
    private final MerchantDashboardService merchantDashboardService;
    private final AuditService auditService;
    private final CustomerCohortRepository cohortRepository;

    public DashboardController(CampaignRepository campaignRepository,
                               CampaignAuditLogRepository auditLogRepository,
                               ProductRepository productRepository,
                               DiscountPolicyValidator policyValidator,
                               CampaignOrchestrationService orchestrator,
                               PaymentGateway paymentGateway,
                               MerchantDashboardService merchantDashboardService,
                               AuditService auditService,
                               CustomerCohortRepository cohortRepository) {
        this.campaignRepository = campaignRepository;
        this.auditLogRepository = auditLogRepository;
        this.productRepository = productRepository;
        this.policyValidator = policyValidator;
        this.orchestrator = orchestrator;
        this.paymentGateway = paymentGateway;
        this.merchantDashboardService = merchantDashboardService;
        this.auditService = auditService;
        this.cohortRepository = cohortRepository;
    }

    private void populateCommonAttributes(Model model, String activeTab) {
        long consumed = policyValidator.consumedTodayPaise();
        long recovered = merchantDashboardService.calculateRecoveredRevenuePaise();
        int unitsMoved = merchantDashboardService.calculateUnitsMoved();

        var pending = campaignRepository.findByStatusOrderByCreatedAtDesc(CampaignStatus.PENDING_MERCHANT_APPROVAL);
        var allCampaigns = campaignRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        var allProducts = productRepository.findAll(Sort.by("sku"));
        long stagnantCount = allProducts.stream().filter(p -> p.getDaysIdle() >= 45).count();
        long activeCount = allCampaigns.stream().filter(c -> c.getStatus() == CampaignStatus.AUTO_DISPATCHED || c.getStatus() == CampaignStatus.APPROVED).count();

        model.addAttribute("activeTab", activeTab);
        model.addAttribute("proposerLabel", orchestrator.proposerLabel());
        model.addAttribute("gatewayLabel", paymentGateway.mode());
        model.addAttribute("minMarginPct", policyValidator.globalMinMarginPct());
        model.addAttribute("dailyCapPaise", DiscountPolicyValidator.DAILY_BUDGET_CAP_PAISE);
        model.addAttribute("consumedPaise", consumed);
        model.addAttribute("budgetPct", Math.min(100, (int) ((consumed * 100L) / DiscountPolicyValidator.DAILY_BUDGET_CAP_PAISE)));
        
        model.addAttribute("recoveredRevenuePaise", recovered);
        model.addAttribute("unitsMoved", unitsMoved);
        model.addAttribute("pendingCount", pending.size());
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("stagnantCount", stagnantCount);
        model.addAttribute("totalProductsCount", allProducts.size());
        model.addAttribute("pending", pending);
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        populateCommonAttributes(model, "dashboard");
        model.addAttribute("products", productRepository.findAll(Sort.by("daysIdle").descending()));
        model.addAttribute("campaigns", campaignRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
        model.addAttribute("auditEntries", auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
        model.addAttribute("settledPayments", merchantDashboardService.getSettledPayments());
        return "dashboard";
    }

    @GetMapping("/campaigns")
    public String campaigns(Model model) {
        populateCommonAttributes(model, "campaigns");
        model.addAttribute("products", productRepository.findAll(Sort.by("sku")));
        model.addAttribute("campaigns", campaignRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
        return "campaigns";
    }

    @GetMapping("/inventory")
    public String inventory(Model model) {
        populateCommonAttributes(model, "inventory");
        model.addAttribute("products", productRepository.findAll(Sort.by("daysIdle").descending()));
        return "inventory";
    }

    @GetMapping("/payments")
    public String payments(Model model) {
        populateCommonAttributes(model, "payments");
        model.addAttribute("settledPayments", merchantDashboardService.getSettledPayments());
        model.addAttribute("allAudits", auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
        return "payments";
    }

    @GetMapping("/activity")
    public String activity(Model model) {
        populateCommonAttributes(model, "activity");
        model.addAttribute("auditEntries", auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
        return "activity";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        populateCommonAttributes(model, "settings");
        return "settings";
    }

    @PostMapping("/orchestrate")
    public String runOrchestration() {
        orchestrator.runCycle();
        return "redirect:/?orchestrated=true";
    }

    @PostMapping("/campaigns/{id}/approve")
    @Transactional
    public String approveCampaign(@PathVariable UUID id) {
        var campaignOpt = campaignRepository.findById(id);
        if (campaignOpt.isPresent()) {
            var campaign = campaignOpt.get();
            if (campaign.getStatus() == CampaignStatus.PENDING_MERCHANT_APPROVAL) {
                String contact = cohortRepository.findTargetable().stream()
                        .findFirst()
                        .map(CustomerCohort::getPhoneNumber)
                        .orElse("+910000000000");
                String linkId = paymentGateway.createPaymentLink(
                        campaign.getId().toString(), campaign.getSku(), campaign.getOfferPricePaise(), contact);
                campaign.setStatus(CampaignStatus.APPROVED);
                campaign.setRazorpayLinkId(linkId);
                campaignRepository.save(campaign);

                var product = productRepository.findById(campaign.getSku()).orElse(null);
                auditService.append(CampaignAuditLog.builder()
                        .campaignId(campaign.getId())
                        .sku(campaign.getSku())
                        .costPricePaise(product == null ? 0L : product.getCostPricePaise())
                        .basePricePaise(product == null ? campaign.getOfferPricePaise() : product.getBasePricePaise())
                        .discountPct(campaign.getDiscountPct())
                        .offerPricePaise(campaign.getOfferPricePaise())
                        .llmReasoning("Merchant approved clearance offer.")
                        .gateVerdict("MANUALLY_APPROVED")
                        .approverUserId("MERCHANT_ADMIN")
                        .razorpayLinkId(linkId)
                        .settlementStatus("PENDING")
                        .build());
            }
        }
        return "redirect:/?approved=true";
    }
}
