package com.Pranav.finance_tracker.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class RazorpayService {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

    private RazorpayClient client;

    @PostConstruct
    public void init() throws Exception {
        this.client = new RazorpayClient(keyId, keySecret);
    }

    public String createOrder(BigDecimal amount, String receiptId) {
        try {
            JSONObject orderRequest = new JSONObject();
            // Razorpay expects amount in paise (multiply by 100)
            orderRequest.put("amount", amount.multiply(new BigDecimal(100)).intValue());
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receiptId != null ? receiptId : UUID.randomUUID().toString());
            
            Order order = client.orders.create(orderRequest);
            return order.get("id");
        } catch (Exception e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage());
            throw new RuntimeException("Payment initiation failed", e);
        }
    }

    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject params = new JSONObject();
            params.put("razorpay_order_id", orderId);
            params.put("razorpay_payment_id", paymentId);
            params.put("razorpay_signature", signature);

            return Utils.verifyPaymentSignature(params, keySecret);
        } catch (Exception e) {
            log.error("Razorpay signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}
