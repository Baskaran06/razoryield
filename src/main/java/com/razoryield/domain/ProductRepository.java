package com.razoryield.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    @Query("select p from Product p where p.daysIdle >= :minDaysIdle and p.stockQty > 0 order by p.daysIdle desc")
    List<Product> findStagnant(int minDaysIdle);
}
