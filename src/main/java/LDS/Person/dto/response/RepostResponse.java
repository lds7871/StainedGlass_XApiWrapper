package LDS.Person.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转发推文响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "RepostResponse", description = "转发推文响应")
public class RepostResponse {

    @ApiModelProperty(value = "HTTP 状态码", example = "200")
    private Integer code;

    @ApiModelProperty(value = "响应消息", example = "转发成功")
    private String message;

    @ApiModelProperty(value = "响应数据（转发结果）", example = "{\"retweeted\": true}")
    private Object data;

    @ApiModelProperty(value = "时间戳（毫秒）", example = "1704067200000")
    private Long timestamp;

    public RepostResponse(Integer code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static RepostResponse success(Object data) {
        return new RepostResponse(200, "转发成功", data);
    }

    public static RepostResponse error(Integer code, String message) {
        return new RepostResponse(code, message, null);
    }

    public static RepostResponse badRequest(String message) {
        return new RepostResponse(400, message, null);
    }

    public static RepostResponse unauthorized(String message) {
        return new RepostResponse(401, message, null);
    }

    public static RepostResponse serverError(String message) {
        return new RepostResponse(500, message, null);
    }
}
