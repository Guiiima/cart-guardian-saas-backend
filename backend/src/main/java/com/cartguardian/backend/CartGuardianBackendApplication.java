package com.cartguardian.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CartGuardianBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CartGuardianBackendApplication.class, args);
	}

}
