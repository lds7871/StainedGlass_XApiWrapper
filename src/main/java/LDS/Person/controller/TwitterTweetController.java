package LDS.Person.controller;

import LDS.Person.dto.request.CreateTweetRequest;
import LDS.Person.dto.response.CreateTweetResponse;
import LDS.Person.entity.TwitterToken;
import LDS.Person.entity.MediaLibrary;
import LDS.Person.service.TwitterTweetService;
import LDS.Person.service.TwitterTokenService;
import LDS.Person.service.MediaLibraryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}

