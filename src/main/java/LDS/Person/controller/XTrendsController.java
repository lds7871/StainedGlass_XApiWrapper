package LDS.Person.controller;

import LDS.Person.dto.response.TrendResponse;
import LDS.Person.entity.TwitterToken;
import LDS.Person.repository.TwitterTokenRepository;
import LDS.Person.service.TwitterTokenService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * X（Twitter）趋势控制器
 * 参考: https://docs.x.com/x-api/trends/get-personalized-trends
 */
@RestController
@RequestMapping("/api/twitter/trends")
@Api(tags = "X 趋势", description = "获取趋势相关接口（需要Premium 订阅）")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class XTrendsController {

    @Autowired
    private TwitterTokenService twitterTokenService;

    @Autowired
    private TwitterTokenRepository twitterTokenRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String TWITTER_API_BASE = "https://api.x.com/2";

    /**
     * 获取个性化趋势
     */
    @GetMapping("/personalized")
    @ApiOperation(value = "获取个性化趋势", notes = "获取当前认证用户的个性化趋势")
    public ResponseEntity<TrendResponse> getPersonalizedTrends() {
        try {
            // 1. 获取 DefaultUID
            String userId = getDefaultUserId();
            log.info("收到获取个性化趋势请求，使用 DefaultUID: {}", userId);

            // 2. 从数据库获取该用户的 Token
            TwitterToken twitterToken = twitterTokenService.getByUserId(userId);
            
            // 3. 如果指定用户的 Token 不存在，尝试获取数据库中任意可用的 Token (Fallback)
            if (twitterToken == null) {
                log.warn("未找到用户 {} 的 Token，尝试获取数据库中任意可用的 Token", userId);
                twitterToken = getLatestValidToken();
            }
            
            if (twitterToken == null || twitterToken.getAccessToken() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(TrendResponse.error("未找到数据库中的有效 access_token，请先登录授权"));
            }
            
            String accessToken = twitterToken.getAccessToken();
            log.info("✅ 已从数据库获取 access_token（userId: {}）", twitterToken.getTwitterUserId());

            // 4. 调用 Twitter API
            String url = TWITTER_API_BASE + "/users/personalized_trends";
            log.debug("调用 Twitter API: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (apiResponse.getStatusCode() != HttpStatus.OK) {
                log.error("Twitter API 返回错误状态: {}", apiResponse.getStatusCode());
                return ResponseEntity.status(apiResponse.getStatusCode())
                        .body(TrendResponse.error("API 请求失败，状态码: " + apiResponse.getStatusCode()));
            }

            // 5. 解析响应
            JSONObject jsonResponse = JSON.parseObject(apiResponse.getBody());
            if (jsonResponse.containsKey("errors")) {
                String errorMsg = jsonResponse.getJSONArray("errors").getJSONObject(0).getString("message");
                log.warn("Twitter API 返回错误: {}", errorMsg);
                return ResponseEntity.badRequest().body(TrendResponse.badRequest(errorMsg));
            }

            JSONArray dataArray = jsonResponse.getJSONArray("data");
            List<TrendResponse.TrendData> trends = new ArrayList<>();

            if (dataArray != null) {
                for (int i = 0; i < dataArray.size(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    TrendResponse.TrendData trend = new TrendResponse.TrendData();
                    trend.setTrendName(item.getString("trend_name"));
                    trend.setPostCount(item.getInteger("post_count"));
                    trend.setCategory(item.getString("category"));
                    trend.setTrendingSince(item.getString("trending_since"));
                    trends.add(trend);
                }
            }

            log.info("成功获取 {} 条个性化趋势", trends.size());
            return ResponseEntity.ok(TrendResponse.success(trends));

        } catch (Exception e) {
            log.error("获取个性化趋势异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(TrendResponse.error("服务器错误: " + e.getMessage()));
        }
    }

    private String getDefaultUserId() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (Exception e) {
            log.warn("读取 config.properties 失败: {}", e.getMessage());
        }
        return props.getProperty("DefaultUID", "000000000");
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
            log.error("获取最新 Token 失败", e);
            return null;
        }
    }
}
