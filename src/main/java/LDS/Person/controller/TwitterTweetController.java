package LDS.Person.controller;

import LDS.Person.dto.request.CreateTweetRequest;
import LDS.Person.dto.request.QuoteTweetRequest;
import LDS.Person.dto.response.CreateTweetResponse;
import LDS.Person.dto.response.QuoteTweetResponse;
import LDS.Person.entity.TwitterToken;
import LDS.Person.entity.MediaLibrary;
import LDS.Person.service.TwitterTweetService;
import LDS.Person.service.TwitterTokenService;
import LDS.Person.service.MediaLibraryService;
import com.alibaba.fastjson2.JSON;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.InputStream;

/**
 * Twitter 推文控制器 - 推文相关 API
 */
@RestController
@RequestMapping("/api/twitter/tweet")
@Api(tags = "X 创建推文", description = "推文相关接口")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class TwitterTweetController {

    @Autowired
    private TwitterTweetService twitterTweetService;

    @Autowired
    private TwitterTokenService twitterTokenService;

    @Autowired
    private MediaLibraryService mediaLibraryService;

    @Autowired
    private RestTemplate restTemplate;

    private static final String TWITTER_API_BASE = "https://api.x.com/2";

    /**
     * 创建推文
     * 
     * @param request 推文请求（包含 text）
     * @return 推文创建结果
     */
    @PostMapping("/create")
    @ApiOperation(value = "创建推文", notes = "根据 config.properties 中的 DefaultUID 从数据库获取 access_token 创建推文")
    public ResponseEntity<Map<String, Object>> createTweet(
            @RequestBody CreateTweetRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证推文文本
            if (request.getText() == null || request.getText().isBlank()) {
                response.put("code", 400);
                response.put("message", "推文文本不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 从 config.properties 获取默认 UID
            Properties props = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                if (input != null) {
                    props.load(input);
                }
            } catch (java.io.IOException e) {
                log.warn("读取 config.properties 失败: {}", e.getMessage());
            }
            String userId = props.getProperty("DefaultUID", "0000000");
            
            log.info("收到创建推文请求，userId: {}（来自 config.properties），文本长度: {}", userId, request.getText().length());
            
            // 从数据库获取该用户的 access_token
            TwitterToken twitterToken = twitterTokenService.getByUserId(userId);
            if (twitterToken == null || twitterToken.getAccessToken() == null) {
                response.put("code", 401);
                response.put("message", "未找到该用户的 access_token，请重新登录");
                log.error("未能从数据库获取用户 {} 的 token", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            String accessToken = twitterToken.getAccessToken();
            log.info("已从数据库获取 access_token（用户: {}），token: {}...", userId, accessToken.substring(0, Math.min(20, accessToken.length())));
            
            // 调用 Service 创建推文
            CreateTweetResponse tweetResponse = twitterTweetService.createTweet(accessToken, request);
            
            if (tweetResponse == null) {
                response.put("code", 500);
                response.put("message", "创建推文失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            response.put("code", 200);
            response.put("message", "推文创建成功");
            response.put("data", tweetResponse);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("创建推文异常", e);
            response.put("code", 500);
            response.put("message", "服务器错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

     /**
     * 创建带媒体的推文
     * 
     * @param request 推文请求（包含 text与media）
     * @return 推文创建结果
     */

    @PostMapping("/createformedia")
    @ApiOperation(
        value = "创建带有媒体的推文",
        notes = "\"media\"字段自动上传，无需手动输入。根据 config.properties 中的 DefaultUID 从数据库获取 access_token，搜索表 media_library 中 endtime 比当前时间大的且 status=0 的媒体创建推文。"
    )
    public ResponseEntity<Map<String, Object>> createTweetWithMedia(
            @RequestBody CreateTweetRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证推文文本
            if (request.getText() == null || request.getText().isBlank()) {
                response.put("code", 400);
                response.put("message", "推文文本不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 从 config.properties 获取默认 UID
            Properties props = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                if (input != null) {
                    props.load(input);
                }
            } catch (java.io.IOException e) {
                log.warn("读取 config.properties 失败: {}", e.getMessage());
            }
            String userId = props.getProperty("DefaultUID", "0000000");
            
            log.info("收到创建带媒体推文请求，userId: {}（来自 config.properties），文本长度: {}", userId, request.getText().length());
            
            // 从数据库获取该用户的 access_token
            TwitterToken twitterToken = twitterTokenService.getByUserId(userId);
            if (twitterToken == null || twitterToken.getAccessToken() == null) {
                response.put("code", 401);
                response.put("message", "未找到该用户的 access_token，请重新登录");
                log.error("未能从数据库获取用户 {} 的 token", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            String accessToken = twitterToken.getAccessToken();
            log.info("已从数据库获取 access_token（用户: {}），token: {}...", userId, accessToken.substring(0, Math.min(20, accessToken.length())));
            
            // 查询媒体库中未过期且未使用的媒体（status=0 且 endtime > 当前时间）
            LocalDateTime now = LocalDateTime.now();
            List<MediaLibrary> allMediaList = mediaLibraryService.findAll();
            
            // 过滤：status=0 且 endtime > 当前时间
            List<MediaLibrary> availableMediaList = allMediaList.stream()
                    .filter(m -> m.getStatus() == 0 && m.getEndTime().isAfter(now))
                    .toList();
            
            if (availableMediaList.isEmpty()) {
                response.put("code", 400);
                response.put("message", "没有可用的媒体，请先上传媒体");
                log.warn("用户 {} 查询可用媒体失败，媒体库为空或全部过期/已使用", userId);
                return ResponseEntity.badRequest().body(response);
            }
            
            // 提取可用媒体的 media_id 列表
            List<String> mediaIds = availableMediaList.stream()
                    .map(MediaLibrary::getMediaId)
                    .toList();
            
            log.info("从媒体库查询到 {} 条可用媒体，media_ids: {}", availableMediaList.size(), mediaIds);
            
            // 构建包含媒体的推文请求
            CreateTweetRequest tweetRequest = new CreateTweetRequest();
            tweetRequest.setText(request.getText());
            
            // 设置媒体 ID 列表
            Map<String, Object> mediaMap = new HashMap<>();
            mediaMap.put("media_ids", mediaIds);
            tweetRequest.setMedia(mediaMap);
            
            log.info("构建推文请求: text={}, media_ids_count={}", request.getText().substring(0, Math.min(50, request.getText().length())), mediaIds.size());
            
            // 调用 Service 创建推文
            CreateTweetResponse tweetResponse = twitterTweetService.createTweet(accessToken, tweetRequest);
            
            if (tweetResponse == null) {
                response.put("code", 500);
                response.put("message", "创建推文失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            // 推文创建成功后，更新这些媒体的状态为已使用（status=1）
            for (MediaLibrary media : availableMediaList) {
                try {
                    media.setStatus(1);
                    mediaLibraryService.save(media);
                    log.info("更新媒体状态为已使用: mediaId={}", media.getMediaId());
                } catch (Exception e) {
                    log.error("更新媒体状态失败: mediaId={}", media.getMediaId(), e);
                    // 继续处理其他媒体，不中断流程
                }
            }
            
            response.put("code", 200);
            response.put("message", "推文创建成功");
            response.put("data", tweetResponse);
            response.put("used_media_count", availableMediaList.size());
            response.put("used_media_ids", mediaIds);
            log.info("推文创建成功，已使用 {} 条媒体，media_ids: {}", availableMediaList.size(), mediaIds);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("创建带媒体推文异常", e);
            response.put("code", 500);
            response.put("message", "服务器错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 引用推文
     * 
     * @param request 引用推文请求（包含 data 推文文本和 quote_tweet_id 被引用推文ID）
     * @return 引用推文结果
     * 参考: https://docs.x.com/x-api/posts/create-post
     */
    @PostMapping("/quote")
    @ApiOperation(
        value = "引用推文",
        notes = "根据 config.properties 中的 DefaultUID 从数据库获取 access_token 创建引用推文。请求JSON格式: {\"Text\": \"推文内容\", \"quote_tweet_id\": \"被引用的推文ID\"}"
    )
    public ResponseEntity<QuoteTweetResponse> quoteTweet(@RequestBody QuoteTweetRequest request) {
        try {
            // 验证请求
            if (request == null || request.getText() == null || request.getText().isBlank()) {
                QuoteTweetResponse resp = QuoteTweetResponse.badRequest("推文文本（Text）不能为空");
                log.warn("引用推文请求缺少 Text 参数");
                return ResponseEntity.badRequest().body(resp);
            }

            if (request.getQuote_tweet_id() == null || request.getQuote_tweet_id().isBlank()) {
                QuoteTweetResponse resp = QuoteTweetResponse.badRequest("引用推文ID（quote_tweet_id）不能为空");
                log.warn("引用推文请求缺少 quote_tweet_id 参数");
                return ResponseEntity.badRequest().body(resp);
            }

            // 从 config.properties 获取默认 UID
            Properties props = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                if (input != null) {
                    props.load(input);
                }
            } catch (java.io.IOException e) {
                log.warn("读取 config.properties 失败: {}", e.getMessage());
            }
            String userId = props.getProperty("DefaultUID", "0000000");

            log.info("收到引用推文请求，userId: {}（来自 config.properties），Text: {}，quote_tweet_id: {}", 
                     userId, request.getText(), request.getQuote_tweet_id());

            // 从数据库获取该用户的 access_token
            TwitterToken twitterToken = twitterTokenService.getByUserId(userId);
            if (twitterToken == null || twitterToken.getAccessToken() == null) {
                QuoteTweetResponse resp = QuoteTweetResponse.unauthorized("未找到该用户的 access_token，请先登录授权");
                log.error("未能从数据库获取用户 {} 的 token", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
            }

            String accessToken = twitterToken.getAccessToken();
            log.info("已从数据库获取 access_token（用户: {}），token: {}...", userId, 
                    accessToken.substring(0, Math.min(20, accessToken.length())));

            // 调用 Twitter API 创建引用推文
            Map<String, Object> quoteResult = sendQuoteTweetRequest(userId, request.getText(), request.getQuote_tweet_id(), accessToken);

            if (quoteResult == null) {
                QuoteTweetResponse resp = QuoteTweetResponse.serverError("引用推文失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
            }

            if (quoteResult.containsKey("error")) {
                QuoteTweetResponse resp = QuoteTweetResponse.badRequest((String) quoteResult.get("error"));
                log.warn("Twitter API 返回错误: {}", quoteResult);
                return ResponseEntity.badRequest().body(resp);
            }

            log.info("✅ 成功创建引用推文，userId: {}，quote_tweet_id: {}", userId, request.getQuote_tweet_id());
            return ResponseEntity.ok(QuoteTweetResponse.success(quoteResult));

        } catch (Exception e) {
            log.error("引用推文异常", e);
            QuoteTweetResponse resp = QuoteTweetResponse.serverError("服务器错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }

    /**
     * 发送引用推文请求到 Twitter API
     * 
     * @param userId 用户 ID
     * @param text 推文文本
     * @param quoteTweetId 被引用的推文 ID
     * @param accessToken 访问令牌
     * @return 引用推文结果
     */
    private Map<String, Object> sendQuoteTweetRequest(String userId, String text, String quoteTweetId, String accessToken) {
        try {
            // 构建 API 请求 URL: POST /2/tweets
            String url = String.format("%s/tweets", TWITTER_API_BASE);
            log.debug("调用 Twitter API: {}", url);

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("text", text);
            
            // 添加引用推文信息
            JSONObject quoteOptions = new JSONObject();
            quoteOptions.put("type", "Quote");
            requestBody.put("quote_tweet_id", quoteTweetId);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            log.debug("发送到 Twitter API 的请求体: {}", requestBody.toJSONString());

            // 执行 POST 请求
            ResponseEntity<String> apiResponse = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    entity, 
                    String.class
            );

            String responseBody = apiResponse.getBody();
            log.debug("Twitter API 引用推文响应: {}", responseBody);

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
            result.put("tweet_id", data.getString("id"));
            result.put("text", text);
            result.put("quote_tweet_id", quoteTweetId);
            result.put("created_at", data.getString("created_at"));
            
            log.info("成功创建引用推文，API 返回: {}", data);
            return result;

        } catch (HttpClientErrorException e) {
            String err = e.getResponseBodyAsString();
            log.error("调用 Twitter API 引用推文失败: {} (状态码: {})", err, e.getStatusCode(), e);
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

