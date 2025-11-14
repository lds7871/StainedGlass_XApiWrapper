package LDS.Person.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "Twitter OAuth 回调响应 DTO")
@Getter
@Setter
@Builder
public class TwitterCallbackResponse {

    @ApiModelProperty(value = "状态码", example = "200")
    private Integer code;

    @ApiModelProperty(value = "消息", example = "认证成功")
    private String message;

    @ApiModelProperty(value = "用户 ID", example = "123456789")
    private String userId;

    @ApiModelProperty(value = "用户名", example = "twitter_username")
    private String username;

    @ApiModelProperty(value = "显示名称", example = "User Name")
    private String displayName;

    @ApiModelProperty(value = "访问令牌", example = "access_token_xxx")
    private String accessToken;

}
