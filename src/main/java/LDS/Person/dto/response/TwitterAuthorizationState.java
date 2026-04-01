package LDS.Person.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 描述 Twitter OAuth 授权前置信息（state 与 PKCE code challenge）。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TwitterAuthorizationState")
public class TwitterAuthorizationState {

  @Schema(description = "state 值", requiredMode = Schema.RequiredMode.REQUIRED)
  private String state;

  @Schema(description = "PKCE code challenge", requiredMode = Schema.RequiredMode.REQUIRED)
  private String codeChallenge;

  @Schema(description = "PKCE 方法", requiredMode = Schema.RequiredMode.REQUIRED, example = "S256")
  private String codeChallengeMethod;
}
