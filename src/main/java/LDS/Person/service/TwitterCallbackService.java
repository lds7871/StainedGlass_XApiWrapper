package LDS.Person.service;

import LDS.Person.dto.request.TwitterCallbackRequest;
import LDS.Person.dto.response.TwitterAuthorizationState;
import LDS.Person.dto.response.TwitterCallbackResponse;

/**
 * Twitter OAuth 回调服务接口
 */
public interface TwitterCallbackService {

    /**
     * 处理 Twitter OAuth 回调
     * 验证 state、交换授权码获取 token、获取用户信息并保存
     */
    TwitterCallbackResponse handleCallback(TwitterCallbackRequest request);

    /**
     * 验证 state 是否有效（防 CSRF）
     */
    boolean validateState(String state);

    /**
     * 生成包含 PKCE 信息的 OAuth state 数据
     */
    TwitterAuthorizationState generateAuthorizationState();

}
