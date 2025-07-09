package com.cartguardian.backend.repository;

import com.cartguardian.backend.model.AbandonedCheckout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AbandonedCheckoutRepository extends JpaRepository<AbandonedCheckout, Long> {
}