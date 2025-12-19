package LDS.Person.config;

import LDS.Person.entity.TwitterToken;
import LDS.Person.repository.TwitterTokenRepository;
import LDS.Person.service.TwitterTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Twitter Token 辅助工具类
 * 统一管理 Twitter Token 的获取逻辑，避免各个控制器重复代码
 */
@Component
@Slf4j
public class TwitterTokenHelper {

    @Autowired
    private TwitterTokenService twitterTokenService;

    @Autowired
    private TwitterTokenRepository twitterTokenRepository;

    /**
     * 获取默认用户 ID（从 config.properties 的 DefaultUID）
     * 使用 ConfigManager 统一读取配置
     * 
     * @return 默认用户 ID
     */
    public String getDefaultUserId() {
        String defaultUserId = ConfigManager.getInstance().getString("DefaultUID", "000000000");
        log.debug("从 ConfigManager 读取 DefaultUID: {}", defaultUserId);
        return defaultUserId;
    }

    /**
     * 根据用户 ID 获取 Token
     * 如果指定用户的 Token 不存在，返回 null
     * 
     * @param userId 用户 ID
     * @return TwitterToken 或 null
     */
    public TwitterToken getTokenByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("用户 ID 为空，无法获取 Token");
            return null;
        }
        
        TwitterToken token = twitterTokenService.getByUserId(userId);
        if (token != null && token.getAccessToken() != null) {
            log.debug("成功获取用户 {} 的 Token", userId);
        } else {
            log.warn("未找到用户 {} 的有效 Token", userId);
        }
        return token;
    }

    /**
     * 获取默认用户的 Token
     * 从 config.properties 读取 DefaultUID，然后获取对应的 Token
     * 
     * @return TwitterToken 或 null
     */
    public TwitterToken getDefaultUserToken() {
        String defaultUserId = getDefaultUserId();
        return getTokenByUserId(defaultUserId);
    }

    /**
     * 从数据库获取最新的有效 Token（按更新时间排序）
     * 适用于没有指定用户 ID 或默认用户 Token 不存在的场景
     * 
     * @return 最新的 TwitterToken 或 null
     */
    public TwitterToken getLatestValidToken() {
        try {
            List<TwitterToken> allTokens = twitterTokenRepository.findAll();
            if (allTokens == null || allTokens.isEmpty()) {
                log.warn("数据库中没有任何 Token 记录");
                return null;
            }
            
            TwitterToken latestToken = allTokens.stream()
                    .max((t1, t2) -> {
                        java.time.Instant time1 = t1.getUpdatedAt() != null ? t1.getUpdatedAt()
                                : java.time.Instant.EPOCH;
                        java.time.Instant time2 = t2.getUpdatedAt() != null ? t2.getUpdatedAt()
                                : java.time.Instant.EPOCH;
                        return time1.compareTo(time2);
                    })
                    .orElse(null);
            
            if (latestToken != null) {
                log.debug("获取到最新的 Token，用户 ID: {}", latestToken.getTwitterUserId());
            } else {
                log.warn("无法从数据库中获取最新的 Token");
            }
            
            return latestToken;
        } catch (Exception e) {
            log.error("获取最新 Token 失败", e);
            return null;
        }
    }

    /**
     * 获取 Token（优先使用默认用户，如果不存在则使用最新的）
     * 这是一个便捷方法，适用于大多数场景
     * 
     * @return TwitterToken 或 null
     */
    public TwitterToken getTokenWithFallback() {
        // 首先尝试获取默认用户的 Token
        TwitterToken token = getDefaultUserToken();
        
        // 如果默认用户的 Token 不存在，尝试获取最新的 Token
        if (token == null || token.getAccessToken() == null) {
            log.info("默认用户 Token 不存在，尝试获取数据库中最新的 Token");
            token = getLatestValidToken();
        }
        
        return token;
    }

    /**
     * 获取 Token 的 Access Token 字符串（带回退机制）
     * 
     * @return Access Token 字符串或 null
     */
    public String getAccessTokenWithFallback() {
        TwitterToken token = getTokenWithFallback();
        return token != null ? token.getAccessToken() : null;
    }

    /**
     * 检查 Token 是否包含指定的权限范围
     * 
     * @param token TwitterToken 对象
     * @param requiredScope 需要的权限（如 "tweet.write"）
     * @return true 如果包含该权限，否则返回 false
     */
    public boolean hasScope(TwitterToken token, String requiredScope) {
        if (token == null || requiredScope == null || requiredScope.isBlank()) {
            return false;
        }
        
        String scope = token.getScope();
        if (scope == null || scope.isBlank()) {
            return false;
        }
        
        return scope.contains(requiredScope);
    }
}
