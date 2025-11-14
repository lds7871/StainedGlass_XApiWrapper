package LDS.Person.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import LDS.Person.entity.TwitterToken;
import LDS.Person.repository.TwitterTokenRepository;
import LDS.Person.service.TwitterTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Twitter Access Token 过滤器 - 自动从 Session 或 Database 读取 token 并注入到请求头
 */
@Component
@Slf4j
public class TwitterAccessTokenFilter implements Filter {

    @Autowired
    private TwitterTokenRepository twitterTokenRepository;

    @Autowired
    private TwitterTokenService twitterTokenService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest http = (HttpServletRequest) request;
        
        // 只处理 /api/twitter 开头的请求
        String requestURI = http.getRequestURI();
        if (!requestURI.startsWith("/api/twitter")) {
            chain.doFilter(request, response);
            return;
        }
        
        log.info("🔍 TwitterAccessTokenFilter 处理请求: {}", requestURI);
        
        // 检查是否已有 X-Access-Token header
        String existing = http.getHeader("X-Access-Token");
        if (existing != null && !existing.isBlank()) {
            log.info("✅ 请求头已包含 X-Access-Token，直接通过");
            chain.doFilter(request, response);
            return;
        }
        
        log.info("🔍 未在请求头中找到 X-Access-Token，尝试从 Session 读取");
        
        // 从 Session 中读取 access token
        HttpSession session = http.getSession(false);
        String token = null;
        String twitterUserId = null;
        
        if (session != null) {
            log.info("📋 Session 存在，session ID: {}", session.getId());
            Object tokenObj = session.getAttribute("accessToken");
            if (tokenObj != null) {
                token = tokenObj.toString();
                log.info("✅ 从 Session 中读取到 access token: {}...", token.substring(0, Math.min(20, token.length())));
            }
            
            // 也从 Session 中读取 twitterUserId
            Object userIdObj = session.getAttribute("twitterUserId");
            if (userIdObj != null) {
                twitterUserId = userIdObj.toString();
                log.info("✅ 从 Session 中读取到 twitterUserId: {}", twitterUserId);
            }
            
            if (token == null) {
                log.warn("⚠️ Session 中不存在 accessToken 属性");
            }
        } else {
            log.warn("⚠️ Session 不存在（http.getSession(false) 返回 null）");
        }
        
        // 如果 Session 中没找到 token，尝试从数据库读取
        // 但首次授权（isFirstTimeAuth=true）时不从数据库读取，只使用 Session 中的 token
        if (token == null && twitterUserId != null) {
            // 检查是否是首次授权
            boolean isFirstTimeAuth = session != null && 
                                     session.getAttribute("isFirstTimeAuth") != null && 
                                     (boolean) session.getAttribute("isFirstTimeAuth");
            
            if (isFirstTimeAuth) {
                log.warn("⚠️ 首次授权状态，不从数据库读取旧 token，跳过数据库查询");
                log.info("📌 首次授权标志已设置 (isFirstTimeAuth=true)，只使用 Session 中的 token");
            } else {
                log.info("🔍 Session 中未找到 token，尝试从数据库查询 twitterUserId: {}", twitterUserId);
                try {
                    String validToken = twitterTokenService.getValidAccessToken(twitterUserId);
                    if (validToken != null && !validToken.isBlank()) {
                        token = validToken;
                        log.info("✅ 从数据库中读取到有效的 access token: {}...", token.substring(0, Math.min(20, token.length())));
                    } else {
                        log.warn("⚠️ 从数据库查询得到的 token 为 null 或 blank");
                    }
                } catch (Exception e) {
                    log.error("❌ 从数据库查询 token 时出错: {}", e.getMessage(), e);
                }
            }
        }
        
        // 如果还是没有 token，尝试从数据库查询任何可用的 token（作为最后的尝试）
        if (token == null) {
            log.info("🔍 Session 和 twitterUserId 都不可用，尝试从数据库查询任何可用的 token");
            try {
                List<TwitterToken> allTokens = twitterTokenRepository.findAll();
                if (!allTokens.isEmpty()) {
                    TwitterToken firstToken = allTokens.get(0);
                    twitterUserId = firstToken.getTwitterUserId();  // 获取 userId
                    log.info("📊 数据库中找到 {} 个 token，使用第一个 (userId: {})", allTokens.size(), twitterUserId);
                    String validToken = twitterTokenService.getValidAccessToken(twitterUserId);
                    if (validToken != null && !validToken.isBlank()) {
                        token = validToken;
                        log.info("✅ 从数据库查询得到有效的 access token: {}...", token.substring(0, Math.min(20, token.length())));
                    }
                } else {
                    log.warn("⚠️ 数据库中不存在任何 token");
                }
            } catch (Exception e) {
                log.error("❌ 从数据库查询任何 token 时出错: {}", e.getMessage(), e);
            }
        }
        
        // 如果找到 token，则包装请求并注入到请求头，同时保存 twitterUserId 到 Session
        if (token != null && !token.isBlank()) {
            final String finalToken = token;
            final String finalTwitterUserId = twitterUserId;
            
            // 确保 twitterUserId 保存到 Session 中
            if (finalTwitterUserId != null && !finalTwitterUserId.isBlank()) {
                if (session == null) {
                    session = http.getSession(true);  // 如果 Session 不存在，创建一个新的
                }
                session.setAttribute("twitterUserId", finalTwitterUserId);
                log.info("✅ 已将 twitterUserId: {} 保存到 Session 中", finalTwitterUserId);
            }
            
            HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(http) {
                @Override
                public String getHeader(String name) {
                    if ("X-Access-Token".equalsIgnoreCase(name)) {
                        log.debug("🔐 返回注入的 X-Access-Token header");
                        return finalToken;
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("X-Access-Token".equalsIgnoreCase(name)) {
                        return Collections.enumeration(Collections.singletonList(finalToken));
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    List<String> names = Collections.list(super.getHeaderNames());
                    if (!names.contains("X-Access-Token")) {
                        names.add("X-Access-Token");
                    }
                    return Collections.enumeration(names);
                }
            };
            log.info("✅ 已将 access token 注入到请求头");
            chain.doFilter(wrapper, response);
        } else {
            log.warn("⚠️ 无法从 Session 或数据库中找到 access token");
            chain.doFilter(request, response);
        }
    }
}
