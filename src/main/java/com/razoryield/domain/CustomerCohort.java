package com.razoryield.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_cohorts")
public class CustomerCohort {

    @Id
    @Column(name = "customer_id", length = 64, nullable = false)
    private String customerId;

    @Column(name = "phone_number", length = 32, nullable = false)
    private String phoneNumber;

    @Column(name = "days_since_last_purchase", nullable = false)
    private int daysSinceLastPurchase;

    @Column(name = "total_orders", nullable = false)
    private int totalOrders;

    protected CustomerCohort() {
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public int getDaysSinceLastPurchase() {
        return daysSinceLastPurchase;
    }

    public int getTotalOrders() {
        return totalOrders;
    }
}
