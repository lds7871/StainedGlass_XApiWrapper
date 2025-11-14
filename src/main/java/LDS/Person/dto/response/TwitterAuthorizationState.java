package LDS.Person.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel("TwitterAuthorizationState")
public class TwitterAuthorizationState {

  @ApiModelProperty(value = "state 值", required = true)
  private String state;

  @ApiModelProperty(value = "PKCE code challenge", required = true)
  private String codeChallenge;

  @ApiModelProperty(value = "PKCE 方法", example = "S256", required = true)
  private String codeChallengeMethod;
}
