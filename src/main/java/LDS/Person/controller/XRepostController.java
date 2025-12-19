package LDS.Person.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import LDS.Person.config.TwitterTokenHelper;
import LDS.Person.entity.TwitterToken;
import LDS.Person.service.TwitterTokenService;
import LDS.Person.dto.request.RepostRequest;
import LDS.Person.dto.response.RepostResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * X（Twitter）转发控制器 - 转发/转推相关 API
 * 参考：https://docs.x.com/x-api/users/repost-post
 * 
 * 仅从数据库获取 token，userId 默认为 config.properties读取
 */
@RestController
@RequestMapping("/api/twitter/tweet/repost")
@Api(tags = "X 转发", description = "转发/转推相关接口")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class XRepostController {

    @Autowired
    private TwitterTokenService twitterTokenService;

    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private TwitterTokenHelper twitterTokenHelper;

    private static final String TWITTER_API_BASE = "https://api.x.com/2";

    /**
     * 转发一条推文
     * 
     * @param request 转发请求（包含 tweetId 和可选的 userId）
     * @return 转发结果
     */
    @PostMapping("/create")
    @ApiOperation(
        value = "转发一条推文",
        notes = "根据请求体中的 tweetId 将推文转发到配置的用户账户（config.properties的DefaultUID读取）"
    )
    public ResponseEntity<RepostResponse> repostTweet(@RequestBody RepostRequest request) {
        try {
            // 验证请求
            if (request == null || request.getTweetId() == null || request.getTweetId().isBlank()) {
                RepostResponse resp = RepostResponse.badRequest("tweetId 不能为空");
                log.warn("转发请求缺少 tweetId 参数");
                return ResponseEntity.badRequest().body(resp);
            }

            String tweetId = request.getTweetId();
            
            // 使用 TwitterTokenHelper 获取默认用户 ID 和 Token
            String userId = twitterTokenHelper.getDefaultUserId();
            log.info("收到转发请求，userId: {}（来自 config.properties），tweetId: {}", userId, tweetId);

            // 从数据库获取用户的 Token
            TwitterToken twitterToken = twitterTokenHelper.getDefaultUserToken();
            if (twitterToken == null || twitterToken.getAccessToken() == null) {
                RepostResponse resp = RepostResponse.unauthorized("未找到该用户的 access_token，请先登录授权");
                log.error("未能从数据库获取用户 {} 的 token", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
            }

            String accessToken = twitterToken.getAccessToken();
            log.info("已从数据库获取 access_token（用户: {}），token: {}...", userId, 
                    accessToken.substring(0, Math.min(20, accessToken.length())));

            // 构建转发请求
            Map<String, Object> repostResult = sendRepostRequest(userId, tweetId, accessToken);

            if (repostResult == null) {
                RepostResponse resp = RepostResponse.serverError("转发失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
            }

            if (repostResult.containsKey("error")) {
                RepostResponse resp = RepostResponse.badRequest((String) repostResult.get("error"));
                log.warn("Twitter API 返回错误: {}", repostResult);
                return ResponseEntity.badRequest().body(resp);
            }

            log.info("✅ 成功转发推文，userId: {}，tweetId: {}", userId, tweetId);
            return ResponseEntity.ok(RepostResponse.success(repostResult));

        } catch (Exception e) {
            log.error("转发推文异常", e);
            RepostResponse resp = RepostResponse.serverError("服务器错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }

    /**
     * 发送转发请求到 Twitter API
     */
    private Map<String, Object> sendRepostRequest(String userId, String tweetId, String accessToken) {
        try {
            // 构建 API 请求 URL: POST /2/users/{id}/retweets
            String url = String.format("%s/users/%s/retweets", TWITTER_API_BASE, userId);
            log.debug("调用 Twitter API: {}", url);

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("tweet_id", tweetId);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            // 执行 POST 请求
            ResponseEntity<String> apiResponse = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    entity, 
                    String.class
            );

            String responseBody = apiResponse.getBody();
            log.debug("Twitter API 转发响应: {}", responseBody);

            // 解析响应
            JSONObject jsonResponse = JSON.parseObject(responseBody);

            // 检查是否有错误
            if (jsonResponse.containsKey("errors")) {
                log.error("Twitter API 返回错误: {}", jsonResponse);
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", jsonResponse.getJSONArray("errors").getJSONObject(0).getString("message"));
                return errorMap;
            }

            // 提取数据
            JSONObject data = jsonResponse.getJSONObject("data");
            if (data == null) {
                Map<String, Object> emptyMap = new HashMap<>();
                emptyMap.put("error", "API 响应缺少 data 字段");
                return emptyMap;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("retweeted", data.getBoolean("retweeted"));
            
            log.info("成功转发推文，API 返回状态: {}", data);
            return result;

        } catch (HttpClientErrorException e) {
            String err = e.getResponseBodyAsString();
            log.error("调用 Twitter API 转发失败: {} (状态码: {})", err, e.getStatusCode(), e);
            Map<String, Object> errorMap = new HashMap<>();
            try {
                JSONObject jsonErr = JSON.parseObject(err);
                if (jsonErr.containsKey("errors")) {
                    errorMap.put("error", jsonErr.getJSONArray("errors").getJSONObject(0).getString("message"));
                } else {
                    errorMap.put("error", jsonErr.getString("detail"));
                }
            } catch (Exception ex) {
                errorMap.put("error", err);
            }
            return errorMap;

        } catch (Exception e) {
            log.error("调用 Twitter API 失败", e);
            return null;
        }
    }
}
