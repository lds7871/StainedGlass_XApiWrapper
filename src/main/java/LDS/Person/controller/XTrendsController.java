package LDS.Person.controller;

import LDS.Person.config.TwitterTokenHelper;
import LDS.Person.dto.request.NewsSearchRequest;
import LDS.Person.dto.response.NewsResponse;
import LDS.Person.dto.response.TrendResponse;
import LDS.Person.entity.TwitterToken;
import LDS.Person.repository.TwitterTokenRepository;
import LDS.Person.service.TwitterTokenService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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
    
    @Autowired
    private TwitterTokenHelper twitterTokenHelper;

    private static final String TWITTER_API_BASE = "https://api.x.com/2";

    /**
     * 获取个性化趋势
     */
    @GetMapping("/personalized")
    @ApiOperation(value = "获取个性化趋势", notes = "获取当前认证用户的个性化趋势")
    public ResponseEntity<TrendResponse> getPersonalizedTrends() {
        try {
            // 使用 TwitterTokenHelper 获取 Token（带回退机制）
            TwitterToken twitterToken = twitterTokenHelper.getTokenWithFallback();
            
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

    /**
     * 搜索新闻
     * 参考: https://docs.x.com/x-api/tweets/search/integrate/build-a-query
     */
    @PostMapping("/search-news")
    @ApiOperation(value = "搜索新闻", notes = "基于关键词搜索相关新闻推文")
    public ResponseEntity<NewsResponse> searchNews(
            @RequestBody @ApiParam(value = "新闻搜索请求", required = true) NewsSearchRequest request) {
        try {
            // 1. 验证请求参数
            if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                log.warn("搜索关键词不能为空");
                return ResponseEntity.badRequest()
                        .body(NewsResponse.badRequest("搜索关键词不能为空"));
            }

            String query = request.getQuery().trim();
            log.info("收到搜索新闻请求，关键词: {}", query);

            // 使用 TwitterTokenHelper 获取 Token（带回退机制）
            TwitterToken twitterToken = twitterTokenHelper.getTokenWithFallback();

            if (twitterToken == null || twitterToken.getAccessToken() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(NewsResponse.unauthorized("未找到数据库中的有效 access_token，请先登录授权"));
            }

            String accessToken = twitterToken.getAccessToken();
            log.info("✅ 已从数据库获取 access_token（userId: {}）", twitterToken.getTwitterUserId());

            // 4. 构建搜索 URL
            String baseUrl = TWITTER_API_BASE + "/tweets/search/recent";
            UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(baseUrl);

            // 添加搜索查询
            urlBuilder.queryParam("query", query);

            // 添加返回字段
            urlBuilder.queryParam("tweet.fields", "created_at,public_metrics,lang,author_id");
            urlBuilder.queryParam("user.fields", "name,username");
            urlBuilder.queryParam("expansions", "author_id");

            // 设置结果数量（默认 10，最大 100）
            int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 10;
            maxResults = Math.min(Math.max(maxResults, 1), 100);
            urlBuilder.queryParam("max_results", maxResults);

            // 设置排序方式
            if (request.getSortBy() != null && !request.getSortBy().isEmpty()) {
                urlBuilder.queryParam("sort_order", request.getSortBy());
            }

            String url = urlBuilder.toUriString();
            log.debug("调用 Twitter API: {}", url);

            // 5. 调用 Twitter API
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (apiResponse.getStatusCode() != HttpStatus.OK) {
                log.error("Twitter API 返回错误状态: {}", apiResponse.getStatusCode());
                return ResponseEntity.status(apiResponse.getStatusCode())
                        .body(NewsResponse.error("API 请求失败，状态码: " + apiResponse.getStatusCode()));
            }

            // 6. 解析响应
            JSONObject jsonResponse = JSON.parseObject(apiResponse.getBody());
            if (jsonResponse.containsKey("errors")) {
                String errorMsg = jsonResponse.getJSONArray("errors").getJSONObject(0).getString("message");
                log.warn("Twitter API 返回错误: {}", errorMsg);
                return ResponseEntity.badRequest().body(NewsResponse.badRequest(errorMsg));
            }

            // 获取用户数据（用于映射作者信息）
            JSONArray usersArray = jsonResponse.getJSONArray("includes") != null
                    ? jsonResponse.getJSONObject("includes").getJSONArray("users")
                    : null;
            java.util.Map<String, String> userMap = new java.util.HashMap<>();
            if (usersArray != null) {
                for (int i = 0; i < usersArray.size(); i++) {
                    JSONObject user = usersArray.getJSONObject(i);
                    userMap.put(user.getString("id"), user.getString("name"));
                }
            }

            // 7. 解析推文数据
            JSONArray dataArray = jsonResponse.getJSONArray("data");
            List<NewsResponse.NewsData> newsList = new ArrayList<>();

            if (dataArray != null) {
                for (int i = 0; i < dataArray.size(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    NewsResponse.NewsData news = new NewsResponse.NewsData();

                    news.setTweetId(item.getString("id"));
                    news.setText(item.getString("text"));
                    news.setCreatedAt(item.getString("created_at"));
                    news.setLang(item.getString("lang"));
                    news.setAuthorId(item.getString("author_id"));
                    news.setAuthorName(userMap.getOrDefault(item.getString("author_id"), "Unknown"));
                    news.setSource("Twitter/X");

                    // 解析公开指标
                    JSONObject metricsJson = item.getJSONObject("public_metrics");
                    if (metricsJson != null) {
                        NewsResponse.NewsData.PublicMetrics metrics = new NewsResponse.NewsData.PublicMetrics();
                        metrics.setLikeCount(metricsJson.getInteger("like_count"));
                        metrics.setRetweetCount(metricsJson.getInteger("retweet_count"));
                        metrics.setQuoteCount(metricsJson.getInteger("quote_count"));
                        metrics.setReplyCount(metricsJson.getInteger("reply_count"));
                        metrics.setImpressionCount(metricsJson.getInteger("impression_count"));
                        news.setPublicMetrics(metrics);
                    }

                    newsList.add(news);
                }
            }

            log.info("成功搜索到 {} 条新闻，关键词: {}", newsList.size(), query);
            return ResponseEntity.ok(NewsResponse.success(newsList, newsList.size()));

        } catch (Exception e) {
            log.error("搜索新闻异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NewsResponse.error("服务器错误: " + e.getMessage()));
        }
    }

    // /**
    //  * 搜索新闻（简化版 - GET 方式）
    //  */
    // @GetMapping("/simple-search-news")
    // @ApiOperation(value = "搜索新闻（GET）", notes = "基于关键词搜索相关新闻推文（简化版，使用 GET 请求）")
    // public ResponseEntity<NewsResponse> searchNewsGet(
    //         @ApiParam(value = "搜索关键词", required = true, example = "cryptocurrency") String query) {
    //     try {
    //         if (query == null || query.trim().isEmpty()) {
    //             return ResponseEntity.badRequest()
    //                     .body(NewsResponse.badRequest("搜索关键词不能为空"));
    //         }

    //         NewsSearchRequest request = new NewsSearchRequest();
    //         request.setQuery(query);
    //         request.setMaxResults(10);

    //         return searchNews(request);

    //     } catch (Exception e) {
    //         log.error("搜索新闻异常", e);
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    //                 .body(NewsResponse.error("服务器错误: " + e.getMessage()));
    //     }
    // }

}
