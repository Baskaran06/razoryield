package com.razoryield.web;

import com.razoryield.campaign.CampaignOrchestrationService;
import com.razoryield.domain.CampaignAuditLogRepository;
import com.razoryield.domain.CampaignRepository;
import com.razoryield.domain.CampaignStatus;
import com.razoryield.domain.ProductRepository;
import com.razoryield.gateway.PaymentGateway;
import com.razoryield.policy.DiscountPolicyValidator;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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

    public DashboardController(CampaignRepository campaignRepository,
                               CampaignAuditLogRepository auditLogRepository,
                               ProductRepository productRepository,
                               DiscountPolicyValidator policyValidator,
                               CampaignOrchestrationService orchestrator,
                               PaymentGateway paymentGateway,
                               MerchantDashboardService merchantDashboardService) {
        this.campaignRepository = campaignRepository;
        this.auditLogRepository = auditLogRepository;
        this.productRepository = productRepository;
        this.policyValidator = policyValidator;
        this.orchestrator = orchestrator;
        this.paymentGateway = paymentGateway;
        this.merchantDashboardService = merchantDashboardService;
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
}
