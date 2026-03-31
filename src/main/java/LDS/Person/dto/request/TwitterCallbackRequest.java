package LDS.Person.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Twitter OAuth 回调请求 DTO")
@Getter
@Setter
public class TwitterCallbackRequest {

    @Schema(description = "授权码", requiredMode = Schema.RequiredMode.REQUIRED, example = "code_from_twitter")
    private String code;

    @Schema(description = "状态令牌（防 CSRF）", requiredMode = Schema.RequiredMode.REQUIRED, example = "state_token_123")
    private String state;

    @Schema(description = "错误信息（如果授权失败）", example = "access_denied")
    private String error;

    @Schema(description = "错误描述", example = "User denied access")
    private String error_description;

}
