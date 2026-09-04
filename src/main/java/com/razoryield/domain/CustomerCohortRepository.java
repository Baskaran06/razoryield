package com.razoryield.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CustomerCohortRepository extends JpaRepository<CustomerCohort, String> {

    /** The targeting criteria: lapsed at least 45 days, and a repeat buyer. */
    @Query("""
            select c from CustomerCohort c
            where c.daysSinceLastPurchase >= 45 and c.totalOrders >= 2
            order by c.totalOrders desc
            """)
    List<CustomerCohort> findTargetable();
}
