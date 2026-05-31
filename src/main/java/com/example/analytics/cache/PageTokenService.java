package com.example.analytics.cache;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PageTokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateResultSetId() {
        return UUID.randomUUID().toString();
    }

    public String generatePageToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
