package com.Pranav.finance_tracker.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.url:}")
    private String cloudinaryUrl;

    @Bean
    public Cloudinary cloudinary() {
        if (cloudinaryUrl != null && !cloudinaryUrl.isEmpty()) {
            return new Cloudinary(cloudinaryUrl);
        } else {
            // Fallback (for local testing without internet if needed, though upload will fail)
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "demo");
            config.put("api_key", "123456789012345");
            config.put("api_secret", "abc123def456ghi789jkl012mno");
            return new Cloudinary(config);
        }
    }
}
