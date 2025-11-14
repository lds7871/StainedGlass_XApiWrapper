package LDS.Person.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 基于内存的 OAuth State 存储实现
 * 适用于单机部署，无需 Redis
 * 
 * 特点：
 * - 简单易用，无外部依赖
 * - 自动清理过期 state
 * - 线程安全
 */
@Component("oauthStateStore")
@Slf4j
public class MemoryOAuthStateStore implements OAuthStateStore {

  // state -> 存储条目（包含 codeVerifier 和过期时间）
  private final ConcurrentHashMap<String, StateEntry> stateCache = new ConcurrentHashMap<>();

  private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);

  public MemoryOAuthStateStore() {
    // 每分钟清理一次过期的 state
    cleanupExecutor.scheduleAtFixedRate(
        this::clearExpiredStates,
        1, // 初始延迟
        1, // 间隔
        TimeUnit.MINUTES);
    log.info("内存 OAuth State 存储已初始化");
  }

  @Override
  public void saveState(String state, String codeVerifier, long expiryMinutes) {
    long expiryTime = System.currentTimeMillis() + (expiryMinutes * 60 * 1000);
    stateCache.put(state, new StateEntry(expiryTime, codeVerifier));
    log.debug("State 已保存: {}，有效期: {} 分钟，包含 codeVerifier: {}", state, expiryMinutes,
        codeVerifier != null ? "是" : "否");
  }

  @Override
  public OAuthStateStore.StateRecord consumeState(String state) {
    if (state == null) {
      log.warn("State 为空");
      return null;
    }

    StateEntry entry = stateCache.remove(state);
    if (entry == null) {
      log.warn("State 不存在或已过期: {}", state);
      return null;
    }

    // 检查是否过期
    if (System.currentTimeMillis() > entry.expiryTime) {
      log.warn("State 已过期: {}", state);
      return null;
    }

    log.debug("State 验证成功并已删除: {}", state);
    return new OAuthStateStore.StateRecord(state, entry.codeVerifier);
  }

  @Override
  public boolean stateExists(String state) {
    if (state == null) {
      return false;
    }

    StateEntry entry = stateCache.get(state);
    if (entry == null) {
      return false;
    }

    // 检查是否过期
    if (System.currentTimeMillis() > entry.expiryTime) {
      stateCache.remove(state);
      return false;
    }

    return true;
  }

  @Override
  public void removeState(String state) {
    stateCache.remove(state);
    log.debug("State 已删除: {}", state);
  }

  @Override
  public void clearExpiredStates() {
    long now = System.currentTimeMillis();
    int removedCount = 0;

    for (Map.Entry<String, StateEntry> entry : stateCache.entrySet()) {
      if (entry != null && now > entry.getValue().expiryTime) {
        stateCache.remove(entry.getKey());
        removedCount++;
      }
    }

    if (removedCount > 0) {
      log.debug("已清理 {} 个过期的 state", removedCount);
    }
  }

  /**
   * 获取当前存储的 state 数量（用于监控）
   */
  public int getStateCount() {
    return stateCache.size();
  }

  /**
   * 关闭清理线程
   */
  public void shutdown() {
    cleanupExecutor.shutdown();
    try {
      if (!cleanupExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
        cleanupExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      cleanupExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
    log.info("内存 OAuth State 存储已关闭");
  }
}

class StateEntry {
  final long expiryTime;
  final String codeVerifier;

  StateEntry(long expiryTime, String codeVerifier) {
    this.expiryTime = expiryTime;
    this.codeVerifier = codeVerifier;
  }
}
