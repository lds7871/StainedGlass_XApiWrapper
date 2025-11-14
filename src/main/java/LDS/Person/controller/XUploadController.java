package LDS.Person.controller;

import LDS.Person.dto.request.UploadLocalMediaRequest;
import LDS.Person.dto.request.UploadFileMediaRequest;
import LDS.Person.dto.response.UploadMediaResponse;
import LDS.Person.entity.TwitterToken;
import LDS.Person.entity.MediaLibrary;
import LDS.Person.repository.TwitterTokenRepository;
import LDS.Person.service.TwitterTokenService;
import LDS.Person.service.MediaLibraryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Properties;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * X (Twitter) 媒体上传控制器 - 媒体上传相关 API
 * 用于上传图片、视频等媒体文件到 Twitter，获取 media_id 后用于推文
 */
@RestController
@RequestMapping("/api/x/media")
@Api(tags = "X 媒体上传", description = "媒体上传接口")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class XUploadController {

    @Autowired
    private TwitterTokenService twitterTokenService;

    @Autowired
    private MediaLibraryService mediaLibraryService;

    @Autowired
    private TwitterTokenRepository twitterTokenRepository;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 上传本地媒体文件（从 config.properties 的 saveimgdir 目录随机选择一个 PNG 文件）
     * 
     * @param request 上传请求（可选的 mediaCategory 和 mediaType，均有默认值）
     * @return 媒体上传结果（包含 media_id, media_key 等）
     */
    @PostMapping("/upload-local")
    @ApiOperation(
        value = "上传本地媒体",
        notes = "从 config.properties 的 saveimgdir 目录随机选择一个 PNG 文件，调用 Twitter /2/media/upload 接口上传媒体。使用 config.properties 的 DefaultUID 作为用户身份，从数据库获取其 Token 进行认证"
    )
    public ResponseEntity<UploadMediaResponse> uploadLocalMedia(
            @RequestBody(required = false) UploadLocalMediaRequest request) {
        
        try {
            // 初始化请求对象（如果为 null）
            if (request == null) {
                request = new UploadLocalMediaRequest();
            }
            
            String mediaCategory = request.getMediaCategory() != null ? request.getMediaCategory() : "tweet_image";
            String mediaType = request.getMediaType() != null ? request.getMediaType() : "image/png";
            
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
            
            log.info("收到上传本地媒体请求，userId（来自 config.properties）: {}, mediaCategory: {}, mediaType: {}", 
                    userId, mediaCategory, mediaType);
            
            // 从数据库获取该用户的 Token
            TwitterToken twitterToken = twitterTokenService.getByUserId(userId);
            if (twitterToken == null || twitterToken.getAccessToken() == null) {
                UploadMediaResponse resp = UploadMediaResponse.unauthorized("未找到该用户的 access_token，请先登录授权");
                log.error("未能从数据库获取用户 {} 的 token", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
            }
            
            String accessToken = twitterToken.getAccessToken();
            log.info("✅ 已从数据库获取 access_token（用户: {}），Token: {}...", 
                    userId, accessToken.substring(0, Math.min(20, accessToken.length())));
            
            // 检查 token 权限范围，确保包含 tweet.write 权限
            String scope = twitterToken.getScope();
            if (scope != null && !scope.contains("tweet.write")) {
                log.warn("Token 权限不足，缺少 tweet.write 权限。当前权限: {}", scope);
                UploadMediaResponse resp = UploadMediaResponse.forbidden(
                    "Token 权限不足，需要 tweet.write 权限来上传媒体。请重新登录并授予必要权限");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resp);
            }
            
            // 从 config.properties 读取 saveimgdir 目录
            String saveImgDir = props.getProperty("saveimgdir");
            File saveImgFolder = new File(saveImgDir);
            
            if (!saveImgFolder.exists() || !saveImgFolder.isDirectory()) {
                UploadMediaResponse resp = UploadMediaResponse.badRequest(
                    String.format("SaveImg 文件夹不存在: %s", saveImgDir));
                log.error("SaveImg 文件夹不存在: {}", saveImgDir);
                return ResponseEntity.badRequest().body(resp);
            }
            
            // 获取所有 PNG 文件
            File[] pngFiles = saveImgFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            
            if (pngFiles == null || pngFiles.length == 0) {
                UploadMediaResponse resp = UploadMediaResponse.badRequest("SaveImg 文件夹中没有 PNG 图片");
                log.error("SaveImg 文件夹中没有找到 PNG 文件");
                return ResponseEntity.badRequest().body(resp);
            }
            
            // 随机抽取一个 PNG 文件
            Random random = new Random();
            File selectedImageFile = pngFiles[random.nextInt(pngFiles.length)];
            
            log.info("从 SaveImg 文件夹随机抽取 PNG 文件: {}，共有 {} 个 PNG 文件可用", 
                    selectedImageFile.getName(), pngFiles.length);
            
            // 读取文件字节流
            byte[] fileBytes = Files.readAllBytes(selectedImageFile.toPath());
            log.info("已读取随机抽取的图片文件，文件名: {}，大小: {} 字节", 
                    selectedImageFile.getName(), fileBytes.length);
            
            // 构建 multipart form-data 请求
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("User-Agent", "PersonLog/1.0 (Twitter Media Upload)");
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            final String fileName = selectedImageFile.getName();
            body.add("media", new org.springframework.core.io.ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            });
            body.add("media_category", mediaCategory);
            body.add("media_type", mediaType);
            
            log.info("Request headers: Authorization Bearer ***, User-Agent: PersonLog/1.0");
            log.info("Request body: media_type={}, media_category={}, file_size={} bytes", mediaType, mediaCategory, fileBytes.length);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // 调用 Twitter API
            String twitterMediaUploadUrl = "https://api.x.com/2/media/upload";
            log.info("准备调用 Twitter 媒体上传接口: {}", twitterMediaUploadUrl);
            
            ResponseEntity<Map> twitterResponse = restTemplate.postForEntity(
                    twitterMediaUploadUrl,
                    requestEntity,
                    Map.class
            );
            
            log.info("Twitter 媒体上传接口返回状态码: {}", twitterResponse.getStatusCode());
            
            if (twitterResponse.getStatusCode().is2xxSuccessful() && twitterResponse.getBody() != null) {
                Map<String, Object> twitterData = twitterResponse.getBody();
                
                // 检查是否有错误信息
                if (twitterData.containsKey("errors")) {
                    UploadMediaResponse resp = UploadMediaResponse.badRequest("Twitter API 返回错误");
                    log.error("Twitter API 返回错误: {}", twitterData.get("errors"));
                    return ResponseEntity.badRequest().body(resp);
                }
                
                // 保存媒体记录到数据库
                try {
                    Map<String, Object> dataMap = (Map<String, Object>) twitterData.get("data");
                    if (dataMap != null) {
                        MediaLibrary mediaLibrary = MediaLibrary.builder()
                                .mediaId(String.valueOf(dataMap.get("id")))
                                .mediaKey(String.valueOf(dataMap.get("media_key")))
                                .createTime(LocalDateTime.now())
                                .endTime(LocalDateTime.now().plusHours(24))
                                .status(0)
                                .build();
                        
                        mediaLibraryService.save(mediaLibrary);
                        log.info("媒体记录已保存到数据库: mediaId={}, mediaKey={}", 
                                mediaLibrary.getMediaId(), 
                                mediaLibrary.getMediaKey());
                    }
                } catch (Exception e) {
                    log.error("保存媒体记录到数据库失败", e);
                    // 继续返回成功，不影响用户流程
                }
                
                // 返回成功响应
                UploadMediaResponse resp = UploadMediaResponse.success(twitterData.get("data"));
                log.info("✅ 媒体上传成功");
                return ResponseEntity.ok(resp);
            } else {
                UploadMediaResponse resp = UploadMediaResponse.serverError(
                    String.format("Twitter API 返回错误状态码: %s", twitterResponse.getStatusCode()));
                log.error("Twitter API 返回错误状态码: {}", twitterResponse.getStatusCode());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
            }
            
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            log.error("Twitter API 返回 HTTP 错误，状态码: {}, 响应体: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            UploadMediaResponse resp = new UploadMediaResponse(
                ex.getStatusCode().value(),
                "Twitter API 错误: " + ex.getStatusCode(),
                null
            );
            return ResponseEntity.status(ex.getStatusCode()).body(resp);
        } catch (Exception e) {
            log.error("媒体上传异常", e);
            UploadMediaResponse resp = UploadMediaResponse.serverError("服务器错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }

    /**
     * 上传指定路径的媒体文件
     * 
     * @param request 上传请求（包含 filePath、mediaCategory、mediaType）
     * @return 媒体上传结果
     */
    @PostMapping("/upload-file")
    @ApiOperation(
        value = "上传指定路径的媒体",
        notes = "上传指定路径的媒体文件到 Twitter，返回 media_id。支持本地文件路径和 URL。使用 config.properties 的 DefaultUID 作为用户身份，从数据库获取其 Token 进行认证"
    )
    public ResponseEntity<UploadMediaResponse> uploadFileMedia(
            @RequestBody UploadFileMediaRequest request) {
        
        try {
            // 验证必填参数
            if (request == null || request.getFilePath() == null || request.getFilePath().isBlank()) {
                UploadMediaResponse resp = UploadMediaResponse.badRequest("filePath 不能为空");
                log.warn("上传请求缺少 filePath 参数");
                return ResponseEntity.badRequest().body(resp);
            }
            
            String filePath = request.getFilePath();
            String mediaCategory = request.getMediaCategory() != null ? request.getMediaCategory() : "tweet_image";
            String mediaType = request.getMediaType() != null ? request.getMediaType() : "image/png";
            
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
            
            log.info("收到上传文件媒体请求，userId（来自 config.properties）: {}，filePath: {}，mediaCategory: {}，mediaType: {}", 
                    userId, filePath, mediaCategory, mediaType);
            
            // 从数据库获取该用户的 Token
            TwitterToken twitterToken = twitterTokenService.getByUserId(userId);
            if (twitterToken == null || twitterToken.getAccessToken() == null) {
                UploadMediaResponse resp = UploadMediaResponse.unauthorized("未找到该用户的 access_token，请先登录授权");
                log.error("未能从数据库获取用户 {} 的 token", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
            }
            
            String accessToken = twitterToken.getAccessToken();
            log.info("✅ 已从数据库获取 access_token（用户: {}），Token: {}...", 
                    userId, accessToken.substring(0, Math.min(20, accessToken.length())));
            
            // 检查 token 权限范围，确保包含 tweet.write 权限
            String scope = twitterToken.getScope();
            if (scope != null && !scope.contains("tweet.write")) {
                log.warn("Token 权限不足，缺少 tweet.write 权限。当前权限: {}", scope);
                UploadMediaResponse resp = UploadMediaResponse.forbidden(
                    "Token 权限不足，需要 tweet.write 权限来上传媒体。请重新登录并授予必要权限");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resp);
            }
            
            // 判断是否为 URL 或本地文件路径
            byte[] fileBytes;
            String fileName;
            
            if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                // URL 方式读取
                log.info("检测到 URL 路径，准备从网络读取文件: {}", filePath);
                try {
                    java.net.URL url = new java.net.URL(filePath);
                    java.net.URLConnection connection = url.openConnection();
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    
                    try (InputStream inputStream = connection.getInputStream()) {
                        fileBytes = inputStream.readAllBytes();
                    }
                    
                    // 从 URL 中提取文件名
                    fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
                    if (fileName.contains("?")) {
                        fileName = fileName.substring(0, fileName.indexOf("?"));
                    }
                    if (fileName.isBlank()) {
                        fileName = "media_" + System.currentTimeMillis();
                    }
                    
                    log.info("已从 URL 读取文件，大小: {} 字节，文件名: {}", fileBytes.length, fileName);
                } catch (Exception e) {
                    UploadMediaResponse resp = UploadMediaResponse.badRequest("无法从 URL 读取文件: " + e.getMessage());
                    log.error("从 URL 读取文件失败: {}", filePath, e);
                    return ResponseEntity.badRequest().body(resp);
                }
            } else {
                // 本地文件方式读取
                File mediaFile = new File(filePath);
                if (!mediaFile.exists()) {
                    UploadMediaResponse resp = UploadMediaResponse.badRequest(
                        String.format("文件不存在: %s", filePath));
                    log.error("文件不存在: {}", mediaFile.getAbsolutePath());
                    return ResponseEntity.badRequest().body(resp);
                }
                
                fileBytes = Files.readAllBytes(Paths.get(filePath));
                fileName = mediaFile.getName();
                log.info("已读取本地文件，大小: {} 字节", fileBytes.length);
            }
            
            log.info("准备校验媒体，文件名: {}, 大小: {}", fileName, fileBytes.length);
            
            // 验证 media_type 和 media_category 的有效性
            if (!isValidMediaType(mediaType)) {
                UploadMediaResponse resp = UploadMediaResponse.badRequest(
                    "无效的媒体类型: " + mediaType + "。支持的类型: image/jpeg, image/png, image/gif, image/webp, video/mp4");
                log.error("无效的媒体类型: {}", mediaType);
                return ResponseEntity.badRequest().body(resp);
            }
            
            if (!isValidMediaCategory(mediaCategory)) {
                UploadMediaResponse resp = UploadMediaResponse.badRequest(
                    "无效的媒体类别: " + mediaCategory + "。支持的类别: tweet_image, tweet_gif, tweet_video, dm_image, dm_gif, dm_video");
                log.error("无效的媒体类别: {}", mediaCategory);
                return ResponseEntity.badRequest().body(resp);
            }
            
            // 构建 multipart form-data 请求
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("User-Agent", "PersonLog/1.0 (Twitter Media Upload)");
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            final String finalFileName = fileName;
            body.add("media", new org.springframework.core.io.ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return finalFileName;
                }
            });
            body.add("media_category", mediaCategory);
            body.add("media_type", mediaType);
            
            log.info("Request headers: Authorization Bearer ***, User-Agent: PersonLog/1.0");
            log.info("Request body: media_type={}, media_category={}, file_size={} bytes", mediaType, mediaCategory, fileBytes.length);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // 调用 Twitter API
            String twitterMediaUploadUrl = "https://api.x.com/2/media/upload";
            ResponseEntity<Map> twitterResponse = restTemplate.postForEntity(
                    twitterMediaUploadUrl,
                    requestEntity,
                    Map.class
            );
            
            if (twitterResponse.getStatusCode().is2xxSuccessful() && twitterResponse.getBody() != null) {
                Map<String, Object> twitterData = twitterResponse.getBody();
                
                if (twitterData.containsKey("errors")) {
                    UploadMediaResponse resp = UploadMediaResponse.badRequest("Twitter API 返回错误");
                    log.error("Twitter API 返回错误: {}", twitterData.get("errors"));
                    return ResponseEntity.badRequest().body(resp);
                }
                
                // 保存媒体记录到数据库
                try {
                    Map<String, Object> dataMap = (Map<String, Object>) twitterData.get("data");
                    if (dataMap != null) {
                        MediaLibrary mediaLibrary = MediaLibrary.builder()
                                .mediaId(String.valueOf(dataMap.get("id")))
                                .mediaKey(String.valueOf(dataMap.get("media_key")))
                                .createTime(LocalDateTime.now())
                                .endTime(LocalDateTime.now().plusHours(24))
                                .status(0)
                                .build();
                        
                        mediaLibraryService.save(mediaLibrary);
                        log.info("媒体记录已保存到数据库: mediaId={}, mediaKey={}", 
                                mediaLibrary.getMediaId(), 
                                mediaLibrary.getMediaKey());
                    }
                } catch (Exception e) {
                    log.error("保存媒体记录到数据库失败", e);
                    // 继续返回成功，不影响用户流程
                }
                
                UploadMediaResponse resp = UploadMediaResponse.success(twitterData.get("data"));
                log.info("✅ 媒体上传成功");
                return ResponseEntity.ok(resp);
            } else {
                UploadMediaResponse resp = UploadMediaResponse.serverError(
                    String.format("Twitter API 返回错误状态码: %s", twitterResponse.getStatusCode()));
                log.error("Twitter API 返回错误状态码: {}", twitterResponse.getStatusCode());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
            }
            
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            log.error("Twitter API 返回 HTTP 错误，状态码: {}, 响应体: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            UploadMediaResponse resp = new UploadMediaResponse(
                ex.getStatusCode().value(),
                "Twitter API 错误: " + ex.getStatusCode(),
                null
            );
            return ResponseEntity.status(ex.getStatusCode()).body(resp);
        } catch (Exception e) {
            log.error("媒体上传异常", e);
            UploadMediaResponse resp = UploadMediaResponse.serverError("服务器错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }

    /**
     * 查询所有媒体记录
     * 返回媒体库中的所有媒体信息（倒序排列，最多前 20 条）
     */
    @GetMapping("/list")
    @ApiOperation(
        value = "查询媒体记录列表",
        notes = "返回媒体库中的媒体信息，按创建时间倒序排列，最多返回前 20 条记录"
    )
    public ResponseEntity<Map<String, Object>> listAllMedia() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<MediaLibrary> mediaList = mediaLibraryService.findAll();
            
            // 按创建时间倒序排列（最新的在前）
            mediaList.sort((m1, m2) -> {
                if (m1.getCreateTime() == null || m2.getCreateTime() == null) {
                    return 0;
                }
                return m2.getCreateTime().compareTo(m1.getCreateTime());
            });
            
            //配置文件读取ediagetlimit
            Properties props = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                if (input != null) {
                    props.load(input);
                }
            }
            int mediagetlimit = Integer.parseInt(props.getProperty("mediagetlimit", "20"));
            int limit = mediagetlimit;
            List<MediaLibrary> topRecords = mediaList.size() > limit 
                    ? mediaList.subList(0, limit) 
                    : mediaList;
            
            response.put("code", 200);
            response.put("message", "✅ 查询成功");
            response.put("data", topRecords);
            response.put("total", mediaList.size());
            response.put("returned", topRecords.size());
            response.put("limit", limit);
            log.info("查询媒体列表成功，共 {} 条记录，返回前 {} 条（倒序排列）", mediaList.size(), topRecords.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询媒体列表异常", e);
            response.put("code", 500);
            response.put("message", "后端服务器错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 验证媒体类型是否有效
     * 支持的类型: image/jpeg, image/png, image/gif, image/webp, video/mp4
     */
    private boolean isValidMediaType(String mediaType) {
        if (mediaType == null) return false;
        String[] validTypes = {"image/jpeg", "image/png", "image/gif", "image/webp", "video/mp4"};
        for (String type : validTypes) {
            if (type.equalsIgnoreCase(mediaType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证媒体类别是否有效
     * 支持的类别: tweet_image, tweet_gif, tweet_video, dm_image, dm_gif, dm_video
     */
    private boolean isValidMediaCategory(String mediaCategory) {
        if (mediaCategory == null) return false;
        String[] validCategories = {"tweet_image", "tweet_gif", "tweet_video", "dm_image", "dm_gif", "dm_video"};
        for (String category : validCategories) {
            if (category.equalsIgnoreCase(mediaCategory)) {
                return true;
            }
        }
        return false;
    }
}
