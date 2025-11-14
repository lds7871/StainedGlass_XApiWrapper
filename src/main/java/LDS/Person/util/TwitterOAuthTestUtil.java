package LDS.Person.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Twitter OAuth 测试工具
 * 用于本地开发和测试
 */
@Component
@Slf4j
public class TwitterOAuthTestUtil {

  /**
   * 模拟生成 OAuth 测试 URL
   * 用于手动测试回调流程
   *
   * @param clientId    Twitter App 的 Client ID
   * @param callbackUrl 回调 URL
   * @param state       State 令牌
   * @return 完整的授权 URL
   */
  public static String generateTestAuthUrl(String clientId, String callbackUrl, String state) {
    return String.format(
        "https://twitter.com/i/oauth2/authorize?" +
            "client_id=%s&" +
            "redirect_uri=%s&" +
            "response_type=code&" +
            "state=%s&" +
            "scope=tweet.read%%20users.read%%20follows.read%%20follows.write",
        clientId,
        urlEncode(callbackUrl),
        state);
  }

  /**
   * 生成测试回调 URL
   *
   * @param callbackUrl 回调 URL
   * @param authCode    授权码
   * @param state       State 令牌
   * @return 完整的回调 URL
   */
  public static String generateTestCallbackUrl(String callbackUrl, String authCode, String state) {
    return String.format("%s?code=%s&state=%s", callbackUrl, authCode, state);
  }

  /**
   * URL 编码
   */
  private static String urlEncode(String str) {
    try {
      return java.net.URLEncoder.encode(str, "UTF-8");
    } catch (Exception e) {
      return str;
    }
  }

  /**
   * 打印测试说明
   */
  public static void printTestInstructions() {
    System.out.println("====== Twitter OAuth 测试说明 ======");
    System.out.println("1. 调用 /callback/twitter/authorize 获取授权 URL");
    System.out.println("2. 在浏览器中访问返回的 authorizationUrl");
    System.out.println("3. 使用你的 Twitter 账户授权");
    System.out.println("4. Twitter 会重定向回 /callback/twitter/oauth");
    System.out.println("5. 检查返回的用户信息是否正确");
    System.out.println("====================================");
  }

}
