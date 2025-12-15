package LDS.Person.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * IP 白名单拦截器 - 限制接口只能通过白名单IP访问，或通过有效的pass_token绕过
 * 
 * 相比Filter的优势：
 * 1. 执行时机在DispatcherServlet之后，可以获取到HandlerMethod
 * 2. 能够正确读取@BypassIpWhitelist注解
 * 3. 支持controller方法级别的细粒度控制
 * 
 * 验证逻辑：
 * 1. 优先检查@BypassIpWhitelist注解，如有则直接放行（公共接口）
 * 2. 检查IP是否在白名单内，如在则放行
 * 3. 检查pass_token是否有效，如有效则放行
 * 4. 否则拒绝访问
 * 
 * IP白名单和pass_token配置从 SecurityConfig 中读取，支持在 application.yml 中配置
 */
@Component
@Slf4j
public class IpWhitelistInterceptor implements HandlerInterceptor {

    private final SecurityConfig securityConfig;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 缓存的IP白名单Set，避免每次请求都创建新对象
     */
    private Set<String> cachedIpWhitelist;

    /**
     * 缓存的pass token Set，避免每次请求都创建新对象
     */
    private Set<String> cachedPassTokens;

    /**
     * 记录上次配置的hash，用于检测配置变化
     */
    private int lastConfigHash = 0;

    /**
     * 记录上次pass token配置的hash，用于检测配置变化
     */
    private int lastTokenConfigHash = 0;

    /**
     * 缓存最近请求，用于合并/error记录
     * key: IP地址, value: 最近请求的详细信息
     */
    private final ConcurrentMap<String, RecentRequest> recentRequests = new ConcurrentHashMap<>();

    /**
     * 构造函数注入，提升可测试性
     */
    public IpWhitelistInterceptor(SecurityConfig securityConfig, JdbcTemplate jdbcTemplate) {
        this.securityConfig = securityConfig;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 如果IP白名单功能已禁用，直接放行
        if (!securityConfig.isIpWhitelistEnabled()) {
            return true;
        }

        // 包装请求以缓存请求体，允许多次读取
        HttpServletRequest cachedRequest = request;
        if (!(request instanceof ContentCachingRequestWrapper)) {
            cachedRequest = new ContentCachingRequestWrapper(request);
        }

        // 提前获取客户端IP和请求路径，用于记录访问日志
        String clientIp = getClientRealIp(cachedRequest);
        String path = getDetailedPath(cachedRequest);

        // 检查IP是否在白名单内
        boolean isIpAllowed = isIpWhitelisted(clientIp);

        // 检查是否有有效的pass_token
        boolean isTokenValid = isPassTokenValid(cachedRequest);

        // 检查handler是否是HandlerMethod
        if (!(handler instanceof HandlerMethod)) {
            // 静态资源等 - 如果IP不在白名单内且token无效，拒绝访问
            if (!isIpAllowed && !isTokenValid) {
                log.warn("拒绝来自非白名单IP的静态资源请求 - IP: {}, 路径: {}, 方法: {}",
                        clientIp, path, cachedRequest.getMethod());

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"Access denied: Your IP is not whitelisted and token is invalid\"}");
                response.getWriter().flush();
                logAccess(clientIp, path, 0, cachedRequest);
                return false;
            }
            // IP在白名单内或token有效，放行并记录
            logAccess(clientIp, path, 1, cachedRequest);
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 检查当前请求的handler是否标注了@BypassIpWhitelist注解（公共接口）
        BypassIpWhitelist annotation = handlerMethod.getMethodAnnotation(BypassIpWhitelist.class);
        if (annotation != null) {
            // 公共接口，允许任意IP访问
            log.debug("公共接口访问 - IP: {}, 路径: {}, 原因: {}", clientIp, path, annotation.reason());
            logAccess(clientIp, path, 1, cachedRequest);
            return true;
        }

        // 非公共接口 - 检查IP白名单或pass_token
        if (!isIpAllowed && !isTokenValid) {
            log.warn("拒绝来自非白名单IP的API请求 - IP: {}, 路径: {}, 方法: {}, 原因: IP不在白名单且token无效",
                    clientIp, path, cachedRequest.getMethod());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Access denied: Your IP is not whitelisted and token is invalid\"}");
            response.getWriter().flush();
            logAccess(clientIp, path, 0, cachedRequest);
            return false;
        }

        // IP在白名单内或token有效，正常的API请求，放行并记录
        if (isTokenValid) {
            log.debug("通过pass_token验证 - IP: {}, 路径: {}, token有效", clientIp, path);
        } else {
            log.debug("IP白名单验证通过 - IP: {}, 路径: {}", clientIp, path);
        }
        logAccess(clientIp, path, 1, cachedRequest);
        return true;
    }

    /**
     * 将访问记录写入数据库表 `api_log`。
     * states: 1 表示通过，0 表示拒绝
     * 
     * 优化逻辑：
     * 1. 不记录 favicon.ico 和直接的 /error 请求
     * 2. 对于 /error 请求，尝试合并之前的相关请求状态为失败
     * 3. 其他请求正常记录并缓存用于后续合并
     */
    private void logAccess(String ip, String api, int states, HttpServletRequest request) {
        if (jdbcTemplate == null) {
            return;
        }

        // 不记录 favicon.ico 请求
        if (api.equals("/favicon.ico")) {
            return;
        }

        // 特殊处理 /error 请求
        if (api.equals("/error")) {
            handleErrorPageRequest(ip, request);
            return;
        }

        try {
            String method = request.getMethod();
            String requestBody = getRequestBody(request);
            String detailedApi = method + " " + api + (requestBody != null ? " | Body: " + requestBody : "");

            // 正常记录请求
            int insertedId = insertLogRecord(ip, detailedApi, states);

            // 缓存请求信息，用于后续可能的合并
            if (insertedId > 0) {
                recentRequests.put(ip, new RecentRequest(detailedApi, System.currentTimeMillis(), insertedId));
            }

        } catch (Exception ex) {
            log.error("记录访问日志失败 - ip: {}, api: {}, states: {}", ip, api, states, ex);
        }
    }

    /**
     * 获取客户端真实IP地址
     * 支持各种代理环境：Nginx、Apache、负载均衡器等
     * 
     * @param request HTTP请求对象
     * @return 客户端真实IP
     */
    private String getClientRealIp(HttpServletRequest request) {
        // 优先检查代理IP头（按优先级）
        String[] ipHeaders = {
                "X-Forwarded-For", // Nginx代理
                "X-Real-IP", // Nginx代理
                "Proxy-Client-IP", // 代理
                "WL-Proxy-Client-IP", // WebLogic代理
                "HTTP_CLIENT_IP", // HTTP代理
                "HTTP_X_FORWARDED_FOR" // HTTP转发
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 如果包含多个IP（逗号分隔），取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        // 如果没有代理头，使用远程地址
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    /**
     * 检查IP是否在白名单内
     * 
     * @param ip 要检查的IP地址
     * @return true表示在白名单内，false表示不在
     */
    private boolean isIpWhitelisted(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // 获取IP白名单（使用缓存优化性能）
        Set<String> ipWhitelist = getIpWhitelistSet();

        // 直接匹配
        if (ipWhitelist.contains(ip)) {
            return true;
        }

        // 处理IPv6地址的各种形式
        String normalizedIp = normalizeIp(ip);
        return ipWhitelist.contains(normalizedIp);
    }

    /**
     * 检查请求中的pass_token是否有效
     * 支持从以下位置获取token：
     * 1. Authorization header (Bearer token)
     * 2. X-Pass-Token header
     * 3. pass_token query parameter
     * 4. pass_token form parameter
     * 
     * @param request HTTP请求对象
     * @return true表示token有效，false表示无效或未启用
     */
    private boolean isPassTokenValid(HttpServletRequest request) {
        // 检查pass_token功能是否启用
        if (!securityConfig.isPassTokenEnabled()) {
            return false;
        }

        String token = getPassTokenFromRequest(request);
        if (token == null || token.isEmpty()) {
            return false;
        }

        // 获取有效的token集合
        Set<String> validTokens = getPassTokenSet();
        boolean isValid = validTokens.contains(token);

        if (isValid) {
            log.debug("pass_token验证成功 - token: {}", maskToken(token));
        } else {
            log.debug("pass_token验证失败 - 无效的token");
        }

        return isValid;
    }

    /**
     * 从请求中提取pass_token
     * 优先级：Authorization header > X-Pass-Token header > 请求参数
     * 
     * @param request HTTP请求对象
     * @return token值，如果未找到返回null
     */
    private String getPassTokenFromRequest(HttpServletRequest request) {
        // 1. 尝试从Authorization header获取 (Bearer token)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring("Bearer ".length()).trim();
        }

        // 2. 尝试从X-Pass-Token header获取
        String passTokenHeader = request.getHeader("X-Pass-Token");
        if (passTokenHeader != null && !passTokenHeader.isEmpty()) {
            return passTokenHeader.trim();
        }

        // 3. 尝试从query parameter获取
        String queryToken = request.getParameter("pass_token");
        if (queryToken != null && !queryToken.isEmpty()) {
            return queryToken.trim();
        }

        return null;
    }

    /**
     * 获取pass_token集合，使用缓存优化性能
     * 只有配置变化时才重新创建Set
     * 
     * @return pass_token集合
     */
    private Set<String> getPassTokenSet() {
        List<String> currentTokens = securityConfig.getPassTokens();
        int currentHash = currentTokens.hashCode();

        // 如果配置未变化且缓存存在，直接返回缓存
        if (currentHash == lastTokenConfigHash && cachedPassTokens != null) {
            return cachedPassTokens;
        }

        // 配置变化或首次访问，重新创建缓存
        lastTokenConfigHash = currentHash;
        cachedPassTokens = new HashSet<>(currentTokens);
        log.debug("pass_token缓存已更新，当前token数量: {}", cachedPassTokens.size());

        return cachedPassTokens;
    }

    /**
     * 对token进行掩码处理，用于日志记录时保护敏感信息
     * 只显示前4个和后4个字符
     * 
     * @param token 原始token
     * @return 掩码后的token
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        String start = token.substring(0, 4);
        String end = token.substring(token.length() - 4);
        return start + "..." + end;
    }

    /**
     * 获取IP白名单Set，使用缓存优化性能
     * 只有配置变化时才重新创建Set
     * 
     * @return IP白名单Set
     */
    private Set<String> getIpWhitelistSet() {
        List<String> currentList = securityConfig.getIpWhitelist();
        int currentHash = currentList.hashCode();

        // 如果配置未变化且缓存存在，直接返回缓存
        if (currentHash == lastConfigHash && cachedIpWhitelist != null) {
            return cachedIpWhitelist;
        }

        // 配置变化或首次访问，重新创建缓存
        lastConfigHash = currentHash;
        cachedIpWhitelist = new HashSet<>(currentList);
        log.debug("IP白名单缓存已更新，当前白名单: {}", cachedIpWhitelist);

        return cachedIpWhitelist;
    }

    /**
     * 规范化IP地址格式
     * 处理IPv6的压缩形式、完整形式等不同表示方法
     * 
     * @param ip 要规范化的IP
     * @return 规范化后的IP
     */
    private String normalizeIp(String ip) {
        // 处理IPv6 localhost的不同表示
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "::1";
        }
        return ip;
    }

    /**
     * 获取详细的请求路径信息
     * 包含URI、查询参数等
     * 
     * @param request HTTP请求对象
     * @return 详细路径信息
     */
    private String getDetailedPath(HttpServletRequest request) {
        StringBuilder path = new StringBuilder();
        path.append(request.getRequestURI());

        // 添加查询参数
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            path.append("?").append(queryString);
        }

        return path.toString();
    }

    /**
     * 获取请求体内容（限制长度，避免记录过大数据）
     * 只对POST/PUT/PATCH请求读取body
     * 
     * @param request HTTP请求对象
     * @return 请求体内容或null
     */
    private String getRequestBody(HttpServletRequest request) {
        String method = request.getMethod();

        // 只对可能有body的请求方法读取
        if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method)) {
            return null;
        }

        try {
            // 检查Content-Type，避免读取文件上传等大数据
            String contentType = request.getContentType();
            if (contentType != null &&
                    (contentType.contains("multipart/form-data") ||
                            contentType.contains("application/octet-stream"))) {
                return "[BINARY_DATA]";
            }

            // 检查Content-Length，避免读取过大数据
            int contentLength = request.getContentLength();
            if (contentLength > 2048) { // 超过2KB的数据不记录
                return "[LARGE_BODY:" + contentLength + "bytes]";
            }

            // 使用 ContentCachingRequestWrapper 安全读取请求体
            if (request instanceof ContentCachingRequestWrapper) {
                ContentCachingRequestWrapper cachedRequest = (ContentCachingRequestWrapper) request;

                byte[] cachedBody = cachedRequest.getContentAsByteArray();
                if (cachedBody != null && cachedBody.length > 0) {
                    String bodyStr = new String(cachedBody,
                            request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8");

                    // 限制长度
                    if (bodyStr.length() > 1024) {
                        return bodyStr.substring(0, 1024) + "...[TRUNCATED]";
                    }
                    return bodyStr;
                }
            }

            return null;

        } catch (Exception e) {
            log.debug("读取请求体失败: {}", e.getMessage());
            return "[READ_ERROR]";
        }
    }

    /**
     * 插入日志记录到数据库
     * 
     * @param ip     IP地址
     * @param api    API详情
     * @param states 状态
     * @return 插入记录的ID，如果失败返回-1
     */
    private int insertLogRecord(String ip, String api, int states) {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement(
                        "INSERT INTO api_log (ip, api, states, create_time) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                        new String[] { "id" });
                ps.setString(1, ip);
                ps.setString(2, api);
                ps.setInt(3, states);
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            return key != null ? key.intValue() : -1;
        } catch (Exception ex) {
            log.error("插入日志记录失败 - ip: {}, api: {}, states: {}", ip, api, states, ex);
            return -1;
        }
    }

    /**
     * 处理错误页面请求
     * 检查是否由于异常导致的错误，如果是则更新之前的请求状态
     * 
     * @param ip      IP地址
     * @param request HTTP请求
     */
    private void handleErrorPageRequest(String ip, HttpServletRequest request) {
        // 检查是否由于异常导致的错误页面
        Object requestFailed = request.getAttribute("request_failed");
        if (requestFailed != null && (Boolean) requestFailed) {
            // 这是由于异常导致的错误页面，更新之前的请求状态
            mergePreviousRequestToError(ip);
        }
        // 否则是直接访问/error页面，不记录
    }

    /**
     * 将之前的请求状态合并为错误状态
     * 当收到/error请求时，更新该IP最近的请求状态为失败
     * 
     * @param ip IP地址
     */
    private void mergePreviousRequestToError(String ip) {
        RecentRequest recent = recentRequests.get(ip);
        if (recent != null) {
            // 检查时间是否在合理范围内（比如10秒内，因为异常处理可能有延迟）
            long timeDiff = System.currentTimeMillis() - recent.getTimestamp();
            if (timeDiff < 10000) { // 10秒内
                try {
                    // 更新数据库中的状态为失败
                    jdbcTemplate.update(
                            "UPDATE api_log SET states = 0 WHERE id = ?",
                            recent.getId());
                    log.debug("合并请求状态为失败 - IP: {}, 原API: {}", ip, recent.getApi());
                } catch (Exception ex) {
                    log.error("更新请求状态失败 - IP: {}, ID: {}", ip, recent.getId(), ex);
                }
            }
            // 清除缓存
            recentRequests.remove(ip);
        }
    }

    /**
     * 最近请求信息类，用于缓存和合并/error记录
     */
    private static class RecentRequest {
        private final String api;
        private final long timestamp;
        private final int id; // 数据库记录ID

        public RecentRequest(String api, long timestamp, int id) {
            this.api = api;
            this.timestamp = timestamp;
            this.id = id;
        }

        public String getApi() {
            return api;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getId() {
            return id;
        }
    }
}
