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
}
