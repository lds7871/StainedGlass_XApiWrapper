package LDS.Person.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Twitter OAuth 配置属性
 * 从 application.yml 中读取 twitter.oauth 配置
 */
@Component
@ConfigurationProperties(prefix = "twitter.oauth")
@Getter
@Setter
public class TwitterProperties {

  /**
   * Twitter API 客户端 ID
   */
  private String clientId;

  /**
   * Twitter API 客户端密钥
   */
  private String clientSecret;

  /**
   * OAuth 回调 URL（必须与 Twitter App 设置中的相同）
   * 示例: http://115.190.170.56/callback/twitter/oauth
   */
  private String callbackUrl;

  /**
   * Twitter Authorization API 端点
   */
  private String authorizationUrl = "https://twitter.com/i/oauth2/authorize";

  /**
   * Twitter Token API 端点
   */
  private String tokenUrl = "https://api.twitter.com/2/oauth2/token";

  /**
   * Twitter User Info API 端点
   */
  private String userInfoUrl = "https://api.twitter.com/2/users/me";

  /**
   * Twitter Create Tweet API 端点
   */
  private String createTweetUrl = "https://api.twitter.com/2/tweets";

  /**
   * OAuth 作用域（权限范围）
   */
  private String scopes = "tweet.read users.read follows.read follows.write tweet.write";

}
