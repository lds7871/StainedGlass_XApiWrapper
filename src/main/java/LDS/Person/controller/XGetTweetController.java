package LDS.Person.controller;

import LDS.Person.entity.TwitterToken;
import LDS.Person.repository.TwitterTokenRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * X（Twitter）推文获取控制器 - 获取用户推文相关 API
 * 参考：https://docs.x.com/x-api/users/get-posts
 */
@RestController
@RequestMapping("/api/twitter/tweet/get")
@Api(tags = "X 推文获取", description = "获取用户推文相关接口")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class XGetTweetController {

    @Autowired
    private TwitterTokenRepository twitterTokenRepository;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Twitter API 基础 URL
     */
    private static final String TWITTER_API_BASE = "https://api.x.com/2";

    /**
     * 从 config.properties 读取 DefaultUID
     */
    private String getDefaultUserId() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                try {
                    props.load(input);
                } catch (java.io.IOException e) {
                    log.warn("读取 config.properties 失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("打开 config.properties 失败: {}", e.getMessage());
        }
        return props.getProperty("DefaultUID", "000000000");
    }

    /**
     * 获取用户最近的推文列表（5）
     * 
     * @param userId       用户 ID（config.properties读取）
     * @param session      HTTP 会话（包含 twitterUserId 和 accessToken）
     * @return 用户最近的 5 条推文
     */
    @GetMapping("/latest")
    @ApiOperation(value = "获取最近的推文列表（5）", notes = "根据用户 ID 获取该用户最近发布的 5 条推文。仅使用数据库最新 Token 进行认证，userId 默认从 config.properties 的 DefaultUID 读取")
    public ResponseEntity<Map<String, Object>> getLatestTweets(
            @RequestParam(required = false, name = "userId") String userId,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 如果 userId 未提供，从 config.properties 读取 DefaultUID
            if (userId == null || userId.isBlank()) {
                userId = getDefaultUserId();
                log.info("userId 未指定，从 config.properties 读取到 DefaultUID: {}", userId);
            }

            log.info("收到获取推文请求，目标用户 ID: {}", userId);

            // 🔑 仅使用数据库最新 Token，并匹配 userId
            TwitterToken latestToken = getLatestValidToken();
            if (latestToken == null) {
                response.put("code", 401);
                response.put("message", "未找到数据库中的有效 Token");
                log.warn("数据库中无可用 Token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String accessToken = latestToken.getAccessToken();
            String tokenUserId = latestToken.getTwitterUserId();

            log.info("✅ 使用数据库最新 Token（userId: {}），目标用户 ID: {}", tokenUserId, userId);
            log.info("已从数据库获取 access_token，token: {}...", 
                    accessToken.substring(0, Math.min(20, accessToken.length())));

            // 调用 Twitter API 获取用户推文
            Map<String, Object> tweetData = fetchUserLatestTweets(userId, accessToken);

            if (tweetData == null) {
                response.put("code", 500);
                response.put("message", "获取推文失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            if (tweetData.containsKey("error")) {
                response.put("code", 400);
                response.put("message", tweetData.get("error"));
                log.warn("Twitter API 返回错误: {}", tweetData);
                return ResponseEntity.badRequest().body(response);
            }

            response.put("code", 200);
            response.put("message", "成功获取推文");
            response.put("data", tweetData);

            log.info("✅ 成功获取用户 {} 的最近推文", userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取推文异常", e);
            response.put("code", 500);
            response.put("message", "服务器错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 从 Twitter API 获取用户最近的 5 条推文
     */
    private Map<String, Object> fetchUserLatestTweets(String userId, String accessToken) {
        try {
            // 构建 API 请求 URL（max_results=5 获取最新5条推文）
            String url = String.format(
                    "%s/users/%s/tweets?max_results=5&tweet.fields=created_at,author_id,public_metrics",
                    TWITTER_API_BASE, userId);

            log.debug("调用 Twitter API: {}", url);

            // 设置请求头
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/json");

            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            
            // 使用 exchange 方法传递请求头（getForEntity 不支持自定义请求头）
            ResponseEntity<String> apiResponse = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);

            if (apiResponse.getStatusCode() != HttpStatus.OK) {
                log.error("Twitter API 返回错误状态: {}", apiResponse.getStatusCode());
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", "API 请求失败，状态码: " + apiResponse.getStatusCode());
                return errorMap;
            }

            String responseBody = apiResponse.getBody();
            log.debug("Twitter API 响应: {}", responseBody);

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
            JSONArray dataArray = jsonResponse.getJSONArray("data");
            if (dataArray == null || dataArray.isEmpty()) {
                log.warn("用户 {} 没有推文", userId);
                Map<String, Object> emptyMap = new HashMap<>();
                emptyMap.put("message", "该用户没有推文");
                emptyMap.put("tweets", new java.util.ArrayList<>());
                return emptyMap;
            }

            // 将所有推文转换为 List
            java.util.List<Map<String, Object>> tweets = new java.util.ArrayList<>();
            
            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject tweet = dataArray.getJSONObject(i);
                
                Map<String, Object> tweetMap = new HashMap<>();
                tweetMap.put("id", tweet.getString("id"));
                tweetMap.put("text", tweet.getString("text"));
                tweetMap.put("created_at", tweet.getString("created_at"));
                tweetMap.put("author_id", tweet.getString("author_id"));

                // 添加公开指标（如果有）
                JSONObject publicMetrics = tweet.getJSONObject("public_metrics");
                if (publicMetrics != null) {
                    tweetMap.put("like_count", publicMetrics.getIntValue("like_count"));
                    tweetMap.put("retweet_count", publicMetrics.getIntValue("retweet_count"));
                    tweetMap.put("reply_count", publicMetrics.getIntValue("reply_count"));
                }

                tweets.add(tweetMap);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("tweets", tweets);
            result.put("tweet_count", tweets.size());
            
            // 添加元数据
            JSONObject meta = jsonResponse.getJSONObject("meta");
            if (meta != null) {
                result.put("result_count", meta.getIntValue("result_count"));
            }

            log.info("成功获取用户 {} 的最近 {} 条推文", userId, tweets.size());
            return result;

        } catch (Exception e) {
            log.error("调用 Twitter API 失败", e);
            return null;
        }
    }

    /**
     * 从数据库获取最新的有效 Token
     */
    private TwitterToken getLatestValidToken() {
        try {
            List<TwitterToken> allTokens = twitterTokenRepository.findAll();
            if (allTokens == null || allTokens.isEmpty()) {
                return null;
            }
            return allTokens.stream()
                    .max((t1, t2) -> {
                        java.time.Instant time1 = t1.getUpdatedAt() != null ? t1.getUpdatedAt()
                                : java.time.Instant.EPOCH;
                        java.time.Instant time2 = t2.getUpdatedAt() != null ? t2.getUpdatedAt()
                                : java.time.Instant.EPOCH;
                        return time1.compareTo(time2);
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.error("Failed to get latest Token", e);
            return null;
        }
    }
}
