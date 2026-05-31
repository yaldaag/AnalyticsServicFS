package com.example.analytics.cache.redis;

import com.example.analytics.cache.CachedPage;
import com.example.analytics.cache.ResultPageCache;
import com.example.analytics.config.AnalyticsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisResultPageCache implements ResultPageCache {

    private static final Logger log = LoggerFactory.getLogger(RedisResultPageCache.class);
    private static final String KEY_PREFIX = "analytics:pages:";

    private final RedisTemplate<String, CachedPage> redisTemplate;
    private final AnalyticsProperties analyticsProperties;

    public RedisResultPageCache(RedisTemplate<String, CachedPage> redisTemplate, AnalyticsProperties analyticsProperties) {
        this.redisTemplate = redisTemplate;
        this.analyticsProperties = analyticsProperties;
    }

    @Override
    public void put(String pageToken, CachedPage page) {
        redisTemplate.opsForValue().set(redisKey(pageToken), page, analyticsProperties.getPageTtl());
        log.info("Cached analytics page resultSetId={} hasMore={}", page.resultSetId(), page.hasMore());
    }

    @Override
    public CachedPage get(String pageToken) {
        CachedPage page = redisTemplate.opsForValue().get(redisKey(pageToken));
        if (page == null) {
            log.warn("Cached analytics page not found for token");
        } else {
            log.info("Loaded cached analytics page resultSetId={} hasMore={}", page.resultSetId(), page.hasMore());
        }
        return page;
    }

    private String redisKey(String pageToken) {
        return KEY_PREFIX + pageToken;
    }
}
