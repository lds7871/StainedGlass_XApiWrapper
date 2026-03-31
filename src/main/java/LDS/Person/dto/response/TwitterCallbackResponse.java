package LDS.Person.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Twitter OAuth 回调响应 DTO")
@Getter
@Setter
@Builder
public class TwitterCallbackResponse {

    @Schema(description = "状态码", example = "200")
    private Integer code;

    @Schema(description = "消息", example = "认证成功")
    private String message;

    @Schema(description = "用户 ID", example = "123456789")
    private String userId;

    @Schema(description = "用户名", example = "twitter_username")
    private String username;

    @Schema(description = "显示名称", example = "User Name")
    private String displayName;

    @Schema(description = "访问令牌", example = "access_token_xxx")
    private String accessToken;

}
