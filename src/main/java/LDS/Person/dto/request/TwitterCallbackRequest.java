package LDS.Person.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "Twitter OAuth 回调请求 DTO")
@Getter
@Setter
public class TwitterCallbackRequest {

    @ApiModelProperty(value = "授权码", example = "code_from_twitter", required = true)
    private String code;

    @ApiModelProperty(value = "状态令牌（防 CSRF）", example = "state_token_123", required = true)
    private String state;

    @ApiModelProperty(value = "错误信息（如果授权失败）", example = "access_denied")
    private String error;

    @ApiModelProperty(value = "错误描述", example = "User denied access")
    private String error_description;

}
