package LDS.Person.service.impl;

import LDS.Person.config.TwitterApiClient;
import LDS.Person.dto.response.TokenRefreshResponse;
import LDS.Person.entity.TwitterToken;
import LDS.Person.repository.TwitterTokenRepository;
import LDS.Person.service.TwitterTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Twitter Token 服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TwitterTokenServiceImpl implements TwitterTokenService {

    private final TwitterTokenRepository repository;
    private final TwitterApiClient twitterApiClient;

    /**
     * 当剩余有效期少于该阈值时触发刷新（例如 30 分钟）
     */
    private static final Duration REFRESH_THRESHOLD = Duration.ofMinutes(30);

    @Override
    @Transactional
    public TwitterToken save(TwitterToken token) {
        token.setUpdatedAt(Instant.now());
        return repository.save(token);
    }

    @Override
    public TwitterToken getByUserId(String twitterUserId) {
        Optional<TwitterToken> opt = repository.findByTwitterUserId(twitterUserId);
        return opt.orElse(null);
    }

    @Override
    @Transactional
    public String getValidAccessToken(String twitterUserId) throws Exception {
        Optional<TwitterToken> opt = repository.findByTwitterUserId(twitterUserId);
        if (opt.isEmpty()) {
            throw new IllegalStateException("未找到 Twitter 用户的 token，用户ID: " + twitterUserId);
        }

        TwitterToken token = opt.get();
        Instant now = Instant.now();

        // 检查是否有 refresh_token（无法刷新的情况）
        if (token.getRefreshToken() == null || token.getRefreshToken().isBlank()) {
            log.warn("⚠️ Token 不存在 refresh_token，无法自动刷新，userId: {}", twitterUserId);
            return token.getAccessToken();
        }

        // 检查是否即将过期
        if (token.getExpiresAt() == null || token.getExpiresAt().isAfter(now.plus(REFRESH_THRESHOLD))) {
            // 仍然有效
            log.debug("✅ Token 仍然有效，userId: {}", twitterUserId);
            return token.getAccessToken();
        }

        // 需要刷新
        log.info("🔄 Token 即将过期，尝试刷新，userId: {}", twitterUserId);
        try {
            TokenRefreshResponse resp = twitterApiClient.refreshAccessToken(token.getRefreshToken());
            
            if (resp == null || resp.getAccessToken() == null) {
                log.error("❌ 刷新 token 失败：响应为空，userId: {}", twitterUserId);
                throw new IllegalStateException("刷新 token 失败");
            }

            // 更新 token 信息
            token.setAccessToken(resp.getAccessToken());
            if (resp.getRefreshToken() != null) {
                token.setRefreshToken(resp.getRefreshToken());
            }
            token.setTokenType(resp.getTokenType());
            token.setScope(resp.getScope());
            if (resp.getExpiresIn() > 0) {
                token.setExpiresAt(Instant.now().plusSeconds(resp.getExpiresIn()));
            }

            save(token);
            log.info("\"\\u001B[36m\"+✅ 成功刷新 token，userId: {}, 新的过期时间: {}"+"\u001B[0m", twitterUserId, token.getExpiresAt());
            return token.getAccessToken();

        } catch (Exception e) {
            log.error("❌ 刷新 token 异常，userId: {}", twitterUserId, e);
            throw e;
        }
    }

    /**
     * 获取服务器运行时间（单位：秒）
     */
    private long getServerUptimeSeconds() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
    }

    @Override
    @Transactional
    public void refreshExpiringTokens() {
        log.info("🔄 开始检查和刷新即将过期的 token...");
        List<TwitterToken> allTokens = repository.findAll();
        
        // 将从数据库读取到的 token 信息输出到控制台和日志，便于调试和核对
        log.info("📊 从数据库读取到 {} 条 token，开始列出每条记录（敏感字段已掩码）", allTokens.size());
        System.out.println("===== TwitterTokens from DB =====");
        for (TwitterToken t : allTokens) {
            String accessMasked = t.getAccessToken() == null ? "" : mask(t.getAccessToken());
            String refreshMasked = t.getRefreshToken() == null ? "" : mask(t.getRefreshToken());
            String line = String.format("id=%s, userId=%s, accessToken=%s, refreshToken=%s, tokenType=%s, scope=%s, createdAt=%s, expiresAt=%s, updatedAt=%s",
                    t.getId(), t.getTwitterUserId(), accessMasked, refreshMasked, t.getTokenType(), t.getScope(), t.getCreatedAt(), t.getExpiresAt(), t.getUpdatedAt());
            System.out.println(line);
            log.debug("DB token: {}", line);
        }
        
        Instant threshold = Instant.now().plus(REFRESH_THRESHOLD);
        int refreshCount = 0;      // 成功刷新的数量
        int failCount = 0;         // 刷新失败的数量
        int noNeedCount = 0;       // 无需刷新的数量

        for (TwitterToken token : allTokens) {
            try {
                // 检查是否需要刷新
                if (token.getExpiresAt() == null || token.getExpiresAt().isAfter(threshold)) {
                    // 无需刷新
                    log.info("✅ Token 无需刷新，userId: {}, 过期时间: {}", token.getTwitterUserId(), token.getExpiresAt());
                    noNeedCount++;
                } else {
                    // 需要刷新
                    log.info("📋 发现即将过期的 token，userId: {}", token.getTwitterUserId());
                    getValidAccessToken(token.getTwitterUserId());
                    refreshCount++;
                }
            } catch (Exception e) {
                log.warn("⚠️ 定时刷新 token 失败，userId: {}", token.getTwitterUserId(), e);
                failCount++;
            }
        }

        long uptimeSeconds = getServerUptimeSeconds();
        log.info("\u001B[36m"+"✅ Token 定时刷新完成 - 服务器运行时间: {}s，成功刷新: {}，刷新失败: {}，无需刷新: {}"+"\u001B[0m", 
                uptimeSeconds, refreshCount, failCount, noNeedCount);
    }

    @Override
    @Transactional
    public void deleteByUserId(String twitterUserId) {
        log.info("🗑️  删除用户 {} 的 token...", twitterUserId);
        var existingToken = repository.findByTwitterUserId(twitterUserId);
        if (existingToken.isPresent()) {
            repository.delete(existingToken.get());
            log.info("✅ 用户 {} 的 token 已删除", twitterUserId);
        } else {
            log.info("ℹ️  用户 {} 不存在任何 token", twitterUserId);
        }
    }

    /**
     * 简单掩码，保留头部和尾部以便识别，但不在控制台泄露完整 token
     */
    private String mask(String s) {
        if (s == null) return "";
        if (s.length() <= 12) return "****";
        return s.substring(0, 6) + "..." + s.substring(s.length() - 4);
    }
}
