package LDS.Person.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 引用推文响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "QuoteTweetResponse", description = "引用推文响应")
public class QuoteTweetResponse {

    @ApiModelProperty(value = "HTTP 状态码", example = "200")
    private Integer code;

    @ApiModelProperty(value = "响应消息", example = "引用推文成功")
    private String message;

    @ApiModelProperty(value = "响应数据（引用推文结果）", example = "{\"tweet_id\": \"1234567890\"}")
    private Object data;

    @ApiModelProperty(value = "时间戳（毫秒）", example = "1704067200000")
    private Long timestamp;

    public QuoteTweetResponse(Integer code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static QuoteTweetResponse success(Object data) {
        return new QuoteTweetResponse(200, "引用推文成功", data);
    }

    public static QuoteTweetResponse error(Integer code, String message) {
        return new QuoteTweetResponse(code, message, null);
    }

    public static QuoteTweetResponse badRequest(String message) {
        return new QuoteTweetResponse(400, message, null);
    }

    public static QuoteTweetResponse unauthorized(String message) {
        return new QuoteTweetResponse(401, message, null);
    }

    public static QuoteTweetResponse serverError(String message) {
        return new QuoteTweetResponse(500, message, null);
    }
}
