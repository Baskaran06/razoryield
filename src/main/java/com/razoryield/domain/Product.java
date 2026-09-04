package com.razoryield.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "sku", length = 64, nullable = false)
    private String sku;

    @Column(name = "cost_price_paise", nullable = false)
    private long costPricePaise;

    @Column(name = "base_price_paise", nullable = false)
    private long basePricePaise;

    @Column(name = "days_idle", nullable = false)
    private int daysIdle;

    @Column(name = "stock_qty", nullable = false)
    private int stockQty;

    protected Product() {
    }

    public String getSku() {
        return sku;
    }

    public long getCostPricePaise() {
        return costPricePaise;
    }

    public long getBasePricePaise() {
        return basePricePaise;
    }

    public int getDaysIdle() {
        return daysIdle;
    }

    public int getStockQty() {
        return stockQty;
    }

    public String getDisplayName() {
        if (sku == null) return "Unknown Product";
        return switch (sku) {
            case "SKU-TEA-250G" -> "Organic Green Tea — 250g";
            case "SKU-COFFEE-500G" -> "Premium Coffee — 500g";
            case "SKU-SOAP-4PK" -> "Gentle Care Body Soap — 4 Pack";
            case "SKU-RICE-10KG" -> "Basmati Rice — 10kg";
            case "SKU-BULB-9W" -> "9W LED Smart Bulb";
            case "SKU-HEADPHONE-BT" -> "Wireless Bluetooth Headphones";
            case "SKU-KETTLE-1L" -> "1L Electric Water Kettle";
            case "SKU-BACKPACK-30L" -> "30L Waterproof Travel Backpack";
            case "SKU-YOGAMAT-6MM" -> "6mm Anti-Slip Yoga Mat";
            case "SKU-LEGACY-PRINTER" -> "Compact Desktop Inkjet Printer";
            default -> sku.replace("SKU-", "").replace("-", " ");
        };
    }

    public String getCategory() {
        if (sku == null) return "General";
        return switch (sku) {
            case "SKU-TEA-250G", "SKU-COFFEE-500G", "SKU-RICE-10KG" -> "Grocery & Staples";
            case "SKU-SOAP-4PK" -> "Personal Care";
            case "SKU-BULB-9W", "SKU-HEADPHONE-BT", "SKU-KETTLE-1L", "SKU-LEGACY-PRINTER" -> "Electronics & Home";
            case "SKU-BACKPACK-30L", "SKU-YOGAMAT-6MM" -> "Lifestyle & Fitness";
            default -> "General Inventory";
        };
    }
}
