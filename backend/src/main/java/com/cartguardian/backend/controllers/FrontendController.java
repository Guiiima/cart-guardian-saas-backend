package com.cartguardian.backend.controllers;

import com.cartguardian.backend.service.ShopServiceFirestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.ExecutionException;

@Controller
public class FrontendController {

    @Autowired
    private ShopServiceFirestore shopService;

    @GetMapping("/")
    public String handleAppEntry(@RequestParam(name = "shop", required = false) String shop) throws ExecutionException, InterruptedException {

        if (shop != null && !shop.isEmpty()) {
            boolean isShopInstalled = shopService.findShopByUrl(shop).isPresent();

            if (isShopInstalled) {
                return "forward:/index.html";
            } else {
                return "redirect:/shopify/install?shop=" + shop;
            }

        } else {
            return "forward:/welcome.html";
        }
    }

}
