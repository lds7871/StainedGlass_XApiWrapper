package LDS.Person.controller;

import LDS.Person.config.TwitterApiClient;
import LDS.Person.dto.request.TwitterCallbackRequest;
import LDS.Person.dto.response.TwitterAuthorizationState;
import LDS.Person.dto.response.TwitterCallbackResponse;
import LDS.Person.service.TwitterCallbackService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Twitter OAuth 回调控制器
 * 处理来自 Twitter 的 OAuth 授权回调请求
 * 部署到: http://115.190.170.56/callback/twitter
 */
@RestController
@RequestMapping("/callback/twitter")
@Tag(name = "Twitter OAuth 回调")
@Slf4j
@RequiredArgsConstructor
public class TwitterCallbackController {

    private final TwitterCallbackService twitterCallbackService;
    private final TwitterApiClient twitterApiClient;
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final String ANSI_BLUE = "\u001B[36m";
    private static final String ANSI_RESET = "\u001B[0m";

    /**
     * 生成 OAuth 授权 URL
     * 客户端调用此端点获取授权 URL，然后重定向用户到 Twitter 授权页面
     *
     * @return 包含授权 URL 和 state 的响应
     */
    @GetMapping("/authorize")
    @Operation(summary = "生成 Twitter OAuth 授权 URL", description = "返回授权 URL，用于重定向用户到 Twitter 授权页面")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功生成授权 URL"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    public Map<String, Object> generateAuthorizationUrl() {
        try {
            log.info("生成 Twitter OAuth 授权 URL");

            // 生成 state（CSRF 防护）
            TwitterAuthorizationState authorizationState = twitterCallbackService.generateAuthorizationState();

            // 生成授权 URL（使用 PKCE）
            String authUrl = twitterApiClient.generateAuthorizationUrl(
                    authorizationState.getState(),
                    authorizationState.getCodeChallenge());

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "授权 URL 生成成功");
            response.put("authorizationUrl", authUrl);
            response.put("state", authorizationState.getState());
            response.put("codeChallenge", authorizationState.getCodeChallenge());
            response.put("codeChallengeMethod", authorizationState.getCodeChallengeMethod());

            log.info("授权 URL 生成完成");
            logJsonResponse("授权 URL 响应", response);
            return response;

        } catch (Exception e) {
            log.error("生成授权 URL 时出错", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "服务器错误: " + e.getMessage());
            logJsonResponse("授权 URL 错误响应", errorResponse);
            return errorResponse;
        }
    }

    /**
     * 处理 Twitter OAuth 回调 - GET 方式
     * Twitter 在用户授权后会重定向到这个端点
     *
     * @param code  授权码
     * @param state 状态令牌
     * @param error 错误信息（如果授权失败）
     * @return 认证结果
     */
    @GetMapping("/oauth")
    @Operation(summary = "处理 Twitter OAuth 回调 (GET)", description = "接收 Twitter 授权服务器的重定向，处理授权码")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "认证成功，返回用户和 token 信息"),
            @ApiResponse(responseCode = "400", description = "请求参数错误或状态验证失败"),
            @ApiResponse(responseCode = "401", description = "用户拒绝授权"),
            @ApiResponse(responseCode = "500", description = "服务器处理错误")
    })
    public TwitterCallbackResponse handleCallbackGet(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, name = "error_description") String errorDescription) {

        try {
            log.info("\n\n");
            log.info("╔════════════════════════════════════════════════════════════════╗");
            log.info("║          🔄 收到 Twitter OAuth 回调请求 (GET)                   ║");
            log.info("╚════════════════════════════════════════════════════════════════╝");
            log.info("📝 URL 参数详情:");
            log.info("   • code: {}", code != null ? "[已接收，长度: " + code.length() + "]" : "[null]");
            log.info("   • state: {}", state != null ? "[已接收]" : "[null]");
            log.info("   • error: {}", error);
            log.info("   • error_description: {}", errorDescription);

            log.debug("URL 参数完整值 - code: [{}], state: [{}], error: [{}], error_description: [{}]",
                    code, state, error, errorDescription);

            // 构建请求对象
            TwitterCallbackRequest request = new TwitterCallbackRequest();
            request.setCode(code);
            request.setState(state);
            request.setError(error);
            request.setError_description(errorDescription);

            log.info("✅ 请求对象已构建");
            log.debug("请求对象详情 - code: {}, state: {}, error: {}",
                    request.getCode(), request.getState(), request.getError());

            // 处理回调
            TwitterCallbackResponse response = twitterCallbackService.handleCallback(request);

            log.info("📤 Twitter 回调处理完成");
            logJsonResponse("回调响应", convertResponseToMap(response));
            log.info("✅ 回调处理结束\n");

            return response;
        } catch (Exception e) {
            log.error("❌ Twitter 回调处理异常", e);
            TwitterCallbackResponse errorResponse = TwitterCallbackResponse.builder()
                    .code(500)
                    .message("回调处理异常: " + e.getMessage())
                    .build();
            return errorResponse;
        }
    }

    private Map<String, Object> convertResponseToMap(TwitterCallbackResponse response) {
        Map<String, Object> map = new HashMap<>();
        if (response != null) {
            map.put("code", response.getCode());
            map.put("message", response.getMessage());
            map.put("userId", response.getUserId());
            map.put("username", response.getUsername());
            map.put("displayName", response.getDisplayName());
            map.put("accessToken", response.getAccessToken() != null ? "****[已隐藏]" : null);
        }
        return map;
    }

    private void logJsonResponse(String title, Map<String, Object> body) {
        try {
            String json = JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(body);
            String[] lines = json.split("\n");
            log.info("{}════════════════════════════════════════════════════════════════{}", ANSI_BLUE, ANSI_RESET);
            log.info("{}📋 {} :{}", ANSI_BLUE, title, ANSI_RESET);
            log.info("{}────────────────────────────────────────────────────────────────{}", ANSI_BLUE, ANSI_RESET);
            for (String line : lines) {
                log.info("{}{}{}", ANSI_BLUE, line, ANSI_RESET);
            }
            log.info("{}════════════════════════════════════════════════════════════════{}", ANSI_BLUE, ANSI_RESET);
        } catch (JsonProcessingException ex) {
            log.warn("无法格式化 JSON 响应", ex);
        }
    }

    // /**
    // * 处理 Twitter OAuth 回调 - POST 方式
    // * 如果配置了 POST 回调，可以使用此端点
    // *
    // * @param request 包含 code、state 或 error 的请求
    // * @return 认证结果响应，包含 token 和用户信息
    // */
    // @PostMapping("/oauth")
    // @ApiOperation(value = "处理 Twitter OAuth 回调 (POST)", notes = "接收 Twitter
    // 授权服务器的回调，进行状态验证和 token 交换")
    // @ApiResponses({
    // @ApiResponse(responseCode = "200", description = "认证成功，返回用户和 token 信息"),
    // @ApiResponse(responseCode = "400", description = "请求参数错误或状态验证失败"),
    // @ApiResponse(responseCode = "401", description = "用户拒绝授权"),
    // @ApiResponse(responseCode = "500", description = "服务器处理错误")
    // })
    // public TwitterCallbackResponse handleCallbackPost(@RequestBody
    // TwitterCallbackRequest request) {
    // log.info("收到 Twitter 回调请求 (POST)，state: {}, code: {}, error: {}",
    // request.getState(),
    // request.getCode() != null ? "已收到" : "未收到",
    // request.getError());

    // TwitterCallbackResponse response =
    // twitterCallbackService.handleCallback(request);

    // log.info("Twitter 回调处理完成 (POST)，响应码: {}", response.getCode());

    // return response;
    // }

}
