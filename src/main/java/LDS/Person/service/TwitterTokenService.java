package LDS.Person.service;

import LDS.Person.entity.TwitterToken;

/**
 * Twitter Token 服务接口
 */
public interface TwitterTokenService {
    /**
     * 保存或更新 token
     */
    TwitterToken save(TwitterToken token);

    /**
     * 根据 Twitter 用户 ID 获取 token
     */
    TwitterToken getByUserId(String twitterUserId);

    /**
     * 获取有效的 access_token（若需要会自动刷新）
     *
     * @param twitterUserId Twitter 用户 ID
     * @return 有效的 access_token
     * @throws Exception 当 token 不存在或刷新失败时
     */
    String getValidAccessToken(String twitterUserId) throws Exception;

    /**
     * 刷新即将过期的所有 token（定时任务使用）
     */
    void refreshExpiringTokens();

    /**
     * 删除指定用户的 token
     *
     * @param twitterUserId Twitter 用户 ID
     */
    void deleteByUserId(String twitterUserId);
}
