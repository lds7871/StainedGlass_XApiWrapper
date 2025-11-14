package LDS.Person.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import LDS.Person.dto.response.TokenRefreshResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Twitter API 客户端 - 处理 OAuth 2.0 流程
 * 支持授权码流程（Authorization Code Flow）
 */
@Component
@Slf4j
public class TwitterApiClient {

  @Autowired
  private TwitterProperties twitterProperties;

  private RestTemplate restTemplate;

  @Autowired
  public TwitterApiClient(RestTemplateBuilder builder) {
    // 硬编码代理配置（用于突破网络限制）
    String proxyHost = "127.0.0.1";
    int proxyPort = 33210; // HTTP 代理端口（支持 HTTPS CONNECT 隧道）
    
    log.info("🔄 使用硬编码代理配置: {}:{}", proxyHost, proxyPort);
    
    try {
      // 创建带代理的 RestTemplate
      ClientHttpRequestFactory requestFactory = createProxyRequestFactory(proxyHost, proxyPort);
      this.restTemplate = builder
          .requestFactory(() -> requestFactory)
          .setConnectTimeout(java.time.Duration.ofSeconds(30))
          .setReadTimeout(java.time.Duration.ofSeconds(30))
          .build();
      log.info("✅ RestTemplate 已初始化（使用代理: {}:{}，用于访问 Twitter API）", proxyHost, proxyPort);
    } catch (Exception e) {
      log.warn("⚠️  代理配置异常，降级到直连: {}", e.getMessage());
      this.restTemplate = builder
          .setConnectTimeout(java.time.Duration.ofSeconds(30))
          .setReadTimeout(java.time.Duration.ofSeconds(30))
          .build();
      log.info("✅ RestTemplate 已初始化（直连）");
    }
  }

  /**
   * 创建支持代理的 ClientHttpRequestFactory
   */
  private ClientHttpRequestFactory createProxyRequestFactory(String proxyHost, int proxyPort) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    
    // 设置代理
    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
    factory.setProxy(proxy);
    
    // 设置超时（秒）
    factory.setConnectTimeout(30000);
    factory.setReadTimeout(30000);
    
    // 使用 BufferingClientHttpRequestFactory 包装（支持重复读取响应体，某些情况下有用）
    return new BufferingClientHttpRequestFactory(factory);
  }

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  /**
   * 生成授权 URL，用于重定向用户到 Twitter 授权页面
   *
   * @param state         CSRF 防护令牌
   * @param codeChallenge PKCE 代码挑战（可选）
   * @return 完整的授权 URL
   */
  public String generateAuthorizationUrl(String state, String codeChallenge) {
    StringBuilder url = new StringBuilder(twitterProperties.getAuthorizationUrl());
    url.append("?client_id=").append(twitterProperties.getClientId());
    url.append("&redirect_uri=").append(urlEncode(twitterProperties.getCallbackUrl()));
    url.append("&response_type=code");
    url.append("&state=").append(state);
    url.append("&scope=").append(urlEncode(twitterProperties.getScopes()));

    if (codeChallenge != null && !codeChallenge.isEmpty()) {
      url.append("&code_challenge=").append(codeChallenge);
      url.append("&code_challenge_method=S256");
    }

    log.info("生成的授权 URL: {}", url);
    return url.toString();
  }

  /**
   * 生成符合规范的 PKCE code verifier
   */
  public String generateCodeVerifier() {
    byte[] code = new byte[32];
    SECURE_RANDOM.nextBytes(code);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(code);
  }

  /**
   * 根据 code verifier 生成 PKCE code challenge（S256）
   */
  public String generateCodeChallenge(String codeVerifier) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("无法生成 code challenge: SHA-256 不可用", e);
    }
  }

  /**
   * 使用授权码交换访问令牌
   *
   * @param code         授权码（来自回调）
   * @param codeVerifier PKCE 代码验证器（可选）
   * @return 包含 access_token 的响应
   */
  public TwitterTokenResponse exchangeCodeForToken(String code, String codeVerifier) {
    try {
      log.info("开始交换授权码以获取 token，code: {}", code);

      // 构建请求参数
      Map<String, String> params = new HashMap<>();
      params.put("grant_type", "authorization_code");
      params.put("code", code);
      params.put("redirect_uri", twitterProperties.getCallbackUrl());
      params.put("client_id", twitterProperties.getClientId());

      if (codeVerifier != null && !codeVerifier.isEmpty()) {
        params.put("code_verifier", codeVerifier);
      }

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("Accept", "application/json");

      // 使用 Basic Auth 携带 client_id:client_secret
      String clientSecret = twitterProperties.getClientSecret();
      if (clientSecret != null && !clientSecret.isBlank()) {
        String credentials = twitterProperties.getClientId() + ":" + clientSecret;
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        headers.put("Authorization", "Basic " + basicAuth);
      }

      // 发送 POST 请求到 Token 端点
      String paramsStr = buildFormUrlEncoded(params);

      String response = postRequest(twitterProperties.getTokenUrl(), paramsStr, headers);

      log.debug("Token 响应: {}", response);

      // 解析响应
      JSONObject jsonResponse = JSON.parseObject(response);
      if (jsonResponse.containsKey("error")) {
        log.error("Token 交换失败: {}", jsonResponse.get("error_description"));
        return null;
      }

      TwitterTokenResponse tokenResponse = new TwitterTokenResponse();
      tokenResponse.setAccessToken(jsonResponse.getString("access_token"));
      tokenResponse.setTokenType(jsonResponse.getString("token_type"));
      tokenResponse.setExpiresIn(jsonResponse.getIntValue("expires_in"));
      tokenResponse.setRefreshToken(jsonResponse.getString("refresh_token"));

      log.info("成功获取 access token，有效期: {} 秒", tokenResponse.getExpiresIn());
      return tokenResponse;

    } catch (Exception e) {
      log.error("交换授权码时出错", e);
      return null;
    }
  }

  /**
   * 使用 access token 获取当前用户信息
   *
   * @param accessToken 访问令牌
   * @return 用户信息
   */
  public TwitterUserInfo getUserInfo(String accessToken) {
    try {
      log.info("获取用户信息");

      Map<String, String> headers = new HashMap<>();
      headers.put("Authorization", "Bearer " + accessToken);
      headers.put("Accept", "application/json");

      // 添加字段参数以获取更多信息
      String url = twitterProperties.getUserInfoUrl() + "?user.fields=id,name,username,created_at,profile_image_url";

      String response = getRequest(url, headers);

      log.debug("用户信息响应: {}", response);

      JSONObject jsonResponse = JSON.parseObject(response);
      if (jsonResponse.containsKey("errors")) {
        log.error("获取用户信息失败: {}", jsonResponse);
        return null;
      }

      JSONObject data = jsonResponse.getJSONObject("data");
      TwitterUserInfo userInfo = new TwitterUserInfo();
      userInfo.setId(data.getString("id"));
      userInfo.setName(data.getString("name"));
      userInfo.setUsername(data.getString("username"));
      userInfo.setCreatedAt(data.getString("created_at"));
      userInfo.setProfileImageUrl(data.getString("profile_image_url"));

      log.info("成功获取用户信息: userId={}, username={}", userInfo.getId(), userInfo.getUsername());
      return userInfo;

    } catch (Exception e) {
      log.error("获取用户信息时出错", e);
      return null;
    }
  }



  /**
   * 执行 GET 请求
   */
  private String getRequest(String url, Map<String, String> headers) {
    try {
      org.springframework.http.HttpHeaders httpHeaders = new org.springframework.http.HttpHeaders();
      headers.forEach(httpHeaders::set);

      org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(httpHeaders);
      org.springframework.http.ResponseEntity<String> response =
          restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);

      return response.getBody();
    } catch (HttpClientErrorException e) {
      log.error("GET 请求失败: {} -> 状态码: {}, 响应体: {}", url, e.getStatusCode(), e.getResponseBodyAsString());
      throw e;
    } catch (Exception e) {
      log.error("GET 请求失败: {}", url, e);
      throw e;
    }
  }

  /**
   * 执行 POST 请求
   */
  private String postRequest(String url, String body, Map<String, String> headers) {
    try {
      org.springframework.http.HttpHeaders httpHeaders = new org.springframework.http.HttpHeaders();
      headers.forEach(httpHeaders::set);

      org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(body, httpHeaders);
      org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

      return response.getBody();
    } catch (HttpClientErrorException e) {
      log.error("POST 请求失败: {} -> 状态码: {}, 响应体: {}", url, e.getStatusCode(), e.getResponseBodyAsString());
      throw e;
    } catch (Exception e) {
      log.error("POST 请求失败: {}", url, e);
      throw e;
    }
  }

  /**
   * 公开的 POST 请求方法，供 Controller 调用
   */
  public String postToTwitterApi(String url, String body, String accessToken) {
    try {
      log.info("🔍 postToTwitterApi 接收到 access token: {}...", 
          accessToken != null ? accessToken.substring(0, Math.min(20, accessToken.length())) : "NULL");
      
      org.springframework.http.HttpHeaders httpHeaders = new org.springframework.http.HttpHeaders();
      
      if (accessToken == null || accessToken.isBlank()) {
        log.error("❌ access token 为空或 null，无法添加 Authorization header");
      } else {
        String authHeader = "Bearer " + accessToken;
        httpHeaders.set("Authorization", authHeader);
        log.info("✅ 已添加 Authorization header: Bearer {}...", accessToken.substring(0, Math.min(20, accessToken.length())));
      }
      
      httpHeaders.set("Content-Type", "application/json");
      httpHeaders.set("Accept", "application/json");

      org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(body, httpHeaders);
      log.debug("📤 发送 POST 请求到: {}, headers: {}", url, httpHeaders);
      
      org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

      return response.getBody();
    } catch (HttpClientErrorException e) {
      log.error("Twitter API POST 请求失败: {} -> 状态码: {}, 响应体: {}", url, e.getStatusCode(), e.getResponseBodyAsString());
      throw e;
    } catch (Exception e) {
      log.error("Twitter API POST 请求失败: {}", url, e);
      throw e;
    }
  }

  /**
   * 使用 refresh_token 刷新 access_token
   * 根据 Twitter 官方文档：https://docs.x.com/fundamentals/authentication/oauth-2-0/authorization-code
   * refresh token 请求只需要 grant_type, refresh_token, client_id
   * 不需要 client_secret（因为 Authorization Code Flow 已在获取 access token 时验证过）
   */
  public TokenRefreshResponse refreshAccessToken(String refreshToken) {
    try {
      log.info("🔄 开始刷新 access_token，refresh_token: {}...", refreshToken.substring(0, Math.min(20, refreshToken.length())));

      Map<String, String> params = new HashMap<>();
      params.put("grant_type", "refresh_token");
      params.put("refresh_token", refreshToken);
      params.put("client_id", twitterProperties.getClientId());

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("Accept", "application/json");

      // 如果配置了 clientSecret，则使用 Basic Auth 做客户端认证（更兼容 Twitter 的要求）
      String clientSecret = twitterProperties.getClientSecret();
      if (clientSecret != null && !clientSecret.isBlank()) {
        String credentials = twitterProperties.getClientId() + ":" + clientSecret;
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        headers.put("Authorization", "Basic " + basicAuth);
        log.debug("使用 Basic Auth 进行 client 认证");
      } else {
        log.debug("未配置 clientSecret，使用无密方式请求（仅 client_id）");
      }

      String paramsStr = buildFormUrlEncoded(params);
      log.debug("刷新 token 请求参数: grant_type=refresh_token, client_id=***, refresh_token=***");
      String response = null;
      try {
        response = postRequest(twitterProperties.getTokenUrl(), paramsStr, headers);
      } catch (HttpClientErrorException he) {
        log.error("POST 请求失败: {} -> 状态码: {}, 响应体: {}", twitterProperties.getTokenUrl(), he.getStatusCode(), he.getResponseBodyAsString());
        throw he;
      }

      log.debug("刷新 token 响应: {}", response);

      JSONObject jsonResponse = JSON.parseObject(response == null ? "{}" : response);
      if (jsonResponse.containsKey("error")) {
        log.error("刷新 token 失败: {}", jsonResponse.get("error_description"));
        return null;
      }

      TokenRefreshResponse result = new TokenRefreshResponse();
      result.setAccessToken(jsonResponse.getString("access_token"));
      result.setTokenType(jsonResponse.getString("token_type"));
      // null-safe 解析 expires_in
      if (jsonResponse.containsKey("expires_in") && !jsonResponse.get("expires_in").toString().isBlank()) {
        try {
          result.setExpiresIn(jsonResponse.getLongValue("expires_in"));
        } catch (Exception ex) {
          log.warn("解析 expires_in 失败: {}", jsonResponse.get("expires_in"));
        }
      }
      result.setRefreshToken(jsonResponse.getString("refresh_token")); // 可能没有
      result.setScope(jsonResponse.getString("scope"));

      log.info("✅ 成功刷新 access_token，有效期: {} 秒", result.getExpiresIn());
      return result;

    } catch (HttpClientErrorException he) {
      // 已在 postRequest 捕获打印，但这里再记录一次堆栈
      log.error("刷新 token 时收到 HTTP 错误", he);
      throw he;
    } catch (Exception e) {
      log.error("刷新 token 时出错", e);
      return null;
    }
  }

  /**
   * 构建 URL 编码的表单数据
   */
  private String buildFormUrlEncoded(Map<String, String> params) {
    StringBuilder sb = new StringBuilder();
    params.forEach((key, value) -> {
      if (sb.length() > 0) {
        sb.append("&");
      }
      sb.append(key).append("=").append(urlEncode(value));
    });
    return sb.toString();
  }

  /**
   * URL 编码
   */
  private String urlEncode(String str) {
    try {
      return java.net.URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
    } catch (Exception e) {
      log.error("URL 编码失败", e);
      return str;
    }
  }

  /**
   * Twitter Token 响应
   */
  public static class TwitterTokenResponse {
    private String accessToken;
    private String tokenType;
    private Integer expiresIn;
    private String refreshToken;

    public String getAccessToken() {
      return accessToken;
    }

    public void setAccessToken(String accessToken) {
      this.accessToken = accessToken;
    }

    public String getTokenType() {
      return tokenType;
    }

    public void setTokenType(String tokenType) {
      this.tokenType = tokenType;
    }

    public Integer getExpiresIn() {
      return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
      this.expiresIn = expiresIn;
    }

    public String getRefreshToken() {
      return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
      this.refreshToken = refreshToken;
    }
  }

  /**
   * Twitter 用户信息
   */
  public static class TwitterUserInfo {
    private String id;
    private String name;
    private String username;
    private String createdAt;
    private String profileImageUrl;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(String createdAt) {
      this.createdAt = createdAt;
    }

    public String getProfileImageUrl() {
      return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
      this.profileImageUrl = profileImageUrl;
    }
  }
}
