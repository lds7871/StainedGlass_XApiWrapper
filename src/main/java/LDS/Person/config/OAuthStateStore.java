package LDS.Person.config;

/**
 * OAuth State 存储接口
 * 用于存储和验证 OAuth state 令牌，防止 CSRF 攻击
 * 
 * 可以有多种实现：
 * 1. MemoryStateStore - 内存存储（单机部署）
 * 2. DatabaseStateStore - 数据库存储（分布式）
 * 3. RedisStateStore - Redis 存储（分布式高性能）
 */
public interface OAuthStateStore {

  /**
   * 保存 state 令牌
   *
   * @param state         状态令牌
   * @param expiryMinutes 有效期（分钟）
   */
  void saveState(String state, String codeVerifier, long expiryMinutes);

  default void saveState(String state, long expiryMinutes) {
    saveState(state, null, expiryMinutes);
  }

  /**
   * 验证并删除 state 令牌
   *
   * @param state 状态令牌
   * @return 是否有效
   */
  default boolean validateAndRemoveState(String state) {
    return consumeState(state) != null;
  }

  /**
   * 验证 state 并返回关联数据（包括 codeVerifier）
   * 验证成功后会移除 state，防止重放
   */
  StateRecord consumeState(String state);

  /**
   * 检查 state 是否存在
   */
  boolean stateExists(String state);

  /**
   * 删除 state
   */
  void removeState(String state);

  /**
   * 清空所有过期的 state
   */
  void clearExpiredStates();

  /**
   * 封装的 state 数据
   */
  class StateRecord {
    private final String state;
    private final String codeVerifier;

    public StateRecord(String state, String codeVerifier) {
      this.state = state;
      this.codeVerifier = codeVerifier;
    }

    public String getState() {
      return state;
    }

    public String getCodeVerifier() {
      return codeVerifier;
    }
  }
}
