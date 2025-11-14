package LDS.Person.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import LDS.Person.config.TwitterApiClient;
import LDS.Person.config.TwitterProperties;
import LDS.Person.dto.request.CreateTweetRequest;
import LDS.Person.dto.response.CreateTweetResponse;
import LDS.Person.service.TwitterTweetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Twitter 推文服务实现
 */
@Service
@Slf4j
public class TwitterTweetServiceImpl implements TwitterTweetService {

    @Autowired
    private TwitterApiClient twitterApiClient;

    @Autowired
    private TwitterProperties twitterProperties;

    @Override
    public CreateTweetResponse createTweet(String accessToken, CreateTweetRequest request) {
        try {
            log.info("开始创建推文，文本长度: {} 字符", request.getText().length());

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("text", request.getText());
            
            // 如果有媒体，添加媒体字段
            if (request.getMedia() != null && !request.getMedia().isEmpty()) {
                log.info("推文包含媒体，媒体数据: {}", request.getMedia());
                requestBody.put("media", request.getMedia());
                
                // 如果媒体对象中有 media_ids，打印用于调试
                if (request.getMedia().containsKey("media_ids")) {
                    log.info("媒体 IDs: {}", request.getMedia().get("media_ids"));
                }
            }

            log.debug("发送到 Twitter API 的请求体: {}", requestBody.toJSONString());

            // 调用 Twitter API
            String response = twitterApiClient.postToTwitterApi(
                    twitterProperties.getCreateTweetUrl(),
                    requestBody.toJSONString(),
                    accessToken
            );

            log.debug("创建推文响应: {}", response);

            // 解析响应
            JSONObject jsonResponse = JSON.parseObject(response);
            if (jsonResponse.containsKey("errors")) {
                log.error("创建推文失败: {}", jsonResponse);
                return null;
            }

            JSONObject data = jsonResponse.getJSONObject("data");
            CreateTweetResponse tweetResponse = new CreateTweetResponse();
            tweetResponse.setTweetId(data.getString("id"));
            tweetResponse.setText(request.getText());
            tweetResponse.setCreatedAt(data.getString("created_at"));

            log.info("成功创建推文: tweetId={}, createdAt={}", tweetResponse.getTweetId(), tweetResponse.getCreatedAt());
            return tweetResponse;

        } catch (Exception e) {
            log.error("创建推文时出错", e);
            return null;
        }
    }
}
