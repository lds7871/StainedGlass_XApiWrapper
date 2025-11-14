package LDS.Person.service;

import LDS.Person.dto.request.CreateTweetRequest;
import LDS.Person.dto.response.CreateTweetResponse;

/**
 * Twitter 推文服务接口
 */
public interface TwitterTweetService {
    /**
     * 创建推文
     *
     * @param accessToken 访问令牌
     * @param request 推文请求
     * @return 推文响应
     */
    CreateTweetResponse createTweet(String accessToken, CreateTweetRequest request);
}
