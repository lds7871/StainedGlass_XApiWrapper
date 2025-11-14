package LDS.Person.service.impl;

import LDS.Person.config.TwitterApiClient;
import LDS.Person.config.TwitterApiClient.TwitterTokenResponse;
import LDS.Person.config.TwitterApiClient.TwitterUserInfo;
import LDS.Person.config.OAuthStateStore;
import LDS.Person.dto.request.TwitterCallbackRequest;
import LDS.Person.dto.response.TwitterAuthorizationState;
import LDS.Person.dto.response.TwitterCallbackResponse;
import LDS.Person.entity.TwitterToken;
import LDS.Person.service.TwitterCallbackService;
import LDS.Person.service.TwitterTokenService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

/**
 * Twitter OAuth 回调服务实现
 * 处理完整的 OAuth 2.0 授权码流程
 * 
 * State 存储方案：
 * - 默认使用内存存储（MemoryOAuthStateStore）
 * - 可自定义其他实现（数据库、Redis 等）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TwitterCallbackServiceImpl implements TwitterCallbackService {

    private final TwitterApiClient twitterApiClient;
    private final OAuthStateStore oauthStateStore;
    private final TwitterTokenService twitterTokenService;

    @Override
    public TwitterCallbackResponse handleCallback(TwitterCallbackRequest request) {
        try {
            log.info("\n🔍 ============= 开始处理 Twitter OAuth 回调 =============");
            
            // 调试：记录所有接收到的参数
            log.info("📥 收到的回调请求参数:");
            log.info("   • code: {}", request.getCode() != null ? "[已接收，长度: " + request.getCode().length() + "]" : "[null]");
            log.info("   • state: {}", request.getState());
            log.info("   • error: {}", request.getError());
            log.info("   • error_description: {}", request.getError_description());

            // 1. 检查错误响应（既检查 null 也检查空字符串）
            if (request.getError() != null && !request.getError().trim().isEmpty()) {
                log.warn("⚠️  Twitter 授权失败: {} - {}", request.getError(), request.getError_description());
                TwitterCallbackResponse response = TwitterCallbackResponse.builder()
                        .code(401)
                        .message("授权被用户拒绝或失败: " + request.getError())
                        .build();
                log.info("❌ 返回错误响应\n");
                return response;
            }

            // 2. 验证 code 和 state
            if (request.getCode() == null || request.getCode().isEmpty()) {
                log.warn("⚠️  缺少授权码 (code)");
                TwitterCallbackResponse response = TwitterCallbackResponse.builder()
                        .code(400)
                        .message("缺少授权码")
                        .build();
                log.info("❌ 返回错误响应\n");
                return response;
            }

            if (request.getState() == null) {
                log.warn("⚠️  缺少 state 参数");
                TwitterCallbackResponse response = TwitterCallbackResponse.builder()
                        .code(400)
                        .message("无效或过期的状态令牌（可能遭受 CSRF 攻击）")
                        .build();
                log.info("❌ 返回错误响应\n");
                return response;
            }

            log.info("🔐 开始验证 state 令牌...");
            OAuthStateStore.StateRecord stateRecord = oauthStateStore.consumeState(request.getState());
            if (stateRecord == null) {
                log.warn("⚠️  state 验证失败或已过期: {}", request.getState());
                TwitterCallbackResponse response = TwitterCallbackResponse.builder()
                        .code(400)
                        .message("无效或过期的状态令牌（可能遭受 CSRF 攻击）")
                        .build();
                log.info("❌ 返回错误响应\n");
                return response;
            }
            log.info("✅ state 验证通过");

            // 3. 交换授权码获取 token
            log.info("🔄 开始交换授权码获取 access token...");
            String codeVerifier = stateRecord.getCodeVerifier();
            if (codeVerifier == null || codeVerifier.isEmpty()) {
                log.error("❌ state 未关联有效的 PKCE code verifier");
                TwitterCallbackResponse response = TwitterCallbackResponse.builder()
                        .code(500)
                        .message("服务器未保存 PKCE 信息，请重试授权")
                        .build();
                log.info("❌ 返回错误响应\n");
                return response;
            }
            log.info("   • code verifier 已获取");

            TwitterTokenResponse tokenResponse = twitterApiClient.exchangeCodeForToken(request.getCode(), codeVerifier);

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                log.error("❌ 无法获取 access token");
                TwitterCallbackResponse response = TwitterCallbackResponse.builder()
                        .code(500)
                        .message("无法交换授权码以获取 token")
                        .build();
                log.info("❌ 返回错误响应\n");
                return response;
            }
            log.info("✅ access token 已获取: {}...", tokenResponse.getAccessToken().substring(0, Math.min(20, tokenResponse.getAccessToken().length())));

            // ✅ 保存 access token 到 Session
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpSession session = attributes.getRequest().getSession();
                session.setAttribute("accessToken", tokenResponse.getAccessToken());
                // 标记这是首次授权（Session 中）- 告诉过滤器不要从数据库读取旧数据
                session.setAttribute("isFirstTimeAuth", true);
                log.info("💾 Access token 已保存到 Session, session ID: {}, token: {}...", 
                    session.getId(), 
                    tokenResponse.getAccessToken().substring(0, Math.min(20, tokenResponse.getAccessToken().length())));
                log.info("📌 标记首次授权状态 (isFirstTimeAuth=true)");
                
                // 验证保存是否成功
                Object retrieved = session.getAttribute("accessToken");
                if (retrieved != null) {
                    log.info("✅ 验证成功：Session 中已确实存有 accessToken");
                } else {
                    log.error("❌ 验证失败：accessToken 未被成功保存到 Session");
                }
            } else {
                log.error("❌ 无法获取 ServletRequestAttributes，无法保存 token 到 Session");
            }

            // 4. 使用 token 获取用户信息
            log.info("👤 开始获取认证用户信息...");
            TwitterUserInfo userInfo = twitterApiClient.getUserInfo(tokenResponse.getAccessToken());

            if (userInfo == null) {
                log.error("❌ 无法获取用户信息");
                TwitterCallbackResponse response = TwitterCallbackResponse.builder()
                        .code(500)
                        .message("无法获取用户信息")
                        .build();
                log.info("❌ 返回错误响应\n");
                return response;
            }
            log.info("✅ 用户信息已获取: userId={}, username={}", userInfo.getId(), userInfo.getUsername());

            // ✅ 保存 twitterUserId 到 Session（用于过滤器后续查询）
            if (attributes != null) {
                HttpSession session = attributes.getRequest().getSession();
                session.setAttribute("twitterUserId", userInfo.getId());
                log.info("💾 Twitter userId 已保存到 Session, userId: {}", userInfo.getId());
            }

            // ✅ 清除该用户的旧 token 并保存新 token 到数据库
            log.info("🗑️  清除数据库中用户 {} 的旧 token...", userInfo.getId());
            twitterTokenService.deleteByUserId(userInfo.getId());
            log.info("✅ 旧 token 已清除");
            
            TwitterToken token = new TwitterToken();
            token.setTwitterUserId(userInfo.getId());
            token.setAccessToken(tokenResponse.getAccessToken());
            token.setRefreshToken(tokenResponse.getRefreshToken());
            token.setTokenType(tokenResponse.getTokenType());
            token.setScope("tweet.read tweet.write users.read offline.access");
            if (tokenResponse.getExpiresIn() != null && tokenResponse.getExpiresIn() > 0) {
                token.setExpiresAt(Instant.now().plusSeconds(tokenResponse.getExpiresIn()));
            }
            twitterTokenService.save(token);
            log.info("💾 新 token 已保存到数据库，userId={}, 过期时间: {}", userInfo.getId(), token.getExpiresAt());

            log.info("🎉 Twitter 用户成功认证: userId={}, username={}", userInfo.getId(), userInfo.getUsername());

            TwitterCallbackResponse response = TwitterCallbackResponse.builder()
                    .code(200)
                    .message("认证成功")
                    .userId(userInfo.getId())
                    .username(userInfo.getUsername())
                    .displayName(userInfo.getName())
                    .accessToken(tokenResponse.getAccessToken())
                    .build();
            
            log.info("✅ 返回成功响应\n");
            return response;

        } catch (Exception e) {
            log.error("❌ 处理 Twitter 回调时出错", e);
            TwitterCallbackResponse response = TwitterCallbackResponse.builder()
                    .code(500)
                    .message("服务器错误: " + e.getMessage())
                    .build();
            log.info("❌ 返回错误响应\n");
            return response;
        }
    }

    @Override
    public boolean validateState(String state) {
        return oauthStateStore.stateExists(state);
    }

    @Override
    public TwitterAuthorizationState generateAuthorizationState() {
        String state = UUID.randomUUID().toString();
        String codeVerifier = twitterApiClient.generateCodeVerifier();
        String codeChallenge = twitterApiClient.generateCodeChallenge(codeVerifier);

        oauthStateStore.saveState(state, codeVerifier, 10); // 10 分钟有效期
        log.debug("生成新 state: {}，已生成 PKCE codeChallenge", state);

        return TwitterAuthorizationState.builder()
                .state(state)
                .codeChallenge(codeChallenge)
                .codeChallengeMethod("S256")
                .build();
    }

}
