package com.cartguardian.backend.repository; // Você pode criar um pacote 'repository'

import com.cartguardian.backend.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

    Optional<Shop> findByShopUrl(String shopUrl);
}