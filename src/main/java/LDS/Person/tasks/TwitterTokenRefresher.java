package LDS.Person.tasks;

import LDS.Person.service.TwitterTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Twitter Token 定时刷新器
 * 每29分钟自动检查并刷新即将过期的 token
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TwitterTokenRefresher {

    private final TwitterTokenService twitterTokenService;

    /**
     * 定时刷新 token
     * 每 29 分钟执行一次（access_token 有效期为 2 小时）
     * 
     * 配置说明：
     * - initialDelay: 1740000ms = 29 分钟
     * - fixedDelay: 1740000ms =29 分钟
     * 
     * 这确保了：
     * 1. 服务器启动时不立即执行
     * 2. 首次执行发生在启动后 29 分钟
     * 3. 后续每 29 分钟执行一次
     */
    @Scheduled(initialDelay = 1740000, fixedDelay = 1740000)
    public void refreshExpiringTokens() {
        log.info("⏰ ============= 开始定时刷新 Token =============");
        try {
            twitterTokenService.refreshExpiringTokens();
            log.info("✅ Token 定时刷新完成");
        } catch (Exception e) {
            log.error("❌ Token 定时刷新异常", e);
        }
        log.info("⏰ ============= Token 定时刷新结束 =============");
    }
}
